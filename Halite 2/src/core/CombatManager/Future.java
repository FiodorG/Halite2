package core.CombatManager;

import core.GameState;
import hlt.Entity;
import hlt.Move;
import hlt.Ship;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import static core.CombatManager.EventType.RETREAT;

public class Future
{
    final private int maxDepth;

    final private ArrayList<Ship> myActiveShips;
    final private ArrayList<Ship> myDockedShips;
    final private ArrayList<Ship> enemyActiveShips;
    final private ArrayList<Ship> enemyDockedShips;
    private Node<Event> futurePossibilities;

    public Future(final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips)
    {
        this.maxDepth = 2;
        this.futurePossibilities = new Node<>(null, null, new LinkedList<>(), new ArrayList<>(), true, 0, 0);

        this.myActiveShips = new ArrayList<>();
        this.myDockedShips = new ArrayList<>();
        for (final Ship ship: myShips)
            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                myDockedShips.add(ship);
            else
                myActiveShips.add(ship);

        this.enemyActiveShips = new ArrayList<>();
        this.enemyDockedShips = new ArrayList<>();
        for (final Ship ship: enemyShips)
            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                enemyDockedShips.add(ship);
            else
                enemyActiveShips.add(ship);
    }

    public ArrayList<Move> generateAllFutureMoves(final GameState gameState)
    {
        return generateMovesFromFuture(solveFuture(this.futurePossibilities));
    }

    private ArrayList<Move> generateMovesFromFuture(final Node<Event> future)
    {
        return new ArrayList<>();
    }

    private Node solveFuture(final Node future)
    {
        //return AlphaBetaPruning(future, -Double.MAX_VALUE, Double.MAX_VALUE);
        return miniMax(future);
    }

    private Node miniMax(final Node parentNode)
    {
        ArrayList<Node<Event>> childrenNodes = getChildren(parentNode);

        if (childrenNodes.isEmpty())
        {
            parentNode.setScore(computeScore(parentNode));
            return parentNode;
        }

        Node childNode = null;
        if (parentNode.isMaxPlayer())
        {
            double bestScore = -Double.MAX_VALUE;
            for (final Node node: childrenNodes)
            {
                childNode = miniMax(node);
                double score = childNode.getScore();
                if (score > bestScore)
                {
                    bestScore = score;
                    node.setScore(bestScore);
                }
            }

            childNode.setScore(bestScore);
            return childNode;
        }
        else
        {
            double bestScore = Double.MAX_VALUE;
            for (final Node node: childrenNodes)
            {
                childNode = miniMax(node);
                double score = childNode.getScore();
                if (score < bestScore)
                {
                    bestScore = score;
                    node.setScore(bestScore);
                }
            }

            childNode.setScore(bestScore);
            return childNode;
        }
    }

    private Node alphaBetaPruning(final Node parentNode, double alpha, double beta)
    {
        ArrayList<Node<Event>> childrenNodes = getChildren(parentNode);

        if (childrenNodes.isEmpty())
        {
            parentNode.setScore(computeScore(parentNode));
            return parentNode;
        }

        Node childNode = null;
        if (parentNode.isMaxPlayer())
        {
            for (final Node node: childrenNodes)
            {
                childNode = alphaBetaPruning(node, alpha, beta);
                double score = childNode.getScore();
                if (score > alpha)
                {
                    alpha = score;
                    node.setScore(alpha);
                }
                if (beta < alpha)
                    break;
            }

            childNode.setScore(alpha);
            return childNode;
        }
        else
        {
            for (final Node node: childrenNodes)
            {
                childNode = alphaBetaPruning(node, alpha, beta);
                double score = childNode.getScore();
                if (score < beta)
                {
                    beta = score;
                    node.setScore(beta);
                }
                if (beta < alpha)
                    break;
            }

            childNode.setScore(beta);
            return childNode;
        }
    }

    private ArrayList<Node<Event>> getChildren(final Node<Event> parentNode)
    {
        if (parentNode.getDepth() == this.maxDepth)
            return new ArrayList<>();

        if (parentNode.isMaxPlayer())
        {
            // Generate moves for all of my ships
            getChildrenForTargetShip(parentNode, this.enemyActiveShips, this.myActiveShips);
            getChildrenForTargetShip(parentNode, this.enemyDockedShips, this.myActiveShips);
            getChildrenForTargetShip(parentNode, this.myDockedShips, this.myActiveShips);
            getChildrenForGroup(parentNode, this.myActiveShips);
            getChildrenForRetreat(parentNode, this.myActiveShips);
        }
        else
        {
            // Generate moves for enemy ships
            getChildrenForTargetShip(parentNode, this.myActiveShips, this.enemyActiveShips);
            getChildrenForTargetShip(parentNode, this.myDockedShips, this.enemyActiveShips);
            getChildrenForTargetShip(parentNode, this.enemyDockedShips, this.enemyActiveShips);
        }

        return parentNode.getChildren();
    }

    private void getChildrenForTargetShip(final Node<Event> parentNode, final ArrayList<Ship> targetShips, final ArrayList<Ship> sourceShips)
    {
        for(final Ship targetShip: targetShips)
        {
            EventType eventType = generateEventType(parentNode, sourceShips, targetShip);
            Event event = new Event(eventType, sourceShips, targetShip);
            parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForGroup(final Node<Event> parentNode, final ArrayList<Ship> sourceShips)
    {
        if (sourceShips.size() > 1)
        {
            Event event = new Event(EventType.GROUP, sourceShips, sourceShips.get(0));
            parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
        }
    }

    private void getChildrenForRetreat(final Node<Event> parentNode, final ArrayList<Ship> sourceShips)
    {
        Event event = new Event(RETREAT, sourceShips, sourceShips.get(0));
        parentNode.addNodeToChildren(event, !parentNode.isMaxPlayer(), parentNode.getDepth() + 1, 0);
    }

    private EventType generateEventType(final Node<Event> parentNode, final ArrayList<Ship> sourceShips, final Ship targetShip)
    {
        //boolean isInRange = (this.selectedShip.getDistanceTo(targetShip) <= 7);
        boolean isFriendly = (targetShip.getOwner() == sourceShips.get(0).getOwner());
        boolean isDocking = (targetShip.getDockingStatus() != Ship.DockingStatus.Undocked);

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

    private double computeScore(final Node<Event> parentNode)
    {
        LinkedList<Node<Event>> parents = parentNode.getParents();

        int numberOfMyAttacks = 0;
        int numberOfMyAttackDocked = 0;
        int numberOfMyGroups = 0;
        int numberOfMyDefends = 0;

        int numberOfEnemyAttacks = 0;
        int numberOfEnemyAttackDocked = 0;
        int numberOfEnemyGroups = 0;
        int numberOfEnemyDefends = 0;

        Iterator<Node<Event>> nodeIterator = parents.descendingIterator();
        while(nodeIterator.hasNext())
        {
            Node<Event> node = nodeIterator.next();
            EventType eventType = node.getData().getEventType();
            switch (eventType)
            {
                case ATTACK:
                    if(node.isMaxPlayer()) numberOfMyAttacks++; else numberOfEnemyAttacks++;
                    break;
                case ATTACKDOCKED:
                    if(node.isMaxPlayer()) numberOfMyAttackDocked++; else numberOfEnemyAttackDocked++;
                    break;
                case GROUP:
                    if(node.isMaxPlayer()) numberOfMyGroups++; else numberOfEnemyGroups++;
                    break;
                case DEFEND:
                    if(node.isMaxPlayer()) numberOfMyDefends++; else numberOfEnemyDefends++;
                    break;
                case RETREAT:
                    break;
            }

            if (eventType == RETREAT)
                break;
        }

        return
                (numberOfMyGroups - numberOfEnemyGroups) * 4 +
                (numberOfMyAttacks - numberOfEnemyAttacks) +
                (numberOfMyAttackDocked - numberOfEnemyAttackDocked) * 2 +
                (numberOfMyDefends - numberOfEnemyDefends) * 2;
    }

//    private ArrayList<Node<Event>> getChildren(final Node<Event> parentNode)
//    {
//        final LinkedList<Node<Event>> parents = parentNode.getParents();
//
//        ArrayList<Ship> myShipsToMove = new ArrayList<>();
//        ArrayList<Ship> enemyShipsToMove = new ArrayList<>();
//
//        for(final Node<Event> node: parents)
//        {
//            Ship ship = node.getData().getSourceEntity();
//            if (!this.myShips.contains(ship))
//                myShipsToMove.add(ship);
//            if (!this.enemyShips.contains(ship))
//                enemyShipsToMove.add(ship);
//        }
//
//        if (!myShipsToMove.isEmpty())
//        {
//            // Generate moves for all of my ships
//            Ship sourceShip = myShipsToMove.remove(0);
//            boolean isMaxPlayer = true;
//
//            for(final Ship targetShip: this.myShips)
//            {
//                EventType eventType = generateEventType(parentNode, sourceShip, targetShip);
//
//                Event event = new Event(eventType, sourceShip, targetShip);
//                parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
//            }
//
//            for(final Ship targetShip: this.enemyShips)
//            {
//                EventType eventType = generateEventType(parentNode, sourceShip, targetShip);
//
//                Event event = new Event(eventType, sourceShip, targetShip);
//                parentNode.addNodeToChildren(event, isMaxPlayer, parentNode.getDepth() + 1, 0);
//            }
//        }
//        else
//        {
//            // Generate moves for enemy ships
//            Ship sourceShip = enemyShipsToMove.remove(0);
//        }
//
//        return parentNode.getChildren();
//    }
}
