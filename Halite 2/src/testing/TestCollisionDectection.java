package testing;

import java.util.ArrayList;
import java.util.List;

import hlt.*;
public class TestCollisionDectection {
	
	public static void main(String args[]) {
		
		Ship ship1 = new Ship(1,1,50,50,255,Ship.DockingStatus.Undocked,0,0,0);
		Ship ship2 = new Ship(1,2,50,51,255,Ship.DockingStatus.Undocked,0,0,0);
		
		System.out.println("Ship1 :" + ship1.toString());
		System.out.println("Ship2 :" + ship2.toString());
		
		int angDeg = ship1.orientTowardsInDeg(ship2);
		System.out.println("angle :" + angDeg);
		
		ThrustMove m1 = new ThrustMove(ship1,45,7);
		ThrustMove m2 = new ThrustMove(ship2,315,7);
		
		System.out.println("Move1 :" + m1.dX() + " "+m1.dY());
		System.out.println("Move2 :" + m2.dX() + " "+m2.dY());
		
		
		boolean willCollide = Navigation.willCollide(m1, m2);
		System.out.println("Will Collide :" + Boolean.toString(willCollide));
		
		VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(),m1.getShip().getYPos());
    	VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(),m2.getShip().getYPos());
    	
    	VectorBasic r1 = new VectorBasic(m1.dX(),m1.dY());
    	VectorBasic r2 = new VectorBasic(m2.dX(),m2.dY());
    	
    	Double cross = r1.cross(r2);
    	VectorBasic diff = v1.subtract(v2);
    	
    	System.out.println("Cross :" + cross);
    	System.out.println("diff :" + diff.x + " " + diff.y);
    	
    	Double c1 = diff.cross(r1);
    	Double c2 = diff.cross(r2);
    	System.out.println("c1 :" + c1);
    	System.out.println("c2 :" + c2);
    	
    	Double t = c1/cross;
    	Double u = c2/cross;
    	
    	System.out.println("t :" + t);
    	System.out.println("u :" + u);
    	
    	
    	
	}


}
