package ai.proj;

/**
 * Heuristic h2 based on h1 (Manhattan distance with tunnel shortcuts).
 * Currently uses the same formulation as h1, serving as an alternative
 * heuristic implementation that can be swapped in for experimentation.
 */
public class h2 implements heuristic {
    @Override
    public int[][] find(String goalState, int numTunnels, int rows, int cols, int[][] tunnels) {
        int[][] heuristicValues = new int[rows][cols];
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

        // Base Manhattan distance
        int h = Math.abs(goalR - r) + Math.abs(goalC - c);

        // Consider tunnel shortcuts via either endpoint
        for (int t = 0; t < numTunnels; t++) {
            int r1 = tunnels[2 * t][0], c1 = tunnels[2 * t][1];
            int r2 = tunnels[2 * t + 1][0], c2 = tunnels[2 * t + 1][1];

            int tunnelCost = Math.abs(r1 - r2) + Math.abs(c1 - c2);

            int viaE1 = Math.abs(r - r1) + Math.abs(c - c1)
                    + tunnelCost
                    + Math.abs(goalR - r2) + Math.abs(goalC - c2);

            int viaE2 = Math.abs(r - r2) + Math.abs(c - c2)
                    + tunnelCost
                    + Math.abs(goalR - r1) + Math.abs(goalC - c1);

            h = Math.min(h, Math.min(viaE1, viaE2));
        }

        return h;
    }
}
