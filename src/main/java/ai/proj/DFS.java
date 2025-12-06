package ai.proj;

import java.util.*;

public class DFS extends GenericSearch {

    private static class Node {
        int x, y;
        List<String> actions;  // List of canonical action names
        int cost;

        Node(int x, int y, List<String> actions, int cost) {
            this.x = x;
            this.y = y;
            this.actions = new ArrayList<>(actions);
            this.cost = cost;
        }
    }

    @Override
    public String search(String goalState) {
        // goalState format = "x,y" coordinates of the goal
        String[] split = goalState.split(",");
        int goalX = Integer.parseInt(split[0]);
        int goalY = Integer.parseInt(split[1]);

        // --- COLLECT BEST RESULT OVER ALL STORES ---
        String bestResult = "FAIL;0;0";
        int bestExpanded = Integer.MAX_VALUE;

        // Try each store as a start
        for (int s = 0; s < numStores; s++) {
            int startX = stores[s][0];
            int startY = stores[s][1];

            Stack<Node> stack = new Stack<>();
            HashSet<String> visited = new HashSet<>();

            stack.push(new Node(startX, startY, new ArrayList<>(), 0));

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
                    
                    // Format: plan;cost;nodesExpanded
                    // plan is comma-separated list of actions
                    String plan = String.join(",", curr.actions);
                    String result = plan + ";" + curr.cost + ";" + nodesExpanded;

                    // Keep best (least expanded)
                    if (nodesExpanded < bestExpanded) {
                        bestExpanded = nodesExpanded;
                        bestResult = result;
                    }

                    break; // DFS store finished
                }

                // Expand DFS neighbors using generic successor generator
                String currentState = curr.x + "," + curr.y;
                List<Successor> successors = getSuccessors(currentState);
                
                // Add successors in reverse order to maintain tieBreakerOrder when popping from stack
                for (int i = successors.size() - 1; i >= 0; i--) {
                    Successor succ = successors.get(i);
                    String newKey = succ.getRow() + "," + succ.getCol();
                    if (!visited.contains(newKey)) {
                        List<String> newActions = new ArrayList<>(curr.actions);
                        newActions.add(succ.action);
                        stack.push(new Node(succ.getRow(), succ.getCol(), newActions, curr.cost + succ.stepCost));
                    }
                }
            }
        }

        return bestResult;
    }
}
