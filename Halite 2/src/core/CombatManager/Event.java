package core.CombatManager;

import hlt.Entity;
import hlt.Position;
import hlt.Ship;
import java.util.ArrayList;

public class Event
{
    final private EventType eventType;
    final private ArrayList<Ship> sourceShips;
    final private Entity targetEntity;

    public Event(final EventType eventType, final ArrayList<Ship> sourceShips, final Entity targetEntity)
    {
        this.eventType = eventType;
        this.sourceShips = sourceShips;
        this.targetEntity = targetEntity;
    }

    public ArrayList<Ship> getSourceEntity() { return sourceShips; }
    public Entity getTargetEntity() { return targetEntity; }
    public EventType getEventType() { return eventType; }


    @Override
    public String toString()
    {
        return "Event<" +
                "type=" + eventType +
                ", Allies(" + sourceShips.toString() + ")" +
                ", Target(" + targetEntity.toString() + ")" +
                ">";
    }
}
