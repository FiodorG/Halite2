package core;

import hlt.DebugLog;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Timer
{
    private final LocalDateTime startTime;
    private final LocalDateTime currentTime;
    private LocalDateTime currentTurnStartTime;
    private final double warmupTime = 30000; // in milliseconds
    private final double turnTime = 2000; // in milliseconds

    public Timer()
    {
        this.startTime = LocalDateTime.now();
        this.currentTime = startTime;
    }

    public void setCurrentTurnStartTime()
    {
        currentTurnStartTime = LocalDateTime.now();
    }

    public double getCurrentTurnElapsedTime()
    {
        double currentTurnElapsedTime = currentTurnStartTime.until(LocalDateTime.now(), ChronoUnit.MILLIS);
        DebugLog.addLog(Double.toString(currentTurnElapsedTime));
        return currentTurnElapsedTime;
    }

    public boolean timeToEndTurn()
    {
        return getCurrentTurnElapsedTime() > getTurnTime() - Config.timeEpsilon;
    }

    public double getTurnTime() { return turnTime; }
}