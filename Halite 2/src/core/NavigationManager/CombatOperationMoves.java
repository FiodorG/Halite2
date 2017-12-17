package core.NavigationManager;

import core.Fleet;
import hlt.Move;
import hlt.Ship;

import java.util.ArrayList;
import java.util.HashMap;

public class CombatOperationMoves
{
    private HashMap<Fleet, ArrayList<Move>> fleetMoves;
    private HashMap<Ship, Move> shipMoves;

    public CombatOperationMoves(final HashMap<Fleet, ArrayList<Move>> fleetMoves, final HashMap<Ship, Move> shipMoves)
    {
        this.fleetMoves = fleetMoves;
        this.shipMoves = shipMoves;
    }

    public CombatOperationMoves()
    {
        this.fleetMoves = new HashMap<>();
        this.shipMoves = new HashMap<>();
    }

    public HashMap<Fleet, ArrayList<Move>> getFleetMoves() { return fleetMoves; }
    public HashMap<Ship, Move> getShipMoves() { return shipMoves; }
}
