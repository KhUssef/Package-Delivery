package ai.proj;

public interface heuristic {
    int[][] find(String goalState, int numTunnels, int rows, int cols, int[][] tunnels);
    int findForPosition(String position, String goalState, int numTunnels, int rows, int cols, int[][] tunnels);
}
