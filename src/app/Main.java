package app;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;

public class Main {
    public static void main(String[] args) {
        System.out.println("Smart City Infrastructure and Route Planning");

        WeightedGraph city = new WeightedGraph(5, false);

        city.addEdge(0, 1, 4);
        city.addEdge(0, 2, 6);
        city.addEdge(1, 2, 3);
        city.addEdge(3, 4, 2);

        System.out.println("Districts: " + city.vertexCount());
        System.out.println("Roads: " + city.edgeCount());

        DisjointSetUnion dsu = new DisjointSetUnion(city.vertexCount());

        for (Edge road : city.edges()) {
            dsu.union(road.from(), road.to());
        }

        System.out.println("Connected components: " + dsu.componentCount());
        System.out.println("0 and 2 connected: " + dsu.connected(0, 2));
        System.out.println("0 and 4 connected: " + dsu.connected(0, 4));
        System.out.println("Component size of 0: " + dsu.componentSize(0));
        System.out.println("Successful unions: "
                + dsu.statistics().successfulUnions());
    }
}