package core;

import hlt.GameMap;
import hlt.Ship;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
        ArrayList<Ship> unassignedShips = allShips(gameMap);

        for(final Objective objective: objectives)
        {
            if(unassignedShips.size()==0)
                return;

            //if(objectiveAlreadyAssigned(objective))
            //    continue;

            int requiredShips = objective.getRequiredShips();
            ArrayList<Ship> ships = new ArrayList<>();

            for (int i = 0; i < requiredShips; i++)
            {
                if(unassignedShips.size() == 0)
                    break;
                else
                    ships.add(unassignedShips.remove(0));
            }

            this.fleets.add(new Fleet(ships, objective));
        }
    }

    private boolean objectiveAlreadyAssigned(final Objective objective)
    {
        for(final Fleet fleet: this.fleets)
            if(fleet.getObjectives().contains(objective))
                return true;

        return false;
    }

    private ArrayList<Ship> allShips(GameMap gameMap)
    {
        return new ArrayList<>(gameMap.getMyPlayer().getShips().values());
    }

    private ArrayList<Ship> unassignedShips(GameMap gameMap)
    {
        ArrayList<Ship> allShips = allShips(gameMap);

        for(final Fleet fleet: this.fleets)
            allShips.removeAll(fleet.getShips());

        return allShips;
    }
}
