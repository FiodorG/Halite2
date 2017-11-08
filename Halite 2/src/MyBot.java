import core.*;
import hlt.*;

import java.util.ArrayList;

public class MyBot
{
    public static void main(final String[] args)
    {
        Timer timer                         = new Timer();
        GameState gameState                 = new GameState();
        BehaviourManager behaviourManager   = new BehaviourManager(Config.behaviour, gameState);
        ObjectiveManager objectiveManager   = new ObjectiveManager();
        FleetManager fleetManager           = new FleetManager();
        NavigationManager navigationManager = new NavigationManager();
        DistanceManager distanceManager     = new DistanceManager();

        final Networking networking         = new Networking();
        final GameMap gameMap               = networking.initialize(Config.botName);
        final ArrayList<Move> moveList      = new ArrayList<>();

        while(true)
        {
            //timer.setCurrentTurnStartTime();
            gameState.updateGameState();
            logNewTurn(gameState.getTurn());

            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            distanceManager.computeDistanceMatrices(gameMap);
            objectiveManager.getObjectives(gameMap, distanceManager, behaviourManager);
            fleetManager.assignFleetsToObjectives(gameMap, objectiveManager.getObjectives(), distanceManager, behaviourManager);
            navigationManager.moveFleetsToObjective(gameMap, moveList, fleetManager.getFleets());

            //if (timer.timeToEndTurn()) break;

            Networking.sendMoves(navigationManager.resolveMoves(moveList));
        }
    }

    private static void logNewTurn(final int turn)  { DebugLog.addLog("Turn: " + Integer.toString(turn) + "\n"); }
}
