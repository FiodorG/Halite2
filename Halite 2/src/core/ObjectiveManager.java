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

    public void getObjectives(final GameState gameState)
    {
        clearObjectives();

        DistanceManager distanceManager = gameState.getDistanceManager();
        BehaviourManager behaviourManager = gameState.getBehaviourManager();

        //this.objectives.addAll(getRushObjectives(gameMap, distanceManager, behaviourManager));
        this.objectives.addAll(getAntiRushObjectives(gameState, distanceManager, behaviourManager));
        this.objectives.addAll(getColonizeObjectives(gameState, distanceManager, behaviourManager));
        this.objectives.addAll(getAttackObjectives(gameState, distanceManager, behaviourManager));
        this.objectives.addAll(getDefendObjectives(gameState, distanceManager, behaviourManager));

        removeZeroPriorityObjectives();
        sortObjectives(this.objectives);
        logObjectives();
    }

    private ArrayList<Objective> getRushObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        if (!behaviourManager.isValidTurnForRush(gameState))
            return new ArrayList<>();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : gameState.getEnemyShips())
        {
            Objective objective;

            // Then attack docking/undocking/docked ships in priority
            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                objective = new Objective(
                    ship,
                    behaviourManager.getDockedShipPriorityForRush(gameState, distanceManager, ship),
                    behaviourManager.getRushShipsPerObjective(gameState, distanceManager, ship),
                    Objective.OrderType.RUSH,
                    true,
                    this.objectiveId++
                );

            else if (ship.getDockingStatus() == Ship.DockingStatus.Undocked)
                objective = new Objective(
                        ship,
                        behaviourManager.getUndockedShipPriorityForRush(gameState, distanceManager, ship),
                        behaviourManager.getRushShipsPerObjective(gameState, distanceManager, ship),
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

    private ArrayList<Objective> getAntiRushObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        if (!behaviourManager.isValidTurnForRush(gameState))
            return new ArrayList<>();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : gameState.getEnemyShips())
        {
            Objective objective;

            // Then antirush anything that goes too close
            objective = new Objective(
                ship,
                behaviourManager.getShipPriorityForAntiRush(gameState, distanceManager, ship),
                2,
                Objective.OrderType.ANTIRUSH,
                false,
                this.objectiveId++
            );

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getColonizeObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Map<Integer, Planet> planets = gameState.getGameMap().getAllPlanets();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Planet planet : planets.values())
        {
            Objective objective;

            // First make sure we fill slots in owned planets
            if ((planet.getOwner() == gameState.getMyId()) && !planet.isFull())
                objective = new Objective(
                    planet,
                    behaviourManager.getPlanetPriorityForReinforceColony(gameState, planet),
                    planet.getFreeDockingSpots(),
                    Objective.OrderType.REINFORCECOLONY,
                    false,
                    this.objectiveId++
                );

            // Then get new planets
            else if (!planet.isOwned())
                objective = new Objective(
                    planet,
                    behaviourManager.getPlanetPriorityForColonize(gameState, planet),
                    planet.getDockingSpots(),
                    Objective.OrderType.COLONIZE,
                    false,
                    this.objectiveId++
                );

            else
                continue;

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getAttackObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : gameState.getEnemyShips())
        {
            Objective objective;

            // Then attack docking/undocking/docked ships in priority
            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                objective = new Objective(
                    ship,
                    behaviourManager.getDockedShipPriorityForAttack(gameState, distanceManager, ship),
                    2,
                    Objective.OrderType.ATTACK,
                    false,
                    this.objectiveId++
                );

            // Go for other ships
            else
                objective = new Objective(
                    ship,
                    behaviourManager.getShipPriorityForAttack(gameState, distanceManager, ship),
                    2,
                    Objective.OrderType.ATTACK,
                    false,
                    this.objectiveId++
                );

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getDefendObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : gameState.getMyShips())
        {
            Objective objective;

            // Look for own ships under attack
            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                objective = new Objective(
                    ship,
                    behaviourManager.getShipPriorityForDefend(gameState, distanceManager, ship),
                    1,
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
    }
    private void sortObjectives(final ArrayList<Objective> objectives)  { objectives.sort(Comparator.comparingDouble(Objective::getPriority).reversed()); }
    private void logObjectives()
    {
        for(final Objective objective: this.objectives)
            DebugLog.addLog(objective.toString());
        DebugLog.addLog("");
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
