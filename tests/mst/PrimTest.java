package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;

import java.util.List;
import java.util.Random;

public class PrimTest {
    public static void main(String[] args) {
        testConnectedGraph();
        testDisconnectedGraph();
        testEqualWeights();
        testParallelEdgesAndSelfLoops();
        testNegativeAndLargeWeights();
        testSingleAndEmptyGraphs();
        testDirectedGraphRejected();
        testInputAndResultProtection();
        testCounters();
        testOverflow();
        testRandomizedComparison();

        System.out.println("All 11 Prim tests passed.");
    }

    private static WeightedGraph sampleCity() {
        WeightedGraph graph = new WeightedGraph(5, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 6);
        graph.addEdge(1, 2, 3);
        graph.addEdge(3, 4, 2);
        graph.addEdge(2, 3, 5);
        return graph;
    }

    private static void testConnectedGraph() {
        WeightedGraph graph = sampleCity();
        verifyForest(graph, Prim.compute(graph), 14, 1);
        System.out.println("PASS: connected graph");
    }

    private static void testDisconnectedGraph() {
        WeightedGraph graph = new WeightedGraph(6, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 6);
        graph.addEdge(1, 2, 3);
        graph.addEdge(3, 4, 2);

        verifyForest(graph, Prim.compute(graph), 9, 3);
        System.out.println("PASS: disconnected graph");
    }

    private static void testEqualWeights() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 0, 1);
        graph.addEdge(0, 2, 1);

        verifyForest(graph, Prim.compute(graph), 3, 1);
        System.out.println("PASS: multiple MSTs and equal weights");
    }

    private static void testParallelEdgesAndSelfLoops() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 0, -100);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);

        verifyForest(graph, Prim.compute(graph), 5, 1);
        System.out.println("PASS: parallel edges and self-loops");
    }

    private static void testNegativeAndLargeWeights() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, -3_000_000_000L);
        graph.addEdge(1, 2, -2);
        graph.addEdge(0, 2, 4_000_000_000L);

        verifyForest(
                graph,
                Prim.compute(graph),
                -3_000_000_002L,
                1
        );

        System.out.println("PASS: negative and large weights");
    }

    private static void testSingleAndEmptyGraphs() {
        WeightedGraph single = new WeightedGraph(1, false);
        single.addEdge(0, 0, -7);
        verifyForest(single, Prim.compute(single), 0, 1);

        WeightedGraph empty = new WeightedGraph(0, false);
        verifyForest(empty, Prim.compute(empty), 0, 0);

        System.out.println("PASS: single and empty graphs");
    }

    private static void testDirectedGraphRejected() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, 4);

        expectException(
                IllegalArgumentException.class,
                () -> Prim.compute(graph)
        );

        System.out.println("PASS: directed graph rejected");
    }

    private static void testInputAndResultProtection() {
        WeightedGraph graph = sampleCity();
        List<Edge> originalEdges = List.copyOf(graph.edges());

        MstResult result = Prim.compute(graph);

        check(graph.edges().equals(originalEdges),
                "Algorithm must not change input edge order");

        expectException(
                UnsupportedOperationException.class,
                () -> result.edges().clear()
        );

        check(result.edges().size() == 4,
                "Result remains unchanged");

        System.out.println("PASS: input and result protection");
    }

    private static void testCounters() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 3);
        graph.addEdge(2, 3, 4);

        MstResult result = Prim.compute(graph);
        verifyForest(graph, result, 7, 1);

        MstMetrics metrics = result.metrics();

        check(metrics.edgeExaminations() == 8,
                "Examine both adjacency entries of every road");
        check(metrics.queueOffers() == 4,
                "Each road enters the queue once");
        check(metrics.queuePolls() == 4,
                "Includes removal of the stale edge");
        check(metrics.weightComparisons() > 0,
                "Queue comparisons are counted");
        check(metrics.dsuFindCalls() == 0,
                "Prim does not use DSU find");
        check(metrics.dsuUnionCalls() == 0,
                "Prim does not use DSU union");

        System.out.println("PASS: operation counters");
    }

    private static void testOverflow() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, Long.MAX_VALUE);
        graph.addEdge(1, 2, Long.MAX_VALUE);

        expectException(
                ArithmeticException.class,
                () -> Prim.compute(graph)
        );

        System.out.println("PASS: weight overflow detected");
    }

    private static void testRandomizedComparison() {
        Random random = new Random(42);

        for (int trial = 0; trial < 100; trial++) {
            int vertices = 2 + random.nextInt(19);
            WeightedGraph graph = new WeightedGraph(vertices, false);

            // A tree guarantees that the generated graph is connected.
            for (int vertex = 1; vertex < vertices; vertex++) {
                int parent = random.nextInt(vertex);
                long weight = random.nextInt(41) - 20;

                if (random.nextBoolean()) {
                    graph.addEdge(vertex, parent, weight);
                } else {
                    graph.addEdge(parent, vertex, weight);
                }
            }

            // Additional edges may include parallel edges and self-loops.
            for (int i = 0; i < vertices * 2; i++) {
                int from = random.nextInt(vertices);
                int to = random.nextInt(vertices);
                long weight = random.nextInt(41) - 20;
                graph.addEdge(from, to, weight);
            }

            MstResult kruskal = Kruskal.compute(graph);
            MstResult prim = Prim.compute(graph);

            check(prim.totalWeight() == kruskal.totalWeight(),
                    "MST weights differ in trial " + trial);

            verifyForest(graph, kruskal, kruskal.totalWeight(), 1);
            verifyForest(graph, prim, kruskal.totalWeight(), 1);
        }

        System.out.println("PASS: comparison on 100 random graphs");
    }

    private static void verifyForest(
            WeightedGraph graph,
            MstResult result,
            long expectedWeight,
            int expectedComponents) {

        check(result.totalWeight() == expectedWeight,
                "Minimum total weight");
        check(result.components() == expectedComponents,
                "Component count");
        check(result.isConnected() == (expectedComponents == 1),
                "Connectivity flag");
        check(result.edges().size()
                        == graph.vertexCount() - expectedComponents,
                "A forest must have V - C edges");

        DisjointSetUnion forest =
                new DisjointSetUnion(graph.vertexCount());

        long sum = 0;

        for (Edge edge : result.edges()) {
            Edge reverse = new Edge(
                    edge.to(), edge.from(), edge.weight()
            );

            check(
                    graph.edges().contains(edge)
                            || graph.edges().contains(reverse),
                    "Selected undirected edge must exist in input"
            );

            check(forest.union(edge.from(), edge.to()),
                    "Selected edges must not form a cycle");

            sum = Math.addExact(sum, edge.weight());
        }

        check(sum == result.totalWeight(),
                "Reported weight matches selected edges");
        check(forest.componentCount() == expectedComponents,
                "Selected edges produce the expected components");

        for (Edge edge : graph.edges()) {
            check(forest.connected(edge.from(), edge.to()),
                    "Forest must span every original component");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectException(
            Class<? extends RuntimeException> expected,
            Runnable action) {

        try {
            action.run();
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                return;
            }

            throw new AssertionError("Unexpected exception", exception);
        }

        throw new AssertionError(
                "Expected exception: " + expected.getSimpleName()
        );
    }
}