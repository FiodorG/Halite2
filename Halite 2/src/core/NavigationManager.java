package core;

import hlt.*;
import hlt.Constants;

import java.util.ArrayList;

import static core.Objective.OrderType.*;

public class NavigationManager
{
    public void moveFleetsToObjective(final GameMap gameMap, final ArrayList<Move> moveList, final FleetManager fleetManager, final BehaviourManager behaviourManager)
    {
        moveList.clear();

        for (final Fleet fleet: fleetManager.getFleets())
            moveFleetToObjective(gameMap, moveList, fleet, behaviourManager);
    }

    public void moveFleetToObjective(GameMap gameMap, ArrayList<Move> moveList, Fleet fleet, final BehaviourManager behaviourManager)
    {
        fleet.computeFleetCentroid();

        for(final Ship ship: fleet.getShips())
        {
            Objective objective = fleet.getObjectives().get(0);
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            Move newMove;

            if (orderType == DEFEND)
            {
                newMove = Navigation.navigateShipToMove(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else if (orderType == COLONIZE)
            {
                Planet planet = (target instanceof Planet ? (Planet)target : null);

                if (ship.canDock(planet))
                    newMove = new DockMove(ship, planet);
                else
                    newMove = Navigation.navigateShipToDock(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else if ((orderType == ATTACK) || (orderType == RUSH) || (orderType == ANTIRUSH))
            {
                if (ship.getHealth() <= behaviourManager.getCrashBelowHealth())
                    newMove = Navigation.navigateShipToCrashInto(gameMap, ship, target, Constants.MAX_SPEED);
                else
                    newMove = Navigation.navigateShipToAttack(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else if (orderType == CRASHINTO)
            {
                newMove = Navigation.navigateShipToCrashInto(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else
                continue;

            if (newMove != null)
                moveList.add(newMove);
        }
    }

    public ArrayList<Move> resolveMoves(ArrayList<Move> moveList)
    {
        int numberOfMoves = moveList.size();
        Move move1;
        Move move2;
        for(int i = 0; i < numberOfMoves; i++)
        {
            move1 = moveList.get(i);
            for(int j = 0; j < i; j++)
            {
                move2 = moveList.get(j);

                Boolean eitherDocking   = move1.getType() == Move.MoveType.Dock || move2.getType() == Move.MoveType.Dock;
                Boolean eitherUnDocking = move1.getType() == Move.MoveType.Undock || move2.getType() == Move.MoveType.Undock;
                Boolean eitherStopped   = move1.getType() == Move.MoveType.Noop || move2.getType() == Move.MoveType.Noop;

                if(eitherDocking || eitherUnDocking || eitherStopped)
                {
                    continue;
                }
                else
                {
                    ThrustMove t1 = (ThrustMove) move1;
                    ThrustMove t2 = (ThrustMove) move2;

                    if(willCollide(t1, t2))
                        moveList.set(j, new ThrustMove(move2.getShip(),0,0));
                }
            }
        }
        return moveList;
    }

    private static boolean willCollide(ThrustMove m1, ThrustMove m2)
    {
        VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());

        VectorBasic r1 = new VectorBasic(m1.dX(), m1.dY());
        VectorBasic r2 = new VectorBasic(m2.dX(), m2.dY());

        VectorBasic p1 = v1.add(r1);
        VectorBasic p2 = v2.add(r2);

        VectorBasic newDiff = p1.subtract(p2);

        Double cross = r1.cross(r2);
        VectorBasic diff = v1.subtract(v2);

        if(newDiff.length() < Constants.SHIP_RADIUS * 2)
        {
            return true;
        }
        else
        {
            if (cross < 0.01 && cross > -0.01)
                return false;

            Double c1 = diff.cross(r1);
            Double c2 = diff.cross(r2);

            Double t = - c1 / cross;
            Double u = - c2 / cross;

            if (t > 0 && t < 1 && u > 0 && u < 1)
                return true;
            else
                return false;
        }
    }
}
