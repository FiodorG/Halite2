package hlt;

import core.VectorBasic;

public class Collision
{
    /**
     * Test whether a given line segment intersects a circular area.
     *
     * @param start  The start of the segment.
     * @param end    The end of the segment.
     * @param circle The circle to test against.
     * @param fudge  An additional safety zone to leave when looking for collisions. Probably set it to ship radius.
     * @return true if the segment intersects, false otherwise
     */
    public static boolean segmentCircleIntersect(final Position start, final Position end, final Entity circle, final double fudge)
    {
        // Parameterize the segment as start + t * (end - start),
        // and substitute into the equation of a circle
        // Solve for t
        final double circleRadius = circle.getRadius();
        final double startX = start.getXPos();
        final double startY = start.getYPos();
        final double endX = end.getXPos();
        final double endY = end.getYPos();
        final double centerX = circle.getXPos();
        final double centerY = circle.getYPos();
        final double dx = endX - startX;
        final double dy = endY - startY;

        final double a = square(dx) + square(dy);

        final double b = -2 * (square(startX) - (startX * endX)
                            - (startX * centerX) + (endX * centerX)
                            + square(startY) - (startY * endY)
                            - (startY * centerY) + (endY * centerY));

        if (a == 0.0)
            // Start and end are the same point
            return start.getDistanceTo(circle) <= circleRadius + fudge;

        // Time along segment when closest to the circle (vertex of the quadratic)
        final double t = Math.min(-b / (2 * a), 1.0);
        if (t < 0)
            return false;

        final double closestX = startX + dx * t;
        final double closestY = startY + dy * t;
        final double closestDistance = new Position(closestX, closestY).getDistanceTo(circle);

        return closestDistance <= circleRadius + fudge;
    }

    public static double square(final double num) {
        return num * num;
    }

    public static boolean willCollideCrossVectors(ThrustMove m1, ThrustMove m2)
    {
        VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());

        VectorBasic r1 = new VectorBasic(m1.dX(), m1.dY());
        VectorBasic r2 = new VectorBasic(m2.dX(), m2.dY());

        VectorBasic p1 = v1.add(r1);
        VectorBasic p2 = v2.add(r2);

        VectorBasic newDiff = p1.subtract(p2);

        Double cross = r1.cross(r2);
        VectorBasic diff = v1.subtract(v2);

        if(newDiff.length() < Constants.SHIP_RADIUS * 2 + 0.1)
        {
            return true;
        }
        else
        {
            if (cross < 0.01 && cross > -0.01)
                return false;

            Double c1 = diff.cross(r1);
            Double c2 = diff.cross(r2);

            Double t = - c1 / cross;
            Double u = - c2 / cross;

            if (t > 0 && t < 1 && u > 0 && u < 1)
                return true;
            else
                return false;
        }
    }

    public static boolean willCollideIterative(ThrustMove m1, ThrustMove m2)
    {
        VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());

        for (double i = 0.0; i <= 1; i += 0.05)
        {
            VectorBasic r1 = new VectorBasic(m1.dX() * i, m1.dY() * i);
            VectorBasic r2 = new VectorBasic(m2.dX() * i, m2.dY() * i);

            VectorBasic p1 = v1.add(r1);
            VectorBasic p2 = v2.add(r2);

            if (p1.subtract(p2).length() < Constants.SHIP_RADIUS * 2 + 0.1)
                return true;
        }

        return false;
    }

    public static void avoidCollisions(ThrustMove m1, ThrustMove m2)
    {
        VectorBasic v1 = new VectorBasic(m1.getShip().getXPos(), m1.getShip().getYPos());
        VectorBasic v2 = new VectorBasic(m2.getShip().getXPos(), m2.getShip().getYPos());

        for (int i = 0; i <= 7; i++)
        {
            double proportion = (i / 7.0);
            VectorBasic r1 = new VectorBasic(m1.dX((int)(m1.getThrust() * proportion)), m1.dY((int)(m1.getThrust() * proportion)));
            VectorBasic r2 = new VectorBasic(m2.dX((int)(m2.getThrust() * proportion)), m2.dY((int)(m2.getThrust() * proportion)));

            VectorBasic p1 = v1.add(r1);
            VectorBasic p2 = v2.add(r2);

            if (p1.subtract(p2).length() < Constants.SHIP_RADIUS * 2 + 0.1)
                m2.setThrust(m2.getThrust() * (int)((i - 1) / 7.0));
        }
    }
}
