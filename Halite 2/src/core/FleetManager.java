package core;

import hlt.DebugLog;
import hlt.GameMap;
import hlt.Ship;

import java.util.ArrayList;
import java.util.HashMap;

import static core.Config.numberOfClosestObjectives;

public class FleetManager
{
    private ArrayList<Fleet> fleets;

    public FleetManager()
    {
        this.fleets = new ArrayList<>();
    }

    public ArrayList<Fleet> getFleets() { return fleets; }
    public void clearFleets() { this.fleets.clear(); }

    public void assignFleetsToObjectives(final GameMap gameMap, final ArrayList<Objective> objectives, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        ArrayList<Ship> availableShips = allAvailableShips(gameMap);

        if(availableShips.isEmpty())
            return;

        clearFleets();

        ArrayList<Objective> unfilledObjectives = new ArrayList<>(objectives);
        ArrayList<Objective> filledObjectives = new ArrayList<>();

        for(final Ship ship: availableShips)
        {
            HashMap<Objective, Double> closestObjectives;

            if (unfilledObjectives.isEmpty())
                closestObjectives = distanceManager.getClosestObjectiveFromShip(filledObjectives, ship, numberOfClosestObjectives);
            else
                closestObjectives = distanceManager.getClosestObjectiveFromShip(unfilledObjectives, ship, numberOfClosestObjectives);

            Objective objective = selectObjective(ship, closestObjectives, behaviourManager);

            objective.decreaseRequiredShips();
            if (objective.getRequiredShips() == 0)
            {
                filledObjectives.add(objective);
                unfilledObjectives.remove(objective);
            }

            updateFleets(ship, objective);
        }

        logFleets();
    }

    private void updateFleets(final Ship ship, final Objective objective)
    {
        boolean joinsExistingFleet = false;
        for(final Fleet fleet: this.fleets)
        {
            if (fleet.getObjectives().contains(objective))
            {
                fleet.getShips().add(ship);
                joinsExistingFleet = true;
            }
        }

        if (!joinsExistingFleet)
            this.fleets.add(new Fleet(ship, objective));
    }

    private Objective selectObjective(final Ship ship, final HashMap<Objective, Double> closestObjectivesPriorities, final BehaviourManager behaviourManager)
    {
        Objective chosenObjective = null;
        double bestScore = -Double.MAX_VALUE;

        for (HashMap.Entry<Objective, Double> entry : closestObjectivesPriorities.entrySet())
        {
            Objective objective = entry.getKey();
            Double distance = entry.getValue();

            double score = behaviourManager.combinePriorityWithDistance(objective.getPriority(), distance);
            if (score > bestScore)
            {
                bestScore = score;
                chosenObjective = objective;
            }
        }

        return chosenObjective;
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

    private void logFleets()
    {
        for(final Fleet fleet: this.fleets)
            DebugLog.addLog(fleet.toString());
    }
}
