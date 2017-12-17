package core.CombatManager;

import core.Fleet;
import core.Objective;
import hlt.Entity;
import hlt.Ship;

import java.util.ArrayList;

public class CombatOperation
{
    private Objective objective;
    private ArrayList<Ship> myShips;
    private ArrayList<Fleet> myFleets;
    private ArrayList<Ship> enemyShips;
    private int id;

    public CombatOperation(final Objective objective, final ArrayList<Ship> myShips, final ArrayList<Fleet> myFleets, final ArrayList<Ship> enemyShips, final int id)
    {
        this.objective = objective;

        if (myShips != null)
            this.myShips = myShips;
        else
            this.myShips = new ArrayList<>();

        if (myFleets != null)
            this.myFleets = myFleets;
        else
            this.myFleets = new ArrayList<>();

        this.enemyShips = enemyShips;
        this.id = id;
    }

    public CombatOperation()
    {
        this.objective = null;
        this.myShips = new ArrayList<>();
        this.myFleets = new ArrayList<>();
        this.enemyShips = new ArrayList<>();
        this.id = 0;
    }

    public Objective getObjective() { return objective; }
    public ArrayList<Ship> getMyShips() { return myShips; }
    public ArrayList<Fleet> getMyFleets() { return myFleets; }
    public ArrayList<Ship> getEnemyShips() { return enemyShips; }

    @Override
    public String toString()
    {
        return "CombatOp" + this.id + "<" +
                "Ships(" + this.myShips.size() + "), " +
                "Fleets(" + this.myFleets.size() + "), " +
                "Enemies(" + this.enemyShips.size() + "), " +
                "Objective(" + this.getObjective().getOrderType().toString() + "), " +
                "MyShips[" + this.myShips.toString() + "], " +
                "MyFleets[" + this.myFleets.toString() + "], " +
                "EnemyShips[" + this.enemyShips.toString() + "]" +
                ">";
    }
}
