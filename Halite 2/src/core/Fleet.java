package core;

import hlt.Ship;

import java.util.ArrayList;

public class Fleet
{
    private ArrayList<Ship> ships;
    private ArrayList<Objective> objectives;

    public ArrayList<Ship> getShips() { return ships; }
    public ArrayList<Objective> getObjectives() { return objectives; }

    public Fleet(final Ship ship, final Objective objective)
    {
        this.ships = new ArrayList<>();
        this.ships.add(ship);

        this.objectives = new ArrayList<>();
        this.objectives.add(objective);
    }

    @Override
    public String toString()
    {
        return "Fleet[" +
                "Objectives<" + this.objectives.toString() + ">," +
                "Ships<" + this.ships.toString() + ">" +
                "]";
    }
}
