import core.*;
import core.CombatManager.CombatManager;

import core.NavigationManager.NavigationManager;
import hlt.GameMap;
import hlt.Networking;
import java.util.Map;

public class MyBotInternal
{
    public static void main(final Map<String,Object> gameDefinitions, int... args)
    {
        CombatManager combatManager         = new CombatManager();
        BehaviourManager behaviourManager   = new BehaviourManager(gameDefinitions);
        ObjectiveManager objectiveManager   = new ObjectiveManager();
        FleetManager fleetManager           = new FleetManager();
        NavigationManager navigationManager = new NavigationManager();
        DistanceManager distanceManager     = new DistanceManager();

        GameState gameState = new GameState(
            combatManager,
            behaviourManager,
            objectiveManager,
            fleetManager,
            navigationManager,
            distanceManager
        );

        final Networking networking = new Networking();
        final GameMap gameMap       = networking.initialize((String) gameDefinitions.get("botName"));

        while(true)
        {
            gameMap.updateMap(Networking.readLineIntoMetadata());
            gameState.updateGameState(gameMap);

            distanceManager.computeDistanceMatrices(gameState);
            objectiveManager.getObjectives(gameState);
            fleetManager.assignShips(gameState);

            combatManager.createCombatOperations(gameState);

            if (gameState.getBehaviourManager().getTestArgument() == 2)
                navigationManager.generateMoves2(gameState);
            else
                navigationManager.generateMoves(gameState);

            gameState.saveGameState(gameMap);
            Networking.sendMoves(navigationManager.getMoves());
        }
    }
}
