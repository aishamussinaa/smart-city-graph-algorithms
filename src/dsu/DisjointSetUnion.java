package dsu;

public class DisjointSetUnion {
    private final int[] parent;
    private final int[] size;
    private int components;

    private long findCalls;
    private long unionCalls;
    private long successfulUnions;
    private long parentTraversals;
    private long pathCompressionWrites;

    public DisjointSetUnion(int elementCount) {
        if (elementCount < 0) {
            throw new IllegalArgumentException(
                    "Element count must be non-negative"
            );
        }

        parent = new int[elementCount];
        size = new int[elementCount];
        components = elementCount;

        for (int i = 0; i < elementCount; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int elementCount() {
        return parent.length;
    }

    public int componentCount() {
        return components;
    }

    public int find(int element) {
        validateElement(element);
        findCalls++;

        int root = element;

        while (root != parent[root]) {
            root = parent[root];
            parentTraversals++;
        }

        // Redirect every vertex on the path to the root.
        while (element != root) {
            int next = parent[element];

            if (parent[element] != root) {
                parent[element] = root;
                pathCompressionWrites++;
            }

            element = next;
        }

        return root;
    }

    public boolean union(int first, int second) {
        validateElement(first);
        validateElement(second);
        unionCalls++;

        int firstRoot = find(first);
        int secondRoot = find(second);

        if (firstRoot == secondRoot) {
            return false;
        }

        // Attach the smaller component to the larger one.
        if (size[firstRoot] < size[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }

        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];

        components--;
        successfulUnions++;
        return true;
    }

    public boolean connected(int first, int second) {
        validateElement(first);
        validateElement(second);
        return find(first) == find(second);
    }

    public int componentSize(int element) {
        return size[find(element)];
    }

    public Statistics statistics() {
        return new Statistics(
                findCalls,
                unionCalls,
                successfulUnions,
                parentTraversals,
                pathCompressionWrites
        );
    }

    public void resetStatistics() {
        findCalls = 0;
        unionCalls = 0;
        successfulUnions = 0;
        parentTraversals = 0;
        pathCompressionWrites = 0;
    }

    private void validateElement(int element) {
        if (element < 0 || element >= parent.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid element: " + element
            );
        }
    }

    public record Statistics(
            long findCalls,
            long unionCalls,
            long successfulUnions,
            long parentTraversals,
            long pathCompressionWrites
    ) {
    }
}