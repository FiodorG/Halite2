package hlt;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import core.Fleet;
import core.GameState;
import core.VectorBasic;

import static hlt.Constants.DOCK_RADIUS;
import static hlt.Constants.WEAPON_RADIUS;

public class Navigation
{
    public static ThrustMove navigateShipToMoveWithThrust(final GameState gameState, final Ship ship, final Entity moveTarget, final int maxThrust, final double priority)
    {
        final int maxCorrections = 92;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/45.0;
        final double minimumDistance = ship.getRadius() + 1.0;
        final Position movePosition = ship.getClosestPoint(moveTarget, 0.0);

        return navigateShipTowardsTarget(gameState, ship, moveTarget, movePosition, maxThrust, avoidObstacles, maxCorrections, minimumDistance, angularStepRad, priority);
    }

    public static ThrustMove navigateShipToMove(final GameState gameState, final Ship ship, final Entity moveTarget)
    {
        return navigateShipToMoveWithThrust(gameState, ship, moveTarget, Constants.MAX_SPEED, 2.0);
    }

    public static ThrustMove navigateShipToDock(final GameState gameState, final Ship ship, final Entity dockTarget)
    {
        return navigateShipToMoveWithThrust(gameState, ship, dockTarget, Constants.MAX_SPEED, 1.0);
    }

    public static ThrustMove navigateShipToAttack(final GameState gameState, final Ship ship, final Entity attackTarget)
    {
        return navigateShipToMoveWithThrust(gameState, ship, attackTarget, Constants.MAX_SPEED, 5.0);
    }

    public static ThrustMove navigateShipToCrashInto(final GameState gameState, final Ship ship, final Entity crashTarget)
    {
        throw new IllegalStateException("Not supposed to be here");
    }

    public static ThrustMove navigateShipTowardsTarget(
            final GameState gameState,
            final Ship ship,
            final Entity targetEntity,
            final Position targetPosition,
            final int maxThrust,
            final boolean avoidObstacles,
            final int maxCorrections,
            final double minimumDistance,
            final double angularStepRad,
            final double priorityMove
    )
    {
        if (maxCorrections <= 0)
            return new ThrustMove(ship, 0, 0, priorityMove);

        // No need to look at objects too far away
        Position newTargetPosition = targetPosition;

        if (ship.getDistanceTo(newTargetPosition) > 14.0)
            newTargetPosition = newTargetPosition.getClosestPoint(ship, 14.0);

        final double distance = ship.getDistanceTo(newTargetPosition);
        final double angleRad = ship.orientTowardsInRad(newTargetPosition);

        final ArrayList<Entity> entities = gameState.objectsBetween(ship, newTargetPosition, ship.getRadius() + 0.1);

        if ((targetEntity instanceof Ship) || (targetEntity instanceof Planet))
            entities.remove(targetEntity);

        if (avoidObstacles && !entities.isEmpty())
        {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            double newAngularStepRad = -angularStepRad + ((angularStepRad < 0)? +1 : -1) * Math.PI/45.0;
            return navigateShipTowardsTarget(gameState, ship, targetEntity, newTarget, maxThrust, true, maxCorrections - 1, minimumDistance, newAngularStepRad, priorityMove);
        }

        final int thrust = (distance - minimumDistance < maxThrust)? (int)(Math.max(distance - minimumDistance, 0.0)) : maxThrust;

        return new ThrustMove(ship, angleRadToDegClipped(angleRad), thrust, priorityMove);
    }

    /******************************************************************************************************************/
    /******************************************************************************************************************/
    /******************************************************************************************************************/

    public static ArrayList<Move> navigateFleetToAttack(final GameState gameState, final Fleet fleet, final Entity attackTarget)
    {
        return navigateFleetToAttackWithThrust(gameState, fleet, attackTarget, Constants.MAX_SPEED, 5.0);
    }

    public static ArrayList<Move> navigateFleetToDefend(final GameState gameState, final Fleet fleet, final Entity defendTarget)
    {
        return navigateFleetToAttackWithThrust(gameState, fleet, defendTarget, Constants.MAX_SPEED, 4.5);
    }

    public static ArrayList<Move> navigateFleetToMove(final GameState gameState, final Fleet fleet, final Entity defendTarget)
    {
        return navigateFleetToAttackWithThrust(gameState, fleet, defendTarget, Constants.MAX_SPEED, 4.2);
    }

    public static ArrayList<Move> navigateFleetToAttackWithThrust(final GameState gameState, final Fleet fleet, final Entity attackTarget, final int thrust, final double priority)
	{
		final int maxCorrections = 92;
		final boolean avoidObstacles = true;
		final double angularStepRad = Math.PI/45.0;
        final double minimumDistance = fleet.getRadius() + 1.0;
        final Position movePosition = fleet.getCentroid().getClosestPoint(attackTarget, 0.0);

		return navigateFleetTowardsTarget(gameState, fleet, attackTarget, movePosition, thrust, avoidObstacles, maxCorrections, minimumDistance, angularStepRad, priority);
	}

    public static ArrayList<Move> navigateFleetToGroup(final GameState gameState, final Fleet fleet)
    {
        final double minimumDistance = Constants.SHIP_RADIUS * 2 + 0.1;
        final double priorityMove = 4.0;
        final int maxThrust = Constants.MAX_SPEED;

        if (fleet.getShips().size() != 2)
            throw new IllegalStateException("Fleet with more than 2 ships cannot group.");

        Ship ship1 = fleet.getShips().get(0);
        Ship ship2 = fleet.getShips().get(1);
        Entity targetPos = fleet.getCentroid();

        final double distance = ship1.getDistanceTo(ship2);
        final double distance1 = ship1.getDistanceTo(targetPos);
        final double angleRad1 = ship1.orientTowardsInRad(targetPos);
        final double distance2 = ship2.getDistanceTo(targetPos);
        final double angleRad2 = ship2.orientTowardsInRad(targetPos);

        int thrust1 = (distance1 < maxThrust)? (int)distance1 : maxThrust;
        int thrust2 = (distance2 < maxThrust)? (int)distance2 : maxThrust;

        if (distance - thrust1 - thrust2 < minimumDistance)
        {
            if (thrust1 > 0)
                thrust1--;
            else if (thrust2 > 0)
                thrust2--;
            else
            {
                thrust1 = 0;
                thrust2 = 0;
            }
        }

        ThrustMove thrustMove1 = new ThrustMove(ship1, angleRadToDegClipped(angleRad1), thrust1, priorityMove);
        ThrustMove thrustMove2 = new ThrustMove(ship2, angleRadToDegClipped(angleRad2), thrust2, priorityMove);

        ArrayList<Move> moves = new ArrayList<>();
        moves.add(thrustMove1);
        moves.add(thrustMove2);

        return moves;
    }

	public static ArrayList<Move> navigateFleetTowardsTarget(
			final GameState gameState,
			final Fleet fleet,
			final Entity targetEntity,
			final Position targetPosition,
			final int maxThrust,
			final boolean avoidObstacles,
			final int maxCorrections,
			final double minimumDistance,
			final double angularStepRad,
            final double priorityMove
	)
	{
		Entity fleetCentroid = fleet.getCentroid();

		if (maxCorrections <= 0)
        {
            if (fleet.getShips().size() == 2)
                return navigateFleetToGroup(gameState, fleet);
            else
                return regroupFleet(gameState, fleet);
        }

        Position newTargetPosition = targetPosition;

		final double distance = fleetCentroid.getDistanceTo(newTargetPosition);
		final double angleRad = fleetCentroid.orientTowardsInRad(newTargetPosition);

		ArrayList<Entity> objectsBetween = gameState.objectsBetween(fleetCentroid, newTargetPosition, fleet.getRadius() + 0.01);
		objectsBetween.removeAll(fleet.getShips());

        if ((targetEntity instanceof Ship) || (targetEntity instanceof Planet))
            objectsBetween.remove(targetEntity);

        if (fleet.getShips().size() > 3)
            objectsBetween.removeAll(gameState.getEnemyShips());

		if (avoidObstacles && !objectsBetween.isEmpty())
		{
			final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
			final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
			final Position newTarget = new Position(fleetCentroid.getXPos() + newTargetDx, fleetCentroid.getYPos() + newTargetDy);
            double newAngularStepRad = -angularStepRad + ((angularStepRad < 0)? +1 : -1) * Math.PI/45.0;

			return navigateFleetTowardsTarget(gameState, fleet, targetEntity, newTarget, maxThrust, true, (maxCorrections - 1), minimumDistance, newAngularStepRad, priorityMove);
		}

		final int thrust = (distance - minimumDistance < maxThrust)? (int)(Math.max(distance - minimumDistance, 0.0)) : maxThrust;

		return createThrustMovesForFleet(gameState, fleet, angleRadToDegClipped(angleRad), thrust, priorityMove);
	}

	private static ArrayList<Move> createThrustMovesForFleet(final GameState gameState, final Fleet fleet, final int angleDeg, final int thrust, final double priorityMove)
	{
		ArrayList<Move> moves = new ArrayList<>();
		for (final Ship ship: fleet.getShips())
			moves.add(new ThrustMove(ship, angleDeg, thrust, priorityMove));

        return mutateMovesToRegroup(fleet, moves);
	}

	private static ArrayList<Move> mutateMovesToRegroup(final Fleet fleet, final ArrayList<Move> moves)
    {
        Entity centroidBeforeMove = fleet.getCentroid();
        Entity centroidAfterMove = new Entity(
                centroidBeforeMove.getOwner(),
                centroidBeforeMove.getId(),
                centroidBeforeMove.getXPos() + ((ThrustMove)moves.get(0)).dX(),
                centroidBeforeMove.getYPos() + ((ThrustMove)moves.get(0)).dY(),
                centroidBeforeMove.getHealth(),
                centroidBeforeMove.getRadius()
        );

        for (int i = 0; i < moves.size(); ++i)
        {
            Move moveToMutate = moves.get(i);
            Ship shipToMutate = moveToMutate.getShip();
            int angleToCentroid = shipToMutate.orientTowardsInDeg(centroidAfterMove);

            Move moveToCentroid = new ThrustMove(shipToMutate, angleToCentroid, ((ThrustMove)moveToMutate).getThrust(), ((ThrustMove)moveToMutate).getPriorityMove());

            boolean canMoveToCentroid = true;
            for (final Move move: moves)
            {
                if (move.equals(moveToMutate))
                    continue;

                if(Collision.willCollideClosedForm((ThrustMove)moveToCentroid, (ThrustMove)move))
                {
                    canMoveToCentroid = false;
                    break;
                }
            }

            if (canMoveToCentroid)
                moves.set(i, moveToCentroid);
        }

        return moves;
    }

    private static ArrayList<Move> regroupFleet(final GameState gameState, final Fleet fleet)
    {
        Entity centroid = fleet.getCentroid();

        ArrayList<Move> moves = new ArrayList<>();
        for (final Ship ship: fleet.getShips())
        {
            int thrust = (int)Math.min(Math.max(ship.getDistanceTo(centroid), 0.0),7.0);
            double angleRad = ship.orientTowardsInRad(centroid);

            moves.add(new ThrustMove(ship, angleRadToDegClipped(angleRad), thrust, 5.0));
        }

        return moves;
    }

    public static int angleRadToDegClipped(final double angleRad)
    {
        final long degUnclipped = Math.round(Math.toDegrees(angleRad));
        // Make sure return value is in [0, 360) as required by game engine.
        return (int) (((degUnclipped % 360L) + 360L) % 360L);
    }

//    public static ThrustMove navigateTowardsTangent(
//        final GameState gameState,
//		final Ship ship,
//		final Position targetPos,
//        final Position initialTarget,
//		final int maxThrust,
//		final boolean avoidObstacles,
//		final int maxCorrections,
//		final double minimumDistance,
//        final double priorityMove,
//		final boolean isFinalDestination
//	)
//	{
//    	// Still iterative, but not in fixed step sizes computes the step size based on the objects
//    	final double distance = ship.getDistanceTo(targetPos);
//
//    	if ((maxCorrections <= 0) || (distance < 1.2))
//    	{
//    		//fall back to old navigation
//    		final int maxCorrectionsFallback = 90;
//            final double angularStepRad = Math.PI/45.0;
//
//            return navigateShipTowardsTarget(gameState, ship, initialTarget, maxThrust, avoidObstacles, maxCorrectionsFallback, minimumDistance, angularStepRad, priorityMove);
//        }
//
//        final double angleRad = ship.orientTowardsInRad(targetPos);
//        ArrayList<Entity> entityList = gameState.objectsBetween(ship, targetPos, ship.getRadius() + 0.1);
//
//        Position newTarget;
//        if (avoidObstacles && !entityList.isEmpty())
//        {
//			Entity entity = getClosestEntity(entityList, ship);
//        	ArrayList<Position> tangents = findTangentPointsToTarget(ship, entity);
//
//        	Position tangent1 = tangents.get(0);
//        	Position tangent2 = tangents.get(1);
//
//			newTarget = (tangent1.getDistanceTo(targetPos) < tangent2.getDistanceTo(targetPos))? tangent1 : tangent2;
//			boolean isFinalDestinationIteration = ((Position)entity).equals(targetPos);
//
//        	return navigateTowardsTangent(gameState, ship, newTarget, initialTarget, maxThrust, avoidObstacles,maxCorrections - 1, Constants.SHIP_RADIUS + 0.1, priorityMove, isFinalDestinationIteration);
//        }
//
//        int thrust;
//        if (distance - minimumDistance < maxThrust)
//        {
//            thrust = (int) (Math.max(distance - minimumDistance, 0.0));
//            //dealing with scenarios where the thrust is rounded down to 0
//
//            if(thrust == 0)
//            	if(!isFinalDestination)
//            		thrust = 1;
//        }
//        else
//            thrust = maxThrust;
//
//        return new ThrustMove(ship, angleRadToDegClipped(angleRad), thrust, priorityMove);
//	}
//
//	public static ArrayList<Position> findTangentPointsToTarget(final Position point, final Entity target)
//	{
//		double circleRadius = target.getRadius()+ 1.5;
//		double distanceToCenter = target.getDistanceTo(point);
//
//		double x = point.getXPos();
//		double y = point.getYPos();
//
//		double tangentLength = Math.sqrt(distanceToCenter * distanceToCenter - circleRadius * circleRadius);
//		double angleRad = point.orientTowardsInRad(target);
//		double radialWidth = Math.atan2(circleRadius, tangentLength);
//
//		double x1 = tangentLength * Math.cos(angleRad + radialWidth);
//		double x2 = tangentLength * Math.cos(angleRad - radialWidth);
//
//		double y1 = tangentLength * Math.sin(angleRad + radialWidth);
//		double y2 = tangentLength * Math.sin(angleRad - radialWidth);
//
//		Position tangent1 = new Position(x + x1,y + y1);
//		Position tangent2 = new Position(x + x2,y + y2);
//
//		ArrayList<Position> tangents = new ArrayList<>();
//		tangents.add(tangent1);
//		tangents.add(tangent2);
//		return tangents;
//	}
//
//	public static Entity getClosestEntity(final ArrayList<Entity> entityList, final Position p)
//	{
//		double minDistance = Double.MAX_VALUE;
//		double distance;
//		Entity closestEntity = null;
//		for(final Entity entity: entityList)
//		{
//			distance = p.getDistanceTo(entity);
//			if(distance < minDistance)
//			{
//				closestEntity = entity;
//				minDistance = distance;
//			}
//		}
//
//		return closestEntity;
//	}

//    public static ArrayList<ThrustMove> navigateFleetSizeTwoTowards(
//    		final GameMap gameMap,
//    		Fleet fleet,
//    		final Position targetPos,
//    		final int maxThrust,
//    		final boolean avoidObstacles,
//    		final int maxCorrections,
//    		final double minimumDistance
//	)
//	{
//    	int fleetSize = fleet.getShips().size();
//    	ArrayList<ThrustMove> moveList = new ArrayList<ThrustMove>();
//    	ArrayList<Ship> shipList = fleet.getShips();
//    	double maxDistance = 0;
//    	double spacing = 1.0;
//    	ArrayList<VectorBasic> relativePositions = new ArrayList<VectorBasic>();
//    	ArrayList<Position> newTargets = new ArrayList<Position>();
//
//    	if(fleetSize != 2) {
//    		for(Ship s:shipList) {
//    			moveList.add(navigateTowards3(gameMap,
//    					s,targetPos,maxThrust,
//    					avoidObstacles,maxCorrections,minimumDistance,true));
//    		}
//    		return moveList;
//    	}else {
//    		Ship anchorShip = shipList.get(0);
//        	ThrustMove anchorMove = navigateTowards3(gameMap,
//        			anchorShip,targetPos,maxThrust,
//        			avoidObstacles,maxCorrections,minimumDistance,true);
//
//        	moveList.add(anchorMove);
//        	Position anchorPosition = new Position(anchorShip.getXPos() + anchorMove.dX(),
//        			anchorShip.getYPos() + anchorMove.dY() );
//
//        	Position newTarget = anchorPosition;
//
//        	Ship followShip = shipList.get(1);
//
//        	ThrustMove newMove = navigateTowards3(gameMap,
//        			followShip,newTarget,maxThrust,
//        			avoidObstacles,maxCorrections,minimumDistance,true);
//
//        	moveList.add(newMove);
//
//        	moveList = combineMoves(moveList);
//
//        	return moveList;
//    	}
//	}
//
//    public static ArrayList<ThrustMove> combineMoves(ArrayList<ThrustMove> inputMoves){
//    	ThrustMove m1 = inputMoves.get(0);
//    	ThrustMove m2 = inputMoves.get(1);
//
//    	VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
//        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());
//
//
//        double scalingFactor = 1;
//        for (double i = 0.0; i <= 1; i += 0.1)
//        {
//            VectorBasic r1 = new VectorBasic(m1.dX() * i, m1.dY() * i);
//            VectorBasic r2 = new VectorBasic(m2.dX() * i, m2.dY() * i);
//
//            VectorBasic p1 = v1.add(r1);
//            VectorBasic p2 = v2.add(r2);
//
//            if (p1.subtract(p2).length() < Constants.SHIP_RADIUS * 2 + 0.1)
//            {
//            	scalingFactor = i - 0.1;
//            	m1 = rescaleMove(m1,scalingFactor);
//            	m2 = rescaleMove(m2,scalingFactor);
//            	inputMoves.set(0, m1);
//            	inputMoves.set(1, m2);
//
//                break;
//            }
//        }
//
//        return inputMoves;
//    }
//
//    public static ThrustMove rescaleMove(ThrustMove m,double scalingFactor)
//	{
//    	int thrust = m.getThrust();
//    	int newThrust = (int)(thrust * scalingFactor);
//    	m.setThrust(newThrust);
//    	return m;
//    }
}