package testing;
import java.util.ArrayList;
import java.util.List;

import hlt.*;
public class TestPotential {
	
	public static void main(String args[]) {
		
		
		
		List<Integer> dockedShips = new ArrayList<Integer>();
		Planet planet = new Planet(1,1,100,100,255,12,3,3,3,dockedShips);
		Ship ship = new Ship(1,1,0,0,255,Ship.DockingStatus.Undocked,0,0,0);
		
		Double dist = ship.getDistanceTo(planet);
		System.out.println("Distance :" + Double.toString(dist));
		
		Double ang = ship.orientTowardsInRad(planet);
		System.out.println("Angle in Rad :" + Double.toString(ang));
		System.out.println("Angle in Rad - 2PI:" + Double.toString(ang - Math.PI*2));
		
		int angDeg = ship.orientTowardsInDeg(planet);
		System.out.println("Angle in Deg :" + Integer.toString(angDeg));
		
		VectorBasic force = Potential.force(planet, ship);
		System.out.println("force dx :" +  force.x);
		System.out.println("force dy :" +  force.y);
		
		double angRad2  = Math.atan2(force.y, force.x);
		int angDeg2 = Util.angleRadToDegClipped(angRad2);

		System.out.println("Angle in Rad :" + Double.toString(angRad2));
		System.out.println("Angle in Rad :" + Integer.toString(angDeg2));
	}

}
