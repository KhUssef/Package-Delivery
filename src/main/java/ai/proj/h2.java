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
        int goalR = Integer.parseInt(goalState.split(",")[0]);
        int goalC = Integer.parseInt(goalState.split(",")[1]);
        int[] goalToTunnel = new int[numTunnels * 2];

        // Precompute distance from goal to each tunnel endpoint
        for (int t = 0; t < numTunnels; t++) {
            int r1 = tunnels[2 * t][0], c1 = tunnels[2 * t][1];
            int r2 = tunnels[2 * t + 1][0], c2 = tunnels[2 * t + 1][1];

            goalToTunnel[2 * t] = Math.abs(goalR - r1) + Math.abs(goalC - c1);
            goalToTunnel[2 * t + 1] = Math.abs(goalR - r2) + Math.abs(goalC - c2);
        }

        // Compute heuristic for each grid cell
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Base Manhattan distance
                int h = Math.abs(goalR - i) + Math.abs(goalC - j);

                // Consider tunnel shortcuts via either endpoint
                for (int t = 0; t < numTunnels; t++) {
                    int r1 = tunnels[2 * t][0], c1 = tunnels[2 * t][1];
                    int r2 = tunnels[2 * t + 1][0], c2 = tunnels[2 * t + 1][1];

                    int tunnelCost = Math.abs(r1 - r2) + Math.abs(c1 - c2);

                    int h1 = Math.abs(i - r1) + Math.abs(j - c1)
                            + tunnelCost
                            + goalToTunnel[2 * t + 1];

                    int h2 = Math.abs(i - r2) + Math.abs(j - c2)
                            + tunnelCost
                            + goalToTunnel[2 * t];

                    h = Math.min(h, Math.min(h1, h2));
                }

                heuristicValues[i][j] = h;
            }
        }
        return heuristicValues;
    }
}
