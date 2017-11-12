import core.*;
import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;

public class MyBotNoRush
{
    public static HashMap<String,Object> gameDefinitions()
    {
        HashMap<String,Object> gameDefinitions = new HashMap<>();
        gameDefinitions.put("behaviourType",            "NORUSH");  // Preset behaviour
        gameDefinitions.put("botName",                  "NORUSH");  // Name of the bot that will appear on screen

        gameDefinitions.put("maxPriority",              100.0);     // Higher bound of priorities (except special ones)
        gameDefinitions.put("distanceDiscountExponent", 1.5);       // Power exponent to discount by distance

        gameDefinitions.put("colonizationTurns",        50);        // Prioritize colonization during first n turns
        gameDefinitions.put("colonizationBump",         100.0);     // Priority Bump when colonization phase

        gameDefinitions.put("maxRushDistance",          130.0);     // Rush if enemy is closed than
        gameDefinitions.put("rushPriority",             0.0);       // Priority for a rush order
        gameDefinitions.put("antiRushPriority",         10e30);     // Priority for a antirush order
        gameDefinitions.put("rushTurns",                40);        // Prioritize rush during first n turns
        gameDefinitions.put("rushMaxObjectives",        0);         // Number of ships sent to rush
        gameDefinitions.put("rushMaxShipsPerObjective", 0);         // Number of ships sent to rush by rush objective

        gameDefinitions.put("defendPriority",           100.0);     // Priority for defending planet under attack
        gameDefinitions.put("enemyShipsToDefend",       5);         // Enemy ships around your planet required to send defend order

        gameDefinitions.put("crashPriority",            0.0);       // Priority for crashing into other planets
        gameDefinitions.put("enemyShipsToCrash",        5);         // Crash when enemies are in range
        gameDefinitions.put("crashBelowHealth",         65.0);      // Crash when health below level

        gameDefinitions.put("dockingShipPriority",      200.0);     // Priority for docking ships
        gameDefinitions.put("attackShipPriority",       50.0);      // Priority for regular enemy ships

        return gameDefinitions;
    }

    public static void main(final String[] args)
    {
        MyBotInternal.main(gameDefinitions());
    }
}
