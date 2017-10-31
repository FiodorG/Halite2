package core;

import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import java.util.ArrayList;

public class FleetManager
{
    private ArrayList<Fleet> fleets;

    public FleetManager()
    {
        this.fleets = new ArrayList<>();
    }

    public ArrayList<Fleet> getFleets() { return fleets; }
    public void clearFleets() { this.fleets.clear(); }

    public void assignFleetsToObjectives(final GameMap gameMap, ArrayList<Objective> objectives, final DistanceManager distanceManager)
    {
        clearFleets();
        ArrayList<Ship> availableShips = allAvailableShips(gameMap);

        for(final Objective objective: objectives)
        {
            if(availableShips.isEmpty())
                return;

            int requiredShips = objective.getRequiredShips();
            ArrayList<Ship> ships = new ArrayList<>();

            for (int i = 0; i < requiredShips; i++)
            {
                if(availableShips.isEmpty())
                    break;
                else
                {
                    Ship ship = distanceManager.getClosestShipFromPlanet(gameMap, (Planet) objective.getTargetEntity(), availableShips);
                    ships.add(ship);
                    availableShips.remove(ship);
                }
            }

            this.fleets.add(new Fleet(ships, objective));
        }
    }

    private ArrayList<Ship> allAvailableShips(GameMap gameMap)
    {
        // For all purposes a docking ships will never be used again

        ArrayList<Ship> allShips = new ArrayList<>();

        for(final Ship ship: gameMap.getMyPlayer().getShips().values())
            if (ship.getDockingStatus() == Ship.DockingStatus.Undocked)
                allShips.add(ship);

        return allShips;
    }
}
