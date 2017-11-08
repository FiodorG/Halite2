package core;

import Jama.Matrix;
import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import java.lang.reflect.Array;
import java.util.*;

public class DistanceManager
{
    private Matrix distanceMatrixPlanetPlanet;
    private Matrix distanceMatrixPlanetShip;
    private Matrix distanceMatrixShipShip;
    private Matrix distanceMatrixObjectiveShip;

    private List<Planet> planets;
    private List<Ship> ships;
    private List<Objective> objectives;

    private Vector<Integer> planetIDs;
    private Vector<Integer> shipIDs;
    private Vector<Integer> objectiveIDs;

    private int numberOfPlanets;
    private int numberOfShips;

    public void computeDistanceMatrices(final GameMap gameMap)
    {
        fillIndices(gameMap);

        computeDistanceMatrixPlanetPlanet();
        computeDistanceMatrixPlanetShip();
        computeDistanceMatrixShipShip();
    }

    private void fillIndices(final GameMap gameMap)
    {
        this.planets = new ArrayList<>(gameMap.getAllPlanets().values());
        this.ships = gameMap.getAllShips();

        this.shipIDs = new Vector<>();
        for(final Ship ship: this.ships)
            this.shipIDs.add(ship.getId());

        this.planetIDs = new Vector<>();
        for(final Planet planet: this.planets)
            this.planetIDs.add(planet.getId());

        this.numberOfPlanets = this.planets.size();
        this.numberOfShips = this.ships.size();
    }

    private void computeDistanceMatrixPlanetPlanet()
    {
        this.distanceMatrixPlanetPlanet = new Matrix(this.numberOfPlanets, this.numberOfPlanets);

        int i = 0;
        for(final Planet planet_i : this.planets)
        {
            int j = 0;
            for(final Planet planet_j : this.planets)
            {
                if (j == i)
                    break;

                double distance = planet_i.getDistanceTo(planet_j);
                this.distanceMatrixPlanetPlanet.set(i, j, distance);
                this.distanceMatrixPlanetPlanet.set(j, i, distance);
                j++;
            }
            i++;
        }
    }

    private void computeDistanceMatrixPlanetShip()
    {
        this.distanceMatrixPlanetShip = new Matrix(this.numberOfPlanets, this.numberOfShips);

        int i = 0;
        for(final Planet planet_i : this.planets)
        {
            int j = 0;
            for(final Ship ship_j : this.ships)
            {
                this.distanceMatrixPlanetShip.set(i, j, planet_i.getDistanceTo(ship_j));
                j++;
            }
            i++;
        }
    }

    private void computeDistanceMatrixShipShip()
    {
        this.distanceMatrixShipShip = new Matrix(this.numberOfShips, this.numberOfShips);

        int i = 0;
        for(final Ship ship_i : this.ships)
        {
            int j = 0;
            for(final Ship ship_j : this.ships)
            {
                if (j == i)
                    break;

                double distance = ship_i.getDistanceTo(ship_j);
                this.distanceMatrixShipShip.set(i, j, distance);
                this.distanceMatrixShipShip.set(j, i, distance);
                j++;
            }
            i++;
        }
    }

    public void computeObjectivesDistanceMatrices(final GameMap gameMap, final ArrayList<Objective> objectives)
    {
        this.objectives = objectives;
        this.distanceMatrixObjectiveShip = new Matrix(objectives.size(), this.numberOfShips);

        this.objectiveIDs = new Vector<>();
        for(final Objective objective: this.objectives)
            this.objectiveIDs.add(objective.getId());

        int i = 0;
        for(final Objective objective: objectives)
        {
            int j = 0;
            for(final Ship ship : this.ships)
            {
                this.distanceMatrixObjectiveShip.set(i, j, objective.getTargetEntity().getDistanceTo(ship));
                j++;
            }
            i++;
        }
    }

    public Ship getClosestShipFromPlanet(final GameMap gameMap, final Planet planet, final ArrayList<Ship> ships)
    {
        // This is O(n^2), need to rework

        int index = this.planetIDs.indexOf(planet.getId());

        Ship closestShip = ships.get(0);
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < this.numberOfShips; i++)
        {
            if (ships.contains(this.ships.get(i)))
            {
                if (this.distanceMatrixPlanetShip.get(index, i) < minDistance)
                {
                    minDistance = this.distanceMatrixPlanetShip.get(index, i);
                    closestShip = this.ships.get(i);
                }
            }
        }

        return closestShip;
    }

    public Ship getClosestShipFromShip(final GameMap gameMap, final Ship targetShip, final ArrayList<Ship> ships)
    {
        // This is O(n^2), need to rework
        int index = this.shipIDs.indexOf(targetShip.getId());

        Ship closestShip = ships.get(0);
        double minDistance = Double.MAX_VALUE;
        for(int i = 0; i < this.numberOfShips; i++)
        {
            if (ships.contains(this.ships.get(i)))
            {
                if(this.distanceMatrixShipShip.get(index, i) < minDistance)
                {
                    minDistance = this.distanceMatrixShipShip.get(index, i);
                    closestShip = this.ships.get(i);
                }
            }
        }

        return closestShip;
    }

    public HashMap<Objective, Double> getClosestObjectiveFromShip(final ArrayList<Objective> objectives, final Ship ship, final int numberOfClosest)
    {
        HashMap<Objective, Double> distances = new HashMap<>();
        for(int i = 0; i < objectives.size(); i++)
            distances.put(objectives.get(i), ship.getDistanceTo(objectives.get(i).getTargetEntity()));

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

    public double averageDistanceFromShips(final Planet planet, final int id)
    {
        int index = this.planetIDs.indexOf(planet.getId());
        double distances = 0;
        int numberOfShips = 0;

        for (int i = 0; i < this.numberOfShips; i++)
        {
            if(this.ships.get(i).getOwner() == id)
            {
                distances += this.distanceMatrixPlanetShip.get(index, i);
                numberOfShips += 1;
            }
        }

        return distances / numberOfShips;
    }

    public Planet findPlanetFromID(final int ID)
    {
        return this.planets.get(this.planetIDs.indexOf(ID));
    }
}
