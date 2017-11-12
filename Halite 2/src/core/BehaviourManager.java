package core;

import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import java.util.Map;

public class BehaviourManager
{
    private GameState gameState;

    private final double maxPriority;
    private final double distanceDiscountExponent;

    // Colonize
    private final int colonizationTurns;
    private final double colonizationBump;

    // Rush
    private final double maxRushDistance;
    private final double antiRushPriority;
    private final double rushPriority;
    private final int rushTurns;
    private final int rushMaxObjectives;
    private final int rushMaxShipsPerObjective;
    private boolean rushActivated;

    // Defend
    private final double defendPriority;
    private final int enemyShipsToDefend;

    // Crash
    private final double crashPriority;
    private final int enemyShipsToCrash;
    private final double crashBelowHealth;

    // Attack
    private final double attackShipPriority;

    public int getRushMaxObjectives() { return rushMaxObjectives; }
    public double getCrashBelowHealth() { return crashBelowHealth; }

    public BehaviourManager(final Map<String,Object> gameDefinitions, final GameState gameState)
    {
        this.gameState                  = gameState;
        this.maxPriority                = (double) gameDefinitions.get("maxPriority");
        this.distanceDiscountExponent   = (double) gameDefinitions.get("distanceDiscountExponent");

        this.colonizationTurns          = (int) gameDefinitions.get("colonizationTurns");
        this.colonizationBump           = (double) gameDefinitions.get("colonizationBump");

        this.maxRushDistance            = (double) gameDefinitions.get("maxRushDistance");
        this.antiRushPriority           = (double) gameDefinitions.get("antiRushPriority");
        this.rushPriority               = (double) gameDefinitions.get("rushPriority");
        this.rushTurns                  = (int) gameDefinitions.get("rushTurns");
        this.rushMaxObjectives          = (int) gameDefinitions.get("rushMaxObjectives");
        this.rushMaxShipsPerObjective   = (int) gameDefinitions.get("rushMaxShipsPerObjective");
        this.rushActivated              = false;

        this.defendPriority             = (double) gameDefinitions.get("defendPriority");
        this.enemyShipsToDefend         = (int) gameDefinitions.get("enemyShipsToDefend");

        this.crashPriority              = (double) gameDefinitions.get("crashPriority");
        this.enemyShipsToCrash          = (int) gameDefinitions.get("enemyShipsToCrash");
        this.crashBelowHealth           = (double) gameDefinitions.get("crashBelowHealth");

        this.attackShipPriority         = (double) gameDefinitions.get("attackShipPriority");
    }

    public double getPlanetPriorityForColonize(final GameMap gameMap, final DistanceManager distanceManager, final Planet targetPlanet)
    {
        // This is just linear in the number of docking spots.
        // We rescale from 0-100 by dividing by highest priority (6-planet at 0 distance = 6), and times 100.

        double priority = (double) targetPlanet.getDockingSpots() / 6.0 * maxPriority;

        if (gameState.getTurn() < colonizationTurns)
            return colonizationBump + priority;
        else
            return priority;
    }

    public double getPlanetPriorityForCrash(final GameMap gameMap, final DistanceManager distanceManager, final Planet targetPlanet)
    {
        if(distanceManager.countCloseEnemyShipsFromPlanet(gameMap, targetPlanet, targetPlanet.getRadius() + Math.max(targetPlanet.getRadius(), 4)) > enemyShipsToCrash)
            return crashPriority;
        else
            return 0;
    }

    public double getPlanetPriorityForDefend(final GameMap gameMap, final DistanceManager distanceManager, final Planet targetPlanet)
    {
        if(distanceManager.countCloseEnemyShipsFromPlanet(gameMap, targetPlanet, 25) > enemyShipsToDefend)
            return defendPriority;
        else
            return 0;
    }

    public double getDockedShipPriorityForAttack(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        // Ideally we should attack either poorly defended planets, or hubs with many ships
        // Otherwise we take a linear map of number of docked ships from 0 to 100.

        Planet planet = distanceManager.findPlanetFromID(targetShip.getDockedPlanet());
        int numberOfDockedShips = planet.getDockedShips().size();

        if (numberOfDockedShips == 1 || numberOfDockedShips >= 4)
            return maxPriority;
        else
            return numberOfDockedShips / 6.0 * maxPriority;
    }

    public double getShipPriorityForAttack(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        return attackShipPriority;
    }

    public double getDockedShipPriorityForRush(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        if(isValidTargetForRush(gameMap, distanceManager, targetShip))
            return rushPriority;
        else
            return 0;
    }

    public double getUndockedShipPriorityForRush(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        if(isValidTargetForRush(gameMap, distanceManager, targetShip))
            return rushPriority * 0.75;
        else
            return 0;
    }

    public double getShipPriorityForAntiRush(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        if(this.gameState.getStartingPointByPlayers().get(gameMap.getMyPlayerId()).getDistanceTo(targetShip) < 50)
            return antiRushPriority;
        else
            return 0;
    }

    public double combinePriorityWithDistance(final double priority, final double distance)
    {
        // Simple way to discount far objectives

        return priority / Math.pow(distance, distanceDiscountExponent);
    }

    public boolean isValidTurnForRush()
    {
        return gameState.getTurn() <= rushTurns;
    }

    public boolean isValidTargetForRush(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        if (gameState.getTurn() <= 1)
        {
            // Enemy needs to be close
            boolean enemyClose = this.gameState.getStartingPointByPlayers().get(gameState.getMyId()).getDistanceTo(targetShip) < maxRushDistance;
            boolean oneVsOne = this.gameState.getNumberOfPlayers() == 2;
            rushActivated = enemyClose && oneVsOne;
            return rushActivated;
        }
        else
        {
            return rushActivated;
        }
    }

    public int getRushShipsPerObjective(final GameMap gameMap, final DistanceManager distanceManager, final Ship targetShip)
    {
        if (!isValidTargetForRush(gameMap, distanceManager, targetShip))
            return 0;
        else if (gameState.getTurn() <= 5)
            return rushMaxShipsPerObjective;
        else
        {
            // if enemy has one planet, send 3 ships
            // if enemy has two planets, send 3 ships
            // if enemy has three planets, send 2 ship
            // cap at allowed max

            int enemyPlanets = gameState.numberOfPlanetsByPlayer.get(targetShip.getOwner());
            int shipsToSend = enemyPlanets < 3? 3 : 1;

            return Math.min(shipsToSend, rushMaxShipsPerObjective);
        }
    }
}
