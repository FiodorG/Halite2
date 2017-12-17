package core;

import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static core.Config.numberOfClosestObjectives;
import static core.Objective.OrderType.FLEE;

public class FleetManager
{
    private ArrayList<Ship> shipsToMove;
    private ArrayList<Ship> shipsAvailable;
    private ArrayList<Ship> shipsDocked;
    private ArrayList<Ship> shipsSuperObjectives;

    private ArrayList<Fleet> fleetsToMove;
    private ArrayList<Fleet> fleetsAvailable;
    private ArrayList<Fleet> fleetsSuperObjectives;
    private HashMap<Integer, Fleet> shipToFleets;
    private int fleetId;

    private ArrayList<Objective> superObjectives;
    private ArrayList<Objective> unfilledObjectives;
    private ArrayList<Objective> filledObjectives;
    private ArrayList<Objective> undockObjectives;

    public FleetManager()
    {
        this.shipsToMove = new ArrayList<>();
        this.shipsAvailable = new ArrayList<>();
        this.shipsDocked = new ArrayList<>();
        this.shipsSuperObjectives = new ArrayList<>();

        this.fleetsToMove = new ArrayList<>();
        this.fleetsAvailable = new ArrayList<>();
        this.fleetsSuperObjectives = new ArrayList<>();
        this.shipToFleets = new HashMap<>();
        this.fleetId = 0;

        this.unfilledObjectives = new ArrayList<>();
        this.filledObjectives = new ArrayList<>();

        this.superObjectives = new ArrayList<>();
        this.undockObjectives = new ArrayList<>();
    }

    public ArrayList<Ship> getShipsToMove() { return shipsToMove; }
    public ArrayList<Fleet> getFleetsToMove() { return fleetsToMove; }
    public ArrayList<Fleet> getFleetsAvailable() { return fleetsAvailable; }
    public HashMap<Integer, Fleet> getShipToFleets() { return shipToFleets; }

    public void assignShips(final GameState gameState)
    {
        resetObjectives(gameState.getObjectiveManager());

        // 0. Refresh all
        refreshFleets(gameState);
        refreshShips(gameState);

        // 1. Move fleets
        assignFleetsToSuperObjectives(gameState);
        assignFleetsToObjectives(gameState);

        // 2. Move Ships
        assignShipsToSuperObjectives(gameState);
        assignShipsToObjectives(gameState);
        undockShips(gameState);

        // 4. Try to create Fleet
        assignShipsToFleets(gameState);

        logShips();
        logFleets();
    }

    private void assignFleetsToObjectives(final GameState gameState)
    {
        ArrayList<Fleet> fleetsAvailable = this.fleetsAvailable;
        sortFleetsAvailable(gameState, fleetsAvailable);

        for(final Fleet fleet: fleetsAvailable)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(fleet, gameState);
            Objective objective = selectObjective(fleet, objectivesAvailable, gameState);
            updateObjective(objective, fleet);
        }
    }

    private void assignFleetsToSuperObjectives(final GameState gameState)
    {
        ArrayList<Fleet> fleetsToRemove = new ArrayList<>();

        for(final Fleet fleet: this.fleetsAvailable)
        {
            HashMap<Objective, Double> objectivesAvailable = DistanceManager.getClosestObjectiveFromEntity(superObjectives, fleet, 1);

            if (objectivesAvailable.isEmpty())
                break;

            Objective objective = selectObjective(fleet, objectivesAvailable, gameState);
            updateSuperObjective(objective, fleet);
            fleetsToRemove.add(fleet);
        }

        this.fleetsAvailable.removeAll(fleetsToRemove);
        this.fleetsSuperObjectives.addAll(fleetsToRemove);
    }

    private void assignShipsToSuperObjectives(final GameState gameState)
    {
        ArrayList<Ship> shipsToRemove = new ArrayList<>();

        for(final Ship ship: this.shipsAvailable)
        {
            HashMap<Objective, Double> objectivesAvailable = DistanceManager.getClosestObjectiveFromEntity(superObjectives, ship, 1);

            if (objectivesAvailable.isEmpty())
                break;

            Objective objective = selectObjective(ship, objectivesAvailable, gameState);

            if ((gameState.getMyShipsPreviousTurn().contains(ship)) && (objective.getOrderType() != FLEE))
                continue;

            updateSuperObjective(objective, ship);
            shipsToRemove.add(ship);
        }

        this.shipsAvailable.removeAll(shipsToRemove);
        this.shipsSuperObjectives.addAll(shipsToRemove);
    }

    private void assignShipsToObjectives(final GameState gameState)
    {
        ArrayList<Ship> shipsAvailable = this.shipsAvailable;
        sortShipsAvailable(gameState, shipsAvailable);

        for(final Ship ship: shipsAvailable)
        {
            HashMap<Objective, Double> objectivesAvailable = getAvailableObjectives(ship, gameState);
            Objective objective = selectObjective(ship, objectivesAvailable, gameState);
            updateObjective(objective, ship);
        }
    }

    private void sortShipsAvailable(final GameState gameState, final ArrayList<Ship> shipsAvailable)
    {
        // Try to impose order on ships and how we compute objectives

        HashMap<Ship, Double> distances = new HashMap<>();
        for (final Ship ship: gameState.getMyShipsPreviousTurn())
            if (ship.getObjective() != null)
                distances.put(ship, ship.getDistanceTo(ship.getObjective().getTargetEntity()));

        for (final Ship ship: shipsAvailable)
            if (!distances.containsKey(ship))
                distances.put(ship, gameState.getDistanceManager().getClosestUndockedEnemyShipDistance(ship));

        shipsAvailable.sort((i, j) -> distances.get(i) <= distances.get(j)? -1:1);
    }

    private void sortFleetsAvailable(final GameState gameState, final ArrayList<Fleet> fleetsAvailable)
    {
        HashMap<Fleet, Double> distances = new HashMap<>();

        for (final Fleet fleet: fleetsAvailable)
            distances.put(fleet, gameState.getDistanceManager().getClosestUndockedEnemyShipDistance(fleet.getCentroid()));

        fleetsAvailable.sort((i, j) -> distances.get(i) <= distances.get(j)? -1:1);
    }

    private void updateObjective(final Objective objective, final Entity entity)
    {
        if (entity instanceof Ship)
        {
            objective.decreaseRequiredShips(1);
            ((Ship)entity).setObjective(objective);

            this.shipsToMove.add((Ship)entity);
        }
        else if (entity instanceof Fleet)
        {
            objective.decreaseRequiredShips(((Fleet)entity).getShips().size());
            ((Fleet)entity).addObjective(objective);

            this.fleetsToMove.add((Fleet)entity);
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
        if (entity instanceof Ship)
        {
            objective.decreaseRequiredShips(1);
            ((Ship)entity).setObjective(objective);

            this.shipsToMove.add((Ship)entity);
        }
        else if (entity instanceof Fleet)
        {
            objective.decreaseRequiredShips(((Fleet)entity).getShips().size());
            ((Fleet)entity).addObjective(objective);

            this.fleetsToMove.add((Fleet)entity);
        }
        else
            throw new IllegalStateException("Can't update objective with unknown entity.");

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
        if (entity instanceof Fleet)
        {
            if (((Fleet)entity).getShips().size() > 2)
                return filterPureAttackObjectives(gameState, objectives);
            else
                return filterAttackObjectives(objectives);
        }
        else if (entity instanceof Ship)
        {
            if (gameState.getBehaviourManager().getAttackOnlyObjectives(gameState, (Ship) entity))
                return filterAttackObjectives(objectives);
            else
                return objectives;
        }
        else
            throw new IllegalStateException("Can't filter objectives for non Ship or Fleet");
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

    private ArrayList<Objective> filterPureAttackObjectives(final GameState gameState, final ArrayList<Objective> objectives)
    {
        // Select only attack objectives when all objectives have been filled (typically end of game)

        ArrayList<Objective> newObjectives = new ArrayList<>();
        for(final Objective objective: objectives)
        {
            if (objective.isPureAttackObjective())
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
            }
        }
        this.shipsSuperObjectives.removeAll(shipsToRemove);
    }

    private void refreshShips(final GameState gameState)
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

        resetShipsSuperObjective(gameState);
    }

    private void resetObjectives(final ObjectiveManager objectiveManager)
    {
        this.unfilledObjectives.clear();
        this.unfilledObjectives = new ArrayList<>(objectiveManager.getObjectives());
        this.filledObjectives.clear();

        this.superObjectives.clear();
        this.superObjectives.addAll(objectiveManager.getSuperObjectives());

        this.undockObjectives.clear();
        this.undockObjectives.addAll(objectiveManager.getUndockObjectives());
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
                HashMap<Fleet, Double> fleetsAvailable = getAvailableFleets(gameState, ship, gameState.getDistanceManager());
                Fleet fleet = selectFleet(ship, fleetsAvailable, gameState);

                if (fleet != null)
                {
                    Objective objective = new Objective(fleet, 100.0, 1, Objective.OrderType.GROUP, false, true,-1);
                    this.shipsToMove.remove(ship);
                    updateObjective(objective, ship);
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
                fleet.addObjective(new Objective(fleet.getCentroid(),100.0,2, Objective.OrderType.GROUP,false, true,-1));
                this.fleetsAvailable.add(fleet);
                this.fleetsToMove.add(fleet);
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
        ArrayList<Fleet> fleetsToRemove = new ArrayList<>();

        for (final Fleet fleet: this.fleetsAvailable)
        {
            fleet.getObjectives().clear();
            boolean removeFleet = updateShipsToFleet(gameState, fleet);

            if (removeFleet)
                fleetsToRemove.add(fleet);
            else
                fleet.computeCentroid();
        }

        for (final Fleet fleet: fleetsToRemove)
        {
            for(final Ship ship: fleet.getShips())
            {
                this.shipsAvailable.add(ship);
                shipToFleets.remove(ship.getId());
            }
        }

        this.fleetsAvailable.removeAll(fleetsToRemove);
        this.fleetsToMove.clear();

        resetFleetsSuperObjective(gameState);
    }

    private boolean updateShipsToFleet(final GameState gameState, Fleet fleet)
    {
        boolean removeFleet = false;

        ArrayList<Ship> shipsToRemove = new ArrayList<>();
        for (final Ship ship: fleet.getShips())
        {
            int index = gameState.getMyShips().indexOf(ship);

            if (index == -1)
            {
                // ship is dead
                shipsToRemove.add(ship);
                shipToFleets.remove(ship.getId());
            }
            else
            {
                // update ship
                Ship updatedShip = gameState.getMyShips().get(index);
                fleet.getShips().set(fleet.getShips().indexOf(ship), updatedShip);
                this.shipToFleets.put(updatedShip.getId(), fleet);
            }
        }

        fleet.getShips().removeAll(shipsToRemove);

        if (fleet.getShips().size() <= 1)
            removeFleet = true;

        return removeFleet;
    }

    private void resetFleetsSuperObjective(final GameState gameState)
    {
        ArrayList<Fleet> fleetsToRemove = new ArrayList<>();
        for(final Fleet fleet: this.fleetsSuperObjectives)
        {
            boolean removeFleet = updateShipsToFleet(gameState, fleet);

            if (removeFleet)
                fleetsToRemove.add(fleet);
            else
                fleet.computeCentroid();
        }
        this.fleetsSuperObjectives.removeAll(fleetsToRemove);

        fleetsToRemove.clear();
        for(final Fleet fleet: this.fleetsSuperObjectives)
        {
            Objective oldSuperObjective = fleet.getFirstObjectives();
            int indexOfSuperObjective = this.superObjectives.indexOf(oldSuperObjective);

            if (indexOfSuperObjective == -1)
                fleetsToRemove.add(fleet);
            else
            {
                Objective newSuperObjective = this.superObjectives.get(indexOfSuperObjective);
                fleet.setObjective(newSuperObjective);
                updateSuperObjective(newSuperObjective, fleet);
            }
        }
        this.fleetsSuperObjectives.removeAll(fleetsToRemove);
        this.fleetsAvailable.addAll(fleetsToRemove);
    }

    private Fleet selectFleet(final Entity entity, final HashMap<Fleet, Double> fleetsAvailable, final GameState gameState)
    {
        Fleet chosenFleet = null;
        double bestScore = -Double.MAX_VALUE;

        for (HashMap.Entry<Fleet, Double> entry : fleetsAvailable.entrySet())
        {
            Fleet fleet = entry.getKey();
            Double distance = entry.getValue();

            if (fleet.priorityReinforcementNeed(gameState) < ((Ship) entity).getObjective().getPriority())
                continue;

            double score = gameState.getBehaviourManager().combinePriorityWithDistance(fleet.priorityReinforcementNeed(gameState), distance);
            if (score > bestScore)
            {
                bestScore = score;
                chosenFleet = fleet;
            }
        }

        return chosenFleet;
    }

    private HashMap<Fleet, Double> getAvailableFleets(final GameState gameState, final Entity entity, final DistanceManager distanceManager)
    {
        HashMap<Fleet, Double> fleetsAvailable = DistanceManager.getClosestFleetsFromShip(this.fleetsAvailable, entity, 5);
        HashMap<Fleet, Double> fleetsToChoose = new HashMap<>();

        for (HashMap.Entry<Fleet, Double> entry : fleetsAvailable.entrySet())
        {
            Fleet fleet = entry.getKey();
            if (fleet.getReinforcementNeed(gameState) > 0)
                fleetsToChoose.put(fleet, entry.getValue());
        }

        return fleetsToChoose;
    }

    private void undockShips(final GameState gameState)
    {
        for(final Ship ship: this.shipsDocked)
            for (final Objective objective: this.undockObjectives)
                if (objective.getTargetEntity().equals(ship))
                    updateObjective(objective, ship);
    }

    private void logShips()
    {
        for(final Ship ship: this.shipsToMove)
            DebugLog.addLog(ship.toString());
        DebugLog.addLog("");
    }

    private void logFleets()
    {
        for(final Fleet fleet: this.fleetsToMove)
            DebugLog.addLog(fleet.toString());
        DebugLog.addLog("");
    }
}
