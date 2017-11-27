package core.CombatManager;

import hlt.Ship;

import java.util.ArrayList;

public class CombatOperation
{
    private ArrayList<Ship> myShips;
    private ArrayList<Ship> enemyShips;
    private double combatBalance;
    private int id;

    public CombatOperation(final ArrayList<Ship> myShips, final ArrayList<Ship> enemyShips, final double combatBalance, final int id)
    {
        this.myShips = myShips;
        this.enemyShips = enemyShips;
        this.combatBalance = combatBalance;
        this.id = id;
    }

    public ArrayList<Ship> getMyShips() { return myShips; }
    public ArrayList<Ship> getEnemyShips() { return enemyShips; }

    @Override
    public String toString()
    {
        return "CombatOp" + this.id + "<" +
                "[Balance:" + this.combatBalance + ", " +
                "Allies(" + this.myShips.size() + ")," +
                "Enemies(" + this.enemyShips.size() + ")]" +
                ", AlliesShips[" + this.myShips.toString() + "]" +
                ", EnemyShips[" + this.enemyShips.toString() + "]" +
                ">";
    }
}
