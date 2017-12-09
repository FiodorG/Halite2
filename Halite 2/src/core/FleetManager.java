package core;

import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static core.Config.numberOfClosestObjectives;

public class FleetManager
{
    private ArrayList<Ship> shipsToMove;

    private ArrayList<Ship> shipsAvailable;
    private ArrayList<Ship> shipsDocked;
    private ArrayList<Ship> shipsSuperObjectives;

    private ArrayList<Fleet> fleets;
    private HashMap<Integer, Fleet> shipToFleets;
    private int fleetId;

    private ArrayList<Objective> superObjectives;

    private ArrayList<Objective> unfilledObjectives;
    private ArrayList<Objective> filledObjectives;

    public FleetManager()
    {
        this.shipsToMove = new ArrayList<>();
        this.shipsAvailable = new ArrayList<>();
        this.shipsDocked = new ArrayList<>();
        this.shipsSuperObjectives = new ArrayList<>();

        this.fleets = new ArrayList<>();
        this.shipToFleets = new HashMap<>();
        this.fleetId = 0;

        this.unfilledObjectives = new ArrayList<>();
        this.filledObjectives = new ArrayList<>();

        this.superObjectives = new ArrayList<>();
    }

    public ArrayList<Fleet> getFleets() { return fleets; }
    public ArrayList<Ship> getShipsToMove() { return shipsToMove; }

    public void assignShips(final GameState gameState)
    {
        resetObjectives(gameState.getObjectiveManager());

        // 1. Move fleets
        refreshFleets(gameState);
        assignFleetsToObjectives(gameState);

        resetAvailableShips(gameState);

        // 2. Move Super Objectives
        resetShipsSuperObjective(gameState);
        assignShipsToSuperObjectives(gameState);

        // 3. Assign Ships to Objectives
        assignShipsToObjectives(gameState);

        // 4. Try to create Fleet
        assignShipsToFleets(gameState);

        logShips();
        logFleets();
    }

    private void assignFleetsToObjectives(final GameState gameState)
    {
        for(final Fleet fleet: this.fleets)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(fleet.getCentroid(), gameState);
            Objective objective = selectObjective(fleet, objectivesAvailable, gameState);
            updateObjective(objective, fleet);
        }
    }

    private void assignShipsToSuperObjectives(final GameState gameState)
    {
        ArrayList<Ship> shipsToRemove = new ArrayList<>();

        for(final Ship ship: this.shipsAvailable)
        {
            // Only target new ships
            if (ship.getObjective() != null)
                continue;

            HashMap<Objective, Double> objectivesAvailable = DistanceManager.getClosestObjectiveFromEntity(superObjectives, ship, 1);

            if (objectivesAvailable.isEmpty())
                break;

            Objective objective = selectObjective(ship, objectivesAvailable, gameState);
            updateSuperObjective(objective, ship);
            shipsToRemove.add(ship);
            this.shipsToMove.add(ship);
        }

        this.shipsAvailable.removeAll(shipsToRemove);
        this.shipsSuperObjectives.addAll(shipsToRemove);
    }

    private void assignShipsToObjectives(final GameState gameState)
    {
        for(final Ship ship: this.shipsAvailable)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(ship, gameState);
            Objective objective = selectObjective(ship, objectivesAvailable, gameState);
            updateObjective(objective, ship);
            this.shipsToMove.add(ship);
        }
    }

    private void updateObjective(final Objective objective, final Entity entity)
    {
        if (entity instanceof Ship)
        {
            objective.decreaseRequiredShips(1);
            ((Ship)entity).setObjective(objective);
        }
        else if (entity instanceof Fleet)
        {
            objective.decreaseRequiredShips(((Fleet)entity).getShips().size());
            ((Fleet)entity).addObjective(objective);
        }
        else
            throw new IllegalStateException("Can't update objective with unknown entity.");

        if (objective.getRequiredShips() <= 0)
        {
            if (!filledObjectives.contains(objective))
                filledObjectives.add(objective);
            unfilledObjectives.remove(objective);
        }
    }

    private void updateSuperObjective(final Objective objective, final Entity entity)
    {
        objective.decreaseRequiredShips(1);
        ((Ship)entity).setObjective(objective);

        if (objective.getRequiredShips() <= 0)
            superObjectives.remove(objective);
    }

    private HashMap<Objective, Double> getAvailableObjectives(final Entity entity, final GameState gameState)
    {
        ArrayList<Objective> filteredFilledObjectives = filterAttackObjectives(this.filledObjectives);
        ArrayList<Objective> filteredUnfilledObjectives = filterObjectives(this.unfilledObjectives, entity, gameState);

        if (filteredUnfilledObjectives.isEmpty())
            return DistanceManager.getClosestObjectiveFromEntity(filteredFilledObjectives, entity, numberOfClosestObjectives);
        else
            return DistanceManager.getClosestObjectiveFromEntity(filteredUnfilledObjectives, entity, numberOfClosestObjectives);
    }

    private Objective selectObjective(final Entity entity, final HashMap<Objective, Double> closestObjectivesPriorities, final GameState gameState)
    {
        Objective chosenObjective = null;
        double bestScore = -Double.MAX_VALUE;

        for (HashMap.Entry<Objective, Double> entry : closestObjectivesPriorities.entrySet())
        {
            Objective objective = entry.getKey();
            Double distance = entry.getValue();

            double score = gameState.getBehaviourManager().combinePriorityWithDistance(objective.getPriority(), distance);
            if (score > bestScore)
            {
                bestScore = score;
                chosenObjective = objective;
            }
        }

        return chosenObjective;
    }

    private ArrayList<Objective> filterObjectives(final ArrayList<Objective> objectives, final Entity entity, final GameState gameState)
    {
        if (!(entity instanceof Ship))
            return filterAttackObjectives(objectives);
        else if (gameState.getBehaviourManager().getAttackOnlyObjectives(gameState, (Ship) entity))
            return filterAttackObjectives(objectives);
        else
            return objectives;
    }

    private ArrayList<Objective> filterAttackObjectives(final ArrayList<Objective> objectives)
    {
        // Select only attack objectives when all objectives have been filled (typically end of game)

        ArrayList<Objective> newObjectives = new ArrayList<>();
        for(final Objective objective: objectives)
        {
            if (objective.isAttackObjective())
                newObjectives.add(objective);
        }
        return newObjectives;
    }

    private void resetShipsSuperObjective(final GameState gameState)
    {
        // Remove dead ships
        ArrayList<Ship> shipsToRemove = new ArrayList<>();
        ArrayList<Ship> shipsToAdd = new ArrayList<>();
        for(final Ship ship: shipsSuperObjectives)
        {
            int indexOfShip = gameState.getMyShips().indexOf(ship);

            if (indexOfShip != -1)
            {
                Objective oldObjective = ship.getObjective();
                Ship newShip = gameState.getMyShips().get(indexOfShip);
                newShip.setObjective(oldObjective);

                shipsToRemove.add(ship);
                shipsToAdd.add(newShip);
            }
            else
                shipsToRemove.add(ship);
        }
        this.shipsSuperObjectives.removeAll(shipsToRemove);
        this.shipsSuperObjectives.addAll(shipsToAdd);

        // Remove ships with invalid objectives
        shipsToRemove.clear();
        for(final Ship ship: this.shipsSuperObjectives)
        {
            Objective oldSuperObjective = ship.getObjective();
            int indexOfSuperObjective = this.superObjectives.indexOf(oldSuperObjective);

            if (indexOfSuperObjective == -1)
                shipsToRemove.add(ship);
            else
            {
                Objective newSuperObjective = this.superObjectives.get(indexOfSuperObjective);
                ship.setObjective(newSuperObjective);
                updateSuperObjective(newSuperObjective, ship);
                this.shipsToMove.add(ship);
            }
        }
        this.shipsSuperObjectives.removeAll(shipsToRemove);
    }

    private void resetAvailableShips(final GameState gameState)
    {
        shipsToMove.clear();
        shipsAvailable.clear();
        shipsDocked.clear();

        for(final Ship ship: gameState.getMyShips())
        {
            if (!this.shipsSuperObjectives.contains(ship))
            {
                if (ship.isUndocked())
                {
                    if (!shipToFleets.containsKey(ship.getId()))
                        shipsAvailable.add(ship);
                }
                else
                    shipsDocked.add(ship);
            }
        }
    }

    private void resetObjectives(final ObjectiveManager objectiveManager)
    {
        this.unfilledObjectives.clear();
        this.unfilledObjectives = new ArrayList<>(objectiveManager.getObjectives());
        this.filledObjectives.clear();

        this.superObjectives.clear();
        this.superObjectives.addAll(objectiveManager.getSuperObjectives());
     }

    private void logShips()
    {
        for(final Ship ship: this.shipsToMove)
            DebugLog.addLog(ship.toString());
        DebugLog.addLog("");
    }

    private void assignShipsToFleets(final GameState gameState)
    {
        for(final Ship ship: shipsAvailable)
        {
            if (shipToFleets.containsKey(ship.getId()) || !ship.getObjective().isAttackObjective())
                continue;

            if (createNewFleet(ship, gameState))
                break;
            else
            {
                HashMap<Fleet, Double> fleetsAvailable = getAvailableFleets(ship, gameState.getDistanceManager());
                Fleet fleet = selectFleet(ship, fleetsAvailable, gameState.getBehaviourManager());

                if (fleet != null)
                {
                    ship.setObjective(new Objective(fleet, 100.0, 0, Objective.OrderType.GROUP, false, -1));
                    fleet.decreaseReinforcementNeed();
                }
            }
        }
    }

    private boolean createNewFleet(final Ship sourceShip, final GameState gameState)
    {
        for(final Ship ship: shipsAvailable)
        {
            if (shipToFleets.containsKey(ship.getId()) || ship.equals(sourceShip))
                continue;

            if (canGroup(sourceShip, ship, gameState))
            {
                sourceShip.setObjective(null);
                ship.setObjective(null);

                Fleet fleet = new Fleet(sourceShip, null, this.fleetId++);
                fleet.addShip(ship);
                fleet.addObjective(new Objective(fleet.getCentroid(),100.0,2, Objective.OrderType.GROUP,false, -1));
                this.fleets.add(fleet);
                this.shipToFleets.put(sourceShip.getId(), fleet);
                this.shipToFleets.put(ship.getId(), fleet);
                this.shipsAvailable.remove(sourceShip);
                this.shipsAvailable.remove(ship);
                this.shipsToMove.remove(sourceShip);
                this.shipsToMove.remove(ship);
                return true;
            }
        }

        return false;
    }

    private boolean canGroup(final Ship ship1, final Ship ship2, final GameState gameState)
    {
        if (
            !shipToFleets.containsKey(ship1.getId())
            && !shipToFleets.containsKey(ship2.getId())
            && (ship1.getDistanceTo(ship2) < 14.0)
            && gameState.objectsBetween(ship1, ship2, ship1.getRadius() + 0.1).isEmpty()
            && ship1.getObjective().equals(ship2.getObjective())
        )
            return true;
        else
            return false;
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
                    // ship is dead
                    j.remove();
                    shipToFleets.remove(ship.getId());

                    if (fleet.getShips().size() <= 1)
                        removeFleet = true;
                }
                else
                {
                    // update ship
                    Ship updatedShip = gameState.getMyShips().get(index);
                    fleet.getShips().set(fleet.getShips().indexOf(ship), updatedShip);
                    this.shipToFleets.put(updatedShip.getId(), fleet);
                }
            }

            if (removeFleet)
            {
                // Put back ship into pool
                for(final Ship ship: fleet.getShips())
                {
                    this.shipsAvailable.add(ship);
                    shipToFleets.remove(ship.getId());
                }
                i.remove();
            }
            else
                fleet.computeCentroid();
        }
    }

    private Fleet selectFleet(final Entity entity, final HashMap<Fleet, Double> fleetsAvailable, final BehaviourManager behaviourManager)
    {
        Fleet chosenFleet = null;
        double bestScore = -Double.MAX_VALUE;

        for (HashMap.Entry<Fleet, Double> entry : fleetsAvailable.entrySet())
        {
            Fleet fleet = entry.getKey();
            Double distance = entry.getValue();

            if (fleet.priorityReinforcementNeed() < ((Ship) entity).getObjective().getPriority())
                continue;

            double score = behaviourManager.combinePriorityWithDistance(fleet.priorityReinforcementNeed(), distance);
            if (score > bestScore)
            {
                bestScore = score;
                chosenFleet = fleet;
            }
        }

        return chosenFleet;
    }

    private HashMap<Fleet, Double> getAvailableFleets(final Entity entity, final DistanceManager distanceManager)
    {
        HashMap<Fleet, Double> fleetsAvailable = distanceManager.getClosestFleetsFromShip(this.fleets, entity, 5);
        HashMap<Fleet, Double> fleetsToChoose = new HashMap<>();

        for (HashMap.Entry<Fleet, Double> entry : fleetsAvailable.entrySet())
        {
            Fleet fleet = entry.getKey();
            if (fleet.getReinforcementNeed() > 0)
                fleetsToChoose.put(fleet, entry.getValue());
        }

        return fleetsToChoose;
    }

    private void logFleets()
    {
        for(final Fleet fleet: this.fleets)
            DebugLog.addLog(fleet.toString());
        DebugLog.addLog("");
    }
}