package ai.proj;

public class h1 implements heuristic {
    public int[][] find(String goalState, int numTunnels, int rows, int cols, int[][] tunnels) {
        int[][] heuristicValues = new int[rows][cols];
        int goalR = Integer.parseInt(goalState.split(",")[0]);
        int goalC = Integer.parseInt(goalState.split(",")[1]);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                heuristicValues[i][j] = Math.abs(goalR - i) + Math.abs(goalC - j);
            }
        }
        return heuristicValues;
    }
}
