package mst;

import graph.Edge;
import java.util.List;

public record MstResult(
        List<Edge> edges,
        long totalWeight,
        int components,
        MstMetrics metrics
) {
    public MstResult {
        edges = List.copyOf(edges);
    }

    public boolean isConnected() {
        return components == 1;
    }
}
