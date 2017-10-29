package core;

import hlt.Ship;

import java.util.ArrayList;

public class Fleet
{
    private ArrayList<Ship> ships;
    private ArrayList<Objective> objectives = new ArrayList<>();

    public ArrayList<Ship> getShips() { return ships; }
    public ArrayList<Objective> getObjectives() { return objectives; }

    public Fleet(final ArrayList<Ship> ships, final Objective objective)
    {
        this.ships = ships;
        this.objectives.add(objective);
    }
}
