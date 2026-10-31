# Smart City Infrastructure and Route Planning

Assignment 3 — Design and Analysis of Algorithms.

## Overview

This Java application models a city's infrastructure as a weighted graph.
Vertices represent districts, and edges represent roads. Weights represent
construction costs or travel distances.

Implemented: weighted graph representation, optimized Disjoint Set Union
(DSU), Kruskal's and Prim's MST algorithms, topological sorting with cycle
detection, and strongly connected components using Kosaraju's algorithm.

Dijkstra's shortest paths and performance benchmarks are still pending.

## Implementation

The project uses Java 17 and requires no external libraries.

### Graph Representation

- `Edge` is an immutable record containing `from`, `to`, and `weight`.
- `WeightedGraph` supports directed and undirected graphs.
- Vertices are numbered from 0 to V - 1.
- Adjacency lists store outgoing edges for each vertex.
- A separate edge list stores each added road once.
- Undirected roads have reverse adjacency entries.
- An undirected self-loop has one adjacency entry.
- Parallel edges, equal weights, and isolated vertices are supported.
- Weights use `long`.
- Invalid vertex IDs are rejected before modifying the graph.
- Returned edge lists are read-only views.

The graph permits negative weights. The future Dijkstra implementation
will reject them because its correctness requires non-negative weights.

### Disjoint Set Union

`DisjointSetUnion` stores a parent array, component sizes, and the
current number of components. Initially, each element is a separate set.

`find` uses iterative full path compression: after finding the root,
it redirects every element on the visited path directly to that root.

`union` uses union by size: the smaller component is attached to the
larger component. It returns false if both elements are already connected.

Component sizes are stored at roots. The representative is not necessarily
the smallest element in a component.

The implementation also provides connectivity queries, component sizes,
component counts, and immutable statistics snapshots.

### Operation Counters

| Counter | Definition |
| --- | --- |
| findCalls | Valid find calls, including calls made by other DSU methods |
| unionCalls | Valid union attempts, including redundant attempts |
| successfulUnions | Attempts that merge different components |
| parentTraversals | Parent links followed during the root-search pass |
| pathCompressionWrites | Parent entries changed during compression |

Parent traversals exclude the compression pass.
Compression writes exclude links created by union.

Resetting statistics does not reset components or undo path compression.
Future experiments will create a fresh DSU for each measured run.

### Kruskal's Algorithm

Kruskal processes a copy of the logical edge list sorted by weight.
An edge is selected only when DSU successfully merges its endpoints'
components. Edges that would create cycles are rejected.

Processing stops when one component remains or all edges have been
examined. A disconnected graph produces a minimum spanning forest.

Directed graphs are rejected. Negative weights, parallel edges, and
equal weights are supported. Self-loops are never selected.

`MstResult` contains an immutable copy of the selected edges, their total
weight, the component count, and operation counters.

A single-vertex graph is connected and has total weight zero.
For an empty graph, the component count is zero and isConnected is false.

Weights and totals use long. Math.addExact throws ArithmeticException
if an intermediate accumulated total exceeds the long range.

### Prim's Algorithm

Prim uses a visited array and Java's PriorityQueue, implemented as a
binary heap. Starting from an unvisited vertex, it adds outgoing edges
whose destinations have not been visited.

The cheapest queued edge is extracted next. If its destination is already
visited, the entry is discarded. Otherwise, the edge is selected and the
new vertex's outgoing edges are examined.

This is lazy Prim: obsolete entries remain in the queue until extracted.
The outer loop starts a new tree for each unvisited component, so
disconnected graphs produce a minimum spanning forest.

Directed graphs are rejected. Negative weights and parallel edges are
supported; self-loops are not selected. Totals use Math.addExact with
the same intermediate-overflow checks as Kruskal.

### MST Operation Counters

| Counter | Kruskal | Prim |
| --- | --- | --- |
| edgeExaminations | Edges inspected after sorting | Adjacency entries inspected when visiting vertices |
| weightComparisons | Sorting comparator calls | Priority queue comparator calls |
| dsuFindCalls | Calls reported by DSU | Zero |
| dsuUnionCalls | Attempts reported by DSU | Zero |
| queueOffers | Zero | Edges inserted into the queue |
| queuePolls | Zero | Entries removed, including obsolete entries |

Kruskal performs one union attempt and two find calls per examined edge.

Prim visits every vertex and scans every adjacency list. For an
undirected graph with E logical edges and L self-loops, it examines
2E - L adjacency entries. Each non-loop edge enters the queue once,
and the queue is fully drained for each component.

The counters describe different operations and must be interpreted
using these definitions rather than treated as interchangeable totals.

### City Demonstration

The current demonstration contains five districts and five roads.
Kruskal selects four roads with weights 2, 3, 4, and 5.
Their total weight is 14, and all districts belong to one component.
Prim also returns four roads with total weight 14. The demonstration
prints both results and confirms that their total weights are equal.

### Topological Sorting

`TopologicalSort` implements Kahn's algorithm for directed graphs.

It first counts incoming edges for every vertex. Vertices with zero
indegree enter an ArrayDeque used as a FIFO queue. Removing a vertex
reduces the indegrees of its outgoing neighbors. A neighbor enters
the queue when its indegree becomes zero.

If every vertex is processed, the result contains a valid topological
ordering. Otherwise, the graph contains a cycle.

On cyclic input, hasCycle is true and the returned order is empty.
A partially processed order is not exposed as a valid result.
An empty directed graph is a DAG with an empty ordering.

Undirected input is rejected. Edge weights do not affect the ordering.
Parallel edges are counted and processed individually.

The result stores an immutable copy of the order and these counters:

- `edgeExaminations`: edges scanned during indegree initialization,
  plus outgoing adjacency entries scanned during vertex processing.
- `queueOffers`: vertices inserted into the queue.
- `queuePolls`: vertices removed from the queue.

For a DAG, edgeExaminations equals 2E and both queue counters equal V.
For cyclic graphs, processing may stop before every vertex is visited.

### Strongly Connected Components

Kosaraju's algorithm identifies strongly connected components in directed
graphs. Within each component, every vertex is reachable from every other
vertex.

The first DFS pass records vertices in finishing order. The algorithm then
reverses every edge and performs a second DFS pass in reverse finishing
order. Each traversal in the second pass produces one SCC.

Both passes use an explicit stack instead of recursion. Isolated vertices
form individual components. Parallel edges and self-loops are supported;
edge weights do not affect connectivity. Undirected graphs are rejected.

The result contains immutable component lists and operation counters.
Every vertex belongs to exactly one component.

The edge examination counter includes the first DFS pass, construction
of the reversed graph, and the second DFS pass: exactly 3E examinations.
The vertex visit counter records one visit per vertex in each DFS pass:
exactly 2V visits.

## Complexity Analysis

### Graph Representation

The representation uses O(V + E) space.

| Operation | Time complexity |
| --- | --- |
| Create a graph with V vertices | O(V) |
| Add an edge | O(1) amortized |
| Get vertex or edge count | O(1) |
| Traverse a vertex's outgoing edges | O(out-degree) |
| Traverse the logical edge list | O(E) |

### DSU

| Operation | Time complexity |
| --- | --- |
| Initialize V elements | O(V) |
| find and union | O(alpha(V)) amortized |
| connected and componentSize | O(alpha(V)) amortized |
| elementCount and componentCount | O(1) |
| Read or reset statistics | O(1) |

Alpha is the inverse Ackermann function, which grows extremely slowly.
The amortized bound applies across a sequence of operations.

Union by size alone limits tree height to O(log V): when an element's
depth increases, its containing component at least doubles in size.
Path compression shortens future searches. Together, these optimizations
give the O(alpha(V)) amortized bound. A single operation can still take
O(log V) time.

DSU stores O(V) data. Its iterative operations use O(1) extra working space.
Processing E roads takes O(V + E alpha(V)) amortized time, including
initialization.

Complexity analysis for the remaining algorithms will be added with
their implementations.
### Kruskal

Creating DSU takes O(V) time. Copying and sorting E edges takes
O(E log(E + 1)) time. Processing edges takes O(E alpha(V)) amortized time.

The general total bound is
O(V + E log(E + 1) + E alpha(V)).
For connected graphs with at least two vertices, this simplifies
to O(E log E).

Additional space is O(V + E), including DSU, the copied edge list,
sorting workspace, and the selected edges.

More edges increase sorting and processing work. More vertices increase
DSU initialization and storage. Isolated vertices are included in V.

### Prim

The visited array and outer loop require O(V) time. Scanning all
adjacency lists takes O(V + E) time.

Each non-loop edge is inserted into and removed from the binary heap
once. The heap may contain O(E) entries, including obsolete entries.
Therefore, this lazy implementation takes O(V + E log(E + 1)) time.

Additional space is O(V + E): the visited array, selected edges,
and priority queue.

The bound uses log E because this implementation stores candidate edges
in the heap and does not use a decrease-key operation with one entry
per vertex.

### Topological Sorting

Initializing indegrees and scanning the edges takes O(V + E) time.
Each vertex enters and leaves the queue at most once.
Each outgoing adjacency entry is processed at most once after
the initial indegree calculation.

Total time is O(V + E), including cycle detection.
Additional space is O(V) for indegrees, the queue, and the output order.
The input graph is not copied.

Increasing V adds vertex initialization and queue work; increasing E
adds indegree updates and adjacency scans.

### Kosaraju's Algorithm

Time complexity: O(V + E). Each DFS pass processes every vertex and edge,
and constructing the reversed graph takes O(V + E) time.

Additional space complexity: O(V + E). The reversed adjacency lists store
V vertices and E edges. Visited arrays, finishing order, the explicit stack,
and component lists require O(V) space.

Increasing either the number of vertices or edges increases the amount
of work linearly. These bounds assume an adjacency-list representation.


## Running the Project

1. Open the repository in IntelliJ IDEA.
2. Select JDK 17 and language level 17.
3. Mark `src` as Sources Root.
4. Mark `tests` as Test Sources Root.
5. Run `app.Main` for the demonstration.
6. Run `graph.WeightedGraphTest` for the graph tests.
7. Run `dsu.DisjointSetUnionTest` for the DSU tests.
8. Run `mst.KruskalTest` for the Kruskal tests.
9. Run `mst.PrimTest` for the Prim tests and randomized MST comparison.
10. Run `connectivity.TopologicalSortTest` for topological sorting tests.
11. Run `connectivity.KosarajuTest` for the SCC tests.
    Expected final message: `All 8 SCC tests passed.`

## Experimental Setup

Planned configurations:

| Graph | Vertices | Edges |
| --- | ---: | ---: |
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1000 | 5000 |

Experiments will use fixed random seeds and five measured runs per
configuration after JVM warm-up. Timing will use System.nanoTime().
Relevant operations will also be counted.

Generation, validation, printing, and file output will be outside
the timed regions.

Both MST algorithms will receive the same connected undirected graph.
Directed experiments will include DAGs and graphs with guaranteed cycles.
Generation details will be documented with the benchmark implementation.

## Results

No algorithm benchmark results have been collected yet.
Timing tables and plots will be added after implementing the algorithms.

## Comparison and Discussion

Kruskal sorts the complete edge list and uses DSU to avoid cycles.
Prim grows trees through adjacency lists and a priority queue.

Both algorithms returned weight 14 for the demonstration graph.
Their total weights also matched on 100 fixed-seed random connected
graphs. Structural checks verified that their outputs were spanning
trees without cycles.

Equal-weight edges may produce different valid MSTs, so correctness
comparisons use total weight and tree properties rather than identical
edge lists or selection order.

These are correctness results, not timing measurements. Execution-time
and operation-count comparisons across the required graph sizes
will be added after running the benchmarks.

DSU tests separately verify union by size and the reduction in
root-search work after path compression.

## Testing

The graph, DSU, Kruskal, Prim, topological sorting, and SCC test suites
have passed on Microsoft OpenJDK 17.0.18: 51 tests in total.

### Graph Tests: 6

1. Undirected edges, reverse directions, and isolated vertices.
2. Directed edges and weights larger than the int range.
3. Single-vertex and empty graphs.
4. Parallel edges, duplicate weights, and self-loops.
5. Invalid inputs and prevention of partial edge insertion.
6. Protection of edge lists from external modification.

### DSU Tests: 8

1. Initial representatives, sizes, and component counts.
2. Transitive connectivity, redundant unions, and component sizes.
3. Union by size with both argument orders.
4. Path compression and reduced work on repeated searches.
5. Counter values, immutable snapshots, and statistics reset.
6. Single-element and empty sets.
7. Invalid inputs without changing components.
8. Randomized comparison against an independent label-array model.

The randomized test uses seed 42 and performs 250 union attempts on
20 elements. After every attempt, it checks all pairwise connections,
component sizes, and the component count against the reference model.

Tests throw AssertionError when a check fails and do not require -ea.
### Kruskal Tests: 10

1. A connected graph with a known minimum weight.
2. A disconnected graph with an isolated vertex.
3. Equal weights and multiple valid MSTs.
4. Parallel edges and self-loops.
5. Negative weights and weights beyond the int range.
6. Single-vertex and empty graphs.
7. Rejection of directed graphs.
8. Preservation of input order and protection of result edges.
9. Operation counters, including a rejected cycle edge.
10. Detection of accumulated weight overflow.

Forest checks verify the expected weight, component count, V - C edges,
absence of cycles, membership of selected edges in the input, and
connectivity within every original component.

### Prim Tests: 11

1. A connected graph with a known minimum weight.
2. A disconnected graph with an isolated vertex.
3. Equal weights and multiple valid MSTs.
4. Parallel edges and self-loops.
5. Negative weights and weights beyond the int range.
6. Single-vertex and empty graphs.
7. Rejection of directed graphs.
8. Preservation of input order and protection of result edges.
9. Adjacency, queue, and comparison counters.
10. Detection of accumulated weight overflow.
11. Comparison with Kruskal on 100 random connected graphs.

The randomized comparison uses seed 42. Each graph has 2 to 20 vertices.
A generated tree guarantees connectivity, then 2V additional edges
are added. Weights range from -20 to 20; parallel edges and self-loops
are permitted.

Both algorithms receive the same graph. Tests compare their weights
and validate edge membership, absence of cycles, component coverage,
and the number of selected edges. Undirected edges are accepted in
either orientation.

These random graphs are correctness fixtures, separate from the
required performance benchmark datasets.

### Topological Sorting Tests: 8

1. A DAG with validation of its order and operation counters.
2. A directed cycle, a graph with both cyclic and acyclic parts,
   and a self-loop.
3. A disconnected DAG with an isolated vertex.
4. Parallel directed edges.
5. Single-vertex and empty DAGs.
6. Rejection of undirected input.
7. Preservation of input and protection of the returned order.
8. Validation on 100 random DAGs.

Every valid result must contain each vertex exactly once.
For every edge u -> v, the position of u must be smaller than
the position of v. Different valid orderings are accepted.

Random DAG tests use seed 42 and 1 to 20 vertices. Vertex IDs are shuffled,
and each possible forward edge in that permutation is added with
probability 0.3. This guarantees acyclicity without relying on numeric
vertex order. Weights range from -10 to 10 and are ignored by sorting.

Cycle tests verify that the algorithm reports a cycle and returns
an empty order, including when some vertices were processed successfully.

### SCC Tests: 8

1. A graph with three known strongly connected components.
2. A DAG, where every vertex forms its own SCC.
3. A disconnected graph with an isolated vertex.
4. Parallel edges, self-loops, and different edge weights.
5. Single-vertex and empty graphs.
6. Rejection of undirected graphs.
7. Preservation of the input and defensive copying of result lists.
8. Comparison against independent reachability checks on 100 random graphs.

Random tests use seed 42 and graphs with 1 to 12 vertices. Each possible
directed edge, including self-loops, is generated with probability 0.25.

An independent transitive-closure calculation checks every vertex pair:
two vertices must belong to the same SCC if and only if each is reachable
from the other. Tests also verify that every vertex occurs exactly once
and validate the operation counters.

Shortest-path tests are still pending.


## Conclusion

The graph representation, optimized DSU, Kruskal, Prim, topological sorting,
and Kosaraju's SCC algorithm are implemented and tested.

Both MST algorithms handle connected and disconnected undirected graphs.
Their weights agree on known examples and 100 random connected graphs.

Topological sorting returns valid orders for DAGs and detects cycles.
Tests validate ordering constraints on 100 generated DAGs.

SCC tests verify unique vertex membership and correct grouping using
independent mutual-reachability checks, including on 100 random graphs.

The remaining work is Dijkstra's shortest paths and reproducible
performance experiments, including tables, plots, and discussion.
No execution-time comparison is claimed before benchmarking.