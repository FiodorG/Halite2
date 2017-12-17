package core.CombatManager;

import core.*;
import core.NavigationManager.CombatOperationMoves;
import hlt.*;

import java.util.*;

import static core.GameState.applyMoveToShip;
import static core.Objective.OrderType.*;

public class CombatManager
{
    private ArrayList<CombatOperation> combatOperations;
    private int combatOperationId;

    private ArrayList<Objective> filledObjectives;

    public CombatManager()
    {
        this.combatOperations = new ArrayList<>();
        this.combatOperationId = 0;
        this.filledObjectives = new ArrayList<>();
    }

    public void createCombatOperations(final GameState gameState)
    {
        this.combatOperations.clear();

        ArrayList<Fleet> allFleets = gameState.getFleetManager().getFleetsToMove();
        ArrayList<Ship> allShips = gameState.getFleetManager().getShipsToMove();

        Set<Objective> objectives = new HashSet<>();

        for (final Fleet fleet: allFleets)
            objectives.add(fleet.getFirstObjectives());

        for (final Ship ship: allShips)
            objectives.add(ship.getObjective());

        HashMap<Objective, ArrayList<Ship>> objectivesToShips = new HashMap<>();
        for (final Ship ship: allShips)
        {
            Objective objective = ship.getObjective();

            if (!objectivesToShips.containsKey(objective))
                objectivesToShips.put(objective, new ArrayList<>());

            objectivesToShips.get(objective).add(ship);
        }

        HashMap<Objective, ArrayList<Fleet>> objectivesToFleets = new HashMap<>();
        for (final Fleet fleet: allFleets)
        {
            Objective objective = fleet.getFirstObjectives();

            if (!objectivesToFleets.containsKey(objective))
                objectivesToFleets.put(objective, new ArrayList<>());

            objectivesToFleets.get(objective).add(fleet);
        }

        for (final Objective objective: objectives)
        {
            ArrayList<Ship> enemyShips = new ArrayList<>();
            for(final Ship enemyShip: gameState.getEnemyShips())
                if (enemyShip.getDistanceTo(objective.getTargetEntity()) <= 14.0)
                    enemyShips.add(enemyShip);

            CombatOperation combatOperation = new CombatOperation(objective, objectivesToShips.get(objective), objectivesToFleets.get(objective), enemyShips, this.combatOperationId++);
            this.combatOperations.add(combatOperation);
        }

        logCombatOperations();
    }

    public ArrayList<CombatOperation> getSortedCombatOperations()
    {
        ArrayList<CombatOperation> sortedCombatOperations = new ArrayList<>();

        Objective.OrderType[] ordersPriority = new Objective.OrderType[]{ATTACK,
                ATTACKDOCKED,
                ANTIRUSH,
                ASSASSINATION,
                RUSH,
                MOVE,
                DEFEND,
                COLONIZE,
                REINFORCECOLONY,
                CRASHINTO,
                LURE,
                FLEE,
                UNDOCK,
                GROUP
        };

        for (final Objective.OrderType orderType: ordersPriority)
            for (final CombatOperation combatOperation: this.combatOperations)
                if (combatOperation.getObjective().getOrderType() == orderType)
                    sortedCombatOperations.add(combatOperation);

        return sortedCombatOperations;
    }

    private ArrayList<CombatOperation> findCombatOperationWithObjective(final Objective objective)
    {
        ArrayList<CombatOperation> similarCombatOperations = new ArrayList<>();

        for (final CombatOperation combatOperation: this.combatOperations)
        {
            if (combatOperation.getObjective().equals(objective))
                similarCombatOperations.add(combatOperation);
        }

        return similarCombatOperations;
    }

//    public ArrayList<Move> resolveCombat(final CombatOperation combatOperation, final GameState gameState)
//    {
//        Future future = new Future(combatOperation.getSourceShip(), combatOperation.getMyShips(), combatOperation.getEnemyShips());
//        return future.generateFutureMoves(gameState, combatOperation);
//    }

    public static double combatBalance(final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips)
    {
        // Returns the survival turns of the player
        // minus the survival turns of second best player
        // so this needs to be positive

        if (myShips.isEmpty() || enemyShips.isEmpty())
            return 1.0;
        else if ((myShips.size() == 1) && (enemyShips.size() == 1))
            return combatBalanceTwoShips(myShips, enemyShips);
        else
            return combatBalanceManyShips(myShips, enemyShips);
    }

    private static double combatBalanceManyShips(final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips)
    {
        int myId = myShips.get(0).getOwner();

        HashMap<Integer,Double> damageTakenPerShip = new HashMap<>();
        HashMap<Integer,Integer> survivalTimePerShip = new HashMap<>();

        HashMap<Integer,Integer> damagePerPlayer = new HashMap<>();
        HashMap<Integer,Integer> survivalTimePerPlayer = new HashMap<>();
        HashMap<Integer,Integer> shipLostPerPlayer = new HashMap<>();

        final ArrayList<Ship> allShips = new ArrayList<>(myShips);
        allShips.addAll(enemyShips);

        // Fill Arrays with default values
        for(final Ship ship: allShips)
        {
            damageTakenPerShip.put(ship.getId(), 0.0);
            survivalTimePerShip.put(ship.getId(), Integer.MAX_VALUE);
            damagePerPlayer.put(ship.getOwner(), 0);
            shipLostPerPlayer.put(ship.getOwner(), 0);
        }

        // Fill who damages who
        ArrayList<Ship> allEnemyShipsInRange = new ArrayList<>();
        for(final Ship ship_i: allShips)
        {
            if (!ship_i.isUndocked())
                continue;

            allEnemyShipsInRange.clear();
            for(final Ship ship_j: allShips)
                if ((ship_i.getOwner() != ship_j.getOwner()) && (ship_i.getDistanceTo(ship_j) <= 6.00001))
                    allEnemyShipsInRange.add(ship_j);

            for(final Ship ship_j: allEnemyShipsInRange)
                damageTakenPerShip.put(ship_j.getId(), damageTakenPerShip.get(ship_j.getId()) + 64.0 / allEnemyShipsInRange.size());

            if (!allEnemyShipsInRange.isEmpty())
                damagePerPlayer.put(ship_i.getOwner(), damagePerPlayer.get(ship_i.getOwner()) + 64);
        }

        if (damagePerPlayer.get(myId) == 0)
            return 1.0;

        // Survival time per ship
        for(final Ship ship: allShips)
            if (damageTakenPerShip.get(ship.getId()) != 0)
                survivalTimePerShip.put(ship.getId(), (int)((double)ship.getHealth() / damageTakenPerShip.get(ship.getId())) + 1);

        for(final Ship ship: allShips)
        {
            if ((survivalTimePerShip.get(ship.getId()) != Integer.MAX_VALUE))
                if ((!survivalTimePerPlayer.containsKey(ship.getOwner())) || (survivalTimePerPlayer.get(ship.getOwner()) < survivalTimePerShip.get(ship.getId())))
                    survivalTimePerPlayer.put(ship.getOwner(), survivalTimePerShip.get(ship.getId()));
        }

        // Combat ends when one player doesn't have ships anymore
        Integer endOfCombat = findSecondLargest(survivalTimePerPlayer.values().toArray());
        Integer mySurvivalTime = survivalTimePerPlayer.containsKey(myId)? survivalTimePerPlayer.get(myId) : Integer.MAX_VALUE;

        // Ships lost at the end of combat, i.e. when only one player's ships remain
        for(final Ship ship: allShips)
            if (survivalTimePerShip.get(ship.getId()) <= endOfCombat)
                shipLostPerPlayer.put(ship.getOwner(), shipLostPerPlayer.get(ship.getOwner()) + 1);

        Integer mostShipsLost = findLargest(shipLostPerPlayer.values().toArray());
        Integer myShipsLost = shipLostPerPlayer.get(myId);

        boolean iWin = mySurvivalTime > endOfCombat;
        boolean iLoose = mySurvivalTime <= endOfCombat;
        boolean enemiesLostMoreShips = myShipsLost < mostShipsLost;
        boolean enemiesLostLessShips = myShipsLost >= mostShipsLost;

        return ((myShipsLost == 0)? 1.0 : 0.0) + (enemiesLostMoreShips? 1.0 : 0.0) + (enemiesLostLessShips? -1.0 : 0.0) + (iWin? 1.0 : 0.0) + (iLoose? -2.0 : 0.0);
    }

    private static double combatBalanceTwoShips(final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips)
    {
        if ((myShips.size() != 1) || (enemyShips.size() != 1))
            throw new IllegalStateException("combatBalanceTwoShips called for more than 2 ships.");

        Ship myShip = myShips.get(0);
        Ship enemyShip = enemyShips.get(0);

        if (myShip.getDistanceTo(enemyShip) > 6.0)
            return 1.0;

        int myPower = myShip.isUndocked()? 64 : 0;
        int enemyPower = enemyShip.isUndocked()? 64 : 0;

        int myRemainingTurns = (enemyPower != 0)? myShip.getHealth() / enemyPower : Integer.MAX_VALUE;
        int enemyRemainingTurns = (myPower != 0)? enemyShip.getHealth() / myPower : Integer.MAX_VALUE;

        if (myRemainingTurns > enemyRemainingTurns)
            return 1.0;
        else if(myRemainingTurns == enemyRemainingTurns)
            return -1.0;
        else
            return -2.0;
    }

    private static Integer findLargest(final Object[] array)
    {
        int first;
        int arraySize = array.length;

        first = Integer.MIN_VALUE;
        for(int i = 0; i < arraySize ; i++)
        {
            int entry = (int)array[i];

            if (entry > first)
                first = entry;
        }

        return first;
    }

    private static Integer findSecondLargest(Object[] array)
    {
        int first, second;
        int arraySize = array.length;

        if (arraySize < 2)
            return (int)array[0];

        first = second = Integer.MIN_VALUE;
        for(int i = 0; i < arraySize ; i++)
        {
            int entry = (int)array[i];

            if (entry > first)
            {
                second = first;
                first = entry;
            }
            else if (entry > second && entry != first)
                second = entry;
        }

        if (second == Integer.MIN_VALUE)
            return first;
        else
            return second;
    }

    private void logCombatOperations()
    {
        for (final CombatOperation combatOperation: this.combatOperations)
            DebugLog.addLog(combatOperation.toString());
        DebugLog.addLog("");
    }

    public static double scoreShipMove(final Ship sourceShip, final Ship targetShip, final ThrustMove move, final GameState gameState)
    {
        Ship newSourceShipPosition = applyMoveToShip(sourceShip, move);

        ArrayList<Ship> closeEnemyShips = new ArrayList<>();
        for(final Ship ship: gameState.getEnemyShips())
            if (ship.getDistanceTo(newSourceShipPosition) <= 7.5)
                closeEnemyShips.add(ship);

        ArrayList<Ship> closeAllyShips = new ArrayList<>();
        for(final Ship ship: gameState.getMyShipsNextTurn())
            if (ship.getDistanceTo(newSourceShipPosition) <= 7.5)
                closeAllyShips.add(ship);

        if (closeEnemyShips.isEmpty())
            return 1.0;

        closeAllyShips.remove(sourceShip);
        closeAllyShips.add(newSourceShipPosition);

        return combatBalance(closeAllyShips, closeEnemyShips);
    }

    public static double scoreFleetMove(final Fleet fleet, final Ship targetShip, final ArrayList<Move> moves, final GameState gameState)
    {
        ArrayList<Ship> newShipsPositions = new ArrayList<>();
        for (int i = 0; i < fleet.getShips().size(); ++i)
            newShipsPositions.add(applyMoveToShip(fleet.getShips().get(i), moves.get(i)));

        Fleet newFleetPosition = new Fleet(newShipsPositions, fleet.getObjectives(), fleet.getId());

        ArrayList<Ship> closeEnemyShips = new ArrayList<>();
        for(final Ship ship: gameState.getEnemyShips())
            if (ship.getDistanceTo(newFleetPosition.getCentroid()) <= 7.0 + fleet.getRadius() + 0.1)
                closeEnemyShips.add(ship);

        ArrayList<Ship> closeAllyShips = new ArrayList<>();
        for(final Ship ship: gameState.getMyShipsNextTurn())
            if (ship.getDistanceTo(newFleetPosition.getCentroid()) <= 7.0 + fleet.getRadius() + 0.1)
                closeAllyShips.add(ship);

        if (closeEnemyShips.isEmpty())
            return 1.0;

        closeAllyShips.removeAll(fleet.getShips());
        closeAllyShips.addAll(newShipsPositions);

        return combatBalance(closeAllyShips, closeEnemyShips);
    }

    public static double scoreCombatOperationMove(final CombatOperation combatOperation, final CombatOperationMoves moves, final GameState gameState)
    {
        ArrayList<Ship> enemyShips = combatOperation.getEnemyShips();

        if (enemyShips.isEmpty())
            return 1.0;

        ArrayList<Ship> myShips = combatOperation.getMyShips();
        ArrayList<Fleet> myFleets = combatOperation.getMyFleets();

        ArrayList<Ship> myShipsNextTurn = new ArrayList<>();

        for (final Fleet fleet: myFleets)
            for (int i = 0; i < fleet.getShips().size(); ++i)
                if (moves.getFleetMoves().containsKey(fleet))
                    myShipsNextTurn.add(applyMoveToShip(fleet.getShips().get(i), moves.getFleetMoves().get(fleet).get(i)));

        for (final Ship ship: myShips)
            if (moves.getShipMoves().containsKey(ship))
                myShipsNextTurn.add(applyMoveToShip(ship, moves.getShipMoves().get(ship)));

        ArrayList<Ship> closeAllyShips = new ArrayList<>();
        for (final Ship myShip: myShipsNextTurn)
        {
            for(final Ship enemyShip: gameState.getEnemyShips())
                if ((enemyShip.getDistanceTo(myShip) <= 14.0) && !(enemyShips.contains(enemyShip)))
                    enemyShips.add(enemyShip);

            for(final Ship allyShip: gameState.getMyShipsNextTurn())
                if ((allyShip.getDistanceTo(myShip) <= 7.5) && !(myShipsNextTurn.contains(allyShip)))
                    closeAllyShips.add(allyShip);
        }

        myShipsNextTurn.addAll(closeAllyShips);

        return combatBalance(myShipsNextTurn, enemyShips);
    }
}
