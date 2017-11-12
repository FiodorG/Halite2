package core;

import hlt.Entity;
import hlt.Position;
import hlt.Ship;
import javafx.geometry.Pos;

import java.util.ArrayList;

public class Fleet
{
    private ArrayList<Ship> ships;
    private ArrayList<Objective> objectives;

    private Entity centroid;

    public ArrayList<Ship> getShips() { return ships; }
    public ArrayList<Objective> getObjectives() { return objectives; }
    public Entity getCentroid() { return centroid; }

    public Fleet(final Ship ship, final Objective objective)
    {
        this.ships = new ArrayList<>();
        this.ships.add(ship);

        this.objectives = new ArrayList<>();
        this.objectives.add(objective);
    }

    public void addShip(final Ship ship) { this.ships.add(ship); }

    public void computeFleetCentroid()
    {
        double sumWeight = 0;
        double meanX = 0;
        double meanY = 0;

        for(final Ship ship: this.ships)
        {
            meanX += ship.getXPos() * ship.getHealth();
            meanY += ship.getYPos() * ship.getHealth();
            sumWeight += ship.getHealth();
        }

        meanX /= sumWeight;
        meanY /= sumWeight;

        this.centroid = new Entity(this.ships.get(0).getOwner(), 0, meanX, meanY, 0, 0);
    }

    @Override
    public String toString()
    {
        return "Fleet[" +
                "Objectives(" + this.objectives.size() + ")<" + this.objectives.toString() + ">," +
                "Ships(" + this.ships.size() + ")<" + this.ships.toString() + ">" +
                "]";
    }
}
