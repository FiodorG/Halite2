package core;

import hlt.Entity;
import hlt.Ship;

import java.util.ArrayList;
import java.util.Objects;

public class Fleet
{
    private ArrayList<Ship> ships;
    private ArrayList<Objective> objectives;
    private final int id;

    public ArrayList<Ship> getShips() { return ships; }
    public ArrayList<Objective> getObjectives() { return objectives; }
    public int getId() { return id; }

    public Fleet(final Ship ship, final Objective objective, final int id)
    {
        this.ships = new ArrayList<>();
        this.ships.add(ship);

        this.objectives = new ArrayList<>();
        this.objectives.add(objective);

        this.id = id;
    }

    public void addShip(final Ship ship) { this.ships.add(ship); }

    public Entity FleetCentroid()
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

        return new Entity(this.ships.get(0).getOwner(), 0, meanX, meanY, 0, 0);
    }

    public int reinforcementNeed()
    {
        if (this.getObjectives().isEmpty())
            // Fleets are 2 ships minimum
            return Math.max(2 - this.getShips().size(), 0);
        else
            return Math.max(this.getObjectives().get(0).getRequiredShips() - this.getShips().size(), 0);
    }

    @Override
    public String toString()
    {
        return "Fleet" + this.id +
                "<Objectives(" + this.objectives.size() + ")<" + this.objectives.toString() + ">," +
                "Ships(" + this.ships.size() + ")<" + this.ships.toString() + ">" +
                ">";
    }

    @Override
    public int hashCode() { return Objects.hash(this.id); }
}
