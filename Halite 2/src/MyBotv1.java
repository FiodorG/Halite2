import hlt.*;
import hlt.Constants;

import java.util.ArrayList;
import java.util.Collection;

public class MyBotv1
{
    public static void main(final String[] args)
    {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("v1");
        final int myId = gameMap.getMyPlayerId();

        final ArrayList<Move> moveList = new ArrayList<>();
        for(;;)
        {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            ships:
            for(final Ship ship : gameMap.getMyPlayer().getShips().values())
            {
                if(ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                    continue;

                double bestPlanetScore = -999;
                double planetScore = 0;

                Collection<Planet> planets;
                if(freePlanetsExist(gameMap))
                    planets = freePlanets(gameMap);
                else
                    planets = gameMap.getAllPlanets().values();

                Planet bestPlanet = planets.iterator().next();

                for(final Planet planet : planets)
                {
                    if (planet.getOwner() == myId && planet.isFull())
                        continue;

                    if (ship.canDock(planet) && !planet.isFull())
                    {
                        moveList.add(new DockMove(ship, planet));
                        continue ships;
                    }

                    planetScore = planet.getDockingSpots() / ship.getDistanceTo(planet);

                    if(planetScore > bestPlanetScore)
                    {
                        bestPlanet = planet;
                        bestPlanetScore = planetScore;
                    }
                }

                final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, bestPlanet, Constants.MAX_SPEED);
                if (newThrustMove != null)
                    moveList.add(newThrustMove);
            }
            Networking.sendMoves(moveList);
        }
    }

    public static boolean freePlanetsExist(GameMap gameMap)
    {
        boolean freePlanetsExist = false;
        for(final Planet planet : gameMap.getAllPlanets().values())
            if (!planet.isOwned())
                return true;

        return freePlanetsExist;
    }

    public static ArrayList<Planet> freePlanets(GameMap gameMap)
    {
        ArrayList<Planet> planets = new ArrayList<Planet>();
        for(final Planet planet : gameMap.getAllPlanets().values())
            if (!planet.isOwned())
                planets.add(planet);

        return planets;
    }
}
