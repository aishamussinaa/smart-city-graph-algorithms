package mst;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    private Prim() {
    }

    public static MstResult compute(WeightedGraph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Prim requires an undirected graph"
            );
        }

        boolean[] visited = new boolean[graph.vertexCount()];
        List<Edge> selectedEdges = new ArrayList<>();

        long[] weightComparisons = {0};

        PriorityQueue<Edge> queue = new PriorityQueue<>(
                (first, second) -> {
                    weightComparisons[0]++;
                    return Long.compare(first.weight(), second.weight());
                }
        );

        long totalWeight = 0;
        long edgeExaminations = 0;
        long queueOffers = 0;
        long queuePolls = 0;
        int components = 0;

        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }

            components++;
            visited[start] = true;

            edgeExaminations += graph.edgesFrom(start).size();
            queueOffers += addOutgoingEdges(
                    graph, start, visited, queue
            );

            while (!queue.isEmpty()) {
                Edge edge = queue.poll();
                queuePolls++;

                if (visited[edge.to()]) {
                    continue;
                }

                selectedEdges.add(edge);
                totalWeight = Math.addExact(totalWeight, edge.weight());

                int next = edge.to();
                visited[next] = true;

                edgeExaminations += graph.edgesFrom(next).size();
                queueOffers += addOutgoingEdges(
                        graph, next, visited, queue
                );
            }
        }

        MstMetrics metrics = new MstMetrics(
                edgeExaminations,
                weightComparisons[0],
                0,
                0,
                queueOffers,
                queuePolls
        );

        return new MstResult(
                selectedEdges,
                totalWeight,
                components,
                metrics
        );
    }

    private static long addOutgoingEdges(
            WeightedGraph graph,
            int vertex,
            boolean[] visited,
            PriorityQueue<Edge> queue) {

        long offers = 0;

        for (Edge edge : graph.edgesFrom(vertex)) {
            if (!visited[edge.to()]) {
                queue.offer(edge);
                offers++;
            }
        }

        return offers;
    }
}