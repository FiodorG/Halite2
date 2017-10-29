package core;

public class BehaviourManager
{
    public enum behaviourTypes{
        RUSH,
        DEFEND,
        MIXED,
        CALIBRATED,
    }
    private behaviourTypes behaviourType;

    public BehaviourManager(final behaviourTypes behaviour)
    {
        this.behaviourType = behaviour;
    }
}
