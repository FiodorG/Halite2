package core;

import core.CombatManager.CombatManager;
import core.NavigationManager.GameGrid;
import core.NavigationManager.NavigationManager;
import hlt.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static core.Config.cornerIds;
import static hlt.Ship.DockingStatus.Undocked;

public class GameState
{
    private GameMap gameMap;

    private final CombatManager combatManager;
    private final BehaviourManager behaviourManager;
    private final ObjectiveManager objectiveManager;
    private final FleetManager fleetManager;
    private final NavigationManager navigationManager;
    private final DistanceManager distanceManager;

    private Timer timer;

    private int turn;
    private int mapSizeX;
    private int mapSizeY;
    private int myId;
    private int numberOfPlayers;

    private final ArrayList<Ship> myShips;
    private final ArrayList<Ship> myShipsNextTurn;
    private final ArrayList<Ship> myShipsPreviousTurn;
    private final ArrayList<Fleet> myFleetsNextTurn;
    private final ArrayList<Ship> enemyShips;
    private final ArrayList<Planet> planets;

    HashMap<Integer, Integer> numberOfPlanetsByPlayer;
    private HashMap<Integer, Integer> numberOfShipsByPlayer;
    private HashMap<Integer, Position> startingPointByPlayers;
    private Position centerOfMap;
    private ArrayList<Entity> corners;
    private GameGrid gameGrid;

    private boolean isEnemyRushing;
    private ArrayList<Double> enemyDistances;

    public HashMap<Integer, Position> getStartingPointByPlayers() { return startingPointByPlayers; }
    public int getMyId() { return myId; }
    public int getTurn() { return turn; }
    public int getNumberOfPlayers() { return numberOfPlayers; }
    public GameMap getGameMap() { return gameMap; }
    public ArrayList<Ship> getEnemyShips() { return enemyShips; }
    public ArrayList<Ship> getMyShips() { return myShips; }
    public ArrayList<Ship> getMyShipsNextTurn() { return myShipsNextTurn; }
    public ArrayList<Ship> getMyShipsPreviousTurn() { return myShipsPreviousTurn; }
    public ArrayList<Fleet> getMyFleetsNextTurn() { return myFleetsNextTurn; }

    public CombatManager getCombatManager() { return combatManager; }
    public BehaviourManager getBehaviourManager() { return behaviourManager; }
    public ObjectiveManager getObjectiveManager() { return objectiveManager; }
    public FleetManager getFleetManager() { return fleetManager; }
    public NavigationManager getNavigationManager() { return navigationManager; }
    public DistanceManager getDistanceManager() { return distanceManager; }
    public Timer getTimer() { return timer; }
    public HashMap<Integer, Position> startingPointByPlayers() { return startingPointByPlayers; }
    public Position startingPoint() { return startingPointByPlayers.get(myId); }
    public Position getCenterOfMap() { return centerOfMap; }
    public int getMapSizeX() { return mapSizeX; }
    public int getMapSizeY() { return mapSizeY; }
    public ArrayList<Entity> getCorners() { return corners; }
    public GameGrid getGameGrid() { return gameGrid; }

    public GameState(final CombatManager combatManager,
                     final BehaviourManager behaviourManager,
                     final ObjectiveManager objectiveManager,
                     final FleetManager fleetManager,
                     final NavigationManager navigationManager,
                     final DistanceManager distanceManager)
    {
        this.combatManager = combatManager;
        this.behaviourManager = behaviourManager;
        this.objectiveManager = objectiveManager;
        this.fleetManager = fleetManager;
        this.navigationManager = navigationManager;
        this.distanceManager = distanceManager;
        this.gameGrid = null;

        this.timer = new Timer();
        this.turn = -1;
        this.myId = 0;
        this.mapSizeX = 0;
        this.mapSizeY = 0;
        this.centerOfMap = null;
        this.corners = new ArrayList<>();
        this.numberOfPlayers = 0;

        this.myShips = new ArrayList<>();
        this.myShipsNextTurn = new ArrayList<>();
        this.myShipsPreviousTurn = new ArrayList<>();
        this.myFleetsNextTurn = new ArrayList<>();
        this.enemyShips = new ArrayList<>();
        this.planets = new ArrayList<>();

        this.numberOfPlanetsByPlayer = new HashMap<>();
        this.numberOfShipsByPlayer = new HashMap<>();
        this.startingPointByPlayers = new HashMap<>();

        this.isEnemyRushing = false;
        this.enemyDistances = new ArrayList<>();
    }

    public void updateGameState(final GameMap gameMap)
    {
        this.turn++;

        this.gameMap = gameMap;

        this.mapSizeX = gameMap.getWidth();
        this.mapSizeY = gameMap.getHeight();
        this.myId = gameMap.getMyPlayerId();
        this.numberOfPlayers = gameMap.getNumberOfPlayers();

        // Store my ships and enemy ones to iterate easily
        this.myShips.clear();
        this.myShipsNextTurn.clear();
        this.myFleetsNextTurn.clear();
        this.enemyShips.clear();

        for(final Ship ship: gameMap.getAllShips())
        {
            if(ship.getOwner() != this.myId)
                this.enemyShips.add(ship);
            else
            {
                this.myShips.add(ship);
                this.myShipsNextTurn.add(new Ship(ship));
            }
        }
        addFutureEnemyShips();

        this.planets.clear();
        this.planets.addAll(gameMap.getAllPlanets().values());

        // Balance of power calculation
        for(final Player player: gameMap.getAllPlayers())
        {
            this.numberOfPlanetsByPlayer.put(player.getId(), 0);
            this.numberOfShipsByPlayer.put(player.getId(), 0);
        }
        this.numberOfPlanetsByPlayer.put(-1, 0);

        for(final Planet planet: gameMap.getAllPlanets().values())
            this.numberOfPlanetsByPlayer.put(planet.getOwner(), this.numberOfPlanetsByPlayer.get(planet.getOwner())+1);

        for(final Ship ship: gameMap.getAllShips())
            this.numberOfShipsByPlayer.put(ship.getOwner(), this.numberOfShipsByPlayer.get(ship.getOwner())+1);

        if(this.turn == 0)
        {
            for(final Player player: gameMap.getAllPlayers())
                this.startingPointByPlayers.put(player.getId(), DistanceManager.computeStartingPoint(player.getShips().values()));
            this.centerOfMap = new Position(this.mapSizeX / 2, this.mapSizeY / 2);
            this.corners.add(new Entity(-1, cornerIds - 1, 0, 0, 0, 0));
            this.corners.add(new Entity(-1, cornerIds - 2, 0, mapSizeY, 0, 0));
            this.corners.add(new Entity(-1, cornerIds - 3, mapSizeX, 0, 0, 0));
            this.corners.add(new Entity(-1, cornerIds - 4, mapSizeX, mapSizeY, 0, 0));
        }

        this.gameGrid = new GameGrid(this);

        logState();
    }

    public void saveGameState(final GameMap gameMap)
    {
        this.myShipsPreviousTurn.clear();
        this.myShipsPreviousTurn.addAll(this.myShips);
    }

    private void addFutureEnemyShips()
    {
        int id = 0;
        for(final Planet planet: gameMap.getAllPlanets().values())
        {
            if ((planet.getOwner() != myId) && (planet.getTurnsToNextShip() == 0))
            {
                Position spawnPosition = centerOfMap.getClosestPoint(planet, 2.0);
                this.enemyShips.add(new Ship(planet.getOwner(), getTurn() * 200 + id, spawnPosition.getXPos(), spawnPosition.getYPos(), 255, Undocked, -1, 0, 0));
            }
        }
    }

    public static Ship applyMoveToShip(final Ship ship, final Move move)
    {
        Ship newShip;
        if (move instanceof ThrustMove)
            newShip = new Ship(
                    ship.getOwner(),
                    ship.getId(),
                    ship.getXPos() + ((ThrustMove)move).dX(),
                    ship.getYPos() + ((ThrustMove)move).dY(),
                    ship.getHealth(),
                    ship.getDockingStatus(),
                    ship.getDockedPlanet(),
                    ship.getDockingProgress(),
                    ship.getWeaponCooldown()
            );
        else if (move instanceof DockMove)
        {
            newShip = new Ship(
                    ship.getOwner(),
                    ship.getId(),
                    ship.getXPos(),
                    ship.getYPos(),
                    ship.getHealth(),
                    Ship.DockingStatus.Docking,
                    ship.getDockedPlanet(),
                    ship.getDockingProgress(),
                    ship.getWeaponCooldown()
            );
        }
        else
            newShip = ship;

        return newShip;
    }

    public void moveShip(final Ship ship, final Move move)
    {
        Ship newShip = applyMoveToShip(ship, move);

        this.myShipsNextTurn.remove(ship);
        this.myShipsNextTurn.add(newShip);
    }

    public void moveShips(final ArrayList<Ship> ships, final ArrayList<Move> moves)
    {
        // ships and moves should be co-ordered.

        if (ships.size() != moves.size())
            throw new IllegalStateException("ships and moves have different sizes.");

        for (int i = 0; i < ships.size(); ++i)
            moveShip(ships.get(i), moves.get(i));
    }

    public void moveFleet(final Fleet fleet, final ArrayList<Move> moves)
    {
        // ships and moves should be co-ordered.

        if (fleet.getShips().size() != moves.size())
            throw new IllegalStateException("ships and moves have different sizes.");

        ArrayList<Ship> newShips = new ArrayList<>();
        for (int i = 0; i < fleet.getShips().size(); ++i)
           newShips.add(applyMoveToShip(fleet.getShips().get(i), moves.get(i)));

        this.myFleetsNextTurn.add(new Fleet(newShips, fleet.getObjectives(), fleet.getId()));
    }

    public ArrayList<Entity> objectsBetween(final Position start, final Position target, final double entityRadius)
    {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        addEntitiesBetween(entitiesFound, start, target, this.planets, entityRadius);
        addEntitiesBetween(entitiesFound, start, target, this.myShipsNextTurn, entityRadius);
        addEntitiesBetween(entitiesFound, start, target, this.enemyShips, entityRadius);

        return entitiesFound;
    }

    private static void addEntitiesBetween(final List<Entity> entitiesFound, final Position start, final Position target, final Collection<? extends Entity> entitiesToCheck, final double entityRadius)
    {
        for (final Entity entity : entitiesToCheck)
        {
            if (entity.equals(start) || entity.equals(target))
                continue;
            if (Collision.segmentCircleIntersect(start, target, entity, entityRadius))
                entitiesFound.add(entity);
        }
    }

    public boolean isEnemyRushing(final GameState gameState)
    {
        // No rush in 4 player games so far
        if (gameState.getNumberOfPlayers() != 2)
            return false;

        if (gameState.enemyShips.size() > 3)
            return false;

        // Usually no rush after 20 turns
        int turn = gameState.getTurn();
        if (turn > 20)
            return false;

        int numberOfDockedEnemies = 0;
        for (final Ship ship: gameState.getEnemyShips())
            if (!ship.isUndocked())
                numberOfDockedEnemies++;

        if (numberOfDockedEnemies > 0)
            return false;

        if ((turn < 7 ) && (gameState.getDistanceManager().getAverageDistanceFromMyShipsToEnemies() < 70.0))
            return true;

        if ((turn > 10) && numberOfDockedEnemies == 0)
            return true;

        return false;
    }

    public void logState()
    {
        DebugLog.addLog("\n\nTurn: " + Integer.toString(turn) + "\n");
    }

    @Override
    public String toString()
    {
        return "GameState[" +
                "turn=" + turn +
                "]";
    }
}
