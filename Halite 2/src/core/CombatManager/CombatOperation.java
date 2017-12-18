package core.CombatManager;

import core.Fleet;
import core.Objective;
import hlt.Entity;
import hlt.Ship;

import java.util.ArrayList;

public class CombatOperation
{
    private Objective objective;
    private ArrayList<Ship> myActiveShips;
    private ArrayList<Fleet> myActiveFleets;

    private int id;

    public CombatOperation(final Objective objective, final ArrayList<Ship> myActiveShips, final ArrayList<Fleet> myActiveFleets, final int id)
    {
        this.objective = objective;

        if (myActiveShips != null)
            this.myActiveShips = myActiveShips;
        else
            this.myActiveShips = new ArrayList<>();

        if (myActiveFleets != null)
            this.myActiveFleets = myActiveFleets;
        else
            this.myActiveFleets = new ArrayList<>();

        this.id = id;
    }

    public CombatOperation()
    {
        this.objective = null;
        this.myActiveShips = new ArrayList<>();
        this.myActiveFleets = new ArrayList<>();
        this.id = 0;
    }

    public Objective getObjective() { return objective; }
    public ArrayList<Ship> getMyActiveShips() { return myActiveShips; }
    public ArrayList<Fleet> getMyActiveFleets() { return myActiveFleets; }

    @Override
    public String toString()
    {
        return "CombatOp" + this.id + "<" +
                "Ships(" + this.myActiveShips.size() + "), " +
                "Fleets(" + this.myActiveFleets.size() + "), " +
                "Objective(" + this.getObjective().getOrderType().toString() + "), " +
                "MyShips[" + this.myActiveShips.toString() + "], " +
                "MyFleets[" + this.myActiveFleets.toString() + "], " +
                ">";
    }
}
