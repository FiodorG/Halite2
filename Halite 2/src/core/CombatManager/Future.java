package core.CombatManager;

import core.Fleet;
import core.GameState;
import core.NavigationManager.NavigationManager;
import hlt.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import static core.CombatManager.EventType.RETREAT;
import static core.GameState.applyMoveToShip;

public class Future
{
    final private int maxDepth;

    final private Entity sourceEntity;
    final private ArrayList<Ship> myActiveShips;
    final private ArrayList<Ship> myDockedShips;
    final private ArrayList<Ship> enemyActiveShips;
    final private ArrayList<Ship> enemyDockedShips;

    private Node<Event> futurePossibilities;

    public Future(final Entity sourceEntity, final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips)
    {
        this.maxDepth = 2;
        this.futurePossibilities = new Node<>(null, null, new LinkedList<>(), new ArrayList<>(), true, 0, 0);

        this.sourceEntity = sourceEntity;
        this.myActiveShips = new ArrayList<>();
        this.myDockedShips = new ArrayList<>();
        for (final Ship ship: myShips)
            if (ship.isUndocked())
                myActiveShips.add(ship);
            else
                myDockedShips.add(ship);

        this.enemyActiveShips = new ArrayList<>();
        this.enemyDockedShips = new ArrayList<>();
        for (final Ship ship: enemyShips)
            if (ship.isUndocked())
                enemyActiveShips.add(ship);
            else
                enemyDockedShips.add(ship);
    }

//    public ArrayList<Move> generateFutureMoves(final GameState gameState, final CombatOperation combatOperation)
//    {
//        final Node<Event> chosenFuture = alphaBetaPruning(gameState, this.futurePossibilities, -Double.MAX_VALUE, Double.MAX_VALUE);
//        //final Node<Event> chosenFuture = miniMax(gameState, this.futurePossibilities);
//
//        Node chosenNode = chosenFuture.getParents().getLast();
//        Event event = (Event)chosenNode.getData();
//        ArrayList<Ship> sourceShips = event.getSourceEntity();
//
//        if (sourceShips.size() == 1)
//        {
//            Move move = generateMoves(gameState, event.getEventType(), sourceShips.get(0), event.getTargetEntity());
//            ArrayList<Move> moves = new ArrayList<>();
//            moves.add(move);
//            return moves;
//        }
//        else
//            return generateMovesForFleet(gameState, event.getEventType(), (Fleet)combatOperation.getSourceShip(), event.getTargetEntity());
//    }

    private Node miniMax(final GameState gameState, final Node parentNode)
    {
        ArrayList<Node<Event>> childrenNodes = getChildren(gameState, parentNode);

        if (childrenNodes.isEmpty())
        {
            parentNode.setScore(computeScore(gameState, parentNode));
            return parentNode;
        }

        Node childNode;
        Node bestNode = null;
        if (parentNode.isMaxPlayer())
        {
            double bestScore = -Double.MAX_VALUE;
            for (final Node node: childrenNodes)
            {
                childNode = miniMax(gameState, node);
                double score = childNode.getScore();
                if (score > bestScore)
                {
                    bestScore = score;
                    bestNode = childNode;
                    node.setScore(bestScore);
                }
            }

            parentNode.setScore(bestScore);
            return bestNode;
        }
        else
        {
            double bestScore = Double.MAX_VALUE;
            for (final Node node: childrenNodes)
            {
                childNode = miniMax(gameState, node);
                double score = childNode.getScore();
                if (score < bestScore)
                {
                    bestScore = score;
                    bestNode = childNode;
                    node.setScore(bestScore);
                }
            }

            parentNode.setScore(bestScore);
            return bestNode;
        }
    }

    private Node alphaBetaPruning(final GameState gameState, final Node parentNode, double alpha, double beta)
    {
        ArrayList<Node<Event>> childrenNodes = getChildren(gameState, parentNode);

        if (childrenNodes.isEmpty())
        {
            parentNode.setScore(computeScore(gameState, parentNode));
            return parentNode;
        }

        Node childNode;
        Node bestNode = null;
        if (parentNode.isMaxPlayer())
        {
            for (final Node node: childrenNodes)
            {
                childNode = alphaBetaPruning(gameState, node, alpha, beta);
                double score = childNode.getScore();
                if (score > alpha)
                {
                    alpha = score;
                    node.setScore(alpha);
                    bestNode = childNode;
                }
                if (beta < alpha)
                    break;
            }

            parentNode.setScore(alpha);
            return bestNode;
        }
        else
        {
            for (final Node node: childrenNodes)
            {
                childNode = alphaBetaPruning(gameState, node, alpha, beta);
                double score = childNode.getScore();
                if (score < beta)
                {
                    beta = score;
                    node.setScore(beta);
                    bestNode = childNode;
                }
                if (beta < alpha)
                    break;
            }

            parentNode.setScore(beta);
            return bestNode;
        }
    }

    private ArrayList<Node<Event>> getChildren(final GameState gameState, final Node<Event> parentNode)
    {
        if (parentNode.getDepth() == this.maxDepth)
            return new ArrayList<>();

        if (parentNode.isMaxPlayer())
        {
            // Generate moves for all of my ships
            getChildrenForTargetShip(parentNode, this.enemyActiveShips, this.sourceEntity);
            getChildrenForTargetShip(parentNode, this.enemyDockedShips, this.sourceEntity);
            getChildrenForTargetShip(parentNode, this.myDockedShips, this.sourceEntity);
            getChildrenForGroup(parentNode, this.myActiveShips, this.sourceEntity);
            getChildrenForRetreatShip(parentNode, this.sourceEntity);
        }
        else
        {
            // Generate moves for enemy ships
            getChildrenForTargetShips(parentNode, this.myActiveShips, this.enemyActiveShips);
            getChildrenForTargetShips(parentNode, this.myDockedShips, this.enemyActiveShips);
            getChildrenForTargetShips(parentNode, this.enemyDockedShips, this.enemyActiveShips);
            getChildrenForRetreatShips(parentNode, this.enemyActiveShips);
        }

        return parentNode.getChildren();
    }

    private void getChildrenForTargetShip(final Node<Event> parentNode, final ArrayList<Ship> targetShips, final Entity sourceEntity)
    {
        for(final Ship targetShip: targetShips)
        {
            EventType eventType = generateEventTypeForShip(sourceEntity, targetShip);

            Event event;
            if (sourceEntity instanceof Ship)
                event = new Event(eventType, new ArrayList<>(Arrays.asList((Ship)sourceEntity)), targetShip);
            else if (sourceEntity instanceof Fleet)
                event = new Event(eventType, ((Fleet)sourceEntity).getShips(), targetShip);
            else
                throw new IllegalStateException("SourceEntity can only be either ship or fleet.");

            parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForTargetShips(final Node<Event> parentNode, final ArrayList<Ship> targetShips, final ArrayList<Ship> sourceShips)
    {
        if (sourceShips.isEmpty())
            return;

        for(final Ship targetShip: targetShips)
        {
            EventType eventType = generateEventTypeForShips(sourceShips, targetShip);
            Event event = new Event(eventType, sourceShips, targetShip);
            parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForGroup(final Node<Event> parentNode, final ArrayList<Ship> myActiveShips, final Entity sourceEntity)
    {
        for(final Ship targetShip: myActiveShips)
        {
            if (sourceEntity instanceof Ship)
            {
                if (targetShip.equals((Ship)sourceEntity))
                    continue;
            }
            else if (sourceEntity instanceof Fleet)
            {
                if (((Fleet)sourceEntity).getShips().contains(targetShip))
                    continue;
            }

            Event event;
            if (sourceEntity instanceof Ship)
                event = new Event(EventType.GROUP, new ArrayList<>(Arrays.asList((Ship)sourceEntity)), targetShip);
            else if (sourceEntity instanceof Fleet)
                event = new Event(EventType.GROUP, ((Fleet)sourceEntity).getShips(), targetShip);
            else
                throw new IllegalStateException("SourceEntity can only be either ship or fleet.");

            parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForRetreatShip(final Node<Event> parentNode, final Entity sourceEntity)
    {
        Event event;
        if (sourceEntity instanceof Ship)
            event = new Event(EventType.RETREAT, new ArrayList<>(Arrays.asList((Ship)sourceEntity)), sourceEntity);
        else if (sourceEntity instanceof Fleet)
            event = new Event(EventType.RETREAT, ((Fleet)sourceEntity).getShips(), sourceEntity);
        else
            throw new IllegalStateException("SourceEntity can only be either ship or fleet.");

        parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
    }

    private void getChildrenForRetreatShips(final Node<Event> parentNode, final ArrayList<Ship> sourceShips)
    {
        for(final Ship ship: sourceShips)
        {
            Event event = new Event(RETREAT, new ArrayList<>(Arrays.asList(ship)), ship);
            parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
        }
    }

    private EventType generateEventTypeForShips(final ArrayList<Ship> sourceShips, final Ship targetShip)
    {
        boolean isFriendly = (targetShip.getOwner() == sourceShips.get(0).getOwner());
        boolean isDocking = (targetShip.getDockingStatus() != Ship.DockingStatus.Undocked);
        return generateEventTypeInternal(isFriendly, isDocking);
    }

    private EventType generateEventTypeForShip(final Entity sourceEntity, final Ship targetShip)
    {
        boolean isFriendly = (targetShip.getOwner() == sourceEntity.getOwner());
        boolean isDocking = (targetShip.getDockingStatus() != Ship.DockingStatus.Undocked);
        return generateEventTypeInternal(isFriendly, isDocking);
    }

    private EventType generateEventTypeInternal(final boolean isFriendly, final boolean isDocking)
    {
        if (!isFriendly && isDocking)
            return EventType.ATTACKDOCKED;

        else if (!isFriendly && !isDocking)
            return EventType.ATTACK;

        else if (isFriendly && isDocking)
            return EventType.DEFEND;

        else if (isFriendly && !isDocking)
            return EventType.GROUP;

        else
            return EventType.STILL;
    }

    private double computeScore(final GameState gameState, final Node<Event> parentNode)
    {
        ArrayList<Ship> myShipsNextTurn = copyMyShips();
        ArrayList<Ship> enemyShipsNextTurn = copyEnemyShips();

        LinkedList<Node<Event>> parents = parentNode.getParents();
        Iterator<Node<Event>> nodeIterator = parents.descendingIterator();

        while(nodeIterator.hasNext())
        {
            Node<Event> node = nodeIterator.next();
            Event event = node.getData();

            for (final Ship ship: event.getSourceEntity())
            {
                Move newMove = generateMoves(gameState, event.getEventType(), ship, event.getTargetEntity());

                if (!node.isMaxPlayer())
                {
                    int indexOfCurrentShip = myShipsNextTurn.indexOf(ship);
                    myShipsNextTurn.set(indexOfCurrentShip, applyMoveToShip(ship, newMove));
                }
                else
                {
                    int indexOfCurrentShip = enemyShipsNextTurn.indexOf(ship);
                    enemyShipsNextTurn.set(indexOfCurrentShip, applyMoveToShip(ship, newMove));
                }
            }

            if (event.getEventType() == RETREAT)
                break;
        }

        return CombatManager.combatBalance(myShipsNextTurn, enemyShipsNextTurn);
    }

    private Move generateMoves(final GameState gameState, final EventType eventType, final Ship ship, final Entity targetEntity)
    {
        switch (eventType)
        {
            case ATTACK: case ATTACKDOCKED:
                return Navigation.navigateShipToAttack(gameState, ship, targetEntity);
            case GROUP: case DEFEND:
                return Navigation.navigateShipToMove(gameState, ship, targetEntity);
            case RETREAT:
                Entity retreatDirection = NavigationManager.retreatDirection(gameState, ship);
                int retreatThrust = NavigationManager.retreatThrust(gameState, ship, retreatDirection);
                return Navigation.navigateShipToMoveWithThrust(gameState, ship, retreatDirection, retreatThrust, 5.0);
            default:
                return Navigation.navigateShipToMove(gameState, ship, ship);
        }
    }

    private ArrayList<Move> generateMovesForFleet(final GameState gameState, final EventType eventType, final Fleet fleet, final Entity targetEntity)
    {
        switch (eventType)
        {
            case ATTACK: case ATTACKDOCKED:
                return Navigation.navigateFleetToAttack(gameState, fleet, targetEntity);
            case GROUP: case DEFEND:
                return Navigation.navigateFleetToAttack(gameState, fleet, targetEntity);
            case RETREAT:
                return Navigation.navigateFleetToAttack(gameState, fleet, NavigationManager.retreatDirection(gameState, fleet.getCentroid()));
            default:
                return Navigation.navigateFleetToAttack(gameState, fleet, targetEntity);
        }
    }

    private ArrayList<Ship> copyMyShips()
    {
        ArrayList<Ship> myShipsNextTurn = new ArrayList<>();

        for (final Ship ship: this.myActiveShips)
            myShipsNextTurn.add(new Ship(ship));

        for (final Ship ship: this.myDockedShips)
            myShipsNextTurn.add(new Ship(ship));

        return myShipsNextTurn;
    }

    private ArrayList<Ship> copyEnemyShips()
    {
        ArrayList<Ship> enemyShipsNextTurn = new ArrayList<>();

        for (final Ship ship: this.enemyActiveShips)
            enemyShipsNextTurn.add(new Ship(ship));

        for (final Ship ship: this.enemyDockedShips)
            enemyShipsNextTurn.add(new Ship(ship));

        return enemyShipsNextTurn;
    }
}
