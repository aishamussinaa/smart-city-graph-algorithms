package connectivity;

import java.util.List;

public record SccResult(
        List<List<Integer>> components,
        long edgeExaminations,
        long vertexVisits
) {
    public SccResult {
        components = components.stream()
                .map(List::copyOf)
                .toList();
    }

    public int componentCount() {
        return components.size();
    }
}