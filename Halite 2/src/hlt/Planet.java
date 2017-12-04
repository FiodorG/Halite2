package hlt;

import java.util.Collections;
import java.util.List;

public class Planet extends Entity
{

    private final int remainingProduction;
    private final int currentProduction;
    private final int dockingSpots;
    private final List<Integer> dockedShips;

    public Planet(final int owner, final int id, final double xPos, final double yPos, final int health,
                  final double radius, final int dockingSpots, final int currentProduction,
                  final int remainingProduction, final List<Integer> dockedShips)
    {

        super(owner, id, xPos, yPos, health, radius);

        this.dockingSpots = dockingSpots;
        this.currentProduction = currentProduction;
        this.remainingProduction = remainingProduction;
        this.dockedShips = Collections.unmodifiableList(dockedShips);
    }

    public int getDockingSpots() { return dockingSpots; }
    public int getFreeDockingSpots() { return dockingSpots - dockedShips.size(); }
    public List<Integer> getDockedShips() { return dockedShips; }
    public boolean isFull() { return dockedShips.size() == dockingSpots; }
    public boolean isOwned() { return getOwner() != -1; }
    public int getCurrentProductionPerTurn() { return dockedShips.size() * 6; }
    public int getTurnsToNextShip() { return (getCurrentProductionPerTurn() == 0)? Integer.MAX_VALUE : (72 - currentProduction) / getCurrentProductionPerTurn(); }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;

        if (this == object)
            return true;

        Planet planet = (Planet) object;
        return (this.getId() == (planet.getId()));
    }

    @Override
    public String toString()
    {
        return "Planet[" +
                super.toString() +
                ", remainingProduction=" + remainingProduction +
                ", currentProduction=" + currentProduction +
                ", dockingSpots=" + dockingSpots +
                ", dockedShips=" + dockedShips +
                "]";
    }
}
