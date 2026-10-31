package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Kosaraju {
    private Kosaraju() {
    }

    public static SccResult compute(WeightedGraph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException(
                    "SCC analysis requires a directed graph"
            );
        }

        int vertices = graph.vertexCount();
        boolean[] visited = new boolean[vertices];
        int[] nextEdge = new int[vertices];
        List<Integer> finishOrder = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();

        long edgeExaminations = 0;
        long vertexVisits = 0;

        // First pass: record vertices when their DFS finishes.
        for (int start = 0; start < vertices; start++) {
            if (visited[start]) {
                continue;
            }

            visited[start] = true;
            vertexVisits++;
            stack.push(start);

            while (!stack.isEmpty()) {
                int vertex = stack.peek();
                List<Edge> outgoing = graph.edgesFrom(vertex);

                if (nextEdge[vertex] < outgoing.size()) {
                    Edge edge = outgoing.get(nextEdge[vertex]++);
                    edgeExaminations++;

                    int next = edge.to();
                    if (!visited[next]) {
                        visited[next] = true;
                        vertexVisits++;
                        stack.push(next);
                    }
                } else {
                    stack.pop();
                    finishOrder.add(vertex);
                }
            }
        }

        // Reverse every directed edge.
        List<List<Integer>> reversed = new ArrayList<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            reversed.add(new ArrayList<>());
        }

        for (Edge edge : graph.edges()) {
            reversed.get(edge.to()).add(edge.from());
            edgeExaminations++;
        }

        boolean[] assigned = new boolean[vertices];
        List<List<Integer>> components = new ArrayList<>();

        // Second pass: traverse the reversed graph in reverse finish order.
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int start = finishOrder.get(i);
            if (assigned[start]) {
                continue;
            }

            List<Integer> component = new ArrayList<>();
            assigned[start] = true;
            vertexVisits++;
            stack.push(start);

            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                component.add(vertex);

                for (int next : reversed.get(vertex)) {
                    edgeExaminations++;

                    if (!assigned[next]) {
                        assigned[next] = true;
                        vertexVisits++;
                        stack.push(next);
                    }
                }
            }

            components.add(component);
        }

        return new SccResult(
                components,
                edgeExaminations,
                vertexVisits
        );
    }
}