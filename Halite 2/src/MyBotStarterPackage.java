import hlt.*;
import hlt.Constants;

import java.util.ArrayList;

public class MyBotStarterPackage
{
    public static void main(final String[] args)
    {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Original");

        final ArrayList<Move> moveList = new ArrayList<>();
        for(;;)
        {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

            for(final Ship ship : gameMap.getMyPlayer().getShips().values())
            {
                if(ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                {
                    continue;
                }

                for(final Planet planet : gameMap.getAllPlanets().values())
                {
                    if (planet.isOwned())
                    {
                        continue;
                    }

                    if (ship.canDock(planet))
                    {
                        moveList.add(new DockMove(ship, planet));
                        break;
                    }

                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED/2);
                    if (newThrustMove != null)
                    {
                        moveList.add(newThrustMove);
                    }

                    break;
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}
