# Smart City Infrastructure and Route Planning

Assignment 3 — Design and Analysis of Algorithms.

## Overview

This Java application models a city's infrastructure as a weighted graph.
Vertices represent districts, and edges represent roads. Edge weights
represent construction costs or travel distances.

Current status: the graph representation and its tests are implemented.
DSU, MST algorithms, SCC, topological sorting, Dijkstra, and benchmarks
are planned for the next development steps.

## Implementation

The project uses Java 17 and requires no external libraries.

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

## Complexity Analysis

The representation uses O(V + E) space.

| Operation | Time complexity |
| --- | --- |
| Create a graph with V vertices | O(V) |
| Add an edge | O(1) amortized |
| Get vertex or edge count | O(1) |
| Traverse all outgoing edges of a vertex | O(out-degree) |
| Traverse the logical edge list | O(E) |

Complexity analysis for each algorithm will be added with its implementation.

## Running the Project

1. Open the repository in IntelliJ IDEA.
2. Select JDK 17 and language level 17.
3. Mark `src` as Sources Root.
4. Mark `tests` as Test Sources Root.
5. Run `app.Main` for the demonstration.
6. Run `graph.WeightedGraphTest` for the graph tests.

## Experimental Setup

Planned configurations:

| Graph | Vertices | Edges |
| --- | ---: | ---: |
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1000 | 5000 |

Experiments will use a fixed random seed, five measured runs per
configuration, System.nanoTime(), and operation counters.
MST input graphs will be connected and undirected.
Directed experiments will include DAGs and graphs containing cycles.

## Results

No algorithm benchmark results have been collected yet.
Timing tables and plots will be added after implementing the algorithms.

## Comparison and Discussion

Algorithm comparisons will be based on measured results and theoretical
complexity. No performance conclusions are claimed at this stage.

## Testing

All six graph tests passed on Microsoft OpenJDK 17.0.18:

1. Undirected edges, reverse directions, and isolated vertices.
2. Directed edges and weights larger than the int range.
3. Single-vertex and empty graphs.
4. Parallel edges, duplicate weights, and self-loops.
5. Invalid inputs and prevention of partial edge insertion.
6. Protection of edge lists from external modification.

Tests throw AssertionError when a check fails and do not require -ea.
Algorithm-specific correctness tests will be added during development.

## Conclusion

The graph representation is implemented and tested.
The next step is DSU with path compression and union by size,
which will later support Kruskal's algorithm.