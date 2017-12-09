package hlt;

public class ThrustMove extends Move
{
    private int angleDeg;
    private int thrust;
    private double priorityMove;

    public ThrustMove(final Ship ship, final int angleDeg, final int thrust, final double priorityMove)
    {
        super(MoveType.Thrust, ship);
        this.thrust = thrust;
        this.angleDeg = angleDeg;
        this.priorityMove = priorityMove;
    }

    public ThrustMove(final ThrustMove thrustMove)
    {
        super(thrustMove.getType(), thrustMove.getShip());
        this.thrust = thrustMove.getThrust();
        this.angleDeg = thrustMove.getAngle();
        this.priorityMove = thrustMove.getPriorityMove();
    }

    public int getAngle() { return angleDeg; }
    public int getThrust() { return thrust; }
    public double getPriorityMove() { return priorityMove; }

    public void setThrust(int thrust) { this.thrust = thrust; }

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

    public Double dX(final int thrust)
    {
        Double angleRad = Math.toRadians((double) angleDeg);
        return thrust * Math.cos(angleRad);
    }

    public Double dY(final int thrust)
    {
        Double angleRad = Math.toRadians((double) angleDeg);
        return thrust * Math.sin(angleRad);
    }

    public void invertAngle()
    {
        this.angleDeg = (this.angleDeg + 180) % 360;
    }

    @Override
    public String toString()
    {
        return "ThrustMove[" +
            super.toString() +
            ", angle=" + angleDeg +
            ", thrust=" + thrust +
            ", pri=" + priorityMove +
            "]";
    }
}
