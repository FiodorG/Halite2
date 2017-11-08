package core;

public class Config
{
    /* Name of the bot */
    public static final String botName = "v5";

    /* Fighting style of the bot */
    public static final String behaviour = "RUSH";

    /* Display info in log */
    public static final boolean verbose = false;

    /* Cutoff epsilon for shutting down turn in milliseconds */
    public static final double timeEpsilon = 10;

    /* Each ship selects this number of close objectives */
    public static final int numberOfClosestObjectives = 5;
}
