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
        String[] parts = goalState.split(",");
        int goalR = Integer.parseInt(parts[0]);
        int goalC = Integer.parseInt(parts[1]);

        int startR = (startRow >= 0 ? startRow : stores[0][0]);
        int startC = (startCol >= 0 ? startCol : stores[0][1]);

        int totalNodesExpanded = 0;

        for (int depth = 0; depth <= maxDepth; depth++) {
            List<String> actions = new ArrayList<>();
            Set<String> pathSet = new HashSet<>();
            MutableInt nodesExpanded = new MutableInt(0);
            MutableInt totalCost = new MutableInt(0);

            boolean found = dfsLimited(startR, startC, goalR, goalC, depth, actions, 0, pathSet, nodesExpanded, totalCost);

            totalNodesExpanded += nodesExpanded.value;

            if (found) {
                String plan = String.join(",", actions);
                return startR + "," + startC + ";" + plan + ";" + totalCost.value + ";" + totalNodesExpanded;
            }

            // Hint GC to reclaim objects from this depth iteration before next
            // Note: This is just a hint; JVM may ignore it
            System.gc();
        }

        return "FAIL;0;" + totalNodesExpanded;
    }

    private boolean dfsLimited(int r, int c,
                               int goalR, int goalC,
                               int depthLimit,
                               List<String> actions,
                               int cost,
                               Set<String> pathSet,
                               MutableInt nodesExpanded,
                               MutableInt totalCost) {

        nodesExpanded.value++;

        if (r == goalR && c == goalC) {
            totalCost.value = cost; // set total cost when goal is found
            return true;
        }

        if (depthLimit == 0) {
            return false;
        }

        String state = r + "," + c;
        if (pathSet.contains(state)) {
            return false; // avoid cycles along current path
        }

        pathSet.add(state);

        List<Successor> successors = getSuccessors(state);
        for (Successor succ : successors) {
            actions.add(succ.action);
            boolean found = dfsLimited(succ.getRow(), succ.getCol(),
                                       goalR, goalC,
                                       depthLimit - 1,
                                       actions,
                                       cost + succ.stepCost,
                                       pathSet,
                                       nodesExpanded,
                                       totalCost);
            if (found) {
                return true; // solution found, propagate upward
            }
            actions.remove(actions.size() - 1); // backtrack
        }

        pathSet.remove(state); // backtrack
        return false;
    }

    // Mutable integer to pass by reference
    private static class MutableInt {
        int value = 0;
        MutableInt(int v) { value = v; }
        MutableInt() {}
    }
}
