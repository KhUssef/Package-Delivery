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
    Greedy() {
        this.heuristic = new h1();
    }

    /**
     * Node class representing a state in the search space
     */
    private class Node {
        int row;                // Current row position in grid
        int col;                // Current column position in grid
        String path;            // Sequence of actions taken to reach this state
        int cost;               // Actual cost g(n) from start to current node
        int heuristicValue;     // Heuristic estimate h(n) from current to goal

        /**
         * Constructor for creating a search node
         * @param row Row position
         * @param col Column position
         * @param path Action sequence
         * @param cost Actual accumulated cost
         * @param heuristicValue Heuristic estimate to goal
         */
        public Node(int row, int col, String path, int cost, int heuristicValue) {
            this.row = row;
            this.col = col;
            this.path = path;
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
     * For Greedy search, use search(String goalState, int[][] heuristicValues) instead
     */
    @Override
    public String search(String goalState) {
        throw new UnsupportedOperationException(
            "Greedy search requires heuristic values. Use search(String goalState, int[][] heuristicValues)"
        );
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

        // Track best solution across all stores
        String bestPath = null;
        int bestStore = -1;
        int bestCost = Integer.MAX_VALUE;
        int totalNodesExpanded = 0;

        // Try searching from each store location
        for (int s = 0; s < stores.length; s++) {
            int startR = stores[s][0];
            int startC = stores[s][1];

            // Run greedy search from current store
            GreedyResult result = greedySearch(startR, startC, goalR, goalC, heuristicValues);
            totalNodesExpanded += result.nodesExpanded;

            // Update best solution if this path has lower cost
            if (result.path != null && result.cost < bestCost) {
                bestCost = result.cost;
                bestPath = result.path;
                bestStore = s;
            }
        }

        // Return failure if no path found from any store
        if (bestPath == null) {
            return "FAIL;0;" + totalNodesExpanded;
        }

        // Reconstruct and return solution with coordinates
        System.out.println(bestStore + ";" + bestPath);
        return rebuild(bestStore + ";" + bestPath) + bestCost + ";" + totalNodesExpanded;
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
    private GreedyResult greedySearch(int startR, int startC, int goalR, int goalC, int[][] heuristicValues) {
        // Priority queue - nodes with lower heuristic values have higher priority
        PriorityQueue<Node> frontier = new PriorityQueue<>(
            Comparator.comparingInt(n -> n.heuristicValue)
        );

        // Set to track already explored states
        Set<String> explored = new HashSet<>();
        int nodesExpanded = 0;

        // Create and add initial node
        Node initialNode = new Node(
            startR,
            startC,
            "",
            0,
            heuristicValues[startR][startC]
        );
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
                return new GreedyResult(currentNode.path, currentNode.cost, nodesExpanded);
            }

            // Generate and add successor states
            List<Move> successors = getSuccessors(currentNode.row, currentNode.col);

            for (Move move : successors) {
                // Build path by appending new action
                String newPath = currentNode.path.isEmpty()
                    ? move.dir
                    : currentNode.path + ";" + move.dir;

                // Update actual cost (tracked but not used for prioritization)
                int newCost = currentNode.cost + move.cost;

                // Get heuristic value for new position
                int newHeuristic = heuristicValues[move.r][move.c];

                // Create successor node
                Node successor = new Node(move.r, move.c, newPath, newCost, newHeuristic);

                // Add to frontier if not already explored
                if (!explored.contains(successor.getStateKey())) {
                    frontier.add(successor);
                }
            }
        }

        // No path found from this starting position
        return new GreedyResult(null, Integer.MAX_VALUE, nodesExpanded);
    }

    /**
     * Generate all valid successor states from current position
     * Includes 4-directional movement and tunnel teleportation
     *
     * @param r Current row
     * @param c Current column
     * @return List of valid moves with costs
     */
    private List<Move> getSuccessors(int r, int c) {
        List<Move> moves = new ArrayList<>();

        // UP - move to cell above (if valid and not blocked)
        if (r - 1 >= 0 && traffic[r - 1][c][1] > 0) {
            int cost = traffic[r - 1][c][1];
            moves.add(new Move(r - 1, c, "U", cost));
        }

        // RIGHT - move to cell on right
        if (c + 1 < cols && traffic[r][c][0] > 0) {
            int cost = traffic[r][c][0];
            moves.add(new Move(r, c + 1, "R", cost));
        }

        // DOWN - move to cell below
        if (r + 1 < rows && traffic[r][c][1] > 0) {
            int cost = traffic[r][c][1];
            moves.add(new Move(r + 1, c, "D", cost));
        }

        // LEFT - move to cell on left
        if (c - 1 >= 0 && traffic[r][c - 1][0] > 0) {
            int cost = traffic[r][c - 1][0];
            moves.add(new Move(r, c - 1, "L", cost));
        }

        // TUNNELS - check for tunnel entrances at current position
        for (int i = 0; i < numTunnels; i++) {
            int r1 = tunnels[2 * i][0], c1 = tunnels[2 * i][1];
            int r2 = tunnels[2 * i + 1][0], c2 = tunnels[2 * i + 1][1];

            // If at tunnel entrance 1, can teleport to entrance 2
            if (r == r1 && c == c1) {
                int cost = Math.abs(r1 - r2) + Math.abs(c1 - c2);
                moves.add(new Move(r2, c2, "T", cost));
            }
            // If at tunnel entrance 2, can teleport to entrance 1
            else if (r == r2 && c == c2) {
                int cost = Math.abs(r1 - r2) + Math.abs(c1 - c2);
                moves.add(new Move(r1, c1, "T", cost));
            }
        }

        return moves;
    }

    /**
     * Reconstruct full path with coordinates from action sequence
     * Converts actions (U/D/L/R/T) into coordinate sequence
     *
     * @param path Action sequence with store index "storeIdx;action1;action2;..."
     * @return Coordinate path "x1,y1;x2,y2;x3,y3;..."
     */
    private String rebuild(String path) {
        String[] moves = path.split(";");
        StringBuilder result = new StringBuilder();

        // Get starting position from store index
        int storeIndex = Integer.parseInt(moves[0]);
        result.append(this.stores[storeIndex][0])
              .append(',')
              .append(this.stores[storeIndex][1])
              .append(';');

        // Track current position during path reconstruction
        int[] current = {
            this.stores[storeIndex][0],
            this.stores[storeIndex][1]
        };

        // Remove store index, keep only actions
        moves = Arrays.copyOfRange(moves, 1, moves.length);

        // Process each action and update position
        for (String move : moves) {
            switch(move) {
                case "R": // Move right
                    current[1] += 1;
                    break;
                case "L": // Move left
                    current[1] -= 1;
                    break;
                case "U": // Move up
                    current[0] -= 1;
                    break;
                case "D": // Move down
                    current[0] += 1;
                    break;
                case "T": // Tunnel - find exit and teleport
                    for (int i = 0; i < numTunnels; i++) {
                        if (tunnels[i * 2][0] == current[0] && tunnels[i * 2][1] == current[1]) {
                            // At entrance 1, go to entrance 2
                            current[0] = tunnels[i * 2 + 1][0];
                            current[1] = tunnels[i * 2 + 1][1];
                            break;
                        } else if (tunnels[i * 2 + 1][0] == current[0] && tunnels[i * 2 + 1][1] == current[1]) {
                            // At entrance 2, go to entrance 1
                            current[0] = tunnels[i * 2][0];
                            current[1] = tunnels[i * 2][1];
                            break;
                        }
                    }
                    break;
            }
            // Append new position to result path
            result.append(current[0]).append(',').append(current[1]).append(';');
        }

        return result.toString();
    }

    /**
     * Helper class representing a possible move/action
     */
    private static class Move {
        int r, c;       // Target row and column
        String dir;     // Direction: U, D, L, R, or T (tunnel)
        int cost;       // Cost of this movement

        Move(int r, int c, String dir, int cost) {
            this.r = r;
            this.c = c;
            this.dir = dir;
            this.cost = cost;
        }
    }

    /**
     * Result class encapsulating search outcome from a single start position
     */
    private static class GreedyResult {
        String path;            // Action sequence to goal
        int cost;               // Total actual cost of path
        int nodesExpanded;      // Number of nodes explored

        GreedyResult(String path, int cost, int nodesExpanded) {
            this.path = path;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
        }
    }
}
