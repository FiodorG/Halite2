package core;

import hlt.Entity;

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
    private final int requiredShips;
    private final OrderType orderType;

    public double getPriority() { return priority; }
    public OrderType getOrderType() { return orderType; }
    public Entity getTargetEntity() { return targetEntity; }
    public int getRequiredShips() { return requiredShips; }

    public Objective(final Entity targetEntity, final double priority, final int requiredShips, final OrderType orderType)
    {
        this.targetEntity = targetEntity;
        this.priority = priority;
        this.requiredShips = requiredShips;
        this.orderType = orderType;
    }

    @Override
    public boolean equals(Object objective)
    {
        boolean sameObject = false;

        if (objective != null && objective instanceof Objective)
            sameObject =
                    (this.getTargetEntity().equals(((Objective) objective).getTargetEntity())) &&
                    (this.getOrderType() == ((Objective) objective).getOrderType()) &&
                    (this.getPriority() == ((Objective) objective).getPriority()) &&
                    (this.getRequiredShips() == ((Objective) objective).getRequiredShips());

        return sameObject;
    }

    @Override
    public String toString()
    {
        return "Objective[" +
                ", targetEntity=" + targetEntity.getId() +
                ", piority=" + priority +
                ", requiredShips=" + requiredShips +
                ", orderType=" + orderType +
                "]";
    }
}
