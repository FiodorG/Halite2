import hlt.*;

import java.util.ArrayList;

public class MyBotNull
{
    public static void main(final String[] args)
    {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Null");

        final ArrayList<Move> moveList = new ArrayList<>();
        while(true)
        {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());
            Networking.sendMoves(moveList);
        }
    }
}
