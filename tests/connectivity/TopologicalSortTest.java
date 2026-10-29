package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.List;
import java.util.Random;

public class TopologicalSortTest {
    public static void main(String[] args) {
        testDag();
        testCycles();
        testDisconnectedDag();
        testParallelEdges();
        testSingleAndEmptyGraphs();
        testUndirectedGraphRejected();
        testInputAndResultProtection();
        testRandomDags();

        System.out.println("All 8 topological sort tests passed.");
    }

    private static WeightedGraph sampleDag() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(3, 1, 5);
        graph.addEdge(3, 0, 2);
        graph.addEdge(1, 2, 4);
        graph.addEdge(0, 2, 7);
        return graph;
    }

    private static void testDag() {
        WeightedGraph graph = sampleDag();
        TopologicalResult result = TopologicalSort.compute(graph);

        verifyOrder(graph, result);

        System.out.println("DAG order: " + result.order());
        System.out.println("PASS: DAG and operation counters");
    }

    private static void testCycles() {
        WeightedGraph cycle = new WeightedGraph(3, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 2, 1);
        cycle.addEdge(2, 0, 1);

        TopologicalResult cycleResult = TopologicalSort.compute(cycle);
        verifyCycle(cycleResult);

        check(cycleResult.edgeExaminations() == 3,
                "Only indegree initialization examines edges");
        check(cycleResult.queuePolls() == 0,
                "No vertex has zero indegree");

        WeightedGraph mixed = new WeightedGraph(5, true);
        mixed.addEdge(0, 1, 1);
        mixed.addEdge(1, 2, 1);
        mixed.addEdge(2, 1, 1);
        mixed.addEdge(3, 4, 1);

        TopologicalResult mixedResult = TopologicalSort.compute(mixed);
        verifyCycle(mixedResult);

        check(mixedResult.queueOffers() == 3,
                "Only vertices 0, 3, and 4 enter the queue");
        check(mixedResult.queuePolls() == 3,
                "Acyclic vertices are processed before stopping");
        check(mixedResult.edgeExaminations() == 6,
                "Four initialization scans and two outgoing scans");

        WeightedGraph selfLoop = new WeightedGraph(1, true);
        selfLoop.addEdge(0, 0, 0);
        verifyCycle(TopologicalSort.compute(selfLoop));

        System.out.println("PASS: cycles, partial processing, and self-loop");
    }

    private static void testDisconnectedDag() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, 1);

        verifyOrder(graph, TopologicalSort.compute(graph));
        System.out.println("PASS: disconnected DAG and isolated vertex");
    }

    private static void testParallelEdges() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 2, 5);

        verifyOrder(graph, TopologicalSort.compute(graph));
        System.out.println("PASS: parallel edges");
    }

    private static void testSingleAndEmptyGraphs() {
        WeightedGraph single = new WeightedGraph(1, true);
        TopologicalResult singleResult = TopologicalSort.compute(single);

        verifyOrder(single, singleResult);
        check(singleResult.order().equals(List.of(0)),
                "Single vertex order");

        WeightedGraph empty = new WeightedGraph(0, true);
        TopologicalResult emptyResult = TopologicalSort.compute(empty);

        verifyOrder(empty, emptyResult);
        check(emptyResult.order().isEmpty(), "Empty DAG order");

        System.out.println("PASS: single and empty graphs");
    }

    private static void testUndirectedGraphRejected() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 1, 1);

        expectException(
                IllegalArgumentException.class,
                () -> TopologicalSort.compute(graph)
        );

        System.out.println("PASS: undirected graph rejected");
    }

    private static void testInputAndResultProtection() {
        WeightedGraph graph = sampleDag();
        List<Edge> originalEdges = List.copyOf(graph.edges());

        TopologicalResult result = TopologicalSort.compute(graph);

        check(graph.edges().equals(originalEdges),
                "Input edge list is unchanged");

        expectException(
                UnsupportedOperationException.class,
                () -> result.order().clear()
        );

        verifyOrder(graph, result);
        System.out.println("PASS: input and result protection");
    }

    private static void testRandomDags() {
        Random random = new Random(42);

        for (int trial = 0; trial < 100; trial++) {
            int vertices = 1 + random.nextInt(20);
            int[] permutation = new int[vertices];

            for (int i = 0; i < vertices; i++) {
                permutation[i] = i;
            }

            // Shuffle vertex IDs to obtain a hidden valid ordering.
            for (int i = vertices - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temporary = permutation[i];
                permutation[i] = permutation[j];
                permutation[j] = temporary;
            }

            WeightedGraph graph = new WeightedGraph(vertices, true);

            // Edges point forward in the permutation, preventing cycles.
            for (int i = 0; i < vertices; i++) {
                for (int j = i + 1; j < vertices; j++) {
                    if (random.nextDouble() < 0.3) {
                        graph.addEdge(
                                permutation[i],
                                permutation[j],
                                random.nextInt(21) - 10
                        );
                    }
                }
            }

            verifyOrder(graph, TopologicalSort.compute(graph));
        }

        System.out.println("PASS: 100 random DAGs");
    }

    private static void verifyOrder(
            WeightedGraph graph,
            TopologicalResult result) {

        check(result.isDag(), "Graph must be recognized as a DAG");
        check(!result.hasCycle(), "No cycle");
        check(result.order().size() == graph.vertexCount(),
                "Every vertex must appear");

        boolean[] seen = new boolean[graph.vertexCount()];
        int[] position = new int[graph.vertexCount()];

        for (int index = 0; index < result.order().size(); index++) {
            int vertex = result.order().get(index);

            check(vertex >= 0 && vertex < graph.vertexCount(),
                    "Vertex ID must be valid");
            check(!seen[vertex], "Vertex must appear only once");

            seen[vertex] = true;
            position[vertex] = index;
        }

        for (Edge edge : graph.edges()) {
            check(position[edge.from()] < position[edge.to()],
                    "Every edge must point forward in the order");
        }

        check(result.edgeExaminations() == 2L * graph.edgeCount(),
                "Each DAG edge is examined twice");
        check(result.queueOffers() == graph.vertexCount(),
                "Each vertex enters the queue once");
        check(result.queuePolls() == graph.vertexCount(),
                "Each vertex leaves the queue once");
    }

    private static void verifyCycle(TopologicalResult result) {
        check(result.hasCycle(), "Cycle must be detected");
        check(!result.isDag(), "Cyclic graph is not a DAG");
        check(result.order().isEmpty(),
                "Do not return a partial order as a valid result");
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