package core;

import hlt.*;

import java.util.*;

public class ObjectiveManager
{
    private ArrayList<Objective> objectives;
    private int objectiveId;

    public ObjectiveManager()
    {
        this.objectives = new ArrayList<>();
        this.objectiveId = 0;
    }

    public ArrayList<Objective> getObjectives() { return objectives; }

    public void getObjectives(final GameMap gameMap, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        clearObjectives();

        //this.objectives.addAll(getRushObjectives(gameMap, distanceManager, behaviourManager));
        this.objectives.addAll(getAntiRushObjectives(gameMap, distanceManager, behaviourManager));
        this.objectives.addAll(getColonizeObjectives(gameMap, distanceManager, behaviourManager));
        this.objectives.addAll(getAttackObjectives(gameMap, distanceManager, behaviourManager));
        //this.objectives.addAll(getDefendObjectives(gameMap, distanceManager, behaviourManager));

        removeZeroPriorityObjectives();
        sortObjectives(this.objectives);
        logObjectives();
    }

    private ArrayList<Objective> getRushObjectives(final GameMap gameMap, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        if (!behaviourManager.isValidTurnForRush())
            return new ArrayList<>();

        List<Ship> ships = gameMap.getAllShips();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : ships)
        {
            Objective objective;

            // If own ship, do nothing
            if (ship.getOwner() == myId)
                continue;

            // Then attack docking/undocking/docked ships in priority
            else if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                objective = new Objective(
                    ship,
                    behaviourManager.getDockedShipPriorityForRush(gameMap, distanceManager, ship),
                    behaviourManager.getRushShipsPerObjective(gameMap, distanceManager, ship),
                    Objective.OrderType.RUSH,
                    true,
                    this.objectiveId++
                );

            else if (ship.getDockingStatus() == Ship.DockingStatus.Undocked)
                objective = new Objective(
                        ship,
                        behaviourManager.getUndockedShipPriorityForRush(gameMap, distanceManager, ship),
                        behaviourManager.getRushShipsPerObjective(gameMap, distanceManager, ship),
                        Objective.OrderType.RUSH,
                        true,
                        this.objectiveId++
                );

            else
                continue;

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getAntiRushObjectives(final GameMap gameMap, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        if (!behaviourManager.isValidTurnForRush())
            return new ArrayList<>();

        List<Ship> ships = gameMap.getAllShips();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : ships)
        {
            Objective objective;

            // If own ship, do nothing
            if (ship.getOwner() == myId)
                continue;

                // Then attack docking/undocking ships in priority
            else if ((ship.getDockingStatus() == Ship.DockingStatus.Undocked))
                objective = new Objective(
                    ship,
                    behaviourManager.getShipPriorityForAntiRush(gameMap, distanceManager, ship),
                    2,
                    Objective.OrderType.ANTIRUSH,
                    false,
                    this.objectiveId++
                );

            else
                continue;

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getColonizeObjectives(final GameMap gameMap, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Map<Integer, Planet> planets = gameMap.getAllPlanets();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Planet planet : planets.values())
        {
            Objective objective;

            // First make sure we fill slots in owned planets
            if (planet.getOwner() == myId && !planet.isFull())
                objective = new Objective(
                    planet,
                    behaviourManager.getPlanetPriorityForColonize(gameMap, distanceManager, planet),
                    planet.getFreeDockingSpots(),
                    Objective.OrderType.COLONIZE,
                    false,
                    this.objectiveId++
                );

            // Then get new planets
            else if (!planet.isOwned())
                objective = new Objective(
                    planet,
                    behaviourManager.getPlanetPriorityForColonize(gameMap, distanceManager, planet),
                    planet.getDockingSpots(),
                    Objective.OrderType.COLONIZE,
                    false,
                    this.objectiveId++
                );

            // Then crash into full enemy planets
            else if (planet.getOwner() != myId)
                objective = new Objective(
                    planet,
                    behaviourManager.getPlanetPriorityForCrash(gameMap, distanceManager, planet),
                    (int) planet.getHealth(),
                    Objective.OrderType.CRASHINTO,
                    false,
                    this.objectiveId++
                );

            else
                continue;

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getAttackObjectives(final GameMap gameMap, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        List<Ship> ships = gameMap.getAllShips();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : ships)
        {
            Objective objective;

            // If own ship, do nothing
            if (ship.getOwner() == myId)
                continue;

            // Then attack docking/undocking/docked ships in priority
            else if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                objective = new Objective(
                    ship,
                    behaviourManager.getDockedShipPriorityForAttack(gameMap, distanceManager, ship),
                    2,
                    Objective.OrderType.ATTACK,
                    false,
                    this.objectiveId++
                );

            // Go for other ships
            else
                objective = new Objective(
                    ship,
                    behaviourManager.getShipPriorityForAttack(gameMap, distanceManager, ship),
                    2,
                    Objective.OrderType.ATTACK,
                    false,
                    this.objectiveId++
                );

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getDefendObjectives(final GameMap gameMap, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Map<Integer, Planet> planets = gameMap.getAllPlanets();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Planet planet : planets.values())
        {
            Objective objective;

            if (planet.getOwner() == myId)
                objective = new Objective(
                    planet,
                    behaviourManager.getPlanetPriorityForDefend(gameMap, distanceManager, planet),
                    5,
                    Objective.OrderType.DEFEND,
                    false,
                    this.objectiveId++
                );
            else
                continue;

            objectives.add(objective);
        }

        return objectives;
    }

    public void clearObjectives()
    {
        this.objectives.clear();
        this.objectiveId = 0;
    }
    private void sortObjectives(final ArrayList<Objective> objectives)  { objectives.sort(Comparator.comparingDouble(Objective::getPriority).reversed()); }
    private void logObjectives()
    {
        for(final Objective objective: this.objectives)
            DebugLog.addLog(objective.toString());
    }
    private void removeZeroPriorityObjectives()
    {
        ArrayList<Objective> filteredObjectives = new ArrayList<>();

        for(final Objective objective: this.objectives)
            if (objective.getPriority() != 0)
                filteredObjectives.add(objective);

        this.objectives = filteredObjectives;
    }

}
