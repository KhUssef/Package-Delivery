package ai.proj;

import java.util.*;

public class AStar extends GenericSearch {
    private int[][] heuristicValues;
    private heuristic heuristicFunction;

    public AStar(int heuristicType) {
        if (heuristicType == 1) {
            this.heuristicFunction = new h1();
        } else if (heuristicType == 2) {
            this.heuristicFunction = new h2();
        } else {
            throw new IllegalArgumentException("Invalid heuristic type");
        }
        
    }

    @Override
    public String search(String goalState) {
        // Parse goal state
        String[] coords = goalState.split(",");
        int goalR = Integer.parseInt(coords[0]);
        int goalC = Integer.parseInt(coords[1]);
        
        // Lazy compute heuristic values per position and cache in matrix
        this.heuristicValues = new int[this.rows][this.cols];
        for (int i = 0; i < this.rows; i++) {
            Arrays.fill(this.heuristicValues[i], -1);
        }
        
        int startR = (startRow >= 0 ? startRow : stores[0][0]);
        int startC = (startCol >= 0 ? startCol : stores[0][1]);

        AStarResult result = aStarSearch(startR, startC, goalR, goalC, goalState);
        if (result.actions == null) {
            return "FAIL;0;" + result.nodesExpanded;
        }
        String plan = String.join(",", result.actions);
        return startR + "," + startC + ";" + plan + ";" + result.cost + ";" + result.nodesExpanded;
    }
    
    private AStarResult aStarSearch(int startR, int startC, int goalR, int goalC, String goalState) {
        // Priority queue ordered by f(n) = g(n) + h(n)
        PriorityQueue<Node> frontier = new PriorityQueue<>(
            Comparator.comparingInt(n -> n.cost + n.heuristicValue)
        );
        
        Set<String> explored = new HashSet<>();
        int nodesExpanded = 0;
        
        // Create and add initial node
        Node initialNode = new Node(
            startR,
            startC,
            new ArrayList<>(),
            0,
            getHeuristicCached(startR, startC, goalState)
        );
        frontier.add(initialNode);
        
        // Main search loop
        while (!frontier.isEmpty()) {
            // Get node with lowest f(n)
            Node currentNode = frontier.poll();
            
            // Skip if already explored
            String stateKey = currentNode.row + "," + currentNode.col;
            if (explored.contains(stateKey)) {
                continue;
            }
            
            // Mark as explored
            explored.add(stateKey);
            nodesExpanded++;
            
            // Goal test - check if destination reached
            if (currentNode.row == goalR && currentNode.col == goalC) {
                return new AStarResult(currentNode.actions, currentNode.cost, nodesExpanded);
            }
            
            // Generate and add successor states using generic method
            String currentState = currentNode.row + "," + currentNode.col;
            List<Successor> successors = getSuccessors(currentState);
            
            for (Successor succ : successors) {
                String newKey = succ.getRow() + "," + succ.getCol();
                if (!explored.contains(newKey)) {
                    // Build action list by appending new action
                    List<String> newActions = new ArrayList<>(currentNode.actions);
                    newActions.add(succ.action);
                    
                    // Update actual cost g(n)
                    int newCost = currentNode.cost + succ.stepCost;
                    
                    // Get heuristic value h(n) for new position
                    int newHeuristic = getHeuristicCached(succ.getRow(), succ.getCol(), goalState);
                    
                    // Create successor node
                    Node successor = new Node(succ.getRow(), succ.getCol(), newActions, newCost, newHeuristic);
                    frontier.add(successor);
                }
            }
        }
        
        // No path found from this starting position
        return new AStarResult(null, Integer.MAX_VALUE, nodesExpanded);
    }

    private int getHeuristicCached(int r, int c, String goalState) {
        if (heuristicValues[r][c] >= 0) return heuristicValues[r][c];
        int h = this.heuristicFunction.findForPosition(r + "," + c, goalState, this.numTunnels, this.rows, this.cols, this.tunnels);
        heuristicValues[r][c] = h;
        return h;
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

        public Node(int row, int col, List<String> actions, int cost, int heuristicValue) {
            this.row = row;
            this.col = col;
            this.actions = new ArrayList<>(actions);
            this.cost = cost;
            this.heuristicValue = heuristicValue;
        }
    }
    
    /**
     * Result class encapsulating search outcome from a single start position
     */
    private static class AStarResult {
        List<String> actions;   // Action sequence to goal (canonical names)
        int cost;               // Total actual cost of path
        int nodesExpanded;      // Number of nodes explored

        AStarResult(List<String> actions, int cost, int nodesExpanded) {
            this.actions = actions;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
        }
    }
    
    public void h(String goalState) {
        this.heuristicFunction = new h1();
        this.heuristicValues = this.heuristicFunction.find(goalState, this.numTunnels, this.rows, this.cols, this.tunnels);
    }
}
