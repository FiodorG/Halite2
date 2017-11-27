package core.CombatManager;

import hlt.Position;
import hlt.Ship;
import java.util.ArrayList;

public class Event
{
    final private EventType eventType;
    final private ArrayList<Ship> sourceShips;
    final private Position targetPosition;

    public Event(final EventType eventType, final ArrayList<Ship> sourceShips, final Position targetPosition)
    {
        this.eventType = eventType;
        this.sourceShips = sourceShips;
        this.targetPosition = targetPosition;
    }

    public ArrayList<Ship> getSourceEntity() { return sourceShips; }
    public Position getTargetEntity() { return targetPosition; }
    public EventType getEventType() { return eventType; }


    @Override
    public String toString()
    {
        return "Event<" +
                "type=" + eventType +
                ", Allies(" + sourceShips.size() + ")" +
                ", Target(" + targetPosition.toString() + ")" +
                ">";
    }
}
