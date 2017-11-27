package core.CombatManager;

import java.util.ArrayList;
import java.util.LinkedList;

public class Node<T>
{
    private T data;
    private Node<T> parent;
    private LinkedList<Node<T>> parents;
    private ArrayList<Node<T>> children;
    private int depth;

    private boolean isMaxPlayer;
    private double score;

    public Node(final T data, final Node<T> parent, final LinkedList<Node<T>> parents, final ArrayList<Node<T>> children, final boolean isMaxPlayer, final int depth, final double score)
    {
        this.data = data;
        this.parent = parent;
        this.parents = new LinkedList<>(parents);
        this.children = children;
        this.depth = depth;
        this.isMaxPlayer = isMaxPlayer;
        this.score = score;
    }

    public Node(final Node<T> node)
    {
        this.data = node.getData();
        this.parent = node.getParent();
        this.parents = new LinkedList<>();
        this.parents.addAll(node.getParents());
        this.children = new ArrayList<>();
        this.children.addAll(node.getChildren());
        this.depth = node.getDepth();
        this.isMaxPlayer = node.isMaxPlayer();
        this.score = node.getScore();
    }

    public ArrayList<Node<T>> getChildren() { return children; }
    public Node<T> getParent() { return parent; }
    public LinkedList<Node<T>> getParents() { return parents; }
    public T getData() { return data; }
    public int getDepth() { return depth; }
    public double getScore() { return score; }
    public boolean isMaxPlayer() { return isMaxPlayer; }

    public void setParent(Node<T> parent) { this.parent = parent; }
    public void addToParents(Node<T> parent) { this.parents.addFirst(parent); }
    public void setDepth(int depth) { this.depth = depth; }
    public void setScore(double score) { this.score = score; }
    public void setMaxPlayer(boolean maxPlayer) { isMaxPlayer = maxPlayer; }

    public void addNodeToChildren(final T data, final boolean isMaxPlayer, final int depth, final double score)
    {
        Node<T> newNode = new Node<>(data, this, parents, new ArrayList<>(), isMaxPlayer, depth, score);
        newNode.addToParents(newNode);
        this.children.add(newNode);
    }

    @Override
    public String toString()
    {
        return "Node[" +
                "data=" + data.toString() +
                ", op=" + (isMaxPlayer? "Max" : "Min") +
                ", score=" + String.format("%.2f", score) +
                ", depth=" + depth +
                ", children=" + children.size() +
                "]";
    }
}