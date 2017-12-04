package core;

import hlt.Entity;
import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import java.util.Map;

public class BehaviourManager
{
    private final double maxPriority;
    private final double distanceDiscountExponent;

    // Colonize
    private final int colonizationTurns;
    private final double colonizationBump;
    private final int colonizationMinShips;

    // Rush
    private final double maxRushDistance;
    private final double antiRushPriority;
    private final double antiRushDistance;
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
    private final double attackDockedShipPriority;
    private final double lurePriority;

    // Assassination
    private final double assassinationPriority;
    private final int assassinationTurns;
    private final int numberOfAssassinationObjectives;

    public int getNumberOfAssassinationObjectives() { return numberOfAssassinationObjectives; }

    public BehaviourManager(final Map<String,Object> gameDefinitions)
    {
        this.maxPriority = (double) gameDefinitions.get("maxPriority");
        this.distanceDiscountExponent = (double) gameDefinitions.get("distanceDiscountExponent");

        this.colonizationTurns = (int) gameDefinitions.get("colonizationTurns");
        this.colonizationBump = (double) gameDefinitions.get("colonizationBump");
        this.colonizationMinShips = (int) gameDefinitions.get("colonizationMinShips");

        this.maxRushDistance = (double) gameDefinitions.get("maxRushDistance");
        this.antiRushPriority = (double) gameDefinitions.get("antiRushPriority");
        this.antiRushDistance = (double) gameDefinitions.get("antiRushDistance");
        this.rushPriority = (double) gameDefinitions.get("rushPriority");
        this.rushTurns = (int) gameDefinitions.get("rushTurns");
        this.rushMaxObjectives = (int) gameDefinitions.get("rushMaxObjectives");
        this.rushMaxShipsPerObjective = (int) gameDefinitions.get("rushMaxShipsPerObjective");
        this.rushActivated = false;

        this.defendPriority = (double) gameDefinitions.get("defendPriority");
        this.enemyShipsToDefend = (int) gameDefinitions.get("enemyShipsToDefend");

        this.crashPriority = (double) gameDefinitions.get("crashPriority");
        this.enemyShipsToCrash = (int) gameDefinitions.get("enemyShipsToCrash");
        this.crashBelowHealth = (double) gameDefinitions.get("crashBelowHealth");

        this.attackShipPriority = (double) gameDefinitions.get("attackShipPriority");
        this.attackDockedShipPriority = (double) gameDefinitions.get("attackDockedShipPriority");
        this.lurePriority = (double) gameDefinitions.get("lurePriority");
        this.assassinationPriority = (double) gameDefinitions.get("assassinationPriority");
        this.assassinationTurns = (int) gameDefinitions.get("assassinationTurns");
        this.numberOfAssassinationObjectives = (int) gameDefinitions.get("numberOfAssassinationObjectives");
    }

    public double getPlanetPriorityForColonize(final GameState gameState, final Planet targetPlanet)
    {
        // This is just linear in the number of docking spots.
        // We rescale from 0-100 by dividing by highest priority (6-planet at 0 distance = 6), and times 100.

        double priority = (double) targetPlanet.getDockingSpots() / 6.0 * maxPriority;

        // In 4 player games, try to go in the corners (hack but let's see)
        if (gameState.getNumberOfPlayers() > 2)
            priority += 2 * gameState.getCenterOfMap().getDistanceTo(targetPlanet);

        if (gameState.getTurn() < colonizationTurns)
            return colonizationBump + priority;
        else
            return priority;
    }

    public double getPlanetPriorityForReinforceColony(final GameState gameState, final Planet targetPlanet)
    {
        // This is just linear in the number of docking spots.
        // We rescale from 0-100 by dividing by highest priority (6-planet at 0 distance = 6), and times 100.

        double priority = maxPriority;

        if (gameState.getTurn() < colonizationTurns)
            return colonizationBump + priority;
        else
            return priority;
    }

    public double getPlanetPriorityForAssassination(final GameState gameState, final Planet targetPlanet)
    {
        if (gameState.getTurn() < assassinationTurns)
            return 0.0;
        else
        {
//            if(gameState.getDistanceManager().getEnemiesCloserThan(targetPlanet, 50.0).size() - targetPlanet.getDockedShips().size() < 5)
            if(targetPlanet.getDockedShips().size() >= 4)
                return assassinationPriority;
            else
                return 0.0;
        }
    }

    public double getPlanetPriorityForLure(final GameState gameState, final Ship ship)
    {
        if (gameState.getNumberOfPlayers() > 2)
            return 0.0;
        else
        {
            if (gameState.getTurn() < 20)
                return 0.0;
            else
                return lurePriority;
        }
    }

    public double getShipPriorityForDefend(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        // If enemy ship less than 2 turns away
        if (distanceManager.getClosestEnemyShipDistance(targetShip) < 14.0)
            return defendPriority;
        else
            return 0.0;
    }

    public double getDockedShipPriorityForAttack(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        // Ideally we should attack either poorly defended planets, or hubs with many ships
        // Otherwise we take a linear map of number of docked ships from 0 to 100.

        Planet planet = gameState.getGameMap().getPlanet(targetShip.getDockedPlanet());
        int numberOfDockedShips = planet.getDockedShips().size();

        if (numberOfDockedShips == 1 || numberOfDockedShips >= 4)
            return attackDockedShipPriority;
        else
            return numberOfDockedShips / 6.0 * maxPriority;
    }

    public double getShipPriorityForAttack(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        return attackShipPriority;
    }

    public double getDockedShipPriorityForRush(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        if(isValidTargetForRush(gameState, distanceManager, targetShip))
            return rushPriority;
        else
            return 0;
    }

    public double getUndockedShipPriorityForRush(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        if(isValidTargetForRush(gameState, distanceManager, targetShip))
            return rushPriority * 0.75;
        else
            return 0;
    }

    public double getShipPriorityForAntiRush(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        // Do not antirush in 4 player games

        if(gameState.getNumberOfPlayers() != 2)
            return 0;
        else if(gameState.getStartingPointByPlayers().get(gameState.getMyId()).getDistanceTo(targetShip) < antiRushDistance)
            return antiRushPriority;
        else
            return 0;
    }

    public double combinePriorityWithDistance(final double priority, final double distance)
    {
        // Simple way to discount far objectives

        return priority / Math.pow(distance, distanceDiscountExponent);
    }

    public boolean isValidTurnForRush(final GameState gameState)
    {
        return gameState.getTurn() <= rushTurns;
    }

    public boolean isValidTargetForRush(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        if (gameState.getTurn() <= 1)
        {
            // Enemy needs to be close
            boolean enemyClose = gameState.getStartingPointByPlayers().get(gameState.getMyId()).getDistanceTo(targetShip) < maxRushDistance;
            boolean oneVsOne = gameState.getNumberOfPlayers() == 2;
            rushActivated = enemyClose && oneVsOne;
            return rushActivated;
        }
        else
        {
            return rushActivated;
        }
    }

    public int getRushShipsPerObjective(final GameState gameState, final DistanceManager distanceManager, final Ship targetShip)
    {
        if (!isValidTargetForRush(gameState, distanceManager, targetShip))
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
