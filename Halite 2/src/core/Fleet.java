package core;

import hlt.Entity;
import hlt.Ship;

import java.util.ArrayList;
import java.util.Objects;

public class Fleet extends Entity
{
    private ArrayList<Ship> ships;
    private ArrayList<Objective> objectives;
    private Entity centroid;

    private int reinforcementNeed;

    public ArrayList<Ship> getShips() { return ships; }
    public ArrayList<Objective> getObjectives() { return objectives; }
    public Entity getCentroid() { return centroid; }

    public int getReinforcementNeed() { return reinforcementNeed; }
    public void decreaseReinforcementNeed() { this.reinforcementNeed--; }

    public void addObjective(final Objective objective) { this.objectives.add(objective); }

    public Fleet(final Ship ship, final Objective objective, final int id)
    {
        super(ship.getOwner(), id, ship.getXPos(), ship.getYPos(), ship.getHealth(), ship.getRadius());

        this.ships = new ArrayList<>();
        this.ships.add(ship);
        this.centroid = ship;

        this.objectives = new ArrayList<>();
        if (objective != null)
            this.objectives.add(objective);

        this.reinforcementNeed = 1;
    }

    public Fleet(final ArrayList<Ship> ships, final ArrayList<Objective> objectives, final int id)
    {
        super(ships.get(0).getOwner(), id, ships.get(0).getXPos(), ships.get(0).getYPos(), ships.get(0).getHealth(), ships.get(0).getRadius());

        this.ships = new ArrayList<>();
        this.ships.addAll(ships);
        computeCentroid();

        this.objectives = new ArrayList<>(objectives);
        this.reinforcementNeed = 1;
    }

    public void addShip(final Ship ship)
    {
        this.ships.add(ship);
        computeCentroid();
    }

    public void setUnderAttackMode()
    {
        this.reinforcementNeed = 2;
    }

    public double priorityReinforcementNeed()
    {
        return getReinforcementNeed() * 100.0;
    }

    public void computeCentroid()
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
    public double getRadius()
    {
        Entity centroid = getCentroid();

        double radius = 0;
        for(final Ship ship: this.ships)
        {
            double shipRadius = ship.getDistanceTo(centroid) + ship.getRadius() + 0.1;
            if (shipRadius > radius)
                radius = shipRadius;
        }

        return radius;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;

        if (this == object)
            return true;

        Fleet fleet = (Fleet) object;
        return (this.getId() == (fleet.getId()));
    }

    @Override
    public String toString()
    {
        return "Fleet" + this.getId() +
                "<Objectives(" + this.objectives.size() + ")<" + this.objectives.toString() + ">," +
                "Ships(" + this.ships.size() + ")<" + this.ships.toString() + ">" +
                ">";
    }

    @Override
    public int hashCode() { return Objects.hash(this.getId()); }
}
