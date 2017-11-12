package hlt;

public class ThrustMove extends Move
{
    private final int angleDeg;
    private final int thrust;

    public ThrustMove(final Ship ship, final int angleDeg, final int thrust)
    {
        super(MoveType.Thrust, ship);
        this.thrust = thrust;
        this.angleDeg = angleDeg;
    }

    public int getAngle() { return angleDeg; }
    public int getThrust() { return thrust; }
    
    public Double dX()
    {
    	Double angleRad = Math.toRadians((double) angleDeg);
        return thrust * Math.cos(angleRad);
    }
    
    public Double dY()
    {
    	Double angleRad = Math.toRadians((double) angleDeg);
        return thrust * Math.sin(angleRad);
    }

    @Override
    public String toString()
    {
        return "ThrustMove[" +
            super.toString() +
            ", angleDeg=" + angleDeg +
            ", thrust=" + thrust +
            "]";
    }
}
