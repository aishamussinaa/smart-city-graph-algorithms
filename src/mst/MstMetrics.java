package mst;

public record MstMetrics(
        long edgeExaminations,
        long weightComparisons,
        long dsuFindCalls,
        long dsuUnionCalls,
        long queueOffers,
        long queuePolls
) {
}