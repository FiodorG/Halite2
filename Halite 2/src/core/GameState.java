package core;

import core.CombatManager.CombatManager;
import core.CombatManager.CombatOperation;
import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    private final ArrayList<Ship> enemyShips;

    private final ArrayList<Planet> planets;

    HashMap<Integer, Integer> numberOfPlanetsByPlayer;
    HashMap<Integer, Integer> numberOfShipsByPlayer;
    HashMap<Integer, Position> startingPointByPlayers;

    public HashMap<Integer, Position> getStartingPointByPlayers() { return startingPointByPlayers; }
    public int getMyId() { return myId; }
    public int getTurn() { return turn; }
    public int getNumberOfPlayers() { return numberOfPlayers; }
    public GameMap getGameMap() { return gameMap; }
    public ArrayList<Ship> getEnemyShips() { return enemyShips; }
    public ArrayList<Ship> getMyShips() { return myShips; }

    public CombatManager getCombatManager() { return combatManager; }
    public BehaviourManager getBehaviourManager() { return behaviourManager; }
    public ObjectiveManager getObjectiveManager() { return objectiveManager; }
    public FleetManager getFleetManager() { return fleetManager; }
    public NavigationManager getNavigationManager() { return navigationManager; }
    public DistanceManager getDistanceManager() { return distanceManager; }
    public Timer getTimer() { return timer; }
    public HashMap<Integer, Position> startingPointByPlayers() { return startingPointByPlayers; }
    public Position startingPoint() { return startingPointByPlayers.get(myId); }

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

        this.timer = new Timer();
        this.turn = 0;
        this.myId = 0;
        this.mapSizeX = 0;
        this.mapSizeY = 0;
        this.numberOfPlayers = 0;

        this.myShips = new ArrayList<>();
        this.enemyShips = new ArrayList<>();
        this.planets = new ArrayList<>();

        this.numberOfPlanetsByPlayer = new HashMap<>();
        this.numberOfShipsByPlayer = new HashMap<>();
        this.startingPointByPlayers = new HashMap<>();
    }

    public void updateGameState(final GameMap gameMap)
    {
        this.gameMap = gameMap;

        this.mapSizeX = gameMap.getWidth();
        this.mapSizeY = gameMap.getHeight();
        this.myId = gameMap.getMyPlayerId();
        this.numberOfPlayers = gameMap.getAllPlayers().size();

        // Store my ships and enemy ones to iterate easily
        this.myShips.clear();
        this.enemyShips.clear();
        for(final Ship ship: gameMap.getAllShips())
        {
            if(ship.getOwner() != this.myId)
                this.enemyShips.add(ship);
            else
                this.myShips.add(ship);
        }

        for(final Planet planet: gameMap.getAllPlanets().values())
            this.planets.add(planet);

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
            for(final Player player: gameMap.getAllPlayers())
                this.startingPointByPlayers.put(player.getId(), DistanceManager.computeStartingPoint(player.getShips().values()));

        this.turn++;

        logState();
    }

    public boolean emptyPlanetsExist()
    {
        for(final Planet planet: this.planets)
            if (!planet.isOwned())
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
