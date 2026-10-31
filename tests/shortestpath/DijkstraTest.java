package shortestpath;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DijkstraTest {
    public static void main(String[] args) {
        testDirectedGraph();
        testUndirectedGraph();
        testUnreachableVertices();
        testZeroWeightsAndParallelEdges();
        testNegativeWeights();
        testSingleAndInvalidInputs();
        testLargeDistancesAndOverflow();
        testProtection();
        testCountersAndStaleEntries();
        testRandomGraphs();

        System.out.println("All 10 Dijkstra tests passed.");
    }

    private static void testDirectedGraph() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 5);
        graph.addEdge(3, 4, 3);

        DijkstraResult result = Dijkstra.compute(graph, 0);
        verify(graph, result, new long[]{0, 3, 1, 4, 7});
        check(result.pathTo(4).equals(List.of(0, 2, 1, 3, 4)),
                "Incorrect route to vertex 4");

        System.out.println("Route to 4: " + result.pathTo(4));
        System.out.println("Distance to 4: "
                + result.distanceTo(4).orElseThrow());
        System.out.println("PASS: directed graph and routes");
    }

    private static void testUndirectedGraph() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 10);

        verify(graph, Dijkstra.compute(graph, 2),
                new long[]{6, 2, 0});
        System.out.println("PASS: undirected graph and nonzero source");
    }

    private static void testUnreachableVertices() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(2, 3, 1);

        verify(graph, Dijkstra.compute(graph, 0),
                new long[]{0, 2, -1, -1});
        System.out.println("PASS: unreachable vertices");
    }

    private static void testZeroWeightsAndParallelEdges() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 0, 0);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 0);
        graph.addEdge(1, 0, 0);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 2);
        graph.addEdge(2, 3, 0);
        graph.addEdge(3, 2, 0);

        verify(graph, Dijkstra.compute(graph, 0),
                new long[]{0, 0, 2, 2});
        System.out.println("PASS: zero cycles, equal weights and parallel edges");
    }

    private static void testNegativeWeights() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, -1);
        expectThrows(IllegalArgumentException.class,
                () -> Dijkstra.compute(graph, 0));

        WeightedGraph disconnected = new WeightedGraph(3, true);
        disconnected.addEdge(1, 2, -5);
        expectThrows(IllegalArgumentException.class,
                () -> Dijkstra.compute(disconnected, 0));

        System.out.println("PASS: negative weights rejected everywhere");
    }

    private static void testSingleAndInvalidInputs() {
        WeightedGraph single = new WeightedGraph(1, true);
        DijkstraResult result = Dijkstra.compute(single, 0);
        verify(single, result, new long[]{0});

        single.addEdge(0, 0, 0);
        verify(single, Dijkstra.compute(single, 0), new long[]{0});

        expectThrows(IndexOutOfBoundsException.class,
                () -> Dijkstra.compute(single, -1));
        expectThrows(IndexOutOfBoundsException.class,
                () -> Dijkstra.compute(single, 1));
        expectThrows(IndexOutOfBoundsException.class,
                () -> Dijkstra.compute(new WeightedGraph(0, true), 0));
        expectThrows(IndexOutOfBoundsException.class,
                () -> result.distanceTo(1));
        expectThrows(IndexOutOfBoundsException.class,
                () -> result.pathTo(-1));
        expectThrows(IndexOutOfBoundsException.class,
                () -> result.isReachable(1));

        System.out.println("PASS: single vertex and invalid inputs");
    }

    private static void testLargeDistancesAndOverflow() {
        WeightedGraph large = new WeightedGraph(3, true);
        large.addEdge(0, 1, 3_000_000_000L);
        large.addEdge(1, 2, 4_000_000_000L);
        verify(large, Dijkstra.compute(large, 0),
                new long[]{0, 3_000_000_000L, 7_000_000_000L});

        WeightedGraph boundary = new WeightedGraph(3, true);
        boundary.addEdge(0, 1, Long.MAX_VALUE);
        boundary.addEdge(1, 2, 0);
        verify(boundary, Dijkstra.compute(boundary, 0),
                new long[]{0, Long.MAX_VALUE, Long.MAX_VALUE});

        WeightedGraph overflow = new WeightedGraph(3, true);
        overflow.addEdge(0, 1, Long.MAX_VALUE);
        overflow.addEdge(1, 2, 1);
        expectThrows(ArithmeticException.class,
                () -> Dijkstra.compute(overflow, 0));

        // An overflowing detour must not hide a valid shorter route.
        WeightedGraph alternative = new WeightedGraph(3, true);
        alternative.addEdge(0, 1, 1);
        alternative.addEdge(1, 2, Long.MAX_VALUE);
        alternative.addEdge(0, 2, 5);
        verify(alternative, Dijkstra.compute(alternative, 0),
                new long[]{0, 1, 5});

        System.out.println("PASS: large distances and overflow handling");
    }

    private static void testProtection() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, 4);
        List<Edge> original = new ArrayList<>(graph.edges());

        DijkstraResult result = Dijkstra.compute(graph, 0);
        check(graph.edges().equals(original), "Input graph changed");
        expectThrows(UnsupportedOperationException.class,
                () -> result.pathTo(1).clear());

        graph.addEdge(0, 1, 1);
        check(result.distanceTo(1).orElseThrow() == 4,
                "Previous result changed with graph");

        long[] distances = {0, 4};
        boolean[] reachable = {true, true};
        int[] predecessors = {-1, 0};
        DijkstraResult snapshot = new DijkstraResult(
                0, distances, reachable, predecessors, result.metrics()
        );

        distances[1] = 99;
        reachable[1] = false;
        predecessors[1] = -1;

        check(snapshot.distanceTo(1).orElseThrow() == 4,
                "Result did not copy arrays");
        check(snapshot.pathTo(1).equals(List.of(0, 1)),
                "Predecessor array was not copied");

        System.out.println("PASS: input and result protection");
    }

    private static void testCountersAndStaleEntries() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 1);
        graph.addEdge(1, 3, 1);

        DijkstraResult result = Dijkstra.compute(graph, 0);
        verify(graph, result, new long[]{0, 2, 1, 3});

        DijkstraMetrics metrics = result.metrics();
        check(metrics.successfulRelaxations() == 4, "Relaxation count");
        check(metrics.queueOffers() == 5, "Queue offer count");
        check(metrics.queuePolls() == 5, "Queue poll count");
        check(metrics.edgeExaminations() == 4,
                "Stale entry caused repeated edge scanning");
        check(metrics.queueComparisons() > 0, "Missing comparisons");

        System.out.println("PASS: operation counters and stale entries");
    }

    private static void testRandomGraphs() {
        Random random = new Random(42);

        for (int trial = 0; trial < 100; trial++) {
            int vertices = 1 + random.nextInt(12);
            WeightedGraph graph = new WeightedGraph(
                    vertices, random.nextBoolean()
            );

            for (int edge = 0; edge < 3 * vertices; edge++) {
                graph.addEdge(
                        random.nextInt(vertices),
                        random.nextInt(vertices),
                        random.nextInt(21)
                );
            }

            for (int source = 0; source < vertices; source++) {
                verify(graph, Dijkstra.compute(graph, source),
                        referenceDistances(graph, source));
            }
        }

        System.out.println("PASS: 100 random graphs against Bellman-Ford");
    }

    private static long[] referenceDistances(
            WeightedGraph graph, int source
    ) {
        int vertices = graph.vertexCount();
        long[] distances = new long[vertices];
        Arrays.fill(distances, -1);
        distances[source] = 0;

        // Independent reference for small graphs with small weights.
        for (int pass = 0; pass < vertices - 1; pass++) {
            boolean changed = false;

            for (int from = 0; from < vertices; from++) {
                if (distances[from] < 0) {
                    continue;
                }

                for (Edge edge : graph.edgesFrom(from)) {
                    long candidate = distances[from] + edge.weight();
                    int to = edge.to();

                    if (distances[to] < 0 || candidate < distances[to]) {
                        distances[to] = candidate;
                        changed = true;
                    }
                }
            }

            if (!changed) {
                break;
            }
        }

        return distances;
    }

    private static void verify(
            WeightedGraph graph,
            DijkstraResult result,
            long[] expected
    ) {
        check(result.vertexCount() == graph.vertexCount(), "Vertex count");
        long expectedScans = 0;

        for (int vertex = 0; vertex < expected.length; vertex++) {
            List<Integer> path = result.pathTo(vertex);

            if (expected[vertex] < 0) {
                check(!result.isReachable(vertex), "Unexpected reachability");
                check(result.distanceTo(vertex).isEmpty(), "Expected no distance");
                check(path.isEmpty(), "Unreachable vertex has a path");
                continue;
            }

            check(result.isReachable(vertex), "Reachable vertex missing");
            check(result.distanceTo(vertex).orElseThrow() == expected[vertex],
                    "Incorrect distance to " + vertex);
            check(!path.isEmpty(), "Missing path");
            check(path.get(0) == result.source(), "Wrong path source");
            check(path.get(path.size() - 1) == vertex, "Wrong path target");

            boolean[] seen = new boolean[graph.vertexCount()];
            for (int step : path) {
                check(step >= 0 && step < seen.length, "Invalid path vertex");
                check(!seen[step], "Cycle in reconstructed path");
                seen[step] = true;
            }

            long pathWeight = 0;
            for (int i = 1; i < path.size(); i++) {
                boolean found = false;
                long minimumWeight = Long.MAX_VALUE;

                for (Edge edge : graph.edgesFrom(path.get(i - 1))) {
                    if (edge.to() == path.get(i)) {
                        found = true;
                        minimumWeight = Math.min(minimumWeight, edge.weight());
                    }
                }

                check(found, "Path contains a nonexistent edge");
                pathWeight = Math.addExact(pathWeight, minimumWeight);
            }

            check(pathWeight == expected[vertex], "Incorrect path weight");
            expectedScans += graph.edgesFrom(vertex).size();
        }

        DijkstraMetrics metrics = result.metrics();
        check(metrics.validationEdgeExaminations() == graph.edgeCount(),
                "Validation count");
        check(metrics.edgeExaminations() == expectedScans,
                "Reachable adjacency scan count");
        check(metrics.queueOffers() == metrics.successfulRelaxations() + 1,
                "Offers must include the source");
        check(metrics.queuePolls() == metrics.queueOffers(),
                "Queue must be fully processed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectThrows(
            Class<? extends RuntimeException> expected,
            Runnable action
    ) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Unexpected exception", exception);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
}