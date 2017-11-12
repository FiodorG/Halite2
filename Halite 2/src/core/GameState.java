package core;

import hlt.*;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;

public class GameState
{
    private int turn;
    private int mapSizeX;
    private int mapSizeY;
    private int myId;
    private int numberOfPlayers;

    HashMap<Integer, Integer> numberOfPlanetsByPlayer;
    HashMap<Integer, Integer> numberOfShipsByPlayer;

    HashMap<Integer, Position> startingPointByPlayers;

    public HashMap<Integer, Position> getStartingPointByPlayers() { return startingPointByPlayers; }
    public int getMyId() { return myId; }
    public int getTurn() { return turn; }
    public int getNumberOfPlayers() { return numberOfPlayers; }

    public GameState()
    {
        this.turn = 0;
        this.myId = 0;
        this.mapSizeX = 0;
        this.mapSizeY = 0;
        this.numberOfPlayers = 0;
        this.numberOfPlanetsByPlayer = new HashMap<>();
        this.numberOfShipsByPlayer = new HashMap<>();
        this.startingPointByPlayers = new HashMap<>();
    }

    public void updateGameState(final GameMap gameMap, final DistanceManager distanceManager)
    {
        this.mapSizeX = gameMap.getWidth();
        this.mapSizeY = gameMap.getHeight();
        this.myId = gameMap.getMyPlayerId();
        this.numberOfPlayers = gameMap.getAllPlayers().size();

        for(final Player player: gameMap.getAllPlayers())
        {
            this.numberOfPlanetsByPlayer.put(player.getId(), 0);
            this.numberOfShipsByPlayer.put(player.getId(), 0);
        }
        this.numberOfPlanetsByPlayer.put(-1, 0);

        for(final Planet planet: gameMap.getAllPlanets().values())
            this.numberOfPlanetsByPlayer.put(planet.getOwner(), numberOfPlanetsByPlayer.get(planet.getOwner())+1);

        for(final Ship ship: gameMap.getAllShips())
            this.numberOfShipsByPlayer.put(ship.getOwner(), this.numberOfShipsByPlayer.get(ship.getOwner())+1);

        if(this.turn == 0)
            for(final Player player: gameMap.getAllPlayers())
                this.startingPointByPlayers.put(player.getId(), distanceManager.computeStartingPoint(player.getShips().values()));

        this.turn++;
    }

    @Override
    public String toString()
    {
        return "GameState[" +
                "turn=" + turn +
                "]";
    }
}
