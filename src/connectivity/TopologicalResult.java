package connectivity;

import java.util.List;

public record TopologicalResult(
        List<Integer> order,
        boolean hasCycle,
        long edgeExaminations,
        long queueOffers,
        long queuePolls
) {
    public TopologicalResult {
        order = List.copyOf(order);
    }

    public boolean isDag() {
        return !hasCycle;
    }
}