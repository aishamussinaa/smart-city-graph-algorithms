package benchmark;

import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class GraphGeneratorTest {
    private static final int[][] SIZES = {
            {10, 30},
            {100, 500},
            {1000, 5000}
    };

    private static final long SEED = 42;

    public static void main(String[] args) {
        testConnectedGraphs();
        testDags();
        testCyclicGraphs();
        testBoundaryCases();
        testInvalidInputs();

        System.out.println("All 5 graph generator tests passed.");
    }

    private static void testConnectedGraphs() {
        for (int[] size : SIZES) {
            WeightedGraph graph = GraphGenerator.connectedUndirected(
                    size[0], size[1], SEED
            );
            WeightedGraph repeated = GraphGenerator.connectedUndirected(
                    size[0], size[1], SEED
            );

            verifyBasic(graph, size[0], size[1], false);
            check(graph.edges().equals(repeated.edges()),
                    "Undirected generation is not reproducible");
            verifyConnected(graph);
        }

        System.out.println("PASS: connected graphs at all required sizes");
    }

    private static void testDags() {
        for (int[] size : SIZES) {
            WeightedGraph graph = GraphGenerator.dag(
                    size[0], size[1], SEED
            );
            WeightedGraph repeated = GraphGenerator.dag(
                    size[0], size[1], SEED
            );

            verifyBasic(graph, size[0], size[1], true);
            check(graph.edges().equals(repeated.edges()),
                    "DAG generation is not reproducible");

            TopologicalResult result = TopologicalSort.compute(graph);
            check(result.isDag(), "Generated DAG contains a cycle");
            check(result.order().size() == size[0], "Incomplete order");

            int[] position = new int[size[0]];
            boolean[] seen = new boolean[size[0]];

            for (int i = 0; i < result.order().size(); i++) {
                int vertex = result.order().get(i);
                check(vertex >= 0 && vertex < size[0], "Invalid vertex");
                check(!seen[vertex], "Repeated vertex in order");
                seen[vertex] = true;
                position[vertex] = i;
            }

            for (Edge edge : graph.edges()) {
                check(position[edge.from()] < position[edge.to()],
                        "Edge violates the topological order");
            }
        }

        System.out.println("PASS: DAGs at all required sizes");
    }

    private static void testCyclicGraphs() {
        for (int[] size : SIZES) {
            WeightedGraph graph = GraphGenerator.cyclicDirected(
                    size[0], size[1], SEED
            );
            WeightedGraph repeated = GraphGenerator.cyclicDirected(
                    size[0], size[1], SEED
            );

            verifyBasic(graph, size[0], size[1], true);
            check(graph.edges().equals(repeated.edges()),
                    "Cyclic generation is not reproducible");

            check(hasEdge(graph, 0, 1)
                            && hasEdge(graph, 1, 2)
                            && hasEdge(graph, 2, 0),
                    "Required cycle is missing");
            check(TopologicalSort.compute(graph).hasCycle(),
                    "Cycle was not detected");
        }

        System.out.println("PASS: cyclic graphs at all required sizes");
    }

    private static void testBoundaryCases() {
        WeightedGraph single = GraphGenerator.connectedUndirected(1, 0, SEED);
        verifyBasic(single, 1, 0, false);
        verifyConnected(single);

        WeightedGraph emptyDag = GraphGenerator.dag(1, 0, SEED);
        verifyBasic(emptyDag, 1, 0, true);
        check(TopologicalSort.compute(emptyDag).isDag(), "Singleton DAG");

        WeightedGraph tree = GraphGenerator.connectedUndirected(10, 9, SEED);
        verifyBasic(tree, 10, 9, false);
        verifyConnected(tree);

        verifyBasic(
                GraphGenerator.connectedUndirected(4, 6, SEED),
                4, 6, false
        );

        WeightedGraph completeDag = GraphGenerator.dag(4, 6, SEED);
        verifyBasic(completeDag, 4, 6, true);
        check(TopologicalSort.compute(completeDag).isDag(), "Complete DAG");

        WeightedGraph cycle = GraphGenerator.cyclicDirected(3, 3, SEED);
        verifyBasic(cycle, 3, 3, true);
        check(TopologicalSort.compute(cycle).hasCycle(), "Minimal cycle");

        verifyBasic(
                GraphGenerator.cyclicDirected(3, 6, SEED),
                3, 6, true
        );

        System.out.println("PASS: boundary cases and maximum edge counts");
    }

    private static void testInvalidInputs() {
        expectInvalid(() -> GraphGenerator.connectedUndirected(0, 0, SEED));
        expectInvalid(() -> GraphGenerator.connectedUndirected(5, 3, SEED));
        expectInvalid(() -> GraphGenerator.connectedUndirected(3, 4, SEED));
        expectInvalid(() -> GraphGenerator.dag(0, 0, SEED));
        expectInvalid(() -> GraphGenerator.dag(3, -1, SEED));
        expectInvalid(() -> GraphGenerator.dag(3, 4, SEED));
        expectInvalid(() -> GraphGenerator.cyclicDirected(2, 2, SEED));
        expectInvalid(() -> GraphGenerator.cyclicDirected(3, 2, SEED));
        expectInvalid(() -> GraphGenerator.cyclicDirected(3, 7, SEED));

        System.out.println("PASS: invalid generation requests rejected");
    }

    private static void verifyBasic(
            WeightedGraph graph, int vertices, int edges, boolean directed
    ) {
        check(graph.vertexCount() == vertices, "Incorrect vertex count");
        check(graph.edgeCount() == edges, "Incorrect edge count");
        check(graph.isDirected() == directed, "Incorrect graph type");

        Set<Long> pairs = new HashSet<>();

        for (Edge edge : graph.edges()) {
            int from = edge.from();
            int to = edge.to();

            check(from >= 0 && from < vertices, "Invalid source");
            check(to >= 0 && to < vertices, "Invalid destination");
            check(from != to, "Unexpected self-loop");
            check(edge.weight() >= 1 && edge.weight() <= 100,
                    "Weight outside the required generator range");

            if (!directed && from > to) {
                int temporary = from;
                from = to;
                to = temporary;
            }

            long key = ((long) from << 32) | (to & 0xffffffffL);
            check(pairs.add(key), "Duplicate edge");
        }
    }

    private static void verifyConnected(WeightedGraph graph) {
        boolean[] visited = new boolean[graph.vertexCount()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        visited[0] = true;
        queue.addLast(0);
        int count = 0;

        while (!queue.isEmpty()) {
            int vertex = queue.removeFirst();
            count++;

            for (Edge edge : graph.edgesFrom(vertex)) {
                if (!visited[edge.to()]) {
                    visited[edge.to()] = true;
                    queue.addLast(edge.to());
                }
            }
        }

        check(count == graph.vertexCount(), "Graph is disconnected");
    }

    private static boolean hasEdge(WeightedGraph graph, int from, int to) {
        for (Edge edge : graph.edgesFrom(from)) {
            if (edge.to() == to) {
                return true;
            }
        }
        return false;
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}