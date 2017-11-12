package hlt;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static hlt.Constants.DOCK_RADIUS;
import static hlt.Constants.WEAPON_RADIUS;

public class Navigation
{
    public static ThrustMove navigateShipToMove( final GameMap gameMap, final Ship ship, final Entity moveTarget, final int maxThrust)
    {
        final int maxCorrections = 180;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final double minimumDistance = moveTarget.getRadius() + ship.getRadius();

        return navigateShipTowardsTarget(gameMap, ship, moveTarget, maxThrust, avoidObstacles, maxCorrections, minimumDistance, angularStepRad);
    }

    public static ThrustMove navigateShipToDock( final GameMap gameMap, final Ship ship, final Entity dockTarget, final int maxThrust)
    {
        final int maxCorrections = 180;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final double minimumDistance = dockTarget.getRadius() + ship.getRadius() + DOCK_RADIUS - 2.0;

        return navigateShipTowardsTarget(gameMap, ship, dockTarget, maxThrust, avoidObstacles, maxCorrections, minimumDistance, angularStepRad);
    }
    
    public static ThrustMove navigateShipToCrashInto( final GameMap gameMap, final Ship ship, final Entity crashTarget, final int maxThrust)
    {
        final int maxCorrections = 180;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final double minimumDistance = 0;
 
        return navigateShipTowardsTarget(gameMap, ship, crashTarget, maxThrust, avoidObstacles, maxCorrections, minimumDistance, angularStepRad);
    }
 
    public static ThrustMove navigateShipToAttack( final GameMap gameMap, final Ship ship, final Entity attackTarget, final int maxThrust)
    {
        final int maxCorrections = 180;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/90.0;
        final double minimumDistance = attackTarget.getRadius() + ship.getRadius() + WEAPON_RADIUS - 2.0;
 
        return navigateShipTowardsTarget(gameMap, ship, attackTarget, maxThrust, avoidObstacles, maxCorrections, minimumDistance, angularStepRad);
    }

    public static ThrustMove navigateShipTowardsTarget(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final boolean avoidObstacles,
            final int maxCorrections,
            final double minimumDistance,
            final double angularStepRad)
    {
        if (maxCorrections <= 0)
            return null;

        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);

        if (avoidObstacles && !gameMap.objectsBetween(ship, targetPos).isEmpty())
        {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), minimumDistance, angularStepRad);
        }

        final int thrust;
        if (distance - minimumDistance < maxThrust)
        {
            // Do not round up, since overshooting might cause collision.
            thrust = (int) (Math.max(distance - minimumDistance, 0.0));
        }
        else
        {
            thrust = maxThrust;
        }

        final int angleDeg = Util.angleRadToDegClipped(angleRad);

        return new ThrustMove(ship, angleDeg, thrust);
    }

    public static ThrustMove navigateTowards2(final GameMap gameMap,Ship ship, final Position targetPos, final int maxThrust,
            final boolean avoidObstacles) {

		double distance = ship.getDistanceTo(targetPos);
		double angleRad = ship.orientTowardsInRad(targetPos);

		final ArrayList<Entity> objectsBetween = gameMap.objectsBetween(ship, targetPos);


		if (avoidObstacles && !objectsBetween.isEmpty()) {
			Map<Double,Entity> nearbyEntitiesByAngle =  new TreeMap<Double,Entity>();
			Double maxAngle = Double.NEGATIVE_INFINITY;
			Entity maxEntity = ship;
			Double maxDistance = 0.0;
			for(final Entity entity : objectsBetween) {
				Double angleToEntity = ship.orientTowardsInRad(entity);
				nearbyEntitiesByAngle.put(angleToEntity, entity);
				if(angleToEntity > maxAngle) {
					maxAngle = angleToEntity;
					maxEntity = entity;
					maxDistance = ship.getDistanceTo(entity);
				}
		}
		DebugLog.addLog(ship.toString());
		DebugLog.addLog(maxEntity.toString());
		DebugLog.addLog(maxAngle.toString());

		Double radialSize = maxEntity.getRadius()*0.5/maxDistance* Math.PI *2;

		angleRad = maxAngle + radialSize;
		distance = maxDistance;

		}


		final int thrust;
		if (distance < maxThrust) {
		// Do not round up, since overshooting might cause collision.
		thrust = (int) distance;
		}
		else {
		thrust = maxThrust;
		}

		final int angleDeg = Util.angleRadToDegClipped(angleRad);

		return new ThrustMove(ship, angleDeg, thrust);
		}

    public static ThrustMove navigateTowards3(final GameMap gameMap,Ship ship, final Position targetPos, final int maxThrust,
            final boolean avoidObstacles,final int maxCorrections) {

    	//still iterative, but not in fixed step sizes
    	// computes the step size based on the objects
    	if (maxCorrections <= 0) {
            return null;
        }

        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);
        double angularStepRad = Math.PI/180;
        double entityDistance = 0;
        double entityRadius = 0;
        double angle = 0;
        ArrayList<Entity> entityList = gameMap.objectsBetween(ship, targetPos);

        if (avoidObstacles && !entityList.isEmpty()) {

        	Entity sampleEntity = entityList.get(0);

        	entityDistance = ship.getDistanceTo(sampleEntity);
        	entityRadius = sampleEntity.getRadius();

        	angle = Math.min(entityRadius/entityDistance, 1);

        	angularStepRad = Math.atan2(entityRadius,entityDistance);

            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), 0, angularStepRad);
        }

        final int thrust;
        if (distance < maxThrust) {
            // Do not round up, since overshooting might cause collision.
            thrust = (int) distance;
        }
        else {
            thrust = maxThrust;
        }

        final int angleDeg = Util.angleRadToDegClipped(angleRad);

        return new ThrustMove(ship, angleDeg, thrust);

		}

//    public static ThrustMove navigateTowardPotential(final GameMap gameMap,Ship ship, final Position targetPos, final int maxThrust) {
//
//    	double distance = ship.getDistanceTo(targetPos);
//		double angleRad = ship.orientTowardsInRad(targetPos);
//
//    	// we just sum over all the forces
//
//		Map<Integer, Planet> allPlanets = gameMap.getAllPlanets();
//
//		VectorBasic force = new VectorBasic();
//		long i = 0;
//		for (Integer key : allPlanets.keySet()) {
//		    Planet planet = allPlanets.get(key);
//
//		    force = VectorBasic.subtract(force, Potential.force(planet, ship));
//		}
//
//		force = force.add(Potential.force(targetPos, ship));
//		force = force.add(Potential.force(targetPos, ship));
//
//		double angRad  = Math.atan2(force.y, force.x);
//		int angleDeg = Util.angleRadToDegClipped(angRad);
//
//		final int thrust;
//        if (distance < maxThrust) {
//            // Do not round up, since overshooting might cause collision.
//            thrust = (int) distance;
//        }
//        else {
//            thrust = maxThrust;
//        }
//
//
//		ThrustMove move = new ThrustMove(ship, angleDeg, thrust);
//
//		return move;
//    }
}
