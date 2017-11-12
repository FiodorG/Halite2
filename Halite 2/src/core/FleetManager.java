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

    private ArrayList<Objective> unfilledSuperObjectives;
    private ArrayList<Objective> filledSuperObjectives;
    private ArrayList<Objective> assignedSuperObjectives;

    private ArrayList<Objective> unfilledObjectives;
    private ArrayList<Objective> filledObjectives;

    private int fleetsAssignedToSuperObjectives;

    public FleetManager()
    {
        this.fleets = new ArrayList<>();
        this.unfilledSuperObjectives = new ArrayList<>();
        this.filledSuperObjectives = new ArrayList<>();
        this.assignedSuperObjectives = new ArrayList<>();
        this.unfilledObjectives = new ArrayList<>();
        this.filledObjectives = new ArrayList<>();
    }

    public ArrayList<Fleet> getFleets() { return fleets; }

    public void assignFleetsToObjectives(final GameMap gameMap, final ArrayList<Objective> objectives, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        ArrayList<Ship> availableShips = allAvailableShips(gameMap);

        if(availableShips.isEmpty())
            return;

        clearFleets();
        getObjectives(objectives, behaviourManager);

        for(final Ship ship: availableShips)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(ship, distanceManager);
            Objective objective = selectObjective(ship, objectivesAvailable, behaviourManager);
            objective.decreaseRequiredShips();

            updateFleets(ship, objective, behaviourManager);
            updateObjectives(objective);
        }

        logFleets();
    }

    private void updateObjectives(final Objective objective)
    {
        if (objective.getRequiredShips() == 0)
        {
            if (unfilledSuperObjectives.contains(objective))
            {
                filledSuperObjectives.add(objective);
                unfilledSuperObjectives.remove(objective);
            }
            else
            {
                filledObjectives.add(objective);
                unfilledObjectives.remove(objective);
            }
        }
    }

    private void updateFleets(final Ship ship, final Objective objective, final BehaviourManager behaviourManager)
    {
        boolean joinsExistingFleet = false;
        for(final Fleet fleet: this.fleets)
        {
            if (fleet.getObjectives().contains(objective))
            {
                fleet.addShip(ship);
                joinsExistingFleet = true;
                break;
            }
        }

        if (!joinsExistingFleet)
        {
            this.fleets.add(new Fleet(ship, objective));

            if (unfilledSuperObjectives.contains(objective))
            {
                this.assignedSuperObjectives.add(objective);
                this.fleetsAssignedToSuperObjectives++;

                if (fleetsAssignedToSuperObjectives >= behaviourManager.getRushMaxObjectives())
                {
                    unfilledSuperObjectives.clear();

                    for(final Objective superObjective: this.assignedSuperObjectives)
                        if(superObjective.getRequiredShips() > 0)
                            unfilledSuperObjectives.add(superObjective);
                }
            }
        }
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

    private HashMap<Objective, Double> getAvailableObjectives( final Ship ship, final DistanceManager distanceManager )
    {
        HashMap<Objective, Double> closestObjectives;
        HashMap<Objective, Double> closestSuperObjectives;

        if (unfilledObjectives.isEmpty())
            closestObjectives = distanceManager.getClosestObjectiveFromShip(filteredAttackObjectives(filledObjectives), ship, numberOfClosestObjectives);
        else
            closestObjectives = distanceManager.getClosestObjectiveFromShip(unfilledObjectives, ship, numberOfClosestObjectives);

        closestSuperObjectives = distanceManager.getClosestObjectiveFromShip(unfilledSuperObjectives, ship, Integer.MAX_VALUE);

        closestObjectives.putAll(closestSuperObjectives);
        return closestObjectives;
    }

    private ArrayList<Objective> filteredAttackObjectives(final ArrayList<Objective> objectives)
    {
        // Select only attack objectives when
        // all objectives have been filled (typically end of game)

        ArrayList<Objective> newObjectives = new ArrayList<>();
        for(final Objective objective: objectives)
        {
            if (objective.getOrderType() == Objective.OrderType.ATTACK)
                newObjectives.add(objective);
        }
        return newObjectives;
    }

    private void getObjectives(final ArrayList<Objective> objectives, final BehaviourManager behaviourManager)
    {
        // Ability to add super objectives, which the ships will consider
        // even though they are far away.

        for(final Objective objective: objectives)
            if (objective.getSuperObjective())
                this.unfilledSuperObjectives.add(objective);
            else
                this.unfilledObjectives.add(objective);
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

    public void clearFleets()
    {
        this.fleets.clear();
        this.unfilledSuperObjectives.clear();
        this.filledSuperObjectives.clear();
        this.assignedSuperObjectives.clear();
        this.unfilledObjectives.clear();
        this.filledObjectives.clear();
        this.fleetsAssignedToSuperObjectives = 0;
    }

    private void logFleets()
    {
        for(final Fleet fleet: this.fleets)
            DebugLog.addLog(fleet.toString());
    }
}
