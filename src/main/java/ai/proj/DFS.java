package ai.proj;

import java.util.*;

public class DFS extends GenericSearch {

    private static class Node {
        int x, y;
        String path;
        int cost;

        Node(int x, int y, String path, int cost) {
            this.x = x;
            this.y = y;
            this.path = path;
            this.cost = cost;
        }
    }

    @Override
    public String search(String goalState) {

        // goalState format = initialState + ";" + destinationIndex
        String[] split = goalState.split(",");
        int goalIndex = Integer.parseInt(split[split.length - 1]);

        int goalX = this.destinations[goalIndex][0];
        int goalY = this.destinations[goalIndex][1];

        // --- COLLECT BEST RESULT OVER ALL STORES ---
        String bestResult = "FAIL";
        int bestExpanded = Integer.MAX_VALUE;

        // Try each store as a start
        for (int s = 0; s < numStores; s++) {

            int startX = stores[s][0];
            int startY = stores[s][1];

            Stack<Node> stack = new Stack<>();
            HashSet<String> visited = new HashSet<>();

            stack.push(new Node(startX, startY, "", 0));

            int nodesExpanded = 0;
            boolean found = false;

            while (!stack.isEmpty()) {

                Node curr = stack.pop();
                nodesExpanded++;

                String key = curr.x + "," + curr.y;

                if (visited.contains(key))
                    continue;

                visited.add(key);

                // Goal check
                if (curr.x == goalX && curr.y == goalY) {
                    found = true;

                    String result = curr.path + ";" + curr.cost + ";" + nodesExpanded;

                    // Keep best (least expanded)
                    if (nodesExpanded < bestExpanded) {
                        bestExpanded = nodesExpanded;
                        bestResult = result;
                    }

                    break; // DFS store finished
                }

                // Expand DFS neighbors
                for (Node nxt : expand(curr)) {
                    if (!visited.contains(nxt.x + "," + nxt.y)) {
                        stack.push(nxt);
                    }
                }
            }
        }

        return bestResult;
    }

    private List<Node> expand(Node n) {
        List<Node> list = new ArrayList<>();

        int x = n.x, y = n.y;

        // RIGHT
        if (y + 1 < cols && traffic[x][y][0] != 0) {
            int c = traffic[x][y][0];
            list.add(new Node(x, y + 1, n.path + "R,", n.cost + c));
        }

        // LEFT
        if (y - 1 >= 0 && traffic[x][y - 1][0] != 0) {
            int c = traffic[x][y - 1][0];
            list.add(new Node(x, y - 1, n.path + "L,", n.cost + c));
        }

        // DOWN
        if (x + 1 < rows && traffic[x][y][1] != 0) {
            int c = traffic[x][y][1];
            list.add(new Node(x + 1, y, n.path + "D,", n.cost + c));
        }

        // UP
        if (x - 1 >= 0 && traffic[x - 1][y][1] != 0) {
            int c = traffic[x - 1][y][1];
            list.add(new Node(x - 1, y, n.path + "U,", n.cost + c));
        }

        // TUNNELS
        for (int i = 0; i < numTunnels; i++) {
            int ax = tunnels[i * 2][0];
            int ay = tunnels[i * 2][1];
            int bx = tunnels[i * 2 + 1][0];
            int by = tunnels[i * 2 + 1][1];

            if (x == ax && y == ay) {
                int manhattan = Math.abs(ax - bx) + Math.abs(ay - by);
                list.add(new Node(bx, by, n.path + "T" + i + ",", n.cost + manhattan));
            } else if (x == bx && y == by) {
                int manhattan = Math.abs(ax - bx) + Math.abs(ay - by);
                list.add(new Node(ax, ay, n.path + "T" + i + ",", n.cost + manhattan));
            }
        }

        return list;
    }
}
