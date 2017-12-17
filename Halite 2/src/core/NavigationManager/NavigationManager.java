package core.NavigationManager;

import core.*;
import core.CombatManager.CombatManager;
import core.CombatManager.CombatOperation;
import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;

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

        moveFleetsToObjective(gameState);
        moveShipsToObjective(gameState);

        resolveOutOfBoundaryMoves(gameState);
        resolveMoves(gameState);
        logMoves();

        return this.moves;
    }

    public ArrayList<Move> generateMoves2(final GameState gameState)
    {
        clearMoves();

        ArrayList<CombatOperation> combatOperations = gameState.getCombatManager().getSortedCombatOperations();

        for (final CombatOperation combatOperation: combatOperations)
        {
            Objective objective = combatOperation.getObjective();
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            CombatOperationMoves newMoves;

            switch (orderType)
            {
                case DEFEND:
                    newMoves = moveCombatOperationToObjectiveDefend(gameState, combatOperation, target);
                    break;
                case COLONIZE: case REINFORCECOLONY:
                    newMoves = moveCombatOperationToObjectiveColonize(gameState, combatOperation, target);
                    break;
                case RUSH: case ANTIRUSH: case ATTACK: case ATTACKDOCKED:
                    newMoves = moveCombatOperationToObjectiveAttack(gameState, combatOperation, target);
                    break;
                case GROUP:
                    newMoves = moveCombatOperationToObjectiveGroup(gameState, combatOperation, target);
                    break;
                case ASSASSINATION:
                    newMoves = moveCombatOperationToObjectiveAssassination(gameState, combatOperation, target);
                    break;
                case FLEE:
                    newMoves = moveCombatOperationToObjectiveFlee(gameState, combatOperation, target);
                    break;
                case UNDOCK:
                    newMoves = undockCombatOperation(gameState, combatOperation);
                    break;
                default:
                    throw new IllegalStateException("Unknown orderType for CombatOperation issued.");
            }

            addCombatOperationMoves(gameState, combatOperation, newMoves);
        }

        resolveOutOfBoundaryMoves(gameState);
        resolveMoves(gameState);
        logMoves();

        return this.moves;
    }

    private CombatOperationMoves moveCombatOperationToObjectiveAttack(final GameState gameState, final CombatOperation combatOperation, final Entity target)
    {
        ArrayList<Fleet> myFleetsFar = new ArrayList<>();
        ArrayList<Fleet> myFleetsNearby = new ArrayList<>();
        for (final Fleet fleet: combatOperation.getMyFleets())
            if (fleet.getCentroid().getDistanceTo(target) < 20.0)
                myFleetsNearby.add(fleet);
            else
                myFleetsFar.add(fleet);

        ArrayList<Ship> myShipsFar = new ArrayList<>();
        ArrayList<Ship> myShipsNearby = new ArrayList<>();
        for (final Ship ship: combatOperation.getMyShips())
            if (ship.getDistanceTo(target) < 20.0)
                myShipsNearby.add(ship);
            else
                myShipsFar.add(ship);

        CombatOperationMoves combatOperationMoves = moveCombatOperationToObjectiveAttackForGroup(gameState, combatOperation, target, myFleetsNearby, myShipsNearby);
        addCombatOperationMovesForFarFleets(gameState, combatOperationMoves, target, myFleetsFar);
        addCombatOperationMovesForFarShips(gameState, combatOperationMoves, target, myShipsFar);

        return combatOperationMoves;
    }

    private CombatOperationMoves moveCombatOperationToObjectiveAttackForGroup(
            final GameState gameState,
            final CombatOperation combatOperation,
            final Entity target,
            final ArrayList<Fleet> myFleetsNearby,
            final ArrayList<Ship> myShipsNearby
    )
    {
        if (myFleetsNearby.isEmpty() && myShipsNearby.isEmpty())
            return new CombatOperationMoves();

        // Attack
        HashMap<Fleet, ArrayList<Move>> fleetMovesAttack = new HashMap<>();
        for (final Fleet fleet: myFleetsNearby)
            fleetMovesAttack.put(fleet, Navigation.navigateFleetToAttack(gameState, fleet, target));

        HashMap<Ship, Move> shipMovesAttack = new HashMap<>();
        for (final Ship ship: myShipsNearby)
            shipMovesAttack.put(ship, Navigation.navigateShipToAttack(gameState, ship, target));

        CombatOperationMoves attackMoves = new CombatOperationMoves(fleetMovesAttack, shipMovesAttack);
        double attackScore = CombatManager.scoreCombatOperationMove(combatOperation, attackMoves, gameState);

        // Retreat towards Allied
        HashMap<Fleet, ArrayList<Move>> fleetMovesRetreatTowardsAllies = new HashMap<>();
        for (final Fleet fleet: myFleetsNearby)
            fleetMovesRetreatTowardsAllies.put(fleet, Navigation.navigateFleetToAttack(gameState, fleet, gameState.getDistanceManager().getClosestAllyShipFromFleet(fleet)));

        HashMap<Ship, Move> shipMovesRetreatTowardsAllies = new HashMap<>();
        for (final Ship ship: myShipsNearby)
            shipMovesRetreatTowardsAllies.put(ship, Navigation.navigateShipToAttack(gameState, ship, gameState.getDistanceManager().getClosestAllyShip(ship)));

        CombatOperationMoves retreatTowardAllyMoves = new CombatOperationMoves(fleetMovesRetreatTowardsAllies, shipMovesRetreatTowardsAllies);
        double retreatTowardAllyScore = CombatManager.scoreCombatOperationMove(combatOperation, retreatTowardAllyMoves, gameState);

        // Retreat
        HashMap<Fleet, ArrayList<Move>> fleetMovesRetreat = new HashMap<>();
        for (final Fleet fleet: myFleetsNearby)
        {
            Entity retreatDirection = retreatDirection(gameState, fleet.getCentroid());
            int retreatThrust = NavigationManager.retreatThrust(gameState, fleet.getCentroid(), retreatDirection);
            fleetMovesRetreat.put(fleet, Navigation.navigateFleetToAttackWithThrust(gameState, fleet, retreatDirection, retreatThrust, 5.0));
        }

        HashMap<Ship, Move> shipMovesRetreat = new HashMap<>();
        for (final Ship ship: myShipsNearby)
        {
            Entity retreatDirection = NavigationManager.retreatDirection(gameState, ship);
            int retreatThrust = NavigationManager.retreatThrust(gameState, ship, retreatDirection);
            shipMovesRetreat.put(ship, Navigation.navigateShipToMoveWithThrust(gameState, ship, retreatDirection, retreatThrust, 5.0));
        }

        CombatOperationMoves retreatMoves = new CombatOperationMoves(fleetMovesRetreat, shipMovesRetreat);
        double retreatScore = CombatManager.scoreCombatOperationMove(combatOperation, retreatMoves, gameState);

        // Take best
        double[] scores = new double[]{attackScore, retreatTowardAllyScore, retreatScore};
        ArrayList<CombatOperationMoves> possibleMoves = new ArrayList<>();
        possibleMoves.add(attackMoves);
        possibleMoves.add(retreatTowardAllyMoves);
        possibleMoves.add(retreatMoves);

        int indexOfBestMove = getIndexOfLargest(scores);
        return possibleMoves.get(indexOfBestMove);
    }

    private void addCombatOperationMovesForFarFleets(final GameState gameState, final CombatOperationMoves combatOperationMoves, final Entity target, final ArrayList<Fleet> myFleetsFar)
    {
        for (final Fleet fleet: myFleetsFar)
            combatOperationMoves.getFleetMoves().put(fleet, moveFleetToObjectiveAttack(gameState, fleet, target));
    }

    private void addCombatOperationMovesForFarShips(final GameState gameState, final CombatOperationMoves combatOperationMoves, final Entity target, final ArrayList<Ship> myShipsFar)
    {
        for (final Ship ship: myShipsFar)
            combatOperationMoves.getShipMoves().put(ship, moveShipToObjectiveAttack(gameState, ship, target));
    }

    private CombatOperationMoves moveCombatOperationToObjectiveDefend(final GameState gameState, final CombatOperation combatOperation, final Entity target)
    {
        HashMap<Fleet, ArrayList<Move>> fleetMoves = new HashMap<>();
        for (final Fleet fleet: combatOperation.getMyFleets())
            fleetMoves.put(fleet, Navigation.navigateFleetToDefend(gameState, fleet, target));

        HashMap<Ship, Move> shipMoves = new HashMap<>();
        for (final Ship ship: combatOperation.getMyShips())
            shipMoves.put(ship, Navigation.navigateShipToMove(gameState, ship, target));

        return new CombatOperationMoves(fleetMoves, shipMoves);
    }

    private CombatOperationMoves moveCombatOperationToObjectiveColonize(final GameState gameState, final CombatOperation combatOperation, final Entity target)
    {
        if (!combatOperation.getMyFleets().isEmpty())
            throw new IllegalStateException("Fleets cannot colonize.");

        HashMap<Ship, Move> shipMoves = new HashMap<>();
        for (final Ship ship: combatOperation.getMyShips())
            shipMoves.put(ship, moveShipToObjectiveColonize(gameState, ship, target));

        return new CombatOperationMoves(new HashMap<>(), shipMoves);
    }

    private CombatOperationMoves moveCombatOperationToObjectiveGroup(final GameState gameState, final CombatOperation combatOperation, final Entity target)
    {
        HashMap<Fleet, ArrayList<Move>> fleetMoves = new HashMap<>();
        for (final Fleet fleet: combatOperation.getMyFleets())
            fleetMoves.put(fleet, moveFleetToObjectiveGroup(gameState, fleet));

        HashMap<Ship, Move> shipMoves = new HashMap<>();
        for (final Ship ship: combatOperation.getMyShips())
            shipMoves.put(ship, moveShipToObjectiveGroup(gameState, ship, target));

        return new CombatOperationMoves(fleetMoves, shipMoves);
    }

    private CombatOperationMoves moveCombatOperationToObjectiveAssassination(final GameState gameState, final CombatOperation combatOperation, final Entity target)
    {
        HashMap<Fleet, ArrayList<Move>> fleetMoves = new HashMap<>();
        for (final Fleet fleet: combatOperation.getMyFleets())
            fleetMoves.put(fleet, moveFleetToObjectiveAssassination(gameState, fleet, target));

        HashMap<Ship, Move> shipMoves = new HashMap<>();
        for (final Ship ship: combatOperation.getMyShips())
            shipMoves.put(ship, moveShipToObjectiveAssassination(gameState, ship, target));

        return new CombatOperationMoves(fleetMoves, shipMoves);
    }

    private CombatOperationMoves moveCombatOperationToObjectiveFlee(final GameState gameState, final CombatOperation combatOperation, final Entity target)
    {
        HashMap<Fleet, ArrayList<Move>> fleetMoves = new HashMap<>();
        for (final Fleet fleet: combatOperation.getMyFleets())
            fleetMoves.put(fleet, moveFleetToObjectiveFlee(gameState, fleet, target));

        HashMap<Ship, Move> shipMoves = new HashMap<>();
        for (final Ship ship: combatOperation.getMyShips())
            shipMoves.put(ship, moveShipToObjectiveFlee(gameState, ship, target));

        return new CombatOperationMoves(fleetMoves, shipMoves);
    }

    private CombatOperationMoves undockCombatOperation(final GameState gameState, final CombatOperation combatOperation)
    {
        if (!combatOperation.getMyFleets().isEmpty())
            throw new IllegalStateException("Fleets cannot colonize.");

        HashMap<Ship, Move> shipMoves = new HashMap<>();
        for (final Ship ship: combatOperation.getMyShips())
            shipMoves.put(ship, moveShipUndock(gameState, ship));

        return new CombatOperationMoves(new HashMap<>(), shipMoves);
    }

    private void addCombatOperationMoves(final GameState gameState, final CombatOperation combatOperation, final CombatOperationMoves combatOperationMoves)
    {
        for (final Fleet fleet: combatOperation.getMyFleets())
        {
            ArrayList<Move> newMoves = combatOperationMoves.getFleetMoves().get(fleet);
            this.moves.addAll(newMoves);
            gameState.moveShips(fleet.getShips(), newMoves);
            gameState.moveFleet(fleet, newMoves);
        }

        for (final Ship ship: combatOperation.getMyShips())
        {
            Move newMove = combatOperationMoves.getShipMoves().get(ship);
            this.moves.add(newMove);
            gameState.moveShip(ship, newMove);
        }
    }

    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /******************************************************************************************************************/

    private void moveFleetsToObjective(final GameState gameState)
    {
        for (final Fleet fleet: gameState.getFleetManager().getFleetsToMove())
        {
            Objective objective = fleet.getFirstObjectives();
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            ArrayList<Move> newMoves;

            switch (orderType)
            {
                case GROUP:
                    newMoves = moveFleetToObjectiveGroup(gameState, fleet);
                    break;
                case RUSH: case ANTIRUSH:  case ATTACK: case ATTACKDOCKED:
                    newMoves = moveFleetToObjectiveAttack(gameState, fleet, target);
                    break;
                case DEFEND:
                    newMoves = moveFleetToObjectiveDefend(gameState, fleet, target);
                    break;
                case ASSASSINATION:
                    newMoves = moveFleetToObjectiveAssassination(gameState, fleet, target);
                    break;
                case FLEE:
                    newMoves = moveFleetToObjectiveFlee(gameState, fleet, target);
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
//        if (gameState.getBehaviourManager().getTestArgument() == 2)
//        {
//            CombatOperation combatOperation = gameState.getCombatManager().createCombatOperationForFleet(fleet, gameState, target);
//
//            if (combatOperation.getEnemyShips().isEmpty())
//                return Navigation.navigateFleetToAttack(gameState, fleet, target);
//
//            return gameState.getCombatManager().resolveCombat(combatOperation, gameState);
//        }
//        else
//        {
            ArrayList<Move> newMoves = Navigation.navigateFleetToAttack(gameState, fleet, target);
            double newMovesScore = CombatManager.scoreFleetMove(fleet, (Ship) target, newMoves, gameState);

            ArrayList<Move> retreatTowardsAllyMoves = Navigation.navigateFleetToAttack(gameState, fleet, gameState.getDistanceManager().getClosestAllyShipFromFleet(fleet));
            double retreatTowardsAllyMovesScore = CombatManager.scoreFleetMove(fleet, (Ship) target, retreatTowardsAllyMoves, gameState);

            Entity retreatDirection = retreatDirection(gameState, fleet.getCentroid());
            int retreatThrust = NavigationManager.retreatThrust(gameState, fleet.getCentroid(), retreatDirection);
            ArrayList<Move> retreatMoves = Navigation.navigateFleetToAttackWithThrust(gameState, fleet, retreatDirection, retreatThrust, 5.0);
            double retreatMovesScore = CombatManager.scoreFleetMove(fleet, (Ship) target, retreatMoves, gameState);

            double[] scores = new double[]{newMovesScore, retreatTowardsAllyMovesScore, retreatMovesScore};
            ArrayList<ArrayList<Move>> possibleMoves = new ArrayList<>();
            possibleMoves.add(newMoves);
            possibleMoves.add(retreatTowardsAllyMoves);
            possibleMoves.add(retreatMoves);

            int indexOfBestMove = getIndexOfLargest(scores);
            return possibleMoves.get(indexOfBestMove);
//        }
    }

    private ArrayList<Move> moveFleetToObjectiveAssassination(final GameState gameState, final Fleet fleet, final Entity target)
    {
        if (fleet.getCentroid().getDistanceTo(target) > 32.0)
            return Navigation.navigateFleetToAttack(gameState, fleet, gameState.getGameGrid().computeShortestPath(fleet.getCentroid(), target));
        else
            return Navigation.navigateFleetToAttack(gameState, fleet, DistanceManager.getClosestDockedShipOnPlanetFromEntity(gameState, fleet.getCentroid(), target));
    }

    private ArrayList<Move> moveFleetToObjectiveDefend(final GameState gameState, final Fleet fleet, final Entity target)
    {
        return(Navigation.navigateFleetToDefend(gameState, fleet, target));
    }

    private ArrayList<Move> moveFleetToObjectiveGroup(final GameState gameState, final Fleet fleet)
    {
        return Navigation.navigateFleetToGroup(gameState, fleet);
    }

    private ArrayList<Move> moveFleetToObjectiveFlee(final GameState gameState, final Fleet fleet, final Entity target)
    {
        return Navigation.navigateFleetToAttack(gameState, fleet, gameState.getGameGrid().computeShortestPath(fleet.getCentroid(), target));
    }

    private void breakFleet(final GameState gameState, final Fleet fleet)
    {
        for (final Ship ship: fleet.getShips())
        {
            ship.setObjective(fleet.getFirstObjectives());
            gameState.getFleetManager().getShipsToMove().add(ship);
            gameState.getFleetManager().getShipToFleets().remove(ship);
        }

        gameState.getFleetManager().getFleetsToMove().remove(fleet);
        gameState.getFleetManager().getFleetsAvailable().remove(fleet);
    }

    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /******************************************************************************************************************/

    private void moveShipsToObjective(final GameState gameState)
    {
        for(final Ship ship: gameState.getFleetManager().getShipsToMove())
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
                case RUSH: case ANTIRUSH: case ATTACK: case ATTACKDOCKED:
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
                case FLEE:
                    newMove = moveShipToObjectiveFlee(gameState, ship, target);
                    break;
                case UNDOCK:
                    newMove = moveShipUndock(gameState, ship);
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

    private Move moveShipUndock(final GameState gameState, final Ship ship)
    {
        return new UndockMove(ship);
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
//        if (gameState.getBehaviourManager().getTestArgument() == 2)
//        {
//            CombatOperation combatOperation = gameState.getCombatManager().createCombatOperationForShip(ship, gameState, target);
//
//            if (combatOperation.getEnemyShips().isEmpty())
//                return Navigation.navigateShipToAttack(gameState, ship, target);
//
//            if (combatOperation.getMyShips().size() > 1)
//                return Navigation.navigateShipToAttack(gameState, ship, target);
//
//            ArrayList<Move> moves = gameState.getCombatManager().resolveCombat(combatOperation, gameState);
//            return moves.get(0);
//        }
//        else
//        {
            Move newMove = Navigation.navigateShipToAttack(gameState, ship, target);
            double newMoveScore = CombatManager.scoreShipMove(ship, (Ship) target, (ThrustMove) newMove, gameState);

            Move retreatTowardsAllyMove = Navigation.navigateShipToMove(gameState, ship, gameState.getDistanceManager().getClosestAllyShip(ship));
            double retreatTowardsAllyMoveScore = CombatManager.scoreShipMove(ship, (Ship) target, (ThrustMove) retreatTowardsAllyMove, gameState);

            Entity retreatDirection = NavigationManager.retreatDirection(gameState, ship);
            int retreatThrust = NavigationManager.retreatThrust(gameState, ship, retreatDirection);
            Move retreatMove = Navigation.navigateShipToMoveWithThrust(gameState, ship, retreatDirection, retreatThrust, 5.0);
            double retreatMoveScore = CombatManager.scoreShipMove(ship, (Ship) target, (ThrustMove) retreatMove, gameState);

            double[] scores = new double[]{newMoveScore, retreatTowardsAllyMoveScore, retreatMoveScore};
            Move[] possibleMoves = new Move[]{newMove, retreatTowardsAllyMove, retreatMove};

            int indexOfBestMove = getIndexOfLargest(scores);
            return possibleMoves[indexOfBestMove];
//        }
    }

    private Move moveShipToObjectiveAssassination(final GameState gameState, final Ship ship, final Entity target)
    {
        if (ship.getDistanceTo(target) > 32.0)
        {
            return Navigation.navigateShipToMove(gameState, ship, gameState.getGameGrid().computeShortestPath(ship, target));
        }
        else
        {
            Planet planet = (Planet)target;

            Ship closestShip = gameState.getGameMap().getShip(planet.getOwner(), planet.getDockedShips().get(0));
            double closestShipDistance = ship.getDistanceTo(closestShip);

            for (final int shipid: planet.getDockedShips())
            {
                Ship shipIterator = gameState.getGameMap().getShip(planet.getOwner(), shipid);
                double distance = ship.getDistanceTo(shipIterator);
                if (distance < closestShipDistance)
                {
                    closestShip = shipIterator;
                    closestShipDistance = distance;
                }
            }

//            CombatOperation combatOperation = gameState.getCombatManager().createCombatOperationForShip(ship, gameState, closestShip);

//            if (combatOperation.getEnemyShips().isEmpty())
            return Navigation.navigateShipToAttack(gameState, ship, closestShip);
//
//            ArrayList<Move> moves = gameState.getCombatManager().resolveCombat(combatOperation, gameState);
//            return moves.get(0);
        }
    }

    private Move moveShipToObjectiveLure(final GameState gameState, final Ship ship, final Entity target)
    {
        if (ship.getDistanceTo(target) > 21.0)
            return Navigation.navigateShipToMove(gameState, ship, gameState.getGameGrid().computeShortestPath(ship, target));
        else
            return Navigation.navigateShipToAttack(gameState, ship, target);
    }

    private Move moveShipToObjectiveFlee(final GameState gameState, final Ship ship, final Entity target)
    {
        return Navigation.navigateShipToMove(gameState, ship, gameState.getGameGrid().computeShortestPath(ship, target));
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

    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /******************************************************************************************************************/

    public static Entity retreatDirection(final GameState gameState, final Entity sourceEntity)
    {
        ArrayList<Ship> closeShips = gameState.getDistanceManager().getOpponentsCloserThan(sourceEntity, 14.0);

        if (closeShips.isEmpty())
            return sourceEntity;

        double sumCos = 0;
        double sumSin = 0;
        for(final Ship ship: closeShips)
        {
            if (ship.isUndocked())
            {
                double targetAngle = sourceEntity.orientTowardsInRad(ship);
                sumCos += Math.cos(targetAngle);
                sumSin += Math.sin(targetAngle);
            }
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
        return new Entity(-1, 0, sourceEntity.getXPos() + newTargetDx, sourceEntity.getYPos() + newTargetDy, 0, 0);
    }

    public static int retreatThrust(final GameState gameState, final Entity sourceEntity, final Entity retreatDirection)
    {
        Ship closestShip = gameState.getDistanceManager().getClosestEnemyShip(sourceEntity);
        double closestShipDistance = closestShip.getDistanceTo(sourceEntity);

        int thrust = (int)Math.ceil(11.0 - closestShipDistance);
        return Math.min(Math.max(thrust, 0), 7);
    }

    private static int getIndexOfLargest(double[] array)
    {
        if (array == null || array.length == 0) return -1;

        int largest = 0;
        for (int i = 1; i < array.length; i++)
            if (array[i] > array[largest]) largest = i;

        return largest;
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

            int thrust = move.getThrust();

            if (newX >= limitX)
            {
                newAngle = (oldAngle < 90) ? 90 : 270;

                if (newAngle == 90)
                    thrust = Math.min(thrust, (int)(limitY - ship.getYPos()));
                else
                    thrust = Math.min(thrust, (int)ship.getYPos());
            }

            if (newX <= 0)
            {
                newAngle = (oldAngle < 180) ? 90 : 270;

                if (newAngle == 90)
                    thrust = Math.min(thrust, (int)(limitY - ship.getYPos()));
                else
                    thrust = Math.min(thrust, (int)ship.getYPos());
            }

            if (newY >= limitY)
            {
                newAngle = (oldAngle < 90) ? 0 : 180;

                if (newAngle == 0)
                    thrust = Math.min(thrust, (int)(limitX - ship.getXPos()));
                else
                    thrust = Math.min(thrust, (int)ship.getXPos());
            }

            if (newY <= 0)
            {
                newAngle = (oldAngle < 270) ? 180 : 0;

                if (newAngle == 0)
                    thrust = Math.min(thrust, (int)(limitX - ship.getXPos()));
                else
                    thrust = Math.min(thrust, (int)ship.getXPos());
            }

            this.moves.set(i, new ThrustMove(ship, newAngle, thrust, move.getPriorityMove()));
        }
    }

    private void resolveMoves(final GameState gameState)
    {
        // Call function below twice, sometimes when moves
        // are changed, it impacts moves that are checked afterwards.

        resolveMovesInternal(gameState);
        resolveMovesInternal(gameState);
    }

    private void resolveMovesInternal(final GameState gameState)
    {
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

                    if(Collision.willCollideClosedForm(thrustMove1, thrustMove2))
                    {
                        if (thrustMove1.getPriorityMove() > thrustMove2.getPriorityMove())
                            Collision.resolveMoves(thrustMove1, thrustMove2);
                        else
                            Collision.resolveMoves(thrustMove2, thrustMove1);
                    }
                }
            }
        }
    }

    private boolean allMovesZeroThrust(final ArrayList<Move> moves)
    {
        for (final Move move: moves)
            if (((ThrustMove)move).getThrust() != 0)
                return false;

        return true;
    }

    private void clearMoves() { this.moves.clear(); }
    private void logMoves()
    {
        for(final Move move: this.moves)
            DebugLog.addLog(move.toString());
    }
}
