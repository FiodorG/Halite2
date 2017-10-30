package core;

public class VectorBasic
{
    double x;
    double y;

    public VectorBasic(Double x1,Double y1)
    {
        x = x1;
        y = y1;
    }

    public VectorBasic subtract(VectorBasic v)  { return new VectorBasic(x - v.x,y - v.y); }

    public Double cross(VectorBasic v) { return x * (v.y) - y * (v.x); }
}
