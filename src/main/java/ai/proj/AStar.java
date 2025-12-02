package ai.proj;

public class AStar extends GenericSearch {
    private int[][] heuristicValues;
    private heuristic heuristicFunction;

    @Override
    public String search(String goalState) {
        // Implementation of A* search algorithm goes here
        return "A* search executed";
    }

    public void h(String goalState) {
        this.heuristicFunction = new h1();
        this.heuristicValues = this.heuristicFunction.find(goalState, this.numTunnels, this.rows, this.cols, this.tunnels);
    }
}
