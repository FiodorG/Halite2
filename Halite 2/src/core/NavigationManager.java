package core;

import hlt.*;
import hlt.Constants;

import java.util.ArrayList;

import static core.Objective.OrderType.ATTACK;
import static core.Objective.OrderType.COLONIZE;
import static core.Objective.OrderType.CRASHINTO;

public class NavigationManager
{
    public void moveFleetsToObjective(GameMap gameMap, ArrayList<Move> moveList, ArrayList<Fleet> fleets)
    {
        for (final Fleet fleet: fleets)
            moveFleetToObjective(gameMap, moveList, fleet);
    }

    public void moveFleetToObjective(GameMap gameMap, ArrayList<Move> moveList, Fleet fleet)
    {
        for(final Ship ship: fleet.getShips())
        {
            Objective objective = fleet.getObjectives().get(0);
            Objective.OrderType orderType = objective.getOrderType();
            Entity target = objective.getTargetEntity();

            final Move newMove;

            if(orderType == COLONIZE)
            {
                Planet planet = (target instanceof Planet ? (Planet)target : null);

                if(ship.getDockingStatus() != Ship.DockingStatus.Undocked)
                    continue;
                else if (ship.canDock(planet))
                    newMove = new DockMove(ship, planet);
                else
                    newMove = Navigation.navigateShipToDock(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else if (orderType == ATTACK)
            {
                newMove = Navigation.navigateShipToAttack(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else if (orderType == CRASHINTO)
            {
                newMove = Navigation.navigateShipToCrashInto(gameMap, ship, target, Constants.MAX_SPEED);
            }
            else
            {
                newMove = null;
            }

            if (newMove != null)
                moveList.add(newMove);
        }
    }

}
