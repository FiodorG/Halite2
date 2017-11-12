import core.*;
import hlt.DebugLog;
import hlt.GameMap;
import hlt.Move;
import hlt.Networking;

import java.util.ArrayList;
import java.util.HashMap;

public class MyBotInternal
{
    public static void main(final HashMap<String,Object> gameDefinitions)
    {
        Timer timer                         = new Timer();
        GameState gameState                 = new GameState();
        BehaviourManager behaviourManager   = new BehaviourManager(gameDefinitions, gameState);
        ObjectiveManager objectiveManager   = new ObjectiveManager();
        FleetManager fleetManager           = new FleetManager();
        NavigationManager navigationManager = new NavigationManager();
        DistanceManager distanceManager     = new DistanceManager();

        final Networking networking         = new Networking();
        final GameMap gameMap               = networking.initialize((String) gameDefinitions.get("botName"));
        final ArrayList<Move> moveList      = new ArrayList<>();

        while(true)
        {
//            timer.setCurrentTurnStartTime();
            logNewTurn(gameState.getTurn());
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());
            distanceManager.computeDistanceMatrices(gameMap);
            gameState.updateGameState(gameMap, distanceManager);

            objectiveManager.getObjectives(gameMap, distanceManager, behaviourManager);
            fleetManager.assignFleetsToObjectives(gameMap, objectiveManager.getObjectives(), distanceManager, behaviourManager);
            navigationManager.moveFleetsToObjective(gameMap, moveList, fleetManager, behaviourManager);

//            if (timer.timeToEndTurn()) break;

            Networking.sendMoves(navigationManager.resolveMoves(moveList));
        }
    }

    private static void logNewTurn(final int turn)  { DebugLog.addLog("\n\nTurn: " + Integer.toString(turn) + "\n"); }
}
