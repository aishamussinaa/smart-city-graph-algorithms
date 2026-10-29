package app;

import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MstResult;

public class Main {
    public static void main(String[] args) {
        System.out.println("Smart City Infrastructure and Route Planning");

        WeightedGraph city = new WeightedGraph(5, false);

        city.addEdge(0, 1, 4);
        city.addEdge(0, 2, 6);
        city.addEdge(1, 2, 3);
        city.addEdge(3, 4, 2);
        city.addEdge(2, 3, 5);

        MstResult result = Kruskal.compute(city);

        System.out.println("Algorithm: Kruskal");
        System.out.println("Connected: " + result.isConnected());
        System.out.println("Components: " + result.components());
        System.out.println("Selected roads: " + result.edges().size());
        System.out.println("Total weight: " + result.totalWeight());

        for (Edge road : result.edges()) {
            System.out.println(
                    road.from() + " - " + road.to()
                            + ", weight: " + road.weight()
            );
        }
    }
}