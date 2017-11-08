package core;

import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import static hlt.Constants.BASE_PRODUCTIVITY;
import static hlt.Constants.DOCK_TURNS;

public class BehaviourManager
{
    public enum BehaviourTypes
    {
        RUSH,
        DEFEND,
        MIXED,
        CALIBRATED,
    }
    private String behaviourType;
    private GameState gameState;

    public BehaviourManager(final String behaviourType, final GameState gameState)
    {
        this.behaviourType = behaviourType;
        this.gameState = gameState;
    }

    public double getPlanetPriorityForColonize(final GameMap gameMap, final DistanceManager distanceManager, final Planet planet)
    {
        // This is the break-even time to reimburse the cost of the immobilized ships
        // sent to capture the planet, assuming it's entirely conquered. Equal to:
        // Distance in travel / max speed = turns to go there   +
        // Turns to Dock                                        +
        // Time to produce the sent ships = 6 / docking spots
        // The rescaling inverts the priority to put more weight to planets closer to you.
        // We rescale from 0-100 by dividing by highest priority (6-planet at 0 distance = 6), and times 100.

        // double priority = distanceManager.averageDistanceFromShips(planet, gameMap.getMyPlayerId()) / 7.0 + DOCK_TURNS + BASE_PRODUCTIVITY / planet.getDockingSpots();
        // double priorityRescaled = 1.0 / priority / 6 * 100;

        double priority = (double) planet.getDockingSpots() / 6.0 * 100;

        if (gameState.getTurn() < 50)
            return 100.0 + priority;
        else
            return priority;
    }

    public double getPlanetPriorityForCrash(final GameMap gameMap, final DistanceManager distanceManager, final Planet planet)
    {
        // Let's not crash for now

        return 0;
    }

    public double getDockedShipPriorityForAttack(final GameMap gameMap, final DistanceManager distanceManager, final Ship ship)
    {
        // Ideally we should attack either poorly defended planets, or hubs with many ships
        // Otherwise we take a linear map of number of docked ships from 0 to 100.

        Planet planet = distanceManager.findPlanetFromID(ship.getDockedPlanet());
        int numberOfDockedShips = planet.getDockingSpots() - planet.getFreeDockingSpots();

        if (numberOfDockedShips == 1 || numberOfDockedShips >= 4)
            return 100;
        else
            return numberOfDockedShips * 100 / 6;
    }

    public double getShipPriorityForAttack(final GameMap gameMap, final DistanceManager distanceManager, final Ship ship)
    {
        return 50;
    }

    public double combinePriorityWithDistance(final double priority, final double distance)
    {
        return priority / distance / distance;
    }
}
