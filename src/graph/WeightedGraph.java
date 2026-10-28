package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeightedGraph {
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges;

    public WeightedGraph(int vertexCount, boolean directed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException(
                    "Vertex count must be non-negative"
            );
        }

        this.directed = directed;
        this.adjacency = new ArrayList<>();
        this.edges = new ArrayList<>();

        for (int i = 0; i < vertexCount; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int vertexCount() {
        return adjacency.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean isDirected() {
        return directed;
    }

    public void addEdge(int from, int to, long weight) {
        validateVertex(from);
        validateVertex(to);

        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);

        if (!directed && from != to) {
            adjacency.get(to).add(new Edge(to, from, weight));
        }
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> edgesFrom(int vertex) {
        validateVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount()) {
            throw new IndexOutOfBoundsException(
                    "Invalid vertex: " + vertex
            );
        }
    }
}