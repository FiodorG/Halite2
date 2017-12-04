package core.NavigationManager;

import core.BehaviourManager;
import core.CombatManager.CombatManager;
import core.Fleet;
import core.GameState;
import core.Objective;
import hlt.*;

import java.util.ArrayList;

public class NavigationManager
{
    private ArrayList<Move> moves;

    public ArrayList<Move> getMoves() { return moves; }

    public NavigationManager()
    {
        this.moves = new ArrayList<>();
    }

    public ArrayList<Move> generateMoves(final GameState gameState)
    {
        clearMoves();

        moveFleetsToObjective(gameState, gameState.getBehaviourManager());
        moveShipsToObjective(gameState, gameState.getFleetManager().getShipsToMove(), gameState.getBehaviourManager());

        resolveOutOfBoundaryMoves(gameState);
        resolveMoves(gameState);
        logMoves();

        return this.moves;
    }

    private void moveFleetsToObjective(final GameState gameState, final BehaviourManager behaviourManager)
    {
        for (final Fleet fleet: gameState.getFleetManager().getFleets())
        {
            Objective objective = fleet.getObjectives().get(0);
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            ArrayList<Move> newMoves;

            switch (orderType)
            {
                case GROUP:
                    newMoves = moveFleetToObjectiveGroup(gameState, fleet);
                    break;
                case RUSH: case ANTIRUSH: case ATTACK:
                    newMoves = moveFleetToObjectiveAttack(gameState, fleet, target);
                    break;
                default:
                    throw new IllegalStateException("Unknown orderType for Fleet issued.");
            }

            if (!newMoves.isEmpty())
            {
                this.moves.addAll(newMoves);
                gameState.moveShips(fleet.getShips(), newMoves);
                gameState.moveFleet(fleet, newMoves);
            }
        }
    }

    private ArrayList<Move> moveFleetToObjectiveAttack(final GameState gameState, final Fleet fleet, final Entity target)
    {
        ArrayList<Move> newMoves = Navigation.navigateFleetToAttack(gameState, fleet, target);
        double newMovesScore = CombatManager.scoreFleetMove(fleet, (Ship) target, newMoves, gameState);

        ArrayList<Move> retreatMoves = Navigation.navigateFleetToAttack(gameState, fleet, gameState.getDistanceManager().getClosestAllyShipFromFleet(fleet));
        double retreatMovesScore = CombatManager.scoreFleetMove(fleet, (Ship) target, retreatMoves, gameState);

        if (newMovesScore < retreatMovesScore)
        {
            fleet.setUnderAttackMode();
            return retreatMoves;
        }
        else
            return newMoves;
    }

    private ArrayList<Move> moveFleetToObjectiveGroup(final GameState gameState, final Fleet fleet)
    {
        return Navigation.navigateFleetToGroup(gameState, fleet);
    }

    private void moveShipsToObjective(final GameState gameState, final ArrayList<Ship> ships, final BehaviourManager behaviourManager)
    {
        for(final Ship ship: ships)
        {
            Objective objective = ship.getObjective();
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            Move newMove;

            switch (orderType)
            {
                case DEFEND:
                    newMove = moveShipToObjectiveDefend(gameState, ship, target);
                    break;
                case COLONIZE: case REINFORCECOLONY:
                    newMove = moveShipToObjectiveColonize(gameState, ship, target);
                    break;
                case RUSH: case ANTIRUSH: case ATTACK:
                    newMove = moveShipToObjectiveAttack(gameState, ship, target);
                    break;
                case CRASHINTO:
                    newMove = moveShipToObjectiveCrashInto(gameState, ship, target);
                    break;
                case GROUP:
                    newMove = moveShipToObjectiveGroup(gameState, ship, target);
                    break;
                case ASSASSINATION:
                    newMove = moveShipToObjectiveAssassination(gameState, ship, target);
                    break;
                case LURE:
                    newMove = moveShipToObjectiveLure(gameState, ship, target);
                    break;
                default:
                    throw new IllegalStateException("Unknown orderType for ship issued.");
            }

            if (newMove != null)
            {
                this.moves.add(newMove);
                gameState.moveShip(ship, newMove);
            }
        }
    }

    private Move moveShipToObjectiveDefend(final GameState gameState, final Ship ship, final Entity target)
    {
        return(Navigation.navigateShipToMove(gameState, ship, target));
    }

    private Move moveShipToObjectiveColonize(final GameState gameState, final Ship ship, final Entity target)
    {
        Planet planet = (target instanceof Planet ? (Planet)target : null);

        if (ship.canDock(planet))
            return new DockMove(ship, planet);
        else
            return Navigation.navigateShipToDock(gameState, ship, target);
    }

    private Move moveShipToObjectiveAttack(final GameState gameState, final Ship ship, final Entity target)
    {
        Move newMove = Navigation.navigateShipToAttack(gameState, ship, target);
        double newMoveScore = CombatManager.scoreShipMove(ship, (Ship) target, (ThrustMove) newMove, gameState);

        Move retreatTowardsAllyMove = Navigation.navigateShipToMove(gameState, ship, gameState.getDistanceManager().getClosestAllyShip(ship));
        double retreatTowardsAllyMoveScore = CombatManager.scoreShipMove(ship, (Ship) target, (ThrustMove) retreatTowardsAllyMove, gameState);

        Move retreatMove = Navigation.navigateShipToMove(gameState, ship, retreatDirection(gameState, ship));
        double retreatMoveScore = CombatManager.scoreShipMove(ship, (Ship) target, (ThrustMove) retreatMove, gameState);

//        Move crashMove = Navigation.navigateShipToCrashInto(gameState.getGameMap(), ship, target, Constants.MAX_SPEED);
//        double crashMoveScore = CombatManager.scoreCrashMove(ship, (Ship) target, (ThrustMove) crashMove, gameState);

        double[] scores = new double[]{newMoveScore, retreatTowardsAllyMoveScore, retreatMoveScore};
        Move[] possibleMoves = new Move[]{newMove, retreatTowardsAllyMove, retreatMove};

//        double[] scores = new double[]{newMoveScore, retreatTowardsAllyMoveScore};
//        Move[] possibleMoves = new Move[]{newMove, retreatTowardsAllyMove};

        int indexOfBestMove = getIndexOfLargest(scores);
        return possibleMoves[indexOfBestMove];
    }

    private static int getIndexOfLargest(double[] array)
    {
        if (array == null || array.length == 0) return -1;

        int largest = 0;
        for (int i = 1; i < array.length; i++)
            if (array[i] > array[largest]) largest = i;

        return largest;
    }

    private Entity retreatDirection(final GameState gameState, final Ship sourceShip)
    {
        ArrayList<Ship> closeShips = gameState.getDistanceManager().getEnemiesCloserThan(sourceShip, 14.0);

        if (closeShips.isEmpty())
            return sourceShip;

        double sumCos = 0;
        double sumSin = 0;
        for(final Ship ship: closeShips)
        {
            double targetAngle = sourceShip.orientTowardsInRad(ship);
            sumCos += Math.cos(targetAngle);
            sumSin += Math.sin(targetAngle);
        }

        double averageAngle = Math.atan2(sumSin, sumCos);

        if (averageAngle < 0)
            averageAngle += 2 * Math.PI;

        if(averageAngle < Math.PI)
            averageAngle += Math.PI;
        else
            averageAngle -= Math.PI;

        final double newTargetDx = Math.cos(averageAngle) * 21.0;
        final double newTargetDy = Math.sin(averageAngle) * 21.0;
        return new Entity(-1, 0, sourceShip.getXPos() + newTargetDx, sourceShip.getYPos() + newTargetDy, 0, 0);
    }

    private Move moveShipToObjectiveAssassination(final GameState gameState, final Ship ship, final Entity target)
    {
        if (ship.getDistanceTo(target) > 21.0)
            return Navigation.navigateShipToMove(gameState, ship, gameState.getGameGrid().computeShortestPath(ship, target));
        else
        {
            Planet planet = (Planet)target;
            return Navigation.navigateShipToAttack(gameState, ship, gameState.getGameMap().getShip(planet.getOwner(), planet.getDockedShips().get(0)));
        }
    }

    private Move moveShipToObjectiveLure(final GameState gameState, final Ship ship, final Entity target)
    {
        if (ship.getDistanceTo(target) > 21.0)
            return Navigation.navigateShipToMove(gameState, ship, gameState.getGameGrid().computeShortestPath(ship, target));
        else
            return Navigation.navigateShipToAttack(gameState, ship, target);
    }

    private Move moveShipToObjectiveCrashInto(final GameState gameState, final Ship ship, final Entity target)
    {
        return(Navigation.navigateShipToCrashInto(gameState, ship, target));
    }

    private Move moveShipToObjectiveGroup(final GameState gameState, final Ship ship, final Entity entity)
    {
        if (!(entity instanceof Fleet))
            throw new IllegalStateException("Can't group to non fleets.");

        // Get fleet after move was updated.
        Fleet oldFleet = (Fleet) entity;
        int indexOfFleet = gameState.getMyFleetsNextTurn().indexOf(oldFleet);
        Fleet fleet = gameState.getMyFleetsNextTurn().get(indexOfFleet);

        boolean isClose = (ship.getDistanceTo(fleet.getCentroid()) < 7.1);

        if (isClose)
        {
            final ArrayList<Entity> entities = gameState.objectsBetween(ship, fleet, ship.getRadius() + 0.1);
            entities.removeAll(fleet.getShips());
            if (entities.isEmpty())
                oldFleet.addShip(ship);
        }

        return Navigation.navigateShipToMove(gameState, ship, fleet.getCentroid());
    }

    private void resolveOutOfBoundaryMoves(final GameState gameState)
    {
        double limitX = gameState.getMapSizeX();
        double limitY = gameState.getMapSizeY();

        for(int i = 0; i < this.moves.size(); i++)
        {
            if (!(this.moves.get(i) instanceof ThrustMove))
                continue;

            ThrustMove move = (ThrustMove) this.moves.get(i);

            Ship ship = move.getShip();
            double newX = ship.getXPos() + (move).dX();
            double newY = ship.getYPos() + (move).dY();

            int oldAngle = move.getAngle();
            int newAngle = oldAngle;

            if (newX >= limitX)
                newAngle = (oldAngle < 359)? 270 : 90;

            if (newX <= 0)
                newAngle = (oldAngle < 180)? 90 : 270;

            if (newY >= limitY)
                newAngle = (oldAngle < 90)? 0 : 180;

            if (newY <= 0)
                newAngle = (oldAngle < 270)? 180 : 0;

            this.moves.set(i, new ThrustMove(ship, newAngle, move.getThrust(), move.getPriorityMove()));
        }
    }

    private void resolveMoves(final GameState gameState)
    {
//        for(int i = 0; i < this.moves.size(); i++)
//        {
//            if (!(this.moves.get(i) instanceof ThrustMove))
//                continue;
//
//            ThrustMove move = (ThrustMove)this.moves.get(i);
//            Ship ship = move.getShip();
//            Position targetPos = applyMoveToShip(ship, move);
//            ArrayList<Entity> entityList = gameState.objectsBetween(ship, targetPos, ship.getRadius() + 0.1);
//
//            if (!entityList.isEmpty())
//                this.moves.set(i, navigateShipTowardsTarget(gameState, ship, targetPos, move.getThrust(), true, 90, ship.getRadius() * 2 + 1.0, Math.PI/45.0, move.getPriorityMove()));
//        }

        for(int i = 0; i < this.moves.size(); i++)
        {
            Move move1 = this.moves.get(i);
            for(int j = 0; j < i; j++)
            {
                Move move2 = this.moves.get(j);

                Boolean eitherDocking   = move1.getType() == Move.MoveType.Dock || move2.getType() == Move.MoveType.Dock;
                Boolean eitherUnDocking = move1.getType() == Move.MoveType.Undock || move2.getType() == Move.MoveType.Undock;
                Boolean eitherStopped   = move1.getType() == Move.MoveType.Noop || move2.getType() == Move.MoveType.Noop;

                if(!(eitherDocking || eitherUnDocking || eitherStopped))
                {
                    ThrustMove thrustMove1 = (ThrustMove) move1;
                    ThrustMove thrustMove2 = (ThrustMove) move2;

                    if(Collision.willCollideIterative(thrustMove1, thrustMove2))
                    {
                        if (thrustMove1.getPriorityMove() > thrustMove2.getPriorityMove())
                            this.moves.set(j, new ThrustMove(thrustMove2.getShip(), 0, 0, thrustMove2.getPriorityMove()));
                        else
                            this.moves.set(i, new ThrustMove(thrustMove1.getShip(), 0, 0, thrustMove1.getPriorityMove()));
                    }
                }
            }
        }
    }

    private void clearMoves() { this.moves.clear(); }
    private void logMoves()
    {
        for(final Move move: this.moves)
            DebugLog.addLog(move.toString());
    }
}
