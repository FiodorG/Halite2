package hlt;

public class Position
{
    private final double xPos;
    private final double yPos;

    public Position(final double xPos, final double yPos)
    {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public double getXPos() { return xPos; }
    public double getYPos() { return yPos; }

    public double getDistanceTo(final Position target)
    {
        final double dx = xPos - target.getXPos();
        final double dy = yPos - target.getYPos();
        return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    public double orientTowardsInRad(final Position target)
    {
        final double dx = target.getXPos() - xPos;
        final double dy = target.getYPos() - yPos;

        return Math.atan2(dy, dx) + 2 * Math.PI;
    }

    public int orientTowardsInDeg(final Position target)
    {
        return Navigation.angleRadToDegClipped(orientTowardsInRad(target));
    }

    public Position getClosestPoint(final Entity target, final double radiusBump)
    {
        final double radius = target.getRadius();
        final double angleRad = target.orientTowardsInRad(this);

        final double x = target.getXPos() + (radius + radiusBump) * Math.cos(angleRad);
        final double y = target.getYPos() + (radius + radiusBump) * Math.sin(angleRad);

        return new Position(x, y);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        final Position position = (Position) o;
        return (Double.compare(position.xPos, xPos) == 0) && (Double.compare(position.yPos, yPos) == 0);
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        temp = Double.doubleToLongBits(xPos);
        result = (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(yPos);
        result = 31 * result + (int)(temp ^ (temp >>> 32));

        return result;
    }

    @Override
    public String toString() { return "(" + String.format("%.2f", xPos) + ", " + String.format("%.2f", yPos) + ")"; }
}
