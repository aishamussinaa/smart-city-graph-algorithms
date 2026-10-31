package benchmark;

import graph.WeightedGraph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    private GraphGenerator() {
    }

    public static WeightedGraph connectedUndirected(
            int vertices, int edges, long seed
    ) {
        validateSize(vertices, edges, false);

        if (edges < vertices - 1) {
            throw new IllegalArgumentException(
                    "A connected graph requires at least V - 1 edges"
            );
        }

        WeightedGraph graph = new WeightedGraph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();

        // A random spanning tree guarantees connectivity.
        for (int vertex = 1; vertex < vertices; vertex++) {
            int parent = random.nextInt(vertex);
            addUnique(graph, used, parent, vertex, random);
        }

        while (graph.edgeCount() < edges) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            addUnique(graph, used, from, to, random);
        }

        return graph;
    }

    public static WeightedGraph dag(
            int vertices, int edges, long seed
    ) {
        validateSize(vertices, edges, false);

        WeightedGraph graph = new WeightedGraph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();

        int[] order = new int[vertices];
        for (int vertex = 0; vertex < vertices; vertex++) {
            order[vertex] = vertex;
        }

        // Randomize the topological order.
        for (int i = vertices - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temporary = order[i];
            order[i] = order[j];
            order[j] = temporary;
        }

        while (graph.edgeCount() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);

            if (first == second) {
                continue;
            }

            int earlier = Math.min(first, second);
            int later = Math.max(first, second);

            // Every edge goes forward in the chosen order.
            addUnique(
                    graph, used, order[earlier], order[later], random
            );
        }

        return graph;
    }

    public static WeightedGraph cyclicDirected(
            int vertices, int edges, long seed
    ) {
        validateSize(vertices, edges, true);

        if (vertices < 3 || edges < 3) {
            throw new IllegalArgumentException(
                    "This generator requires V >= 3 and E >= 3"
            );
        }

        WeightedGraph graph = new WeightedGraph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();

        // Guarantee a cycle without forcing all vertices into one SCC.
        addUnique(graph, used, 0, 1, random);
        addUnique(graph, used, 1, 2, random);
        addUnique(graph, used, 2, 0, random);

        while (graph.edgeCount() < edges) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            addUnique(graph, used, from, to, random);
        }

        return graph;
    }

    private static void addUnique(
            WeightedGraph graph,
            Set<Long> used,
            int from,
            int to,
            Random random
    ) {
        if (from == to) {
            return;
        }

        if (!graph.isDirected() && from > to) {
            int temporary = from;
            from = to;
            to = temporary;
        }

        long key = ((long) from << 32) | (to & 0xffffffffL);

        if (used.add(key)) {
            long weight = 1 + random.nextInt(100);
            graph.addEdge(from, to, weight);
        }
    }

    private static void validateSize(
            int vertices, int edges, boolean allowBothDirections
    ) {
        if (vertices < 1) {
            throw new IllegalArgumentException(
                    "Vertex count must be positive"
            );
        }

        long maximumEdges = (long) vertices * (vertices - 1);
        if (!allowBothDirections) {
            maximumEdges /= 2;
        }

        if (edges < 0 || edges > maximumEdges) {
            throw new IllegalArgumentException(
                    "Invalid edge count: " + edges
            );
        }
    }
}