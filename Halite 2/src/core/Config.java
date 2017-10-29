package core;

public class Config
{
    /* Name of the bot */
    public static final String botName = "v3";

    /* Fighting style of the bot */
    public static final BehaviourManager.behaviourTypes behaviour = BehaviourManager.behaviourTypes.RUSH;

    /* Display info in log */
    public static final boolean verbose = false;

    /* Cutoff epsilon for shutting down turn in milliseconds */
    public static final double timeEpsilon = 10;
}
