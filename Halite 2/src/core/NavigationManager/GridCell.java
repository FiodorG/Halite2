package core.NavigationManager;

public class GridCell
{
    private int row;
    private int col;

    public int getRow() { return row; }
    public int getCol() { return col; }

    public GridCell(final int row, final int col)
    {
        this.row = row;
        this.col = col;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;

        if (this == object)
            return true;

        GridCell gridCell = (GridCell) object;
        return ((this.row == gridCell.getRow()) && (this.col == gridCell.getCol()));
    }
}