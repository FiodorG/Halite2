package hlt;

public class VectorBasic {

	public double x;
	public double y;
	
	public VectorBasic(Double x1,Double y1) {
		x = x1;
		y = y1;
	}
	
	public VectorBasic() {
		x = 0.0;
		y = 0.0;
	}
	
	public void polarInputs(Double r,Double angRad) {
		x = r * Math.cos(angRad);
		y = r * Math.sin(angRad);
	}
	
	public VectorBasic subtract(VectorBasic v) {
		return new VectorBasic(x - v.x,y - v.y);
		
	}
	
	public VectorBasic add(VectorBasic v) {
		return new VectorBasic(x + v.x,y +v.y) ;
		
	}
	
	public static VectorBasic add(VectorBasic v1,VectorBasic v2) {
		return new VectorBasic(v1.x + v2.x,v1.y +v2.y) ;
		
	}
	
	public static VectorBasic subtract(VectorBasic v1,VectorBasic v2) {
		return new VectorBasic(v1.x - v2.x,v1.y -v2.y) ;
		
	}
	public Double cross(VectorBasic v) {
		return x * (v.y) - y * (v.x);
		
	}
	
	public VectorBasic negate() {
		return new VectorBasic(-this.x,-this.y);
		
	}
	
	
}
