import core.*;
import core.CombatManager.CombatManager;

import hlt.GameMap;
import hlt.Move;
import hlt.Networking;

import java.util.ArrayList;
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

        ArrayList<Move> moveList    = new ArrayList<>();
        final Networking networking = new Networking();
        final GameMap gameMap       = networking.initialize((String) gameDefinitions.get("botName"));

        while(true)
        {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());
            gameState.updateGameState(gameMap);

            distanceManager.computeDistanceMatrices(gameState);
            objectiveManager.getObjectives(gameState);
            fleetManager.assignFleetsToObjectives(gameState);


            //combatManager.createCombatOperations(gameState);
            //combatManager.resolveCombats(gameState, moveList);

            navigationManager.moveFleetsToObjective(gameState, moveList);

            Networking.sendMoves(moveList);
        }
    }
}
