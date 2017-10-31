package core;

import hlt.*;
import java.util.*;

public class ObjectiveManager
{
    private ArrayList<Objective> objectives;
    private BehaviourManager behaviourManager;

    public ObjectiveManager(final BehaviourManager.behaviourTypes behaviour)
    {
        this.behaviourManager = new BehaviourManager(behaviour);
        this.objectives = new ArrayList<>();
    }

    public ArrayList<Objective> getObjectives() { return objectives; }

    public void getObjectives(final GameMap gameMap, final DistanceManager distanceManager)
    {
        clearObjectives();

        this.objectives.addAll(getColonizeObjectives(gameMap, distanceManager));
        //this.objectives.addAll(getAttackObjectives(gameMap, distanceManager));

        sortObjectives();
        logObjectives();
    }

    private ArrayList<Objective> getColonizeObjectives(final GameMap gameMap, final DistanceManager distanceManager)
    {
        Map<Integer, Planet> planets = gameMap.getAllPlanets();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Planet planet : planets.values())
        {
            Objective objective;

            // First make sure we fill slots in owned planets
            if (planet.getOwner() == myId && !planet.isFull())
                objective = new Objective(planet, 2 * getPlanetPriority(planet), planet.getFreeDockingSpots(), Objective.OrderType.COLONIZE);

            // Then get new planets
            else if (!planet.isOwned())
                objective = new Objective(planet, getPlanetPriority(planet), planet.getDockingSpots(), Objective.OrderType.COLONIZE);

            // Then crash into full enemy planets
            else if (planet.getOwner() != myId)
                objective = new Objective(planet, 200, (int)(planet.getRadius() * 2), Objective.OrderType.CRASHINTO);

            else
                continue;

            objectives.add(objective);
        }

        return objectives;
    }

    private ArrayList<Objective> getAttackObjectives(final GameMap gameMap, final DistanceManager distanceManager)
    {
        List<Ship> ships = gameMap.getAllShips();
        final int myId = gameMap.getMyPlayerId();

        ArrayList<Objective> objectives = new ArrayList<>();
        for(final Ship ship : ships)
        {
            Objective objective;

            // If own ship, don't do anything
            if (ship.getOwner() == myId)
                continue;

            // Then go for docked ships
            else if (ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                objective = new Objective(ship, 75, 2, Objective.OrderType.ATTACK);

            // Go for other ships
            else
                objective = new Objective(ship, 50, 2, Objective.OrderType.ATTACK);

            objectives.add(objective);
        }

        return objectives;
    }

    public void clearObjectives() { this.objectives.clear(); }
    private void sortObjectives()  { this.objectives.sort(Comparator.comparingDouble(Objective::getPriority).reversed()); }
    private void logObjectives()
    {
        for(final Objective objective: this.objectives)
            DebugLog.addLog(objective.toString());
    }

    private int getPlanetPriority(Planet planet)
    {
        return planet.getDockingSpots() * 100;
    }
}
