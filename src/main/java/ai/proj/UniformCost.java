package ai.proj;

import java.util.*;

public class UniformCost extends GenericSearch {
    
    private static class Node {
        int row, col, cost;
        Node parent;
        String action; // Track the action taken to reach this node
        
        Node(int row, int col, int cost, Node parent, String action) {
            this.row = row;
            this.col = col;
            this.cost = cost;
            this.parent = parent;
            this.action = action;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return row == node.row && col == node.col;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
    }
    
    @Override
    public String search(String goalState) {
        // goalState format: "goalRow,goalCol" 
        String[] goalParts = goalState.split("[,;]");
        int goalRow = Integer.parseInt(goalParts[0]);
        int goalCol = Integer.parseInt(goalParts[1]);
        
        // Find the store with minimum path cost to the goal
        Node bestSolution = null;
        int minCost = Integer.MAX_VALUE;
        int bestStoreIndex = -1;
        
        for (int i = 0; i < numStores; i++) {
            Node result = uniformCostSearch(stores[i][0], stores[i][1], goalRow, goalCol);
            if (result != null && result.cost < minCost) {
                minCost = result.cost;
                bestSolution = result;
                bestStoreIndex = i;
            }
        }
        
        if (bestSolution == null) {
            return "FAIL";
        }
        
        // Reconstruct path with actions
        List<String> actions = new ArrayList<>();
        List<String> pathCoords = new ArrayList<>();
        Node current = bestSolution;
        
        while (current != null) {
            pathCoords.add(0, current.row + "," + current.col);
            if (current.action != null) {
                actions.add(0, current.action);
            }
            current = current.parent;
        }
        
        // Format: path coordinates separated by semicolons
        return String.join(";", pathCoords);
    }
    
    private Node uniformCostSearch(int startRow, int startCol, int goalRow, int goalCol) {
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<String> visited = new HashSet<>();
        
        Node startNode = new Node(startRow, startCol, 0, null, null);
        frontier.add(startNode);
        
        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            
            // Check if already visited
            String currentKey = current.row + "," + current.col;
            if (visited.contains(currentKey)) {
                continue;
            }
            
            // Mark as visited AFTER polling (guarantees optimal path in UCS)
            visited.add(currentKey);
            
            // Goal check
            if (current.row == goalRow && current.col == goalCol) {
                return current;
            }
            
            // Expand neighbors
            expandNode(current, frontier, visited);
        }
        
        return null; // No path found
    }
    
    private void expandNode(Node current, PriorityQueue<Node> frontier, Set<String> visited) {
        // Direction vectors: right, down, left, up
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        String[] dirNames = {"right", "down", "left", "up"};
        
        // Check regular moves in all 4 directions
        for (int d = 0; d < 4; d++) {
            int[] dir = directions[d];
            int newRow = current.row + dir[0];
            int newCol = current.col + dir[1];
            
            // Check bounds
            if (newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols) {
                continue;
            }
            
            String newKey = newRow + "," + newCol;
            if (visited.contains(newKey)) {
                continue;
            }
            
            // Determine traffic cost based on direction
            int cost = 0;
            if (dir[0] == 0 && dir[1] == 1) { 
                // Moving right: use traffic[current][0]
                cost = traffic[current.row][current.col][0];
            } else if (dir[0] == 1 && dir[1] == 0) { 
                // Moving down: use traffic[current][1]
                cost = traffic[current.row][current.col][1];
            } else if (dir[0] == 0 && dir[1] == -1) { 
                // Moving left: reverse of right from newCell
                cost = traffic[newRow][newCol][0];
            } else { 
                // Moving up: reverse of down from newCell
                cost = traffic[newRow][newCol][1];
            }
            
            // Skip if blocked (0 means blocked road per project spec)
            if (cost == 0 || cost == Integer.MAX_VALUE) {
                continue;
            }
            
            int newCost = current.cost + cost;
            frontier.add(new Node(newRow, newCol, newCost, current, dirNames[d]));
        }
        
        // Check for tunnel teleportation
        for (int i = 0; i < numTunnels; i++) {
            int entrance1Row = tunnels[i * 2][0];
            int entrance1Col = tunnels[i * 2][1];
            int entrance2Row = tunnels[i * 2 + 1][0];
            int entrance2Col = tunnels[i * 2 + 1][1];
            
            // Calculate Manhattan distance for tunnel cost
            int manhattanDist = Math.abs(entrance1Row - entrance2Row) + 
                               Math.abs(entrance1Col - entrance2Col);
            
            // If at entrance 1, can teleport to entrance 2
            if (current.row == entrance1Row && current.col == entrance1Col) {
                String newKey = entrance2Row + "," + entrance2Col;
                if (!visited.contains(newKey)) {
                    int newCost = current.cost + manhattanDist;
                    frontier.add(new Node(entrance2Row, entrance2Col, newCost, current, "tunnel"));
                }
            }
            
            // If at entrance 2, can teleport to entrance 1
            if (current.row == entrance2Row && current.col == entrance2Col) {
                String newKey = entrance1Row + "," + entrance1Col;
                if (!visited.contains(newKey)) {
                    int newCost = current.cost + manhattanDist;
                    frontier.add(new Node(entrance1Row, entrance1Col, newCost, current, "tunnel"));
                }
            }
        }
    }
}
