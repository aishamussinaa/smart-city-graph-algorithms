package app;

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
        System.out.println("Roads from district 0: " + city.edgesFrom(0));
        System.out.println("Roads from district 4: " + city.edgesFrom(4));
    }
}