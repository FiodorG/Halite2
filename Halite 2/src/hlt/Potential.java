package hlt;

public class Potential {
	
	public static Double potential(Position position, Ship ship) {
		Double distance = ship.getDistanceTo(position);
		
		return 1.0/distance;
	}
	
	public static VectorBasic force(Position position, Ship ship) {
		Double distance = ship.getDistanceTo(position);
		Double angRad = ship.orientTowardsInRad(position);
		VectorBasic f = new VectorBasic(0.0,0.0);
		
		f.polarInputs(1.0/(distance *distance), angRad);
		return f;
	}

}
