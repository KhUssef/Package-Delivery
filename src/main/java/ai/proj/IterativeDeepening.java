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

        List<String> bestActions = null;
        int bestCost = Integer.MAX_VALUE;
        int totalNodesExpanded = 0;

        for (int s = 0; s < stores.length; s++) {
            int startR = stores[s][0];
            int startC = stores[s][1];

            for (int depth = 0; depth <= maxDepth; depth++) {
                Set<String> visited = new HashSet<>();
                PathResult result = dfsLimited(startR, startC, goalR, goalC, depth, visited, new ArrayList<>(), 0);

                // Always accumulate nodes expanded
                if (result != null) {
                    totalNodesExpanded += result.nodesExpanded;
                }

                if (result != null && result.actions != null) {
                    if (result.cost < bestCost) {
                        bestCost = result.cost;
                        bestActions = result.actions;
                    }
                    break; // No need deeper for this store
                }
            }
        }

        if (bestActions == null) {
            return "FAIL;0;" + totalNodesExpanded;
        }

        // Return unified format: plan;cost;nodesExpanded
        String plan = String.join(",", bestActions);
        return plan + ";" + bestCost + ";" + totalNodesExpanded;
    }
    
    private PathResult dfsLimited(int r, int c,
                                  int goalR, int goalC,
                                  int depthLimit,
                                  Set<String> visited,
                                  List<String> actions,
                                  int cost) {

        // Count this node as expanded
        int nodesExpanded = 1;

        if (r == goalR && c == goalC) {
            return new PathResult(new ArrayList<>(actions), cost, nodesExpanded);
        }

        if (depthLimit == 0) {
            return new PathResult(null, Integer.MAX_VALUE, nodesExpanded);
        }

        String state = r + "," + c;
        if (visited.contains(state)) {
            return new PathResult(null, Integer.MAX_VALUE, nodesExpanded);
        }

        visited.add(state);

        // Use generic successor generator
        String currentState = r + "," + c;
        List<Successor> successors = getSuccessors(currentState);

        for (Successor succ : successors) {
            List<String> newActions = new ArrayList<>(actions);
            newActions.add(succ.action);
            int newCost = cost + succ.stepCost;

            PathResult res = dfsLimited(
                    succ.getRow(), succ.getCol(),
                    goalR, goalC,
                    depthLimit - 1,
                    visited,
                    newActions,
                    newCost
            );

            if (res != null && res.actions != null) {
                visited.remove(state);
                // Accumulate nodes expanded
                return new PathResult(res.actions, res.cost, nodesExpanded + res.nodesExpanded);
            }
            // Accumulate nodes expanded even if this path didn't lead to goal
            if (res != null) {
                nodesExpanded += res.nodesExpanded;
            }
        }

        visited.remove(state);
        return new PathResult(null, Integer.MAX_VALUE, nodesExpanded);
    }

    private static class PathResult {
        List<String> actions;
        int cost;
        int nodesExpanded;
        
        PathResult(List<String> actions, int cost, int nodesExpanded) {
            this.actions = actions;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
        }
    }
}

