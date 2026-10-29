package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.List;

public final class Kruskal {
    private Kruskal() {
    }

    public static MstResult compute(WeightedGraph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException(
                    "Kruskal requires an undirected graph"
            );
        }

        List<Edge> sortedEdges = new ArrayList<>(graph.edges());

        long[] weightComparisons = {0};

        sortedEdges.sort((first, second) -> {
            weightComparisons[0]++;
            return Long.compare(first.weight(), second.weight());
        });

        DisjointSetUnion dsu =
                new DisjointSetUnion(graph.vertexCount());

        List<Edge> selectedEdges = new ArrayList<>();
        long totalWeight = 0;
        long edgeExaminations = 0;

        for (Edge edge : sortedEdges) {
            if (dsu.componentCount() <= 1) {
                break;
            }

            edgeExaminations++;

            if (dsu.union(edge.from(), edge.to())) {
                selectedEdges.add(edge);
                totalWeight = Math.addExact(totalWeight, edge.weight());
            }
        }

        DisjointSetUnion.Statistics statistics = dsu.statistics();

        MstMetrics metrics = new MstMetrics(
                edgeExaminations,
                weightComparisons[0],
                statistics.findCalls(),
                statistics.unionCalls(),
                0,
                0
        );

        return new MstResult(
                selectedEdges,
                totalWeight,
                dsu.componentCount(),
                metrics
        );
    }
}