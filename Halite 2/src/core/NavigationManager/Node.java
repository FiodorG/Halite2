package core.NavigationManager;

import core.Objective;

import java.util.*;

public class Node
{
    private GameGrid gameGrid;
    private GridCell gridCell;
    private List<Node> shortestPath = new LinkedList<>();
    private Double distance = Double.MAX_VALUE;

    public GridCell getGridCell() { return gridCell; }
    public List<Node> getShortestPath() { return shortestPath; }
    public Double getDistance() { return distance; }

    public void setShortestPath(List<Node> shortestPath) { this.shortestPath = shortestPath; }
    public void setDistance(Double distance) { this.distance = distance; }

    public Node(final GameGrid gameGrid, final GridCell gridCell)
    {
        this.gameGrid = gameGrid;
        this.gridCell = gridCell;
    }

    public Map<Node,Double> getAdjacentNodes(final Node destination)
    {
        Map<Node,Double> adjacentNodes = new HashMap<>();

        int numberOfRows = gameGrid.getNumberOfRows();
        int numberOfCols = gameGrid.getNumberOfCols();

        int row = gridCell.getRow();
        int col = gridCell.getCol();

        if (row > 0)
            adjacentNodes.put(new Node(gameGrid, new GridCell(row - 1, col)), gameGrid.getHitMapValue(row - 1, col));

        if (row < numberOfRows - 1)
            adjacentNodes.put(new Node(gameGrid, new GridCell(row + 1, col)), gameGrid.getHitMapValue(row + 1, col));

        if (col > 0)
            adjacentNodes.put(new Node(gameGrid, new GridCell(row, col - 1)), gameGrid.getHitMapValue(row, col - 1));

        if (col < numberOfCols - 1)
            adjacentNodes.put(new Node(gameGrid, new GridCell(row, col + 1)), gameGrid.getHitMapValue(row, col + 1));

        return adjacentNodes;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != getClass())
            return false;

        if (this == object)
            return true;

        Node node = (Node) object;
        return (this.gridCell.getRow() == node.getGridCell().getRow()) && (this.gridCell.getCol() == node.getGridCell().getCol());
    }

    @Override
    public int hashCode() { return Objects.hash(gridCell.getRow(), gridCell.getCol()); }

    @Override
    public String toString()
    {
        return "Node(" + gridCell.getRow() + ", " + gridCell.getCol() + ")";
    }
}