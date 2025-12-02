package ai.proj;

import java.util.*;

public class IterativeDeepening extends GenericSearch {

    private final int maxDepth;

    public IterativeDeepening() {
        this(20);
    }

    public IterativeDeepening(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public String search(String goalState) {

        // Parse goal
        String[] parts = goalState.split(",");
        int goalR = Integer.parseInt(parts[0]);
        int goalC = Integer.parseInt(parts[1]);

        String bestPath = null;
        int bestStore = -1;
        int bestDepth = Integer.MAX_VALUE;

        for (int s = 0; s < stores.length; s++) {
            int startR = stores[s][0];
            int startC = stores[s][1];

            for (int depth = 0; depth <= maxDepth; depth++) {

                Set<String> visited = new HashSet<>();
                PathResult result =
                        dfsLimited(startR, startC, goalR, goalC, depth, visited, "");

                if (result != null) {
                    if (depth < bestDepth) {
                        bestDepth = depth;
                        bestPath = result.path;
                        bestStore = s;
                    }
                    break; // No need deeper for this store
                }
            }
        }

        if (bestPath == null) return "FAIL";
        System.out.println(bestStore + ";" + bestPath);

        return rebuild(bestStore + ";" + bestPath);
    }
    private PathResult dfsLimited(int r, int c,
                                  int goalR, int goalC,
                                  int depthLimit,
                                  Set<String> visited,
                                  String path) {

        if (r == goalR && c == goalC)
            return new PathResult(path);

        if (depthLimit == 0)
            return null;

        String state = r + "," + c;
        if (visited.contains(state))
            return null;

        visited.add(state);

        for (Move mv : successors(r, c)) {

            String newPath = path.isEmpty() ? mv.dir : path + ";" + mv.dir;

            PathResult res = dfsLimited(
                    mv.r, mv.c,
                    goalR, goalC,
                    depthLimit - 1,
                    visited,
                    newPath
            );

            if (res != null) {
                visited.remove(state);
                return res;
            }
        }

        visited.remove(state);
        return null;
    }

    private List<Move> successors(int r, int c) {
        List<Move> mv = new ArrayList<>();

        // UP
        if (r - 1 >= 0 && traffic[r - 1][c][1] > 0)
            mv.add(new Move(r - 1, c, "U"));

        // RIGHT
        if (c + 1 < cols && traffic[r][c][0] > 0)
            mv.add(new Move(r, c + 1, "R"));

        // DOWN
        if (r + 1 < rows && traffic[r][c][1] > 0)
            mv.add(new Move(r + 1, c, "D"));

        // LEFT
        if (c - 1 >= 0 && traffic[r][c - 1][0] > 0)
            mv.add(new Move(r, c - 1, "L"));

        // TUNNELS (always last)
        for (int i = 0; i < numTunnels; i++) {
            int r1 = tunnels[2 * i][0], c1 = tunnels[2 * i][1];
            int r2 = tunnels[2 * i + 1][0], c2 = tunnels[2 * i + 1][1];

            if (r == r1 && c == c1) mv.add(new Move(r2, c2, "T"));
            else if (r == r2 && c == c2) mv.add(new Move(r1, c1, "T"));
        }

        return mv;
    }

    private static class Move {
        int r, c;
        String dir;

        Move(int r, int c, String dir) {
            this.r = r;
            this.c = c;
            this.dir = dir;
        }
    }

    private static class PathResult {
        String path;
        PathResult(String path) { this.path = path; }
    }
    private String  rebuild(String path){
        String[] moves = path.split(";");
        StringBuilder result = new StringBuilder();
        result.append(this.stores[Integer.parseInt(moves[0])][0]).append(',').append(this.stores[Integer.parseInt(moves[0])][1]).append(';');
        int current[] = {this.stores[Integer.parseInt(moves[0])][0], this.stores[Integer.parseInt(moves[0])][1]};
        moves = Arrays.copyOfRange(moves, 1, moves.length);
        for (String move : moves) {
            switch( move) {
                case "R":
                    current[1] += 1;
                    break;
                case "L":
                    current[1] -= 1;
                    break;
                case "U":
                    current[0] -= 1;
                    break;
                case "D":
                    current[0] += 1;
                    break;
                case "T":
                    // Find the tunnel exit
                    for (int i = 0; i < numTunnels; i++) {
                        if (tunnels[i * 2][0] == current[0] && tunnels[i * 2][1] == current[1]) {
                            current[0] = tunnels[i * 2 + 1][0];
                            current[1] = tunnels[i * 2 + 1][1];
                            break;
                        } else if (tunnels[i * 2 + 1][0] == current[0] && tunnels[i * 2 + 1][1] == current[1]) {
                            current[0] = tunnels[i * 2][0];
                            current[1] = tunnels[i * 2][1];
                            break;
                        }
                    }
                    break;
            }
            result.append(current[0]).append(',').append(current[1]).append(';');
        }
        return result.toString();
    }
}

