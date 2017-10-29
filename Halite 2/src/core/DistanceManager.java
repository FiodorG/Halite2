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
    private Matrix distanceMatrix;
    private List<Entity> entities;

    public Matrix getDistanceMatrix() { return distanceMatrix; }

    public void computeDistanceMatrix(final GameMap gameMap)
    {
        List<Planet> planets = new ArrayList<>(gameMap.getAllPlanets().values());
        List<Ship> ships = gameMap.getAllShips();

        this.entities = new ArrayList<>(planets);
        this.entities.addAll(ships);

        int matrixSize = this.entities.size();
        this.distanceMatrix = new Matrix(matrixSize, matrixSize);

        int i = 0;
        for(final Entity entity_i : this.entities)
        {
            int j = 0;
            for(final Entity entity_j : this.entities)
            {
                if (j == i)
                    break;

                this.distanceMatrix.set(i, j, entity_i.getDistanceTo(entity_j));
                j++;
            }
            i++;
        }
    }
}
