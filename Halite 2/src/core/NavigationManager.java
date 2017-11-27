package core;

import core.CombatManager.CombatManager;
import hlt.*;
import hlt.Constants;

import java.util.ArrayList;

import static core.Objective.OrderType.*;

public class NavigationManager
{
    public void moveFleetsToObjective(final GameState gameState, final ArrayList<Move> moveList)
    {
        for (final Fleet fleet: gameState.getFleetManager().getFleets())
            moveFleetToObjective(gameState, moveList, fleet, gameState.getBehaviourManager());

        resolveMoves(moveList);
        logMoves(moveList);
    }

    public void moveFleetToObjective(final GameState gameState, ArrayList<Move> moveList, final Fleet fleet, final BehaviourManager behaviourManager)
    {
        for(final Ship ship: fleet.getShips())
        {
            Objective objective = fleet.getObjectives().get(0);
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            Move newMove;

            switch (orderType)
            {
                case DEFEND:
                    newMove = moveFleetToObjectiveDefend(gameState, ship, target);
                    break;
                case COLONIZE: case REINFORCECOLONY:
                    newMove = moveFleetToObjectiveColonize(gameState, ship, target);
                    break;
                case RUSH: case ANTIRUSH: case ATTACK:
                    newMove = moveFleetToObjectiveAttack(gameState, ship, fleet, target);
                    break;
                case CRASHINTO:
                    newMove = moveFleetToObjectiveCrashInto(gameState, ship, target);
                    break;
                default:
                    continue;
            }

            if (newMove != null)
                moveList.add(newMove);
        }
    }

    private Move moveFleetToObjectiveDefend(final GameState gameState, final Ship ship, final Entity target)
    {
        return(Navigation.navigateShipToMove(gameState.getGameMap(), ship, target, Constants.MAX_SPEED));
    }

    private Move moveFleetToObjectiveColonize(final GameState gameState, final Ship ship, final Entity target)
    {
        Planet planet = (target instanceof Planet ? (Planet)target : null);

        if (ship.canDock(planet))
            return new DockMove(ship, planet);
        else
            return Navigation.navigateShipToDock(gameState.getGameMap(), ship, target, Constants.MAX_SPEED);
    }

    private Move moveFleetToObjectiveAttack(final GameState gameState, final Ship ship, final Fleet fleet, final Entity target)
    {
        if (ship.getHealth() <= gameState.getBehaviourManager().getCrashBelowHealth())
            return Navigation.navigateShipToCrashInto(gameState.getGameMap(), ship, target, Constants.MAX_SPEED);
        else
        {
            Move newMove = Navigation.navigateShipToAttack(gameState.getGameMap(), ship, target, Constants.MAX_SPEED);
            double newMoveScore = CombatManager.IsGoodMove(ship, (Ship) target, (ThrustMove) newMove, gameState);

            Move retreatMove = Navigation.navigateShipToMove(gameState.getGameMap(), ship, retreatTarget(gameState, ship, fleet), Constants.MAX_SPEED);
            double retreatMoveScore = CombatManager.IsGoodMove(ship, (Ship) target, (ThrustMove) retreatMove, gameState);

            if (newMoveScore < retreatMoveScore)
                return retreatMove;
            else
                return newMove;
        }
    }

    private Move moveFleetToObjectiveCrashInto(final GameState gameState, final Ship ship, final Entity target)
    {
        return(Navigation.navigateShipToCrashInto(gameState.getGameMap(), ship, target, Constants.MAX_SPEED));
    }

    private Entity retreatTarget(final GameState gameState, final Ship targetShip, final Fleet fleet)
    {
        if (fleet.getShips().size() > 1)
            return fleet.FleetCentroid();
        else
            return gameState.getDistanceManager().getClosestShip(targetShip);
    }

    private void resolveMoves(final ArrayList<Move> moveList)
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

                    if(willCollide2(t1, t2))
                        moveList.set(j, new ThrustMove(move2.getShip(),0,0));
//                    avoidCollisions(t1, t2);
                }
            }
        }
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

        if(newDiff.length() < Constants.SHIP_RADIUS * 2 + 0.1)
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

    private static boolean willCollide2(ThrustMove m1, ThrustMove m2)
    {
        VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());

        for (double i = 0.0; i <= 1; i += 0.1)
        {
            VectorBasic r1 = new VectorBasic(m1.dX() * i, m1.dY() * i);
            VectorBasic r2 = new VectorBasic(m2.dX() * i, m2.dY() * i);

            VectorBasic p1 = v1.add(r1);
            VectorBasic p2 = v2.add(r2);

            if (p1.subtract(p2).length() < Constants.SHIP_RADIUS * 2 + 0.1)
                return true;
        }

        return false;
    }

    private static void avoidCollisions(ThrustMove m1, ThrustMove m2)
    {
        VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());

        for (int i = 0; i <= 7; i++)
        {
            double proportion = (i / 7.0);
            VectorBasic r1 = new VectorBasic(m1.dX((int)(m1.getThrust() * proportion)), m1.dY((int)(m1.getThrust() * proportion)));
            VectorBasic r2 = new VectorBasic(m2.dX((int)(m2.getThrust() * proportion)), m2.dY((int)(m2.getThrust() * proportion)));

            VectorBasic p1 = v1.add(r1);
            VectorBasic p2 = v2.add(r2);

            if (p1.subtract(p2).length() < Constants.SHIP_RADIUS * 2 + 0.1)
                m2.setThrust(m2.getThrust() * (int)((i - 1) / 7.0));
        }
    }

    private void logMoves(final ArrayList<Move> moveList)
    {
        for(final Move move: moveList)
            DebugLog.addLog(move.toString());
    }
}
