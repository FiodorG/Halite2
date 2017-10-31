import core.*;
import hlt.*;

import java.util.ArrayList;

public class MyBot
{
    public static void main(final String[] args)
    {
        Timer timer                         = new Timer();
        ObjectiveManager objectiveManager   = new ObjectiveManager(Config.behaviour);
        FleetManager fleetManager           = new FleetManager();
        NavigationManager navigationManager = new NavigationManager();
        DistanceManager distanceManager     = new DistanceManager();

        final Networking networking         = new Networking();
        final GameMap gameMap               = networking.initialize(Config.botName);
        final ArrayList<Move> moveList      = new ArrayList<>();

        int turn = 1;
        while(true)
        {
            logNewTurn(turn++);
            timer.setCurrentTurnStartTime();

            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            distanceManager.computeDistanceMatrices(gameMap);
            objectiveManager.getObjectives(gameMap, distanceManager);
            fleetManager.assignFleetsToObjectives(gameMap, objectiveManager.getObjectives(), distanceManager);
            navigationManager.moveFleetsToObjective(gameMap, moveList, fleetManager.getFleets());

            if (timer.timeToEndTurn()) break;

            Networking.sendMoves(navigationManager.resolveMoves(moveList));
        }
    }

    private static void logNewTurn(final int turn)  { DebugLog.addLog("Turn: " + Integer.toString(turn) + "\n"); }
}
