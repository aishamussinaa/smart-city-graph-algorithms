package shortestpath;

public record DijkstraMetrics(
        long validationEdgeExaminations,
        long edgeExaminations,
        long successfulRelaxations,
        long queueOffers,
        long queuePolls,
        long queueComparisons
) {
}