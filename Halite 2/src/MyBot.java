import core.*;
import hlt.*;

import java.util.ArrayList;

public class MyBot
{
    public static void main(final String[] args)
    {
        final Networking networking = new Networking();
        Timer timer = new Timer(Config.verbose);

        ObjectiveManager objectiveManager   = new ObjectiveManager(Config.behaviour);
        FleetManager fleetManager           = new FleetManager();
        NavigationManager navigationManager = new NavigationManager();
        DistanceManager distanceManager     = new DistanceManager();

        final ArrayList<Move> moveList = new ArrayList<>();

        final GameMap gameMap = networking.initialize(Config.botName);

        int turn = 1;
        while(true)
        {
            timer.setCurrentTurnStartTime();

            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            distanceManager.computeDistanceMatrix(gameMap);
            objectiveManager.getObjectives(gameMap, distanceManager);
            fleetManager.assignFleetsToObjectives(gameMap, objectiveManager.getObjectives(), distanceManager);
            navigationManager.moveFleetsToObjective(gameMap, moveList, fleetManager.getFleets());

            // if (timer.timeToEndTurn())
            //    break;

            Networking.sendMoves(moveList);
            turn++;
        }
    }
}
