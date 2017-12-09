import java.util.HashMap;

public class MyBotRush
{
    public static HashMap<String,Object> gameDefinitions()
    {
        HashMap<String,Object> gameDefinitions = new HashMap<>();
        gameDefinitions.put("botName",                  "RUSH");    // Name of the bot that will appear on screen

        gameDefinitions.put("testArgument",             1);         // Argument to quickly test new features

        gameDefinitions.put("maxPriority",              100.0);     // Higher bound of priorities (except special ones)
        gameDefinitions.put("distanceDiscountExponent", 2.0);       // Power exponent to discount by distance

        gameDefinitions.put("colonizationTurns",        50);        // Prioritize colonization during first n turns
        gameDefinitions.put("colonizationBump",         50.0);      // Priority Bump when colonization phase
        gameDefinitions.put("colonizationMinShips",     0);         // Minimum ships allocated to colonization

        gameDefinitions.put("maxRushDistance",          120.0);     // Rush if enemy is closer than
        gameDefinitions.put("rushPriority",             0.0);       // Priority for a rush order
        gameDefinitions.put("antiRushPriority",         10e30);     // Priority for a antirush order
        gameDefinitions.put("antiRushDistance",         50.0);      // Enable Antirush if enemy closer than
        gameDefinitions.put("rushTurns",                40);        // Enable rush/antirush during first n turns
        gameDefinitions.put("rushMaxObjectives",        0);         // Max number of rush objectives issued
        gameDefinitions.put("rushMaxShipsPerObjective", 0);         // Number of ships sent to rush by rush objective

        gameDefinitions.put("defendPriority",           100.0);     // Priority for defending planet under attack
        gameDefinitions.put("enemyShipsToDefend",       5);         // Enemy ships around your planet required to send defend order

        gameDefinitions.put("crashPriority",            0.0);       // Priority for crashing into other planets
        gameDefinitions.put("enemyShipsToCrash",        5);         // Crash when #enemies are in range
        gameDefinitions.put("crashBelowHealth",         63.0);      // Crash when health below level

        gameDefinitions.put("attackShipPriority",       100.0);     // Priority for regular enemy ships
        gameDefinitions.put("attackDockedShipPriority", 100.0);     // Priority for docked enemy ships
        gameDefinitions.put("lurePriority",             0.0);       // Priority to send ship to lure adversary
        gameDefinitions.put("assassinationPriority",    0.0);       // Priority to send ships around to try to target docked ships
        gameDefinitions.put("assassinationTurns",       70);        // Turns after which initiate assassination missions
        gameDefinitions.put("numberOfAssassinationObjectives", 1);  // Number of assassination objectives to issue

        gameDefinitions.put("fleePriority",             10e40);     // Priority to flee at end of game when getting smashed

        return gameDefinitions;
    }

    public static void main(final String[] args)
    {
        MyBotInternal.main(gameDefinitions(), 2);
    }
}
