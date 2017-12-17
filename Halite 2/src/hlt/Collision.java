package hlt;

import core.VectorBasic;

import static hlt.Constants.SHIP_RADIUS;

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

    private static boolean mightCollide(ThrustMove move1, ThrustMove move2)
    {
        return move1.getShip().getDistanceTo(move2.getShip()) <= move1.getThrust() + move2.getThrust() + SHIP_RADIUS * 2;
    }

    public static boolean willCollideClosedForm(ThrustMove move1, ThrustMove move2)
    {
        if (!mightCollide(move1, move2))
            return false;

        double radius = SHIP_RADIUS * 2;
        double x1 = move1.getShip().getXPos();
        double x2 = move2.getShip().getXPos();
        double y1 = move1.getShip().getYPos();
        double y2 = move2.getShip().getYPos();

        double dx = x1 - x2;
        double dy = y1 - y2;
        double dvx = move1.dX() - move2.dX();
        double dvy = move1.dY() - move2.dY();

        double a = dvx * dvx + dvy * dvy;
        double b = 2 * (dx * dvx + dy * dvy);
        double c = dx * dx + dy * dy - radius * radius;
        double d = b * b - 4 * a * c;

        double time;
        if (a == 0.0)
        {
            if (b == 0.0)
            {
                if (c <= 0.0)
                    time = 0;
                else
                    time = -1;
            }
            else
            {
                double t = - c / b;
                if (t >= 0.0)
                    time = t;
                else
                    time = -1;
            }
        }
        else if (d == 0.0)
        {
            time = - b / (2 * a);
        }
        else if (d > 0)
        {
            double t1 = - b + Math.sqrt(d);
            double t2 = - b - Math.sqrt(d);

            if (t1 >= 0.0 && t2 >= 0.0)
                time = Math.min(t1, t2) / (2 * a);
            else if (t1 <= 0.0 && t2 <= 0.0)
                time = Math.max(t1, t2) / (2 * a);
            else
                time = 0;
        }
        else
            time = -1;

        return ((time >= 0 ) && (time <= 1));
    }

    public static void resolveMoves(ThrustMove firstMove, ThrustMove secondMove)
    {
        // Find the further possible move for second move.

        ThrustMove newSecondMove = new ThrustMove(secondMove);
        int thrustSecondMove = secondMove.getThrust();

        int thrustCrashSecondMove = thrustSecondMove;
        for (int i = 0; i <= thrustSecondMove; i++)
        {
            newSecondMove.setThrust(i);

            if (willCollideClosedForm(firstMove, newSecondMove))
            {
                thrustCrashSecondMove = i;
                break;
            }
        }

        secondMove.setThrust(Math.max(thrustCrashSecondMove - 1, 0));

        // If second move cannot be tweaked, change the first move

        if (thrustCrashSecondMove == 0)
        {
            ThrustMove newFirstMove = new ThrustMove(firstMove);
            int thrustFirstMove = firstMove.getThrust();

            int thrustCrashFirstMove = thrustFirstMove;
            for (int i = 0; i <= thrustFirstMove; i++)
            {
                newFirstMove.setThrust(i);

                if (willCollideClosedForm(newFirstMove, secondMove))
                {
                    thrustCrashFirstMove = i;
                    break;
                }
            }

            firstMove.setThrust(Math.max(thrustCrashFirstMove - 1, 0));
        }
    }
}

