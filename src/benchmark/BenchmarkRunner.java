package benchmark;

import connectivity.Kosaraju;
import connectivity.SccResult;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MstMetrics;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraMetrics;
import shortestpath.DijkstraResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

public final class BenchmarkRunner {
    private static final long SEED = 42;
    private static final int WARMUP_RUNS = 20;
    private static final int MEASURED_RUNS = 5;

    private static final int[][] SIZES = {
            {10, 30},
            {100, 500},
            {1000, 5000}
    };

    // Keep the most recent result observable.
    private static volatile Object resultSink;

    private static final String METRIC_HEADER =
            "edge_examinations,comparisons,dsu_find_calls,dsu_union_calls,"
                    + "queue_offers,queue_polls,vertex_visits,"
                    + "validation_edge_examinations,successful_relaxations";

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws IOException {
        Path output = Path.of("results", "tables").toAbsolutePath();
        Files.createDirectories(output);

        StringBuilder raw = new StringBuilder(
                "algorithm,dataset,vertices,edges,seed,run,time_ns,"
                        + METRIC_HEADER + "\n"
        );

        StringBuilder averages = new StringBuilder(
                "algorithm,dataset,vertices,edges,seed,runs,"
                        + "average_time_ns,average_time_ms,"
                        + METRIC_HEADER + "\n"
        );

        for (int[] size : SIZES) {
            int vertices = size[0];
            int edges = size[1];

            // Graph generation is outside every timed interval.
            WeightedGraph undirected =
                    GraphGenerator.connectedUndirected(vertices, edges, SEED);
            WeightedGraph dag =
                    GraphGenerator.dag(vertices, edges, SEED);
            WeightedGraph cyclic =
                    GraphGenerator.cyclicDirected(vertices, edges, SEED);

            verifyFixtures(undirected, dag, cyclic);

            measure("Kruskal", "undirected", undirected,
                    () -> Kruskal.compute(undirected), raw, averages);
            measure("Prim", "undirected", undirected,
                    () -> Prim.compute(undirected), raw, averages);

            measure("TopologicalSort", "dag", dag,
                    () -> TopologicalSort.compute(dag), raw, averages);
            measure("TopologicalSort", "cyclic", cyclic,
                    () -> TopologicalSort.compute(cyclic), raw, averages);

            measure("Kosaraju", "dag", dag,
                    () -> Kosaraju.compute(dag), raw, averages);
            measure("Kosaraju", "cyclic", cyclic,
                    () -> Kosaraju.compute(cyclic), raw, averages);

            measure("Dijkstra", "undirected", undirected,
                    () -> Dijkstra.compute(undirected, 0), raw, averages);
            measure("Dijkstra", "dag", dag,
                    () -> Dijkstra.compute(dag, 0), raw, averages);
            measure("Dijkstra", "cyclic", cyclic,
                    () -> Dijkstra.compute(cyclic, 0), raw, averages);
        }

        Files.writeString(
                output.resolve("raw-runs.csv"),
                raw.toString(),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("averages.csv"),
                averages.toString(),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                output.resolve("environment.txt"),
                environmentDescription(),
                StandardCharsets.UTF_8
        );

        System.out.println();
        System.out.println("Benchmark completed.");
        System.out.println("Measured runs: 135");
        System.out.println("Average rows: 27");
        System.out.println("Results directory: " + output);
    }

    private static void measure(
            String algorithm,
            String dataset,
            WeightedGraph graph,
            Supplier<?> operation,
            StringBuilder raw,
            StringBuilder averages
    ) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            resultSink = operation.get();
        }

        long totalNanoseconds = 0;
        long[] referenceMetrics = null;

        String prefix = algorithm + "," + dataset + ","
                + graph.vertexCount() + "," + graph.edgeCount() + ","
                + SEED + ",";

        for (int run = 1; run <= MEASURED_RUNS; run++) {
            long start = System.nanoTime();
            Object result = operation.get();
            long elapsed = System.nanoTime() - start;

            resultSink = result;

            // Verification and formatting are outside the timed interval.
            validateResult(result, graph, dataset);
            long[] metrics = extractMetrics(result);

            if (referenceMetrics == null) {
                referenceMetrics = metrics;
            } else if (!Arrays.equals(referenceMetrics, metrics)) {
                throw new AssertionError(
                        "Operation counts changed between identical runs"
                );
            }

            totalNanoseconds += elapsed;
            raw.append(prefix)
                    .append(run).append(',')
                    .append(elapsed).append(',')
                    .append(metricsCsv(metrics)).append('\n');
        }

        double averageNs = totalNanoseconds / (double) MEASURED_RUNS;
        double averageMs = averageNs / 1_000_000.0;

        averages.append(prefix)
                .append(MEASURED_RUNS).append(',')
                .append(String.format(Locale.ROOT, "%.1f", averageNs))
                .append(',')
                .append(String.format(Locale.ROOT, "%.6f", averageMs))
                .append(',')
                .append(metricsCsv(referenceMetrics)).append('\n');

        System.out.printf(
                Locale.ROOT,
                "%s / %s / V=%d E=%d: %.6f ms%n",
                algorithm, dataset,
                graph.vertexCount(), graph.edgeCount(), averageMs
        );
    }

    private static long[] extractMetrics(Object result) {
        // -1 represents a metric that does not apply to this algorithm.
        long[] values = new long[9];
        Arrays.fill(values, -1);

        if (result instanceof MstResult mst) {
            MstMetrics metrics = mst.metrics();
            values[0] = metrics.edgeExaminations();
            values[1] = metrics.weightComparisons();
            values[2] = metrics.dsuFindCalls();
            values[3] = metrics.dsuUnionCalls();
            values[4] = metrics.queueOffers();
            values[5] = metrics.queuePolls();
        } else if (result instanceof TopologicalResult topology) {
            values[0] = topology.edgeExaminations();
            values[4] = topology.queueOffers();
            values[5] = topology.queuePolls();
        } else if (result instanceof SccResult scc) {
            values[0] = scc.edgeExaminations();
            values[6] = scc.vertexVisits();
        } else if (result instanceof DijkstraResult shortestPaths) {
            DijkstraMetrics metrics = shortestPaths.metrics();
            values[0] = metrics.edgeExaminations();
            values[1] = metrics.queueComparisons();
            values[4] = metrics.queueOffers();
            values[5] = metrics.queuePolls();
            values[7] = metrics.validationEdgeExaminations();
            values[8] = metrics.successfulRelaxations();
        } else {
            throw new IllegalArgumentException("Unknown result type");
        }

        return values;
    }

    private static String metricsCsv(long[] metrics) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < metrics.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            if (metrics[i] >= 0) {
                text.append(metrics[i]);
            }
        }

        return text.toString();
    }

    private static void verifyFixtures(
            WeightedGraph undirected,
            WeightedGraph dag,
            WeightedGraph cyclic
    ) {
        MstResult kruskal = Kruskal.compute(undirected);
        MstResult prim = Prim.compute(undirected);

        check(kruskal.isConnected() && prim.isConnected(),
                "MST dataset is disconnected");
        check(kruskal.totalWeight() == prim.totalWeight(),
                "MST weights disagree");
        check(TopologicalSort.compute(dag).isDag(),
                "DAG dataset contains a cycle");
        check(TopologicalSort.compute(cyclic).hasCycle(),
                "Cyclic dataset has no cycle");
    }

    private static void validateResult(
            Object result, WeightedGraph graph, String dataset
    ) {
        int vertices = graph.vertexCount();

        if (result instanceof MstResult mst) {
            check(mst.isConnected(), "Expected connected MST");
            check(mst.edges().size() == vertices - 1, "MST edge count");
        } else if (result instanceof TopologicalResult topology) {
            if (dataset.equals("dag")) {
                check(topology.isDag(), "Expected DAG");
                check(topology.order().size() == vertices, "Order size");
                int[] position = new int[vertices];
                boolean[] seen = new boolean[vertices];

                for (int i = 0; i < vertices; i++) {
                    int vertex = topology.order().get(i);
                    check(vertex >= 0 && vertex < vertices, "Invalid vertex");
                    check(!seen[vertex], "Repeated vertex");
                    seen[vertex] = true;
                    position[vertex] = i;
                }

                for (var edge : graph.edges()) {
                    check(position[edge.from()] < position[edge.to()],
                            "Invalid topological order");
                }
            } else {
                check(topology.hasCycle() && topology.order().isEmpty(),
                        "Expected cycle detection");
            }
        } else if (result instanceof SccResult scc) {
            boolean[] seen = new boolean[vertices];
            int count = 0;

            for (var component : scc.components()) {
                check(!component.isEmpty(), "Empty SCC");
                for (int vertex : component) {
                    check(vertex >= 0 && vertex < vertices, "Invalid vertex");
                    check(!seen[vertex], "Repeated SCC vertex");
                    seen[vertex] = true;
                    count++;
                }
            }

            check(count == vertices, "Missing SCC vertices");
            if (dataset.equals("dag")) {
                check(scc.componentCount() == vertices, "DAG SCC count");
            }
        } else if (result instanceof DijkstraResult paths) {
            check(paths.distanceTo(0).orElseThrow() == 0, "Source distance");

            for (int vertex = 0; vertex < vertices; vertex++) {
                if (dataset.equals("undirected")) {
                    check(paths.isReachable(vertex), "Unreachable district");
                }
                if (!paths.isReachable(vertex)) {
                    continue;
                }

                long distance = paths.distanceTo(vertex).orElseThrow();
                for (var edge : graph.edgesFrom(vertex)) {
                    check(paths.isReachable(edge.to()), "Reachability mismatch");
                    check(paths.distanceTo(edge.to()).orElseThrow()
                                    <= distance + edge.weight(),
                            "An edge can still improve a distance");
                }
            }
        }
    }

    private static String environmentDescription() {
        return "Recorded at: " + Instant.now() + "\n"
                + "Java version: " + System.getProperty("java.version") + "\n"
                + "Java vendor: " + System.getProperty("java.vendor") + "\n"
                + "VM: " + System.getProperty("java.vm.name") + "\n"
                + "OS: " + System.getProperty("os.name") + "\n"
                + "OS version: " + System.getProperty("os.version") + "\n"
                + "Architecture: " + System.getProperty("os.arch") + "\n"
                + "Available processors: "
                + Runtime.getRuntime().availableProcessors() + "\n"
                + "Maximum JVM memory bytes: "
                + Runtime.getRuntime().maxMemory() + "\n"
                + "Seed: " + SEED + "\n"
                + "Warmup runs per case: " + WARMUP_RUNS + "\n"
                + "Measured runs per case: " + MEASURED_RUNS + "\n"
                + "Dijkstra source: 0\n"
                + "One fixed graph per dataset and size; reused for all runs.\n"
                + "Times include algorithm allocation and operation counters.\n"
                + "Generation, result validation, formatting and file I/O "
                + "are excluded from timed intervals.\n"
                + "Metric columns in averages.csv are per-run counts; "
                + "identical counts across the five runs are verified.\n"
                + "Blank metric cells mean not applicable.\n"
                + "Comparisons mean sorting/heap comparator calls, "
                + "depending on the algorithm.\n";
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}