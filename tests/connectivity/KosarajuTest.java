package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class KosarajuTest {
    public static void main(String[] args) {
        testKnownComponents();
        testDag();
        testDisconnectedGraph();
        testParallelEdgesAndLoops();
        testSingleAndEmptyGraphs();
        testUndirectedRejected();
        testProtection();
        testRandomGraphs();

        System.out.println("All 8 SCC tests passed.");
    }

    private static void testKnownComponents() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);
        graph.addEdge(4, 5, 1);

        SccResult result = verify(graph);
        check(result.componentCount() == 3, "Expected 3 SCCs");
        System.out.println("Components: " + result.components());
        System.out.println("PASS: known strongly connected components");
    }

    private static void testDag() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 3, 3);
        graph.addEdge(2, 3, 5);

        check(verify(graph).componentCount() == 4,
                "Every DAG vertex must form its own SCC");
        System.out.println("PASS: DAG");
    }

    private static void testDisconnectedGraph() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(2, 3, 1);

        check(verify(graph).componentCount() == 4,
                "Expected 4 SCCs including the isolated vertex");
        System.out.println("PASS: disconnected graph and isolated vertex");
    }

    private static void testParallelEdgesAndLoops() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 0, -5);
        graph.addEdge(0, 1, 7);
        graph.addEdge(0, 1, 7);
        graph.addEdge(1, 0, 0);
        graph.addEdge(1, 2, 3);
        graph.addEdge(2, 2, 9);

        check(verify(graph).componentCount() == 2, "Expected 2 SCCs");
        System.out.println("PASS: parallel edges, self-loops and weights");
    }

    private static void testSingleAndEmptyGraphs() {
        WeightedGraph single = new WeightedGraph(1, true);
        check(verify(single).componentCount() == 1,
                "Single vertex must form one SCC");

        single.addEdge(0, 0, 1);
        check(verify(single).componentCount() == 1,
                "Self-loop must not change the component count");

        WeightedGraph empty = new WeightedGraph(0, true);
        check(verify(empty).componentCount() == 0,
                "Empty graph must have no SCCs");
        System.out.println("PASS: single and empty graphs");
    }

    private static void testUndirectedRejected() {
        expectThrows(IllegalArgumentException.class,
                () -> Kosaraju.compute(new WeightedGraph(2, false)));
        System.out.println("PASS: undirected graph rejected");
    }

    private static void testProtection() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        List<Edge> original = new ArrayList<>(graph.edges());

        SccResult result = verify(graph);
        check(graph.edges().equals(original), "Input graph was modified");

        expectThrows(UnsupportedOperationException.class,
                () -> result.components().clear());
        expectThrows(UnsupportedOperationException.class,
                () -> result.components().get(0).clear());

        List<Integer> inner = new ArrayList<>(List.of(0));
        List<List<Integer>> outer = new ArrayList<>();
        outer.add(inner);
        SccResult snapshot = new SccResult(outer, 0, 0);
        inner.add(1);
        outer.clear();

        check(snapshot.components().equals(List.of(List.of(0))),
                "Result must copy both list levels");
        System.out.println("PASS: input and result protection");
    }

    private static void testRandomGraphs() {
        Random random = new Random(42);

        for (int trial = 0; trial < 100; trial++) {
            int vertices = 1 + random.nextInt(12);
            WeightedGraph graph = new WeightedGraph(vertices, true);

            for (int from = 0; from < vertices; from++) {
                for (int to = 0; to < vertices; to++) {
                    if (random.nextDouble() < 0.25) {
                        graph.addEdge(from, to, random.nextInt(21) - 10);
                    }
                }
            }

            verify(graph);
        }

        System.out.println("PASS: 100 random graphs against reachability");
    }

    private static SccResult verify(WeightedGraph graph) {
        SccResult result = Kosaraju.compute(graph);
        int vertices = graph.vertexCount();
        int[] componentOf = new int[vertices];
        Arrays.fill(componentOf, -1);

        for (int id = 0; id < result.componentCount(); id++) {
            List<Integer> component = result.components().get(id);
            check(!component.isEmpty(), "Empty component");

            for (int vertex : component) {
                check(vertex >= 0 && vertex < vertices,
                        "Invalid vertex in result");
                check(componentOf[vertex] == -1,
                        "Vertex occurs in multiple components");
                componentOf[vertex] = id;
            }
        }

        for (int vertex = 0; vertex < vertices; vertex++) {
            check(componentOf[vertex] != -1, "Missing vertex");
        }

        // Independent reference: compute reachability for every pair.
        boolean[][] reachable = new boolean[vertices][vertices];
        for (int vertex = 0; vertex < vertices; vertex++) {
            reachable[vertex][vertex] = true;
        }
        for (Edge edge : graph.edges()) {
            reachable[edge.from()][edge.to()] = true;
        }

        for (int middle = 0; middle < vertices; middle++) {
            for (int from = 0; from < vertices; from++) {
                for (int to = 0; to < vertices; to++) {
                    reachable[from][to] |=
                            reachable[from][middle] && reachable[middle][to];
                }
            }
        }

        for (int from = 0; from < vertices; from++) {
            for (int to = 0; to < vertices; to++) {
                boolean mutuallyReachable =
                        reachable[from][to] && reachable[to][from];
                check((componentOf[from] == componentOf[to])
                                == mutuallyReachable,
                        "Incorrect SCC grouping");
            }
        }

        check(result.edgeExaminations() == 3L * graph.edgeCount(),
                "Incorrect edge examination count");
        check(result.vertexVisits() == 2L * vertices,
                "Incorrect vertex visit count");

        return result;
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