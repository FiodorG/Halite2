package core;

import hlt.*;

import java.lang.reflect.Array;
import java.util.*;

public class DistanceManager
{
    public static class EntityAndDistance
    {
        private Entity entity;
        private double distance;

        public EntityAndDistance(final Entity entity, final double distance)
        {
            this.entity = entity;
            this.distance = distance;
        }

        public double getDistance() { return distance; }
        public Entity getEntity() { return entity; }
    }

    private HashMap<Integer, TreeSet<EntityAndDistance>> distanceMatrixShipPlanet;
    private HashMap<Integer, TreeSet<EntityAndDistance>> distanceMatrixShipShip;

    private List<Planet> planets;
    private List<Ship> myShips;
    private List<Ship> enemyShips;

    public void computeDistanceMatrices(final GameState gameState)
    {
        this.planets = new ArrayList<>(gameState.getGameMap().getAllPlanets().values());
        this.myShips = new ArrayList<>(gameState.getMyShips());
        this.enemyShips = new ArrayList<>(gameState.getEnemyShips());

        computeDistanceMatrixShipPlanet();
        computeDistanceMatrixShipShip();
    }

    private void computeDistanceMatrixShipPlanet()
    {
        this.distanceMatrixShipPlanet = new HashMap<>();

        for(final Ship ship_i : this.myShips)
        {
            this.distanceMatrixShipPlanet.put(ship_i.getId(), new TreeSet<>(Comparator.comparingDouble(EntityAndDistance::getDistance)));

            for (final Planet planet_j : this.planets)
                this.distanceMatrixShipPlanet.get(ship_i.getId()).add(new EntityAndDistance(planet_j, ship_i.getDistanceTo(planet_j)));
        }
    }

    private void computeDistanceMatrixShipShip()
    {
        this.distanceMatrixShipShip = new HashMap<>();

        for(final Ship ship_i : this.myShips)
        {
            this.distanceMatrixShipShip.put(ship_i.getId(), new TreeSet<>(Comparator.comparingDouble(EntityAndDistance::getDistance)));

            for(final Ship ship_j : this.enemyShips)
                this.distanceMatrixShipShip.get(ship_i.getId()).add(new EntityAndDistance(ship_j, ship_i.getDistanceTo(ship_j)));
        }
    }

    public double getClosestEnemyShipDistance(final Ship ship)
    {
        return this.distanceMatrixShipShip.get(ship.getId()).first().getDistance();
    }

    public Ship getClosestEnemyShip(final Entity entity)
    {
        double minDistance = Double.MAX_VALUE;
        Ship closestShip = this.enemyShips.get(0);

        for(final Ship enemyShip: this.enemyShips)
        {
            double distance = entity.getDistanceTo(enemyShip);
            if (distance < minDistance)
            {
                minDistance = distance;
                closestShip = enemyShip;
            }
        }

        return closestShip;
    }

    public ArrayList<Ship> getEnemiesCloserThan(final Entity entity, final double minDistance)
    {
        ArrayList<Ship> closeEnemyShips = new ArrayList<>();
        for(final Ship enemyShip: this.enemyShips)
            if (enemyShip.getDistanceTo(entity) < minDistance)
                closeEnemyShips.add(enemyShip);

        return closeEnemyShips;
    }

    public Ship getClosestAllyShip(final Ship ship)
    {
        double minDistance = Double.MAX_VALUE;
        Ship closestShip = ship;

        for(final Ship alliedShip: this.myShips)
        {
            double distance = alliedShip.getDistanceTo(ship);
            if ((alliedShip != ship) && (distance < minDistance))
            {
                minDistance = distance;
                closestShip = alliedShip;
            }
        }

        return closestShip;
    }

    public Ship getClosestAllyShipFromFleet(final Fleet fleet)
    {
        double minDistance = Double.MAX_VALUE;
        Ship closestShip = fleet.getShips().get(0);

        for(final Ship alliedShip: this.myShips)
        {
            double distance = alliedShip.getDistanceTo(fleet.getCentroid());
            if (!fleet.getShips().contains(alliedShip) && (distance < minDistance))
            {
                minDistance = distance;
                closestShip = alliedShip;
            }
        }

        return closestShip;
    }

    public static HashMap<Objective, Double> getClosestObjectiveFromEntity(final ArrayList<Objective> objectives, final Entity entity, final int numberOfClosest)
    {
        HashMap<Objective, Double> distances = new HashMap<>();
        for(int i = 0; i < objectives.size(); i++)
            distances.put(objectives.get(i), entity.getDistanceTo(objectives.get(i).getTargetEntity()));

        objectives.sort((i, j) -> distances.get(i) <= distances.get(j)? -1:1);

        HashMap<Objective, Double> closestObjectives = new HashMap<>();
        for(int i = 0; i < numberOfClosest; i++)
        {
            if (i < objectives.size())
                closestObjectives.put(objectives.get(i), distances.get(objectives.get(i)));
            else
                break;
        }

        return closestObjectives;
    }

    public static HashMap<Fleet, Double> getClosestFleetsFromShip(final ArrayList<Fleet> fleets, final Entity entity, final int numberOfClosest)
    {
        HashMap<Fleet, Double> distances = new HashMap<>();
        for(final Fleet fleet: fleets)
            distances.put(fleet, entity.getDistanceTo(fleet.getCentroid()));

        fleets.sort((i, j) -> distances.get(i) <= distances.get(j)? -1:1);

        HashMap<Fleet, Double> closestFleets = new HashMap<>();
        for(int i = 0; i < numberOfClosest; i++)
        {
            if (i < fleets.size())
                closestFleets.put(fleets.get(i), distances.get(fleets.get(i)));
            else
                break;
        }

        return closestFleets;
    }

    public static Position computeStartingPoint(final Collection<Ship> ships)
    {
        double x = 0;
        double y = 0;

        for(final Ship ship: ships)
        {
            x += ship.getXPos();
            y += ship.getYPos();
        }

        return new Position(x / ships.size(), y / ships.size());
    }
}
