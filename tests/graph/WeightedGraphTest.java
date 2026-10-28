package graph;

import java.util.List;

public class WeightedGraphTest {
    public static void main(String[] args) {
        testUndirectedGraph();
        testDirectedGraph();
        testSingleAndEmptyGraphs();
        testParallelEdgesAndSelfLoop();
        testInvalidInputs();
        testReadOnlyLists();

        System.out.println("All 6 graph tests passed.");
    }

    private static void testUndirectedGraph() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 7);

        check(graph.vertexCount() == 3, "Vertex count");
        check(graph.edgeCount() == 1, "Count road only once");
        check(!graph.isDirected(), "Undirected flag");

        check(graph.edgesFrom(0).equals(
                List.of(new Edge(0, 1, 7))), "Forward direction");

        check(graph.edgesFrom(1).equals(
                List.of(new Edge(1, 0, 7))), "Reverse direction");

        check(graph.edgesFrom(2).isEmpty(), "Isolated vertex");
        System.out.println("PASS: undirected graph");
    }

    private static void testDirectedGraph() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, 3_000_000_000L);

        check(graph.isDirected(), "Directed flag");
        check(graph.edgeCount() == 1, "Directed edge count");
        check(graph.edgesFrom(1).isEmpty(), "No reverse edge");

        check(graph.edgesFrom(0).equals(
                        List.of(new Edge(0, 1, 3_000_000_000L))),
                "Preserve weight larger than int");

        System.out.println("PASS: directed graph");
    }

    private static void testSingleAndEmptyGraphs() {
        WeightedGraph single = new WeightedGraph(1, false);

        check(single.vertexCount() == 1, "Single vertex");
        check(single.edgeCount() == 0, "No roads");
        check(single.edgesFrom(0).isEmpty(), "Empty adjacency");

        WeightedGraph empty = new WeightedGraph(0, true);
        check(empty.vertexCount() == 0, "Empty graph");
        check(empty.edges().isEmpty(), "Empty edge list");

        System.out.println("PASS: single and empty graphs");
    }

    private static void testParallelEdgesAndSelfLoop() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 1, 0);

        check(graph.edgeCount() == 3, "Keep duplicate roads");
        check(graph.edgesFrom(0).size() == 2, "Parallel edges");

        check(graph.edgesFrom(1).equals(List.of(
                new Edge(1, 0, 5),
                new Edge(1, 0, 5),
                new Edge(1, 1, 0)
        )), "Reverse edges and one self-loop");

        System.out.println("PASS: parallel edges and self-loop");
    }

    private static void testInvalidInputs() {
        expectException(IllegalArgumentException.class,
                () -> new WeightedGraph(-1, false));

        WeightedGraph graph = new WeightedGraph(2, false);

        expectException(IndexOutOfBoundsException.class,
                () -> graph.addEdge(-1, 1, 4));

        expectException(IndexOutOfBoundsException.class,
                () -> graph.addEdge(0, 2, 4));

        expectException(IndexOutOfBoundsException.class,
                () -> graph.edgesFrom(2));

        check(graph.edgeCount() == 0, "Invalid edges not added");
        check(graph.edgesFrom(0).isEmpty(), "No partial insertion");
        check(graph.edgesFrom(1).isEmpty(), "Other vertex unchanged");

        System.out.println("PASS: invalid inputs");
    }

    private static void testReadOnlyLists() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 1, 4);

        expectException(UnsupportedOperationException.class,
                () -> graph.edges().clear());

        expectException(UnsupportedOperationException.class,
                () -> graph.edgesFrom(0).add(new Edge(0, 1, 9)));

        check(graph.edgeCount() == 1, "Edge list protected");
        check(graph.edgesFrom(0).size() == 1, "Adjacency protected");

        System.out.println("PASS: read-only lists");
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