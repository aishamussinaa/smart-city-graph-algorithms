# Smart City Infrastructure and Route Planning

Assignment 3 — Design and Analysis of Algorithms.

## Overview

This Java application models a city's infrastructure as a weighted graph.
Vertices represent districts, and edges represent roads. Weights represent
construction costs or travel distances.

Implemented: weighted graph representation and Disjoint Set Union (DSU).
MST algorithms, SCC, topological sorting, Dijkstra, and benchmarks
are planned for the next development steps.

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

### City Demonstration

The demonstration has five districts and four undirected roads.
Processing the roads with DSU produces two components:
{0, 1, 2} and {3, 4}.

Three unions succeed. The remaining road connects districts that already
belong to the same component. This demonstrates undirected connectivity;
DSU does not compute directed strongly connected components.

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

## Running the Project

1. Open the repository in IntelliJ IDEA.
2. Select JDK 17 and language level 17.
3. Mark `src` as Sources Root.
4. Mark `tests` as Test Sources Root.
5. Run `app.Main` for the demonstration.
6. Run `graph.WeightedGraphTest` for the graph tests.
7. Run `dsu.DisjointSetUnionTest` for the DSU tests.

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

Algorithm comparisons will be based on measured results and theoretical
complexity. No experimental performance conclusions are claimed yet.

DSU optimizations are checked by correctness tests, including a test
that verifies fewer root-search links after path compression.

## Testing

All 14 tests passed on Microsoft OpenJDK 17.0.18.

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
MST, SCC, topological-order, and shortest-path tests are still pending.

## Conclusion

The weighted graph representation and optimized DSU are implemented
and tested. The application can represent city roads and determine
undirected connectivity.

The next step is implementing and comparing Kruskal's and Prim's
minimum-spanning-tree algorithms.