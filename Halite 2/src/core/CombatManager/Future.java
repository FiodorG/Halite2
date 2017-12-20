package core.CombatManager;

import core.Fleet;
import core.GameState;
import core.NavigationManager.CombatOperationMoves;
import core.NavigationManager.NavigationManager;
import hlt.*;

import java.util.*;

import static core.CombatManager.EventType.RETREAT;
import static core.GameState.applyMoveToShip;
import static core.NavigationManager.NavigationManager.retreatDirection;

public class Future
{
    final private int maxDepth;

    private ArrayList<Fleet> myFleets;
    private ArrayList<Ship> myShips;

    private ArrayList<Ship> alliedShipsActive;
    private ArrayList<Ship> alliedShipsDocked;
    private ArrayList<Ship> enemyShipsActive;
    private ArrayList<Ship> enemyShipsDocked;

    private Entity targetEntity;

    // Helpers
    private ArrayList<Entity> myEntities;
    private ArrayList<Ship> allMyShipsActive;
    private Fleet enemyShipsFleet;

    private Node<Event> futurePossibilities;

    public Future(final ArrayList<Fleet> myFleets, final ArrayList<Ship> myShips, final ArrayList<Ship> allMyShips, final ArrayList<Ship> alliedShips, final ArrayList<Ship> enemyShips, final Entity target)
    {
        this.futurePossibilities = new Node<>(null, null, new LinkedList<>(), new ArrayList<>(), true, 0, 0);

        this.myFleets = myFleets;
        this.myShips = myShips;

        this.alliedShipsActive = new ArrayList<>();
        this.alliedShipsDocked = new ArrayList<>();
        for (final Ship ship: alliedShips)
            if (ship.isUndocked())
                alliedShipsActive.add(ship);
            else
                alliedShipsDocked.add(ship);

        this.enemyShipsActive = new ArrayList<>();
        this.enemyShipsDocked = new ArrayList<>();
        for (final Ship ship: enemyShips)
            if (ship.isUndocked())
                enemyShipsActive.add(ship);
            else
                enemyShipsDocked.add(ship);

        this.targetEntity = target;

        this.allMyShipsActive = allMyShips;
        this.allMyShipsActive.addAll(alliedShipsActive);

        this.myEntities = new ArrayList<>();
        this.myEntities.addAll(myFleets);
        this.myEntities.addAll(myShips);

        // One move per my entities plus enemy's turn.
//        this.maxDepth = this.myEntities.size() + enemyShipsActive.size();
        this.maxDepth = this.myEntities.size() + 1;

        this.enemyShipsFleet = new Fleet(this.enemyShipsActive, new ArrayList<>(), -965);
    }

    public CombatOperationMoves generateFutureMoves(final GameState gameState)
    {
        final Node<Event> chosenFuture = alphaBetaPruning(gameState, this.futurePossibilities, -Double.MAX_VALUE, Double.MAX_VALUE);
        //final Node<Event> chosenFuture = miniMax(gameState, this.futurePossibilities);

        return generateMovesForFleet(gameState, chosenFuture.getParents());
    }

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
        Node bestNode = childrenNodes.get(0);
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
        Entity sourceEntity;
        if (parentNode.getDepth() < this.myEntities.size())
        {
            sourceEntity = this.myEntities.get(parentNode.getDepth());
            boolean isMaxPlayer = (parentNode.getDepth() != this.myEntities.size() - 1);

            getChildrenForTargetShip(parentNode, this.enemyShipsActive, sourceEntity, isMaxPlayer);
            getChildrenForTargetShip(parentNode, this.enemyShipsDocked, sourceEntity, isMaxPlayer);
            getChildrenForTargetShip(parentNode, this.alliedShipsDocked, sourceEntity, isMaxPlayer);
            getChildrenForGroup2(parentNode, this.myEntities, sourceEntity, isMaxPlayer);
            getChildrenForRetreatShip(parentNode, sourceEntity, isMaxPlayer);

            return parentNode.getChildren();
        }
        else if (parentNode.getDepth() < this.maxDepth)
        {
            sourceEntity = this.enemyShipsFleet;
//            sourceEntity = this.enemyShipsActive.get(parentNode.getDepth() - this.myEntities.size());

            getChildrenForTargetShip(parentNode, this.allMyShipsActive, sourceEntity, false);
            getChildrenForTargetShip(parentNode, this.alliedShipsDocked, sourceEntity, false);
            getChildrenForTargetShip(parentNode, this.enemyShipsDocked, sourceEntity, false);
            getChildrenForGroup(parentNode, this.enemyShipsActive, sourceEntity, false);
            getChildrenForRetreatShip(parentNode, sourceEntity, false);

            return parentNode.getChildren();
        }
        else
            return new ArrayList<>();
    }

    private void getChildrenForTargetShip(final Node<Event> parentNode, final ArrayList<Ship> targetShips, final Entity sourceEntity, final Boolean isMaxPlayer)
    {
        for(final Ship targetShip: targetShips)
        {
            EventType eventType = generateEventTypeForShip(sourceEntity, targetShip);
            Event event = new Event(eventType, sourceEntity, targetShip);
            parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForGroup(final Node<Event> parentNode, final ArrayList<Ship> myActiveShips, final Entity sourceEntity, final Boolean isMaxPlayer)
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

            Event event = new Event(EventType.GROUP, sourceEntity, targetShip);
            parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForGroup2(final Node<Event> parentNode, final ArrayList<Entity> myActiveShips, final Entity sourceEntity, final Boolean isMaxPlayer)
    {
        for(final Entity targetEntity: myActiveShips)
        {
            if (sourceEntity instanceof Ship)
            {
                if (targetEntity.equals((Ship)sourceEntity))
                    continue;
            }
            else if (sourceEntity instanceof Fleet)
            {
                if (((Fleet)sourceEntity).getShips().contains(targetEntity))
                    continue;

                if (((Fleet)sourceEntity).equals(targetEntity))
                    continue;
            }

            Event event = new Event(EventType.GROUP, sourceEntity, targetEntity);
            parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForRetreatShip(final Node<Event> parentNode, final Entity sourceEntity, final Boolean isMaxPlayer)
    {
        Event event = new Event(EventType.RETREAT, sourceEntity, sourceEntity);
        parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
    }

//    private void getChildrenForTargetShips(final Node<Event> parentNode, final ArrayList<Ship> targetShips, final ArrayList<Ship> sourceShips, final Boolean isMaxPlayer)
//    {
//        if (sourceShips.isEmpty())
//            return;
//
//        for(final Ship targetShip: targetShips)
//        {
//            EventType eventType = generateEventTypeForShips(sourceShips, targetShip);
//            Event event = new Event(eventType, sourceShips, targetShip);
//            parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
//        }
//    }

//    private void getChildrenForRetreatShips(final Node<Event> parentNode, final ArrayList<Ship> sourceShips, final Boolean isMaxPlayer)
//    {
//        for(final Ship ship: sourceShips)
//        {
//            Event event = new Event(RETREAT, new ArrayList<>(Arrays.asList(ship)), ship);
//            parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
//        }
//    }
//
//    private EventType generateEventTypeForShips(final ArrayList<Ship> sourceShips, final Ship targetShip)
//    {
//        boolean isFriendly = (targetShip.getOwner() == sourceShips.get(0).getOwner());
//        boolean isDocking = (targetShip.getDockingStatus() != Ship.DockingStatus.Undocked);
//        return generateEventTypeInternal(isFriendly, isDocking);
//    }

    private EventType generateEventTypeForShip(final Entity sourceEntity, final Ship targetShip)
    {
        boolean isFriendly = (targetShip.getOwner() == sourceEntity.getOwner());
        boolean isDocking = !targetShip.isUndocked();
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
            Entity sourceEntity = event.getSourceEntity();
            Entity targetEntity = event.getTargetEntity();

            ArrayList<Move> newMoves = generateMoves(gameState, event.getEventType(), sourceEntity, targetEntity);

            if (node.getParent().isMaxPlayer())
            {
                if (sourceEntity instanceof Ship)
                {
                    int indexOfCurrentShip = myShipsNextTurn.indexOf((Ship) sourceEntity);
                    myShipsNextTurn.set(indexOfCurrentShip, applyMoveToShip((Ship) sourceEntity, newMoves.get(0)));
                }
                else if (sourceEntity instanceof Fleet)
                {
                    Fleet fleet = (Fleet)sourceEntity;
                    for (int i = 0; i < fleet.getShips().size(); i++)
                    {
                        Ship ship = fleet.getShips().get(i);
                        int indexOfCurrentShip = myShipsNextTurn.indexOf(ship);
                        myShipsNextTurn.set(indexOfCurrentShip, applyMoveToShip(ship, newMoves.get(i)));
                    }
                }
                else
                    throw new IllegalStateException("Future Compute Score.");
            }
            else
            {
                if (sourceEntity instanceof Ship)
                {
                    int indexOfCurrentShip = enemyShipsNextTurn.indexOf((Ship) sourceEntity);
                    enemyShipsNextTurn.set(indexOfCurrentShip, applyMoveToShip((Ship) sourceEntity, newMoves.get(0)));
                }
                else if (sourceEntity instanceof Fleet)
                {
                    Fleet fleet = (Fleet)sourceEntity;
                    for (int i = 0; i < fleet.getShips().size(); i++)
                    {
                        Ship ship = fleet.getShips().get(i);
                        int indexOfCurrentShip = enemyShipsNextTurn.indexOf(ship);
                        enemyShipsNextTurn.set(indexOfCurrentShip, applyMoveToShip(ship, newMoves.get(i)));
                    }
                }
                else
                    throw new IllegalStateException("Future computeScore.");
            }
        }

        return CombatManager.combatBalance(myShipsNextTurn, enemyShipsNextTurn);
    }

    private ArrayList<Move> generateMoves(final GameState gameState, final EventType eventType, final Entity sourceEntity, final Entity targetEntity)
    {
        if (sourceEntity instanceof Ship)
            return new ArrayList<>(Arrays.asList(generateMovesForShip(gameState, eventType, (Ship)sourceEntity, targetEntity)));
        else if (sourceEntity instanceof Fleet)
            return generateMovesForFleet(gameState, eventType, (Fleet) sourceEntity, targetEntity);
        else
            throw new IllegalStateException("Future generateMoves.");
    }

    private Move generateMovesForShip(final GameState gameState, final EventType eventType, final Ship sourceShip, final Entity targetEntity)
    {
        switch (eventType)
        {
            case ATTACK: case ATTACKDOCKED:
                return Navigation.navigateShipToAttack(gameState, sourceShip, targetEntity);
            case GROUP: case DEFEND:
                return Navigation.navigateShipToMove(gameState, sourceShip, targetEntity);
            case RETREAT:
                Entity retreatDirection = retreatDirection(gameState, sourceShip);
                int retreatThrust = 7; //NavigationManager.retreatThrust(gameState, sourceShip, retreatDirection);
                return Navigation.navigateShipToMoveWithThrust(gameState, sourceShip, retreatDirection, retreatThrust, 5.0);
            default:
                return Navigation.navigateShipToMove(gameState, sourceShip, sourceShip);
        }
    }

    private ArrayList<Move> generateMovesForFleet(final GameState gameState, final EventType eventType, final Fleet sourceFleet, final Entity targetEntity)
    {
        switch (eventType)
        {
            case ATTACK: case ATTACKDOCKED:
                return Navigation.navigateFleetToAttack(gameState, sourceFleet, targetEntity);
            case GROUP: case DEFEND:
                return Navigation.navigateFleetToDefend(gameState, sourceFleet, targetEntity);
            case RETREAT:
                Entity retreatDirection = retreatDirection(gameState, sourceFleet.getCentroid());
                int retreatThrust = NavigationManager.retreatThrust(gameState, sourceFleet.getCentroid(), retreatDirection);
                return Navigation.navigateFleetToAttackWithThrust(gameState, sourceFleet, retreatDirection, retreatThrust, 5.0);
            default:
                return Navigation.navigateFleetToMove(gameState, sourceFleet, sourceFleet);
        }
    }

    private CombatOperationMoves generateMovesForFleet(final GameState gameState, final LinkedList<Node<Event>> events)
    {
        Iterator<Node<Event>> nodeIterator = events.descendingIterator();

        HashMap<Fleet, ArrayList<Move>> fleetMoves = new HashMap<>();
        HashMap<Ship, Move> shipMoves = new HashMap<>();

        while(nodeIterator.hasNext())
        {
            Node<Event> node = nodeIterator.next();
            Event event = node.getData();
            Entity sourceEntity = event.getSourceEntity();
            Entity targetEntity = event.getTargetEntity();

            ArrayList<Move> newMoves = generateMoves(gameState, event.getEventType(), sourceEntity, targetEntity);

            if (sourceEntity.getOwner() == gameState.getMyId())
            {
                if (sourceEntity instanceof Ship)
                    shipMoves.put((Ship)sourceEntity, newMoves.get(0));
                else if (sourceEntity instanceof Fleet)
                    fleetMoves.put((Fleet)sourceEntity, newMoves);
                else
                    throw new IllegalStateException("Future generateMovesForFleet.");
            }
        }

        return new CombatOperationMoves(fleetMoves, shipMoves);
    }

    private ArrayList<Ship> copyMyShips()
    {
        ArrayList<Ship> myShipsNextTurn = new ArrayList<>();

        for (final Fleet fleet: this.myFleets)
            for (final Ship ship: fleet.getShips())
                myShipsNextTurn.add(new Ship(ship));

        for (final Ship ship: this.myShips)
            myShipsNextTurn.add(new Ship(ship));

        for (final Ship ship: this.alliedShipsActive)
            myShipsNextTurn.add(new Ship(ship));

        for (final Ship ship: this.alliedShipsDocked)
            myShipsNextTurn.add(new Ship(ship));

        return myShipsNextTurn;
    }

    private ArrayList<Ship> copyEnemyShips()
    {
        ArrayList<Ship> enemyShipsNextTurn = new ArrayList<>();

        for (final Ship ship: this.enemyShipsActive)
            enemyShipsNextTurn.add(new Ship(ship));

        for (final Ship ship: this.enemyShipsDocked)
            enemyShipsNextTurn.add(new Ship(ship));

        return enemyShipsNextTurn;
    }
}
