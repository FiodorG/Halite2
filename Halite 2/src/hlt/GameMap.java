package hlt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Collection;

public class GameMap
{
    private final int width, height;
    private final int playerId;
    private final List<Player> players;
    private final List<Player> playersUnmodifiable;
    private final Map<Integer, Planet> planets;
    private final List<Ship> allShips;
    private final List<Ship> allShipsUnmodifiable;

    // used only during parsing to reduce memory allocations
    private final List<Ship> currentShips = new ArrayList<>();

    public GameMap(final int width, final int height, final int playerId)
    {
        this.width = width;
        this.height = height;
        this.playerId = playerId;
        players = new ArrayList<>(Constants.MAX_PLAYERS);
        playersUnmodifiable = Collections.unmodifiableList(players);
        planets = new TreeMap<>();
        allShips = new ArrayList<>();
        allShipsUnmodifiable = Collections.unmodifiableList(allShips);
    }

    public int getHeight() { return height; }
    public int getWidth() { return width; }
    public int getMyPlayerId() { return playerId; }
    public List<Player> getAllPlayers() { return playersUnmodifiable; }
    public Player getMyPlayer() { return getAllPlayers().get(getMyPlayerId()); }
    public Planet getPlanet(final int entityId) { return planets.get(entityId); }
    public Map<Integer, Planet> getAllPlanets() { return planets; }
    public List<Ship> getAllShips() { return allShipsUnmodifiable; }
    public int getNumberOfPlayers() { return players.size(); }
    public Ship getShip(final int playerId, final int entityId) throws IndexOutOfBoundsException { return players.get(playerId).getShip(entityId); }

    public GameMap updateMap(final Metadata mapMetadata)
    {
        final int numberOfPlayers = MetadataParser.parsePlayerNum(mapMetadata);

        players.clear();
        planets.clear();
        allShips.clear();

        // update players info
        for (int i = 0; i < numberOfPlayers; ++i)
        {
            currentShips.clear();
            final Map<Integer, Ship> currentPlayerShips = new TreeMap<>();
            final int playerId = MetadataParser.parsePlayerId(mapMetadata);

            final Player currentPlayer = new Player(playerId, currentPlayerShips);
            MetadataParser.populateShipList(currentShips, playerId, mapMetadata);
            allShips.addAll(currentShips);

            for (final Ship ship : currentShips)
                currentPlayerShips.put(ship.getId(), ship);

            players.add(currentPlayer);
        }

        final int numberOfPlanets = Integer.parseInt(mapMetadata.pop());

        for (int i = 0; i < numberOfPlanets; ++i)
        {
            final List<Integer> dockedShips = new ArrayList<>();
            final Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, mapMetadata);
            planets.put(planet.getId(), planet);
        }

        if (!mapMetadata.isEmpty())
            throw new IllegalStateException("Failed to parse data from Halite game engine. Please contact maintainers.");

        return this;
    }
}
