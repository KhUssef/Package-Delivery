package ai.proj;

public class h1 implements heuristic {
    public int[][] find(String goalState, int numTunnels, int rows, int cols, int[][] tunnels) {
        int[][] heuristicValues = new int[rows][cols];
        // goal parsing handled in findForPosition
            // Delegate per-cell computation to single-position overload
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    heuristicValues[i][j] = findForPosition(i + "," + j, goalState, numTunnels, rows, cols, tunnels);
                }
            }
        return heuristicValues;
    }

    @Override
    public int findForPosition(String position, String goalState, int numTunnels, int rows, int cols, int[][] tunnels) {
        int r = Integer.parseInt(position.split(",")[0]);
        int c = Integer.parseInt(position.split(",")[1]);
        int goalR = Integer.parseInt(goalState.split(",")[0]);
        int goalC = Integer.parseInt(goalState.split(",")[1]);
        return Math.abs(goalR - r) + Math.abs(goalC - c);
    }
}
