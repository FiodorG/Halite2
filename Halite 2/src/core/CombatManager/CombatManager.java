package core.CombatManager;

import core.*;
import hlt.*;

import java.util.*;

import static core.GameState.applyMoveToShip;

public class CombatManager
{
    private ArrayList<CombatOperation> combatOperations;
    private HashMap<Integer, CombatOperation> shipsToCombatOperations;
    private HashMap<Integer, CombatOperation> fleetToCombatOperations;

    private int combatOperationId;

    public CombatManager()
    {
        this.combatOperations = new ArrayList<>();
        this.shipsToCombatOperations = new HashMap<>();
        this.fleetToCombatOperations = new HashMap<>();
        this.combatOperationId = 0;
    }

    public void resolveCombats(final GameState gameState, final ArrayList<Move> moveList)
    {
        for(final CombatOperation combatOperation: this.combatOperations)
            resolveCombat(combatOperation, gameState, moveList);
    }

    private void resolveCombat(final CombatOperation combatOperation, final GameState gameState, final ArrayList<Move> moveList)
    {
        Future future = new Future(combatOperation.getMyShips(), combatOperation.getEnemyShips());
        moveList.addAll(future.generateAllFutureMoves(gameState));
    }

    public void createCombatOperations(final GameState gameState)
    {
        clearCombatOperations();

        for(final Fleet fleet: gameState.getFleetManager().getFleets())
            for(final Ship ship: fleet.getShips())
            {
                if(createCombatOperation(ship, fleet, gameState))
                   break;
            }

        logCombatOperations();
    }

    public boolean createCombatOperation(final Ship sourceShip, final Fleet fleet, final GameState gameState)
    {
        ArrayList<Ship> enemyShips = gameState.getDistanceManager().getEnemiesCloserThan(sourceShip, 14);

        if (enemyShips.isEmpty())
            return false;

        // Add all close ships from same fleet
        ArrayList<Ship> myShips = new ArrayList<>();
        for(final Ship ship: fleet.getShips())
            if (sourceShip.getDistanceTo(ship) <= 14)
                myShips.add(ship);

        // Add all other friendly ship nearby
        for(final Ship ship: gameState.getMyShips())
            if ((sourceShip.getDistanceTo(ship) <= 14) && (!myShips.contains(ship)))
                myShips.add(ship);

        CombatOperation combatOperation = new CombatOperation(myShips, enemyShips, combatBalance(myShips, enemyShips), this.combatOperationId++);

        for (final Ship ship: combatOperation.getMyShips())
            this.shipsToCombatOperations.put(ship.getId(), combatOperation);

        for (final Ship ship: combatOperation.getEnemyShips())
            this.shipsToCombatOperations.put(ship.getId(), combatOperation);

        this.fleetToCombatOperations.put(fleet.getId(), combatOperation);
        this.combatOperations.add(combatOperation);

        return true;
    }

    private static double combatBalance(final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips)
    {
        // Returns the survival turns of the player
        // minus the survival turns of second best player
        // so this needs to be positive

        if (myShips.isEmpty() || enemyShips.isEmpty())
            throw new IllegalStateException("combatBalance called for 1 ship.");
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
                if ((ship_i.getOwner() != ship_j.getOwner()) && (ship_i.getDistanceTo(ship_j) <= 6.0))
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

        int myPower = (myShip.getDockingStatus() == Ship.DockingStatus.Undocked)? 64 : 0;
        int enemyPower = (enemyShip.getDockingStatus() == Ship.DockingStatus.Undocked)? 64 : 0;

        int myRemainingTurns = (enemyPower != 0)? myShip.getHealth() / enemyPower : Integer.MAX_VALUE;
        int enemyRemainingTurns = (myPower != 0)? enemyShip.getHealth() / myPower : Integer.MAX_VALUE;

        if (myRemainingTurns > enemyRemainingTurns)
            return 1.0;
        else if(myRemainingTurns == enemyRemainingTurns)
            return -1.0;
        else
            return -2.0;
    }

    private void clearCombatOperations()
    {
        this.combatOperations.clear();
        this.fleetToCombatOperations.clear();
        this.shipsToCombatOperations.clear();
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
        for(final CombatOperation combatOperation: this.combatOperations)
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

    public static double scoreCrashMove(final Ship sourceShip, final Ship targetShip, final ThrustMove move, final GameState gameState)
    {
        if ((sourceShip.getDistanceTo(targetShip) <= 7.0) &&
            (sourceShip.getHealth() <= 127) &&
            (targetShip.getHealth() == 255))
            return 1.0;
        else
            return -1.0;
    }
}
