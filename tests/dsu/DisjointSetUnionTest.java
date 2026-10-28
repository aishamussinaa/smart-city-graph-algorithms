package dsu;

import java.util.Random;

public class DisjointSetUnionTest {
    public static void main(String[] args) {
        testInitialization();
        testUnionsAndSizes();
        testUnionBySize();
        testPathCompression();
        testStatistics();
        testSingleAndEmptySets();
        testInvalidInputs();
        testRandomized();

        System.out.println("All 8 DSU tests passed.");
    }

    private static void testInitialization() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);

        check(dsu.elementCount() == 5, "Element count");
        check(dsu.componentCount() == 5, "Initial components");

        for (int i = 0; i < 5; i++) {
            check(dsu.find(i) == i, "Initial representative");
            check(dsu.componentSize(i) == 1, "Initial size");
        }

        check(!dsu.connected(0, 1), "Separate elements");
        System.out.println("PASS: initialization");
    }

    private static void testUnionsAndSizes() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);

        check(dsu.union(0, 1), "First merge");
        check(dsu.union(1, 2), "Second merge");
        check(dsu.connected(0, 2), "Transitive connectivity");

        check(!dsu.union(2, 0), "Reject redundant merge");
        check(!dsu.union(3, 3), "Reject self-union");

        check(dsu.componentCount() == 3, "Component count");

        for (int i = 0; i < 3; i++) {
            check(dsu.componentSize(i) == 3, "Component size");
        }

        check(dsu.componentSize(4) == 1, "Isolated element");

        dsu.union(3, 4);
        dsu.union(0, 3);

        check(dsu.componentCount() == 1, "All elements connected");
        check(dsu.componentSize(4) == 5, "Final size");

        System.out.println("PASS: unions and sizes");
    }

    private static void testUnionBySize() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);

        dsu.union(0, 1);
        dsu.union(0, 2);
        int largeRoot = dsu.find(0);

        dsu.union(3, 0);
        check(dsu.find(3) == largeRoot,
                "Smaller component must join larger component");

        dsu.union(0, 4);
        check(dsu.find(4) == largeRoot,
                "Size rule works with either argument order");

        System.out.println("PASS: union by size");
    }

    private static void testPathCompression() {
        DisjointSetUnion dsu = new DisjointSetUnion(8);

        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.union(4, 5);
        dsu.union(6, 7);
        dsu.union(0, 2);
        dsu.union(4, 6);
        dsu.union(0, 4);

        dsu.resetStatistics();
        int root = dsu.find(7);

        check(dsu.statistics().parentTraversals() > 1,
                "Initial path must contain multiple links");
        check(dsu.statistics().pathCompressionWrites() > 0,
                "Find must shorten the path");

        dsu.resetStatistics();

        check(dsu.find(7) == root,
                "Compression preserves the representative");
        check(dsu.statistics().parentTraversals() == 1,
                "Repeated find follows one link");
        check(dsu.statistics().pathCompressionWrites() == 0,
                "Compressed path needs no further changes");
        check(dsu.componentCount() == 1,
                "Compression preserves components");

        System.out.println("PASS: path compression");
    }

    private static void testStatistics() {
        DisjointSetUnion dsu = new DisjointSetUnion(3);

        dsu.union(0, 1);
        dsu.union(1, 0);

        DisjointSetUnion.Statistics snapshot = dsu.statistics();

        check(snapshot.unionCalls() == 2, "Two union attempts");
        check(snapshot.findCalls() == 4, "Two finds per union");
        check(snapshot.successfulUnions() == 1, "One actual merge");

        dsu.resetStatistics();

        check(dsu.statistics().equals(
                new DisjointSetUnion.Statistics(0, 0, 0, 0, 0)
        ), "All counters reset");

        check(snapshot.unionCalls() == 2, "Snapshot is unchanged");
        check(dsu.componentCount() == 2, "Reset preserves groups");
        check(dsu.connected(0, 1), "Reset preserves connectivity");
        check(dsu.componentSize(0) == 2, "Reset preserves sizes");

        System.out.println("PASS: statistics");
    }

    private static void testSingleAndEmptySets() {
        DisjointSetUnion single = new DisjointSetUnion(1);

        check(single.find(0) == 0, "Single representative");
        check(single.connected(0, 0), "Connected to itself");
        check(!single.union(0, 0), "No merge needed");
        check(single.componentCount() == 1, "Single component");
        check(single.componentSize(0) == 1, "Single size");

        DisjointSetUnion empty = new DisjointSetUnion(0);

        check(empty.elementCount() == 0, "No elements");
        check(empty.componentCount() == 0, "No components");

        expectException(IndexOutOfBoundsException.class,
                () -> empty.find(0));

        System.out.println("PASS: single and empty sets");
    }

    private static void testInvalidInputs() {
        expectException(IllegalArgumentException.class,
                () -> new DisjointSetUnion(-1));

        DisjointSetUnion dsu = new DisjointSetUnion(3);

        expectException(IndexOutOfBoundsException.class,
                () -> dsu.find(-1));
        expectException(IndexOutOfBoundsException.class,
                () -> dsu.union(0, 3));
        expectException(IndexOutOfBoundsException.class,
                () -> dsu.connected(0, 3));
        expectException(IndexOutOfBoundsException.class,
                () -> dsu.componentSize(3));

        check(dsu.componentCount() == 3, "Invalid calls change no groups");
        check(dsu.statistics().findCalls() == 0, "No valid find calls");
        check(dsu.statistics().unionCalls() == 0, "No valid union calls");

        System.out.println("PASS: invalid inputs");
    }

    private static void testRandomized() {
        int count = 20;
        DisjointSetUnion dsu = new DisjointSetUnion(count);
        int[] labels = new int[count];

        for (int i = 0; i < count; i++) {
            labels[i] = i;
        }

        Random random = new Random(42);
        int expectedComponents = count;

        for (int step = 0; step < 250; step++) {
            int first = random.nextInt(count);
            int second = random.nextInt(count);

            int firstLabel = labels[first];
            int secondLabel = labels[second];
            boolean shouldMerge = firstLabel != secondLabel;

            check(dsu.union(first, second) == shouldMerge,
                    "Merge result agrees with reference model");

            if (shouldMerge) {
                for (int i = 0; i < count; i++) {
                    if (labels[i] == secondLabel) {
                        labels[i] = firstLabel;
                    }
                }
                expectedComponents--;
            }

            check(dsu.componentCount() == expectedComponents,
                    "Component count agrees with reference model");

            for (int i = 0; i < count; i++) {
                int expectedSize = 0;

                for (int j = 0; j < count; j++) {
                    boolean sameGroup = labels[i] == labels[j];

                    check(dsu.connected(i, j) == sameGroup,
                            "Connectivity agrees with reference model");

                    if (sameGroup) {
                        expectedSize++;
                    }
                }

                check(dsu.componentSize(i) == expectedSize,
                        "Size agrees with reference model");
            }
        }

        System.out.println("PASS: randomized reference comparison");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectException(
            Class<? extends RuntimeException> expected,
            Runnable action) {

        try {
            action.run();
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                return;
            }

            throw new AssertionError("Unexpected exception", exception);
        }

        throw new AssertionError(
                "Expected exception: " + expected.getSimpleName()
        );
    }
}