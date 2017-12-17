package core;

import hlt.*;

import java.util.*;

public class ObjectiveManager
{
    private ArrayList<Objective> objectives;
    private ArrayList<Objective> undockObjectives;
    private ArrayList<Objective> superObjectives;
    private int objectiveId;

    public ObjectiveManager()
    {
        this.objectives = new ArrayList<>();
        this.undockObjectives = new ArrayList<>();
        this.superObjectives = new ArrayList<>();
        this.objectiveId = 0;
    }

    public ArrayList<Objective> getObjectives() { return objectives; }
    public ArrayList<Objective> getSuperObjectives() { return superObjectives; }
    public ArrayList<Objective> getUndockObjectives() { return undockObjectives; }

    public void getObjectives(final GameState gameState)
    {
        clearObjectives();

        DistanceManager distanceManager = gameState.getDistanceManager();
        BehaviourManager behaviourManager = gameState.getBehaviourManager();

        getRushObjectives(gameState, distanceManager, behaviourManager);
        getAntiRushObjectives(gameState, distanceManager, behaviourManager);
        getColonizeObjectives(gameState, distanceManager, behaviourManager);
        getAttackObjectives(gameState, distanceManager, behaviourManager);
        getDefendObjectives(gameState, distanceManager, behaviourManager);
        getUndockObjectives(gameState, distanceManager, behaviourManager);

        getAssassinationObjectives(gameState, distanceManager, behaviourManager);
        getLureObjectives(gameState, distanceManager, behaviourManager);
        getFleeObjectives(gameState, distanceManager, behaviourManager);
        getHideInCornerObjectives(gameState, distanceManager, behaviourManager);

        removeZeroPriorityObjectives();
        sortObjectives(this.objectives);
        logObjectives();
    }

    private void getRushObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        if (!behaviourManager.isValidTurnForRush(gameState))
            return;

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
                        true,
                        this.objectiveId++
                );

            else
                continue;

            superObjectives.add(objective);
        }
    }

    private void getAntiRushObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        if (!behaviourManager.isValidTurnForRush(gameState))
            return;

        for(final Ship ship : gameState.getEnemyShips())
        {
            Objective objective;

            // Antirush anything that goes too close
            objective = new Objective(
                ship,
                behaviourManager.getShipPriorityForAntiRush(gameState, distanceManager, ship),
                2,
                Objective.OrderType.ANTIRUSH,
                false,
                true,
                this.objectiveId++
            );

            objectives.add(objective);
        }
    }

    private void getColonizeObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Map<Integer, Planet> planets = gameState.getGameMap().getAllPlanets();

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
                    false,
                    this.objectiveId++
                );

            else
                continue;

            objectives.add(objective);
        }
    }

    private void getAttackObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        for(final Ship ship : gameState.getEnemyShips())
        {
            Objective objective;

            // Then attack docking/undocking/docked ships in priority
            if (!ship.isUndocked())
                objective = new Objective(
                    ship,
                    behaviourManager.getDockedShipPriorityForAttack(gameState, distanceManager, ship),
                    behaviourManager.getRequiredShipsForAttack(gameState, distanceManager, ship),
                    Objective.OrderType.ATTACKDOCKED,
                    false,
                    true,
                    this.objectiveId++
                );

            // Go for other ships
            else
                objective = new Objective(
                    ship,
                    behaviourManager.getShipPriorityForAttack(gameState, distanceManager, ship),
                    behaviourManager.getRequiredShipsForAttack(gameState, distanceManager, ship),
                    Objective.OrderType.ATTACK,
                    false,
                    true,
                    this.objectiveId++
                );

            objectives.add(objective);
        }
    }

    private void getDefendObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
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
                    true,
                    this.objectiveId++
                );

            else
                continue;

            objectives.add(objective);
        }
    }

    private void getUndockObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        for(final Ship ship : gameState.getMyShips())
        {
            Objective objective;

            if (!ship.isUndocked())
                objective = new Objective(
                        ship,
                        behaviourManager.getShipPriorityForUndock(gameState, distanceManager, ship),
                        1,
                        Objective.OrderType.UNDOCK,
                        false,
                        false,
                        this.objectiveId++
                );

            else
                continue;

            undockObjectives.add(objective);
        }
    }

    private void getAssassinationObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Map<Integer, Planet> planets = gameState.getGameMap().getAllPlanets();

        int numberOfAssassinationObjectives = behaviourManager.getNumberOfAssassinationObjectives();

        for(final Planet planet : planets.values())
        {
            Objective objective;

            if ((planet.getOwner() != gameState.getMyId()) && planet.isFull())
            {
                double priority = behaviourManager.getPlanetPriorityForAssassination(gameState, planet);
                if (priority > 0)
                {
                    objective = new Objective(
                            planet,
                            priority,
                            1,
                            Objective.OrderType.ASSASSINATION,
                            true,
                            true,
                            this.objectiveId++
                    );
                    superObjectives.add(objective);
                    numberOfAssassinationObjectives--;
                    if (numberOfAssassinationObjectives == 0)
                        return;
                }
            }
        }
    }

    private void getLureObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Objective objective;

        // Try docked ships...
        for(final Ship ship : gameState.getEnemyShips())
        {
            if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
            {
                objective = new Objective(
                        ship,
                        behaviourManager.getPlanetPriorityForLure(gameState, ship),
                        1,
                        Objective.OrderType.LURE,
                        true,
                        false,
                        this.objectiveId++
                );
                this.superObjectives.add(objective);
                return;
            }
        }

        // ... Otherwise, go for non-docked ones.
        for(final Ship ship : gameState.getEnemyShips())
        {
            objective = new Objective(
                    ship,
                    behaviourManager.getPlanetPriorityForLure(gameState, ship),
                    1,
                    Objective.OrderType.LURE,
                    true,
                    false,
                    this.objectiveId++
            );
            this.superObjectives.add(objective);
            return;
        }
    }

    private void getFleeObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Objective objective;

        for (final Entity corner: gameState.getCorners())
        {
            objective = new Objective(
                    corner,
                    behaviourManager.getCornerPriorityForFlee(gameState, corner),
                    100,
                    Objective.OrderType.FLEE,
                    true,
                    true,
                    this.objectiveId++
            );
            this.superObjectives.add(objective);
        }
    }

    private void getHideInCornerObjectives(final GameState gameState, final DistanceManager distanceManager, final BehaviourManager behaviourManager)
    {
        Objective objective = new Objective(
            gameState.getCornerCloserToStart(),
            behaviourManager.getCornerPriorityForHide(gameState),
            1,
            Objective.OrderType.HIDEINCORNER,
            true,
            false,
            this.objectiveId++
        );

        superObjectives.add(objective);
    }

    public void clearObjectives()
    {
        this.objectives.clear();
        this.undockObjectives.clear();
        this.superObjectives.clear();
    }
    private void sortObjectives(final ArrayList<Objective> objectives)  { objectives.sort(Comparator.comparingDouble(Objective::getPriority).reversed()); }
    private void logObjectives()
    {
        for(final Objective objective: this.superObjectives)
            DebugLog.addLog(objective.toString());
        for(final Objective objective: this.objectives)
            DebugLog.addLog(objective.toString());
        for(final Objective objective: this.undockObjectives)
            DebugLog.addLog(objective.toString());
        DebugLog.addLog("");
    }
    private void removeZeroPriorityObjectives()
    {
        this.objectives = removeZeroPriorityObjectivesInternal(this.objectives);
        this.superObjectives = removeZeroPriorityObjectivesInternal(this.superObjectives);
        this.undockObjectives = removeZeroPriorityObjectivesInternal(this.undockObjectives);
    }

    private ArrayList<Objective> removeZeroPriorityObjectivesInternal(ArrayList<Objective> objectives)
    {
        ArrayList<Objective> filteredObjectives = new ArrayList<>();

        for(final Objective objective: objectives)
            if (objective.getPriority() != 0)
                filteredObjectives.add(objective);

        return filteredObjectives;
    }
}
