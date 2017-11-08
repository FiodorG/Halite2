package core;

public class GameState
{
    private int turn;

    public GameState()
    {
        turn = 0;
    }

    public int getTurn() { return turn; }

    public void updateGameState()
    {
        this.turn++;
    }
}
