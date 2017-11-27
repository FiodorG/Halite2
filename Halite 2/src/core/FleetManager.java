package core;

import hlt.DebugLog;
import hlt.Entity;
import hlt.Planet;
import hlt.Ship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static core.Config.numberOfClosestObjectives;

public class FleetManager
{
    private ArrayList<Fleet> fleets;
    private HashMap<Integer, Fleet> shipToFleets;
    private int FleetId;

    private ArrayList<Objective> unfilledObjectives;
    private ArrayList<Objective> filledObjectives;

    private int shipsAssignedToAttack;

    public FleetManager()
    {
        this.fleets = new ArrayList<>();
        this.shipToFleets = new HashMap<>();
        this.FleetId = 0;

        this.unfilledObjectives = new ArrayList<>();
        this.filledObjectives = new ArrayList<>();

        this.shipsAssignedToAttack = 0;
    }

    public ArrayList<Fleet> getFleets() { return fleets; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void assignFleetsToObjectives2(final GameState gameState)
    {
        resetObjectives(gameState.getObjectiveManager().getObjectives());
        refreshFleets(gameState);

        for(final Fleet fleet: this.fleets)
        {
            Entity fleetCentroid = fleet.FleetCentroid();
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(fleetCentroid, gameState.getDistanceManager());
            Objective objective = selectObjective(fleetCentroid, objectivesAvailable, gameState.getBehaviourManager());
            objective.decreaseRequiredShips(fleet.getShips().size());

            fleet.getObjectives().add(objective);
            updateObjectives(objective);
        }

        ArrayList<Ship> availableShips = allAvailableShipsNotInFleets(gameState);

        if(availableShips.isEmpty())
            return;

        for(final Ship ship: availableShips)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(ship, gameState.getDistanceManager());
            HashMap<Fleet, Double> fleetsAvailable = getAvailableFleets(ship, gameState.getDistanceManager());
            Objective objective = selectObjectiveAndFleets(ship, objectivesAvailable, fleetsAvailable, gameState.getBehaviourManager());
            objective.decreaseRequiredShips(1);

            addShipToFleets(ship, objective);
            updateObjectives(objective);
        }

        logFleets();
    }

    private void refreshFleets(final GameState gameState)
    {
        // Remove dead ships, and refresh new ones,
        // remove all objectives.

        Iterator<Fleet> i = this.fleets.iterator();
        while (i.hasNext())
        {
            Fleet fleet = i.next();
            fleet.getObjectives().clear();
            boolean removeFleet = false;

            Iterator<Ship> j = fleet.getShips().iterator();
            while (j.hasNext())
            {
                Ship ship = j.next();
                int index = gameState.getMyShips().indexOf(ship);

                if (index == -1)
                {
                    j.remove();
                    shipToFleets.remove(ship);

                    if (fleet.getShips().isEmpty())
                        removeFleet = true;
                }
                else
                {
                    Ship updatedShip = gameState.getMyShips().get(index);

                    if (updatedShip.isUndocked())
                        fleet.getShips().set(fleet.getShips().indexOf(ship), updatedShip);
                    else
                    {
                        j.remove();
                        shipToFleets.remove(ship);

                        if (fleet.getShips().isEmpty())
                            removeFleet = true;
                    }
                }
            }

            if (removeFleet)
                i.remove();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void assignFleetsToObjectives(final GameState gameState)
    {
        resetObjectives(gameState.getObjectiveManager().getObjectives());
        resetFleets();

        ArrayList<Ship> availableShips = allAvailableShips(gameState);
        if(availableShips.isEmpty())
            return;

        for(final Ship ship: availableShips)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(ship, gameState.getDistanceManager());
            HashMap<Fleet, Double> fleetsAvailable = getAvailableFleets(ship, gameState.getDistanceManager());
            Objective objective = selectObjectiveAndFleets(ship, objectivesAvailable, fleetsAvailable, gameState.getBehaviourManager());
            objective.decreaseRequiredShips(1);

            addShipToFleets(ship, objective);
            updateObjectives(objective);
        }

        logFleets();
    }

    private void updateObjectives(final Objective objective)
    {
        if (objective.getRequiredShips() == 0)
        {
            filledObjectives.add(objective);
            unfilledObjectives.remove(objective);
        }

        if (objective.getOrderType() == Objective.OrderType.ATTACK)
            this.shipsAssignedToAttack++;
    }

    private void addShipToFleets(final Ship ship, final Objective objective)
    {
        boolean joinsExistingFleet = false;
        for(final Fleet fleet: this.fleets)
        {
            if (fleet.getObjectives().contains(objective))
            {
                fleet.addShip(ship);
                shipToFleets.put(ship.getId(), fleet);
                joinsExistingFleet = true;
                break;
            }
        }

        if (!joinsExistingFleet)
        {
            Fleet fleet = new Fleet(ship, objective, this.FleetId++);
            this.fleets.add(fleet);
            this.shipToFleets.put(ship.getId(), fleet);
        }
    }

    private Objective selectObjective(final Entity entity, final HashMap<Objective, Double> closestObjectivesPriorities, final BehaviourManager behaviourManager)
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

    private Objective selectObjectiveAndFleets(final Entity entity, final HashMap<Objective, Double> objectivesAvailable, final HashMap<Fleet, Double> fleetsAvailable, final BehaviourManager behaviourManager)
    {
        Objective chosenObjective = null;

        HashMap<Objective, Double> objectivesToChoose = new HashMap<>(objectivesAvailable);
        for (HashMap.Entry<Fleet, Double> entry : fleetsAvailable.entrySet())
        {
            Fleet fleet = entry.getKey();
            if (fleet.reinforcementNeed() > 0)
                objectivesToChoose.put(fleet.getObjectives().get(0), entry.getValue());
        }

        double bestScore = -Double.MAX_VALUE;
        for (HashMap.Entry<Objective, Double> entry : objectivesToChoose.entrySet())
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

    private HashMap<Objective, Double> getAvailableObjectives(final Entity entity, final DistanceManager distanceManager)
    {
        ArrayList<Objective> filteredFilledObjectives = filteredAttackObjectives(this.filledObjectives);
        ArrayList<Objective> filteredUnfilledObjectives = filteredObjectives(this.unfilledObjectives, entity, distanceManager);

        if (filteredUnfilledObjectives.isEmpty())
            return DistanceManager.getClosestObjectiveFromEntity(filteredFilledObjectives, entity, numberOfClosestObjectives);
        else
            return DistanceManager.getClosestObjectiveFromEntity(filteredUnfilledObjectives, entity, numberOfClosestObjectives);
    }

    private HashMap<Fleet, Double> getAvailableFleets(final Entity entity, final DistanceManager distanceManager)
    {
        return distanceManager.getClosestFleetsFromShip(this.fleets, entity, 5);
    }

    private ArrayList<Objective> filteredObjectives(final ArrayList<Objective> objectives, final Entity entity, final DistanceManager distanceManager)
    {
        if (!(entity instanceof Ship))
            return objectives;

        if (distanceManager.getClosestEnemyShipDistance((Ship) entity) < 21)
            return filteredAttackObjectives(objectives);
        else
            return objectives;
    }

    private ArrayList<Objective> filteredAttackObjectives(final ArrayList<Objective> objectives)
    {
        // Select only attack objectives when
        // all objectives have been filled (typically end of game)

        ArrayList<Objective> newObjectives = new ArrayList<>();
        for(final Objective objective: objectives)
        {
            if ((objective.getOrderType() != Objective.OrderType.COLONIZE) && (objective.getOrderType() != Objective.OrderType.REINFORCECOLONY))
                newObjectives.add(objective);
        }
        return newObjectives;
    }

    private ArrayList<Ship> allAvailableShips(GameState gameState)
    {
        ArrayList<Ship> allShips = new ArrayList<>();

        for(final Ship ship: gameState.getMyShips())
            if (ship.getDockingStatus() == Ship.DockingStatus.Undocked)
                allShips.add(ship);

        return allShips;
    }

    private ArrayList<Ship> allAvailableShipsNotInFleets(GameState gameState)
    {
        ArrayList<Ship> allShips = new ArrayList<>();

        for(final Ship ship: gameState.getMyShips())
            if ((ship.getDockingStatus() == Ship.DockingStatus.Undocked) && (!this.shipToFleets.containsKey(ship.getId())))
                allShips.add(ship);

        return allShips;
    }

    private void resetObjectives(final ArrayList<Objective> objectives)
    {
        this.unfilledObjectives.clear();
        this.unfilledObjectives = new ArrayList<>(objectives);
        this.filledObjectives.clear();

        this.shipsAssignedToAttack = 0;
    }

    private void resetFleets()
    {
        this.fleets.clear();
    }

    private void logFleets()
    {
        for(final Fleet fleet: this.fleets)
            DebugLog.addLog(fleet.toString());
        DebugLog.addLog("");
    }
}

