package core.CombatManager;

import core.*;
import hlt.*;

import java.util.*;

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
        class MutableInt
        {
            int value;
            public MutableInt(final int value) { this.value = value; }
            public void increment(int increment) { this.value += increment; }
            public void setValue(int newValue) { this.value = newValue; }
            public int getValue() { return value; }

            @Override
            public String toString() { return Integer.toString(this.value); }
        }

        int myId = myShips.get(0).getOwner();

        HashMap<Integer,MutableInt> powerPerPlayer = new HashMap<>();
        HashMap<Integer,MutableInt> lifePerPlayer = new HashMap<>();
        HashMap<Integer,MutableInt> shipsPerPlayer = new HashMap<>();
        HashMap<Integer,Integer> survivalTimePerShip = new HashMap<>();
        HashMap<Integer,Integer> survivalTimePerPlayer = new HashMap<>();

        final ArrayList<Ship> allShips = new ArrayList<>(myShips);
        allShips.addAll(enemyShips);

        for(final Ship ship: allShips)
        {
            int owner = ship.getOwner();
            MutableInt powerCount = powerPerPlayer.get(owner);
            MutableInt lifeCount = lifePerPlayer.get(owner);
            MutableInt shipsCount = shipsPerPlayer.get(owner);

            int shipPower = (ship.getDockingStatus() == Ship.DockingStatus.Undocked)? 64 : 0;

            if (powerCount == null)
            {
                powerPerPlayer.put(owner, new MutableInt(shipPower));
                lifePerPlayer.put(owner, new MutableInt(ship.getHealth()));
                shipsPerPlayer.put(owner, new MutableInt(1));
                survivalTimePerPlayer.put(owner, 0);
            }
            else
            {
                powerCount.increment(shipPower);
                lifeCount.increment(ship.getHealth());
                shipsCount.increment(1);
            }
        }

        int totalPower = 0;
        for(MutableInt power: powerPerPlayer.values())
            totalPower += power.getValue();

        for(final Ship ship: allShips)
        {
            int playerId = ship.getOwner();

            double damageTakenPerShip = (double)(totalPower - powerPerPlayer.get(playerId).getValue()) / (double)shipsPerPlayer.get(playerId).getValue();
            int remainingTurnsToLive = (damageTakenPerShip != 0)? (int)((double)ship.getHealth() / damageTakenPerShip) + 1 : Integer.MAX_VALUE;

            survivalTimePerShip.put(ship.getId(), remainingTurnsToLive);
        }

        for(final Ship ship: allShips)
        {
            int playerId = ship.getOwner();
            int remainingTurnsToLive = survivalTimePerShip.get(ship.getId());

            if (remainingTurnsToLive > survivalTimePerPlayer.get(playerId))
                survivalTimePerPlayer.put(playerId, remainingTurnsToLive);
        }

        int turnsOfSurvivor = findLargest(survivalTimePerPlayer.values().toArray());
        int combatDuration = findSecondLargest(survivalTimePerPlayer.values().toArray());
        int noSurvivors = (turnsOfSurvivor == combatDuration)? 1 : 0;
        int iWin = 0;
        int alliedLosses = 0;
        int enemyLosses = 0;

        for(final Ship ship: allShips)
        {
            if (survivalTimePerShip.get(ship.getId()) <= combatDuration)
            {
                if (ship.getOwner() == myId)
                    alliedLosses++;
                else
                    enemyLosses++;
            }
            else
            {
                if (ship.getOwner() == myId)
                    iWin = 1;
                else
                    iWin = 0;
            }
        }

        return (-1) * alliedLosses + enemyLosses + (-1) * noSurvivors;
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
            return 1;
        else if(myRemainingTurns == enemyRemainingTurns)
            return -1;
        else
            return -1;
    }

    private void clearCombatOperations()
    {
        this.combatOperations.clear();
        this.fleetToCombatOperations.clear();
        this.shipsToCombatOperations.clear();
    }

    private static Integer findLargest(Object[] array)
    {
        int first;
        int arraySize = array.length;

        if (arraySize < 2)
            throw new IllegalStateException("Array should be at least 2 elements.");

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
            throw new IllegalStateException("Array should be at least 2 elements.");

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

    public static double IsGoodMove(final Ship sourceShip, final Ship targetShip, final ThrustMove move, final GameState gameState)
    {
        if (sourceShip.getDistanceTo(targetShip) > 12)
            return 1;

        double scoreNoMove = combatBalanceForPosition(sourceShip, sourceShip, gameState);

        Entity newPosition = new Entity(sourceShip.getOwner(), -1, sourceShip.getXPos() + move.dX(), sourceShip.getYPos() + move.dY(), 0, 0);
        double scoreMove = combatBalanceForPosition(sourceShip, newPosition, gameState);

        return scoreMove - scoreNoMove;
    }

    private static double combatBalanceForPosition(final Ship targetShip, final Entity entity, final GameState gameState)
    {
        ArrayList<Ship> closeEnemyShips = new ArrayList<>();
        ArrayList<Ship> closeAllyShips = new ArrayList<>();

        for(final Ship ship: gameState.getEnemyShips())
            if (ship.getDistanceTo(entity) <= 7)
                closeEnemyShips.add(ship);

        for(final Ship ship: gameState.getMyShips())
            if (ship.getDistanceTo(entity) <= 7)
                closeAllyShips.add(ship);

        if (closeEnemyShips.isEmpty())
            return 0;

        if (!closeAllyShips.contains(targetShip))
            closeAllyShips.add(targetShip);

        return combatBalance(closeAllyShips, closeEnemyShips);
    }
}
