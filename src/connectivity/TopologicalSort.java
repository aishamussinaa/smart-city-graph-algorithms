package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class TopologicalSort {
    private TopologicalSort() {
    }

    public static TopologicalResult compute(WeightedGraph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Topological sorting requires a directed graph"
            );
        }

        int vertices = graph.vertexCount();
        int[] inDegree = new int[vertices];

        long edgeExaminations = 0;
        long queueOffers = 0;
        long queuePolls = 0;

        // Count incoming edges for every vertex.
        for (Edge edge : graph.edges()) {
            inDegree[edge.to()]++;
            edgeExaminations++;
        }

        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int vertex = 0; vertex < vertices; vertex++) {
            if (inDegree[vertex] == 0) {
                queue.addLast(vertex);
                queueOffers++;
            }
        }

        List<Integer> order = new ArrayList<>();

        while (!queue.isEmpty()) {
            int vertex = queue.removeFirst();
            queuePolls++;
            order.add(vertex);

            for (Edge edge : graph.edgesFrom(vertex)) {
                edgeExaminations++;

                int next = edge.to();
                inDegree[next]--;

                if (inDegree[next] == 0) {
                    queue.addLast(next);
                    queueOffers++;
                }
            }
        }

        boolean hasCycle = order.size() != vertices;

        List<Integer> validOrder = hasCycle
                ? List.of()
                : order;

        return new TopologicalResult(
                validOrder,
                hasCycle,
                edgeExaminations,
                queueOffers,
                queuePolls
        );
    }
}