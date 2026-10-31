package app;

import connectivity.Kosaraju;
import connectivity.SccResult;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int source = 0;

        if (args.length > 1) {
            System.out.println("Usage: app.Main [source district: 0..5]");
            return;
        }

        if (args.length == 1) {
            try {
                source = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                System.out.println("Source district must be an integer.");
                return;
            }
        }

        if (source < 0 || source > 5) {
            System.out.println("Source district must be between 0 and 5.");
            return;
        }

        System.out.println("Smart City Infrastructure and Route Planning");

        demonstrateMst();
        demonstrateTopologicalSort();
        demonstrateScc();
        demonstrateShortestPaths(source);
    }

    private static void demonstrateMst() {
        System.out.println("\n=== Infrastructure planning ===");

        WeightedGraph city = new WeightedGraph(5, false);
        city.addEdge(0, 1, 4);
        city.addEdge(0, 2, 6);
        city.addEdge(1, 2, 3);
        city.addEdge(3, 4, 2);
        city.addEdge(2, 3, 5);

        MstResult kruskal = Kruskal.compute(city);
        MstResult prim = Prim.compute(city);

        printMst("Kruskal", kruskal);
        printMst("Prim", prim);

        System.out.println(
                "Same total weight: "
                        + (kruskal.totalWeight() == prim.totalWeight())
        );
    }

    private static void printMst(String algorithm, MstResult result) {
        System.out.println("\nAlgorithm: " + algorithm);
        System.out.println("Connected: " + result.isConnected());
        System.out.println("Components: " + result.components());
        System.out.println("Selected roads: " + result.edges().size());
        System.out.println("Total weight: " + result.totalWeight());

        for (Edge edge : result.edges()) {
            System.out.println(
                    edge.from() + " - " + edge.to()
                            + ", weight: " + edge.weight()
            );
        }
    }

    private static void demonstrateTopologicalSort() {
        System.out.println("\n=== Infrastructure dependencies ===");

        WeightedGraph dependencies = new WeightedGraph(4, true);
        dependencies.addEdge(3, 1, 1);
        dependencies.addEdge(3, 0, 1);
        dependencies.addEdge(1, 2, 1);
        dependencies.addEdge(0, 2, 1);

        TopologicalResult dag = TopologicalSort.compute(dependencies);
        System.out.println("DAG order: " + dag.order());

        // This creates the directed cycle 3 -> 1 -> 2 -> 3.
        dependencies.addEdge(2, 3, 1);

        TopologicalResult cyclic = TopologicalSort.compute(dependencies);
        System.out.println("Cycle detected: " + cyclic.hasCycle());

        if (cyclic.hasCycle()) {
            System.out.println("Topological ordering is impossible.");
        }
    }

    private static void demonstrateScc() {
        System.out.println("\n=== Directed connectivity ===");

        WeightedGraph roads = new WeightedGraph(6, true);
        roads.addEdge(0, 1, 1);
        roads.addEdge(1, 2, 1);
        roads.addEdge(2, 0, 1);
        roads.addEdge(2, 3, 1);
        roads.addEdge(3, 4, 1);
        roads.addEdge(4, 3, 1);
        roads.addEdge(4, 5, 1);

        SccResult result = Kosaraju.compute(roads);
        System.out.println("SCC count: " + result.componentCount());

        for (int i = 0; i < result.componentCount(); i++) {
            System.out.println(
                    "Component " + (i + 1) + ": "
                            + result.components().get(i)
            );
        }
    }

    private static void demonstrateShortestPaths(int source) {
        System.out.println("\n=== Route planning ===");

        WeightedGraph roads = new WeightedGraph(6, true);
        roads.addEdge(0, 1, 4);
        roads.addEdge(0, 2, 1);
        roads.addEdge(2, 1, 2);
        roads.addEdge(1, 3, 1);
        roads.addEdge(2, 3, 5);
        roads.addEdge(3, 4, 3);
        // District 5 is isolated.

        DijkstraResult result = Dijkstra.compute(roads, source);
        System.out.println("Source district: " + source);

        for (int district = 0; district < result.vertexCount(); district++) {
            if (!result.isReachable(district)) {
                System.out.println("District " + district + ": unreachable");
                continue;
            }

            System.out.println(
                    "District " + district
                            + ": distance = "
                            + result.distanceTo(district).orElseThrow()
                            + ", path = " + result.pathTo(district)
            );
        }
    }
}