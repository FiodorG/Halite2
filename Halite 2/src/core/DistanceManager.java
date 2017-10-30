package core;

import Jama.Matrix;
import hlt.Entity;
import hlt.GameMap;
import hlt.Planet;
import hlt.Ship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DistanceManager
{
    private Matrix distanceMatrixPlanetPlanet;
    private Matrix distanceMatrixPlanetShip;
    private Matrix distanceMatrixShipShip;

    private List<Planet> planets;
    private List<Ship> ships;

    private int numberOfPlanets;
    private int numberOfShips;

    public Matrix getdistanceMatrixPlanetPlanet() { return distanceMatrixPlanetPlanet; }
    public Matrix getdistanceMatrixPlanetShip() { return distanceMatrixPlanetShip; }
    public Matrix getdistanceMatrixShipShip() { return distanceMatrixShipShip; }

    public void computeDistanceMatrices(final GameMap gameMap)
    {
        this.planets = new ArrayList<>(gameMap.getAllPlanets().values());
        this.ships = gameMap.getAllShips();

        computeDistanceMatrixPlanetPlanet();
        computeDistanceMatrixPlanetShip();
        computeDistanceMatrixShipShip();
    }

    private void computeDistanceMatrixPlanetPlanet()
    {
        this.numberOfPlanets = this.planets.size();
        this.distanceMatrixPlanetPlanet = new Matrix(this.numberOfPlanets, this.numberOfPlanets);

        int i = 0;
        for(final Planet planet_i : this.planets)
        {
            int j = 0;
            for(final Planet planet_j : this.planets)
            {
                if (j == i)
                    break;

                this.distanceMatrixPlanetPlanet.set(i, j, planet_i.getDistanceTo(planet_j));
                j++;
            }
            i++;
        }
    }

    private void computeDistanceMatrixPlanetShip()
    {
        this.numberOfPlanets = this.planets.size();
        this.numberOfShips = this.ships.size();
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
        this.numberOfShips = this.ships.size();
        this.distanceMatrixShipShip = new Matrix(this.numberOfShips, this.numberOfShips);

        int i = 0;
        for(final Ship ship_i : this.ships)
        {
            int j = 0;
            for(final Ship ship_j : this.ships)
            {
                if (j == i)
                    break;

                this.distanceMatrixShipShip.set(i, j, ship_i.getDistanceTo(ship_j));
                j++;
            }
            i++;
        }
    }

    public Ship getClosestShipFromPlanet(final GameMap gameMap, final Planet planet, final ArrayList<Ship> ships)
    {
        // This is O(n^2), need to rework

        int index = this.planets.indexOf(planet);

        Ship closestShip = ships.get(0);
        double minDistance = Double.MAX_VALUE;
        for(int i = 0; i < this.numberOfShips; i++)
        {
            if (ships.contains(this.ships.get(i)))
            {
                if(this.distanceMatrixPlanetShip.get(index, i) < minDistance)
                {
                    minDistance = this.distanceMatrixPlanetShip.get(index, i);
                    closestShip = this.ships.get(i);
                }
            }
        }

        return closestShip;
    }
}
