package shortestpath;

import graph.Edge;
import graph.WeightedGraph;

import java.util.Arrays;
import java.util.PriorityQueue;

public final class Dijkstra {
    private Dijkstra() {
    }

    private record QueueEntry(int vertex, long distance) {
    }

    public static DijkstraResult compute(
            WeightedGraph graph,
            int source
    ) {
        int vertices = graph.vertexCount();

        if (source < 0 || source >= vertices) {
            throw new IndexOutOfBoundsException(
                    "Invalid source vertex: " + source
            );
        }

        long validationEdgeExaminations = 0;

        // Validate all edges, including unreachable parts of the graph.
        for (Edge edge : graph.edges()) {
            validationEdgeExaminations++;

            if (edge.weight() < 0) {
                throw new IllegalArgumentException(
                        "Dijkstra requires non-negative edge weights"
                );
            }
        }

        long[] distances = new long[vertices];
        boolean[] reachable = new boolean[vertices];
        int[] predecessors = new int[vertices];
        Arrays.fill(predecessors, -1);

        boolean[] overflowTargets = new boolean[vertices];
        long[] queueComparisons = {0};

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(
                (first, second) -> {
                    queueComparisons[0]++;

                    int comparison = Long.compare(
                            first.distance(),
                            second.distance()
                    );

                    if (comparison != 0) {
                        return comparison;
                    }

                    return Integer.compare(
                            first.vertex(),
                            second.vertex()
                    );
                }
        );

        long edgeExaminations = 0;
        long successfulRelaxations = 0;
        long queueOffers = 0;
        long queuePolls = 0;

        distances[source] = 0;
        reachable[source] = true;
        queue.offer(new QueueEntry(source, 0));
        queueOffers++;

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            queuePolls++;

            int vertex = current.vertex();

            // A shorter distance may have been inserted after this entry.
            if (current.distance() != distances[vertex]) {
                continue;
            }

            for (Edge edge : graph.edgesFrom(vertex)) {
                edgeExaminations++;
                int next = edge.to();

                // Defer overflow handling: a shorter valid route may exist.
                if (distances[vertex] > Long.MAX_VALUE - edge.weight()) {
                    overflowTargets[next] = true;
                    continue;
                }

                long candidate = distances[vertex] + edge.weight();

                if (!reachable[next] || candidate < distances[next]) {
                    distances[next] = candidate;
                    reachable[next] = true;
                    predecessors[next] = vertex;
                    successfulRelaxations++;

                    queue.offer(new QueueEntry(next, candidate));
                    queueOffers++;
                }
            }
        }

        for (int vertex = 0; vertex < vertices; vertex++) {
            if (overflowTargets[vertex] && !reachable[vertex]) {
                throw new ArithmeticException(
                        "A shortest distance exceeds the long range"
                );
            }
        }

        DijkstraMetrics metrics = new DijkstraMetrics(
                validationEdgeExaminations,
                edgeExaminations,
                successfulRelaxations,
                queueOffers,
                queuePolls,
                queueComparisons[0]
        );

        return new DijkstraResult(
                source,
                distances,
                reachable,
                predecessors,
                metrics
        );
    }
}