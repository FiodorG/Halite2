package hlt;

public class Ship extends Entity
{
    public enum DockingStatus
    {
        Undocked,
        Docking,
        Docked,
        Undocking
    }

    private final DockingStatus dockingStatus;
    private final int dockedPlanet;
    private final int dockingProgress;
    private final int weaponCooldown;

    public Ship(final int owner, final int id, final double xPos, final double yPos,
                final int health, final DockingStatus dockingStatus, final int dockedPlanet,
                final int dockingProgress, final int weaponCooldown)
    {

        super(owner, id, xPos, yPos, health, Constants.SHIP_RADIUS);

        this.dockingStatus = dockingStatus;
        this.dockedPlanet = dockedPlanet;
        this.dockingProgress = dockingProgress;
        this.weaponCooldown = weaponCooldown;
    }

    public int getWeaponCooldown() { return weaponCooldown; }
    public DockingStatus getDockingStatus() { return dockingStatus; }
    public int getDockingProgress() { return dockingProgress; }
    public int getDockedPlanet() { return dockedPlanet; }

    public boolean canDock(final Planet planet)
    {
        return getDistanceTo(planet) <= Constants.DOCK_RADIUS + planet.getRadius();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;

        if (this == object)
            return true;

        Ship ship = (Ship) object;
        return (this.getId() == (ship.getId())) && (this.getOwner() == (ship.getOwner()));
    }

    @Override
    public String toString()
    {
        return "Ship[" +
                super.toString() +
                ", dockingStatus=" + dockingStatus +
                ", dockedPlanet=" + dockedPlanet +
                ", dockingProgress=" + dockingProgress +
                "]";
    }
}
