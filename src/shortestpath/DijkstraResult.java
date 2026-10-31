package shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

public final class DijkstraResult {
    private final int source;
    private final long[] distances;
    private final boolean[] reachable;
    private final int[] predecessors;
    private final DijkstraMetrics metrics;

    DijkstraResult(
            int source,
            long[] distances,
            boolean[] reachable,
            int[] predecessors,
            DijkstraMetrics metrics
    ) {
        this.source = source;
        this.distances = distances.clone();
        this.reachable = reachable.clone();
        this.predecessors = predecessors.clone();
        this.metrics = metrics;
    }

    public int source() {
        return source;
    }

    public int vertexCount() {
        return distances.length;
    }

    public DijkstraMetrics metrics() {
        return metrics;
    }

    public boolean isReachable(int vertex) {
        validateVertex(vertex);
        return reachable[vertex];
    }

    public OptionalLong distanceTo(int vertex) {
        validateVertex(vertex);

        if (!reachable[vertex]) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(distances[vertex]);
    }

    public List<Integer> pathTo(int vertex) {
        validateVertex(vertex);

        if (!reachable[vertex]) {
            return List.of();
        }

        List<Integer> path = new ArrayList<>();
        int current = vertex;

        while (current != source) {
            path.add(current);
            current = predecessors[current];
        }

        path.add(source);
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= distances.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid vertex: " + vertex
            );
        }
    }
}