package ai.proj;

import java.util.*;

/**
 * Greedy Best-First Search implementation for package delivery routing
 *
 * This algorithm uses only the heuristic function h(n) to guide the search,
 * always expanding the node that appears closest to the goal according to
 * the heuristic, without considering the actual path cost g(n).
 */
public class Greedy extends GenericSearch {

    // Heuristic function to estimate distance to goal
    private heuristic heuristic;

    /**
     * Constructor to initialize Greedy search with a heuristic function
     * @param heuristic The heuristic to use for estimating goal distance
     */
    Greedy(heuristic heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Default constructor using h1 heuristic (Manhattan distance with tunnels)
     */
    Greedy(int heuristicType) {
        if (heuristicType == 1) {
            this.heuristic = new h1();
        } else if (heuristicType == 2) {
            this.heuristic = new h2();
        } else {
            throw new IllegalArgumentException("Invalid heuristic type");
        }
    }

    /**
     * Node class representing a state in the search space
     */
    private class Node {
        int row;                // Current row position in grid
        int col;                // Current column position in grid
        List<String> actions;   // Sequence of canonical actions taken to reach this state
        int cost;               // Actual cost g(n) from start to current node
        int heuristicValue;     // Heuristic estimate h(n) from current to goal

        /**
         * Constructor for creating a search node
         * @param row Row position
         * @param col Column position
         * @param actions Action sequence (list of canonical action names)
         * @param cost Actual accumulated cost
         * @param heuristicValue Heuristic estimate to goal
         */
        public Node(int row, int col, List<String> actions, int cost, int heuristicValue) {
            this.row = row;
            this.col = col;
            this.actions = new ArrayList<>(actions);
            this.cost = cost;
            this.heuristicValue = heuristicValue;
        }

        /**
         * Generate unique state identifier based on position
         * @return State key in format "row,col"
         */
        public String getStateKey() {
            return row + "," + col;
        }
    }

    /**
     * Required override from GenericSearch
     * Compute heuristic values first (like A*) then run Greedy using them
     */
    @Override
    public String search(String goalState) {
        // Ensure extract(...) was called and grid metadata is available
        if (this.rows <= 0 || this.cols <= 0) {
            return "FAIL;0;0";
        }

        // Lazy compute heuristic per position and cache
        int[][] heuristicValues = new int[this.rows][this.cols];
        for (int i = 0; i < this.rows; i++) {
            Arrays.fill(heuristicValues[i], -1);
        }

        // Use provided heuristic or default to h1
        heuristic h = (this.heuristic != null) ? this.heuristic : new h1();

        // Delegate to greedySearch with per-position cache usage
        // Parse goal coordinates for initial nodes
        String[] coords = goalState.split(",");
        int goalR = Integer.parseInt(coords[0]);
        int goalC = Integer.parseInt(coords[1]);

        int startR = (startRow >= 0 ? startRow : stores[0][0]);
        int startC = (startCol >= 0 ? startCol : stores[0][1]);

        GreedyResult result = greedySearchWithCache(startR, startC, goalR, goalC, heuristicValues, h, goalState);
        if (result.actions == null) {
            return "FAIL;0;" + result.nodesExpanded;
        }
        String plan = String.join(",", result.actions);
        return startR + "," + startC + ";" + plan + ";" + result.cost + ";" + result.nodesExpanded;
    }

    /**
     * Main search method implementing Greedy Best-First Search algorithm
     * Searches from all available stores and returns the best path found
     *
     * @param goalState Target destination in format "row,col"
     * @param heuristicValues Precomputed heuristic values for each cell in the grid
     *                       (computed externally using the heuristic interface)
     * @return Solution string "path;cost;nodesExpanded" or "FAIL;0;nodesExpanded"
     */
    public String search(String goalState, int[][] heuristicValues) {
        // Parse goal coordinates
        String[] coords = goalState.split(",");
        int goalR = Integer.parseInt(coords[0]);
        int goalC = Integer.parseInt(coords[1]);

        int startR = (startRow >= 0 ? startRow : stores[0][0]);
        int startC = (startCol >= 0 ? startCol : stores[0][1]);

        heuristic h = (this.heuristic != null) ? this.heuristic : new h1();
        GreedyResult result = greedySearchWithCache(startR, startC, goalR, goalC, heuristicValues, h, goalState);
        if (result.actions == null) {
            return "FAIL;0;" + result.nodesExpanded;
        }
        String plan = String.join(",", result.actions);
        return startR + "," + startC + ";" + plan + ";" + result.cost + ";" + result.nodesExpanded;
    }

    /**
     * Execute greedy best-first search from a specific starting position
     * Uses priority queue ordered by heuristic value only (not actual cost)
     *
     * @param startR Starting row coordinate
     * @param startC Starting column coordinate
     * @param goalR Goal row coordinate
     * @param goalC Goal column coordinate
     * @param heuristicValues Precomputed heuristic values for each cell
     * @return GreedyResult containing path, cost, and statistics
     */
    private GreedyResult greedySearchWithCache(int startR, int startC, int goalR, int goalC, int[][] heuristicValues, heuristic h, String goalState) {
        // Priority queue - nodes with lower heuristic values have higher priority
        PriorityQueue<Node> frontier = new PriorityQueue<>(
            Comparator.comparingInt(n -> n.heuristicValue)
        );

        // Set to track already explored states
        Set<String> explored = new HashSet<>();
        int nodesExpanded = 0;

        // Create and add initial node
        int hStart = getHeuristicCached(startR, startC, heuristicValues, h, goalState);
        Node initialNode = new Node(startR, startC, new ArrayList<>(), 0, hStart);
        frontier.add(initialNode);

        // Main search loop
        while (!frontier.isEmpty()) {
            // Get node with lowest heuristic value (greedy selection)
            Node currentNode = frontier.poll();

            // Skip if already explored
            String stateKey = currentNode.getStateKey();
            if (explored.contains(stateKey)) {
                continue;
            }

            // Mark as explored
            explored.add(stateKey);
            nodesExpanded++;

            // Goal test - check if destination reached
            if (currentNode.row == goalR && currentNode.col == goalC) {
                return new GreedyResult(currentNode.actions, currentNode.cost, nodesExpanded);
            }

            // Generate and add successor states using generic method
            String currentState = currentNode.row + "," + currentNode.col;
            List<Successor> successors = getSuccessors(currentState);

            for (Successor succ : successors) {
                // Build action list by appending new action
                List<String> newActions = new ArrayList<>(currentNode.actions);
                newActions.add(succ.action);

                // Update actual cost (tracked but not used for prioritization)
                int newCost = currentNode.cost + succ.stepCost;

                // Get heuristic value for new position
                int newHeuristic = getHeuristicCached(succ.getRow(), succ.getCol(), heuristicValues, h, goalState);

                // Create successor node
                Node successor = new Node(succ.getRow(), succ.getCol(), newActions, newCost, newHeuristic);

                // Add to frontier if not already explored
                if (!explored.contains(successor.getStateKey())) {
                    frontier.add(successor);
                }
            }
        }

        // No path found from this starting position
        return new GreedyResult(null, Integer.MAX_VALUE, nodesExpanded);
    }

    // Note: getSuccessors is now inherited from GenericSearch, no need to override


    /**
     * Result class encapsulating search outcome from a single start position
     */
    private static class GreedyResult {
        List<String> actions;   // Action sequence to goal (canonical names)
        int cost;               // Total actual cost of path
        int nodesExpanded;      // Number of nodes explored

        GreedyResult(List<String> actions, int cost, int nodesExpanded) {
            this.actions = actions;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
        }
    }

    private int getHeuristicCached(int r, int c, int[][] cache, heuristic h, String goalState) {
        if (cache[r][c] >= 0) return cache[r][c];
        int hv = h.findForPosition(r + "," + c, goalState, this.numTunnels, this.rows, this.cols, this.tunnels);
        cache[r][c] = hv;
        return hv;
    }
}
