package hlt;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Navigation {

    public static ThrustMove navigateShipToDock(
            final GameMap gameMap,
            final Ship ship,
            final Entity dockTarget,
            final int maxThrust)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(dockTarget);

        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections, angularStepRad);
    }
    
    public static ThrustMove navigateShipToCrashInto(
            final GameMap gameMap,
            final Ship ship,
            final Entity crashTarget,
            final int maxThrust)
    {
        final int maxCorrections = 1;
        final boolean avoidObstacles = false;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(crashTarget);
 
        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections, angularStepRad);
    }
 
    public static ThrustMove navigateShipToAttack(
            final GameMap gameMap,
            final Ship ship,
            final Entity attackTarget,
            final int maxThrust)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(attackTarget);
 
        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections, angularStepRad);
    }
    
    public static ThrustMove navigateShipToDock_2(
            final GameMap gameMap,
            final Ship ship,
            final Entity dockTarget,
            final int maxThrust)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(dockTarget);

        return navigateTowards2(gameMap, ship, targetPos, maxThrust, avoidObstacles);
    }

    public static ThrustMove navigateShipToDock_3(
            final GameMap gameMap,
            final Ship ship,
            final Entity dockTarget,
            final int maxThrust)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(dockTarget);

        return navigateTowards3(gameMap, ship, targetPos, maxThrust, avoidObstacles,maxCorrections);
    }
    public static ThrustMove navigateShipTowardsTarget(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final boolean avoidObstacles,
            final int maxCorrections,
            final double angularStepRad)
    {
        if (maxCorrections <= 0) {
            return null;
        }

        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);

        if (avoidObstacles && !gameMap.objectsBetween(ship, targetPos).isEmpty()) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), angularStepRad);
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

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), angularStepRad);
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
    
    public static ArrayList<Move> resolveMoves(ArrayList<Move> moveList){
    	
    	int N =  moveList.size();
    	Move m1;
    	Move m2;
    	for(int i = 0; i< N ; i++) {
    		m1 = moveList.get(i);
    		for(int j = 0; j < i; j ++) {
    			m2 = moveList.get(j);
    			
    			Boolean eitherDocking = m1.getType() == Move.MoveType.Dock ||m2.getType() == Move.MoveType.Dock;
    	    	Boolean eitherUnDocking = m1.getType() == Move.MoveType.Undock ||m2.getType() == Move.MoveType.Undock;
    	    	Boolean eitherStopped = m1.getType() == Move.MoveType.Noop ||m2.getType() == Move.MoveType.Noop;
    	    	
    	    	if(eitherDocking ||eitherUnDocking || eitherStopped ) {
    	    		continue;
    	    	}else {
    	    		ThrustMove t1 = (ThrustMove) m1;
    	    		ThrustMove t2 = (ThrustMove) m2;
    	    		
    	    		if(willCollide(t1,t2)) {
    	    			moveList.set(j, new ThrustMove(m2.getShip(),0,0));
    	    		}
    	    	}
    			
    		}
    	}
    	return moveList;
    	
    	
    	
    }
    
    public static boolean willCollide(ThrustMove m1,ThrustMove m2) {
    	
    	
    	VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(),m1.getShip().getYPos());
    	VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(),m2.getShip().getYPos());
    	
    	VectorBasic r1 = new VectorBasic(m1.dX(),m1.dY());
    	VectorBasic r2 = new VectorBasic(m2.dX(),m2.dY());
    	
    	Double cross = r1.cross(r2);
    	VectorBasic diff = v1.subtract(v2);
    	
    	if(cross < 0.01 && cross > -0.01 ) {
    		return false;
    	}
    	
    	Double c1 = diff.cross(r1);
    	Double c2 = diff.cross(r2);
    	
    	Double t = c1/cross;
    	Double u = c2/cross;
    	
    	if ( t > 0 && t < 1 && u > 0 && u < 1 ) {
    		return true;
    	}else {
    		return false;
    	}
    	
    }
    
}
