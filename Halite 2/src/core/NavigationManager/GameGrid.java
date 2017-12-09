package core.NavigationManager;

import Jama.Matrix;
import core.GameState;
import hlt.Entity;
import hlt.Position;
import hlt.Ship;

import java.util.*;

public class GameGrid
{
    private Matrix hitMap;
    private int numberOfRows;
    private int numberOfCols;

    private final double gridUnit = 14.0;

    public int getNumberOfRows() { return numberOfRows; }
    public int getNumberOfCols() { return numberOfCols; }
    public double getHitMapValue(final int row, final int col) { return hitMap.get(row, col); }

    public GameGrid(final GameState gameState)
    {
        this.hitMap = new Matrix((int)Math.ceil(gameState.getMapSizeX() / gridUnit), (int)Math.ceil(gameState.getMapSizeY() / gridUnit), 0.0);
        this.numberOfRows = this.hitMap.getRowDimension();
        this.numberOfCols = this.hitMap.getColumnDimension();

        fillHitMap(gameState, gameState.getEnemyShips());
    }

    private void fillHitMap(final GameState gameState, final ArrayList<Ship> ships)
    {
        for(final Ship ship: ships)
        {
            int index_row = (int)(ship.getXPos() / gridUnit);
            int index_col = (int)(ship.getYPos() / gridUnit);

            double previousValue = this.hitMap.get(index_row, index_col);
            this.hitMap.set(index_row, index_col, previousValue + 64.0);

            // above and below
            if (index_row > 0)
                this.hitMap.set(index_row - 1, index_col, previousValue + 32.0);

            if (index_row < numberOfRows - 1)
                this.hitMap.set(index_row + 1, index_col, previousValue + 32.0);

            // Left
            if ((index_row < numberOfRows - 1) && (index_col > 0))
                this.hitMap.set(index_row + 1, index_col - 1, previousValue + 32.0);

            if ((index_row > 0) && (index_col > 0))
                this.hitMap.set(index_row - 1, index_col - 1, previousValue + 32.0);

            if (index_col > 0)
                this.hitMap.set(index_row, index_col - 1, previousValue + 32.0);

            // Right
            if ((index_row < numberOfRows - 1) && (index_col < numberOfCols - 1))
                this.hitMap.set(index_row + 1, index_col + 1, previousValue + 32.0);

            if ((index_row > 0) && (index_col < numberOfCols - 1))
                this.hitMap.set(index_row - 1, index_col + 1, previousValue + 32.0);

            if (index_col < numberOfCols - 1)
                this.hitMap.set(index_row, index_col + 1, previousValue + 32.0);
        }
    }

    public Entity computeShortestPath(final Ship sourceShip, final Entity entity)
    {
        GridCell sourceGridCell = new GridCell((int)(sourceShip.getXPos() / gridUnit), (int)(sourceShip.getYPos() / gridUnit));
        Node source = new Node(this, sourceGridCell);

        GridCell endGridCell = new GridCell((int)(entity.getXPos() / gridUnit), (int)(entity.getYPos() / gridUnit));
        Node destination = new Node(this, endGridCell);

        List<Node> shortestPath = dijkstra(source, destination);

        if (shortestPath.size() > 2)
        {
            GridCell firstStep = shortestPath.get(2).getGridCell();
            return new Entity(-1, -1, firstStep.getRow() * gridUnit + gridUnit / 2, firstStep.getCol() * gridUnit + gridUnit / 2, 0, 0);
        }
        else if (shortestPath.size() == 1)
            return new Entity(-1, -1, endGridCell.getRow() * gridUnit + gridUnit / 2, endGridCell.getCol() * gridUnit + gridUnit / 2, 0, 0);
        else if (shortestPath.size() == 0)
            return entity;

        GridCell firstStep = shortestPath.get(1).getGridCell();

        return new Entity(-1, -1, firstStep.getRow() * gridUnit + gridUnit / 2, firstStep.getCol() * gridUnit + gridUnit / 2, 0, 0);
    }

    private static List<Node> dijkstra(Node source, Node destination)
    {
        source.setDistance(0.0);

        Set<Node> settledNodes = new HashSet<>();
        Set<Node> unsettledNodes = new HashSet<>();

        unsettledNodes.add(source);

        while (!unsettledNodes.isEmpty())
        {
            Node currentNode = getLowestDistanceNode(unsettledNodes);
            unsettledNodes.remove(currentNode);

            for (Map.Entry<Node,Double> adjacencyPair: currentNode.getAdjacentNodes(destination).entrySet())
            {
                Node adjacentNode = adjacencyPair.getKey();
                Double edgeWeight = adjacencyPair.getValue();

                if (!settledNodes.contains(adjacentNode))
                {
                    calculateMinimumDistance(adjacentNode, edgeWeight, currentNode);
                    unsettledNodes.add(adjacentNode);
                }
            }
            settledNodes.add(currentNode);

            if (currentNode.equals(destination))
                return currentNode.getShortestPath();
        }

        return new ArrayList<>();
    }

    private static Node getLowestDistanceNode(final Set<Node> unsettledNodes)
    {
        // The getLowestDistanceNode() method, returns the node with the
        // lowest distance from the unsettled nodes set.

        Node lowestDistanceNode = null;
        double lowestDistance = Integer.MAX_VALUE;

        for (Node node: unsettledNodes)
        {
            double nodeDistance = node.getDistance();
            if (nodeDistance < lowestDistance)
            {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }

    private static void calculateMinimumDistance(final Node evaluationNode, final Double edgeWeigh, final Node sourceNode)
    {
        // calculateMinimumDistance() method compares the actual distance with the newly
        // calculated one while following the newly explored path.

        double sourceDistance = sourceNode.getDistance();

        if (sourceDistance + edgeWeigh < evaluationNode.getDistance())
        {
            evaluationNode.setDistance(sourceDistance + edgeWeigh);
            LinkedList<Node> shortestPath = new LinkedList<>(sourceNode.getShortestPath());
            shortestPath.add(sourceNode);
            evaluationNode.setShortestPath(shortestPath);
        }
    }
}
