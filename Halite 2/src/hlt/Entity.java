package hlt;

public class Entity extends Position
{
    private final int owner;
    private final int id;
    private final int health;
    private final double radius;

    public Entity(final int owner, final int id, final double xPos, final double yPos, final int health, final double radius) {
        super(xPos, yPos);
        this.owner = owner;
        this.id = id;
        this.health = health;
        this.radius = radius;
    }

    public int getOwner() { return owner; }
    public int getId() { return id; }
    public int getHealth() { return health; }
    public double getRadius() { return radius; }

    @Override
    public boolean equals(Object object)
    {
        if (this == object)
            return true;

        if (object == null || object.getClass() != getClass())
            return false;

        Entity entity = (Entity) object;

        return (this.getId() == (entity.getId())) &&
               (this.getXPos() == (entity.getXPos())) &&
               (this.getYPos() == (entity.getYPos()));
    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        temp = Double.doubleToLongBits((double)this.getId());
        result = (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.getXPos());
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(this.getYPos());
        result = 31 * result + (int)(temp ^ (temp >>> 32));

        return result;
    }

    @Override
    public String toString() {
        return "Entity[" +
                super.toString() +
                ", owner=" + owner +
                ", id=" + id +
                ", health=" + health +
                ", radius=" + radius +
                "]";
    }
}
