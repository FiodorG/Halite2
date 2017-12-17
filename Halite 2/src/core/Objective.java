package core;

import hlt.Entity;

import java.util.Objects;

public class Objective
{
    public enum OrderType
    {
        ATTACK,
        ATTACKDOCKED,
        DEFEND,
        COLONIZE,
        REINFORCECOLONY,
        CRASHINTO,
        MOVE,
        GROUP,
        RUSH,
        ANTIRUSH,
        ASSASSINATION,
        LURE,
        FLEE,
        UNDOCK
    }

    private final Entity targetEntity;
    private final double priority;
    private int requiredShips;
    private final OrderType orderType;
    private final boolean superObjective;
    private final int Id;

    public double getPriority() { return priority; }
    public OrderType getOrderType() { return orderType; }
    public Entity getTargetEntity() { return targetEntity; }
    public int getRequiredShips() { return requiredShips; }
    public int getId() { return Id; }
    public void decreaseRequiredShips(int value) { this.requiredShips -= value; }
    public boolean isAttackObjective() { if ((this.orderType == OrderType.ATTACK) || (this.orderType == OrderType.RUSH) || (this.orderType == OrderType.DEFEND) || (this.orderType == OrderType.ATTACKDOCKED)) return true; else return false; }
    public boolean isSuperObjective() { return superObjective; }

    public Objective(final Entity targetEntity, final double priority, final int requiredShips, final OrderType orderType, final boolean superObjective, final int Id)
    {
        this.targetEntity = targetEntity;
        this.priority = priority;
        this.requiredShips = requiredShips;
        this.orderType = orderType;
        this.superObjective = superObjective;
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
                (this.getOrderType() == objective.getOrderType());
    }

    @Override
    public String toString()
    {
        return "Objective" + Id + "[" +
                "order=" + orderType +
                ", target=" + targetEntity.getClass().toString().replace("class hlt.","") + targetEntity.getId() +
                ", pri=" + String.format("%.2f", priority) +
                ", requiredShips=" + requiredShips +
                ", super=" + superObjective +
                "]";
    }

    @Override
    public int hashCode() { return Objects.hash(Id, orderType, targetEntity.getId()); }
}
