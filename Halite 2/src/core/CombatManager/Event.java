package core.CombatManager;

import hlt.Entity;
import hlt.Position;
import hlt.Ship;
import java.util.ArrayList;

public class Event
{
    private final EventType eventType;
    private final Entity sourceEntity;
    private final Entity targetEntity;

    public Event(final EventType eventType, final Entity sourceEntity, final Entity targetEntity)
    {
        this.eventType = eventType;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
    }

    public Entity getSourceEntity() { return sourceEntity; }
    public Entity getTargetEntity() { return targetEntity; }
    public EventType getEventType() { return eventType; }

    @Override
    public String toString()
    {
        return "Event<" +
                "type=" + eventType +
                ", Allies(" + sourceEntity.toString() + ")" +
                ", Target(" + targetEntity.toString() + ")" +
                ">";
    }
}
