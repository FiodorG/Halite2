package core;

import hlt.Entity;

import java.util.Objects;

public class Objective
{
    public enum OrderType{
        ATTACK,
        DEFEND,
        COLONIZE,
        CRASHINTO,
        MOVE,
    }

    private final Entity targetEntity;
    private final double priority;
    private int requiredShips;
    private final OrderType orderType;
    private final int Id;

    public double getPriority() { return priority; }
    public OrderType getOrderType() { return orderType; }
    public Entity getTargetEntity() { return targetEntity; }
    public int getRequiredShips() { return requiredShips; }
    public int getId() { return Id; }
    public void decreaseRequiredShips() { this.requiredShips--; }

    public Objective(final Entity targetEntity, final double priority, final int requiredShips, final OrderType orderType, final int Id)
    {
        this.targetEntity = targetEntity;
        this.priority = priority;
        this.requiredShips = requiredShips;
        this.orderType = orderType;
        this.Id = Id;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;

        if (this == object)
            return true;

        Objective objective = (Objective) object;
        return  (this.getTargetEntity().equals(objective.getTargetEntity())) &&
                (this.getOrderType() == objective.getOrderType()) &&
                (this.getId() == objective.getId());
    }

    @Override
    public String toString()
    {
        return "Objective[" +
                ", orderType=" + orderType +
                ", targetEntity=" + targetEntity.getClass() + targetEntity.getId() +
                ", priority=" + priority +
                ", requiredShips=" + requiredShips +
                ", id=" + Id +
                "]";
    }

    @Override
    public int hashCode() { return Objects.hash(Id, orderType, targetEntity.getId()); }
}
