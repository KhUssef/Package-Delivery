package ai.proj;

import java.util.*;

public class UniformCost extends GenericSearch {
    
    private static class Node {
        int row, col, cost;
        Node parent;
        String action; // Track the canonical action taken to reach this node
        
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

        int startRowEff = (startRow >= 0 ? startRow : stores[0][0]);
        int startColEff = (startCol >= 0 ? startCol : stores[0][1]);

        UCSResult result = uniformCostSearch(startRowEff, startColEff, goalRow, goalCol);
        if (result.solution == null) {
            return "FAIL;0;" + result.nodesExpanded;
        }

        // Reconstruct path with actions
        List<String> actions = new ArrayList<>();
        Node current = result.solution;
        while (current != null) {
            if (current.action != null) {
                actions.add(0, current.action);
            }
            current = current.parent;
        }
        String plan = String.join(",", actions);
        return startRowEff + "," + startColEff + ";" + plan + ";" + result.solution.cost + ";" + result.nodesExpanded;
    }
    
    private UCSResult uniformCostSearch(int startRow, int startCol, int goalRow, int goalCol) {
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Set<String> visited = new HashSet<>();
        int nodesExpanded = 0;
        
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
            nodesExpanded++;
            
            // Goal check
            if (current.row == goalRow && current.col == goalCol) {
                return new UCSResult(current, nodesExpanded);
            }
            
            // Expand neighbors using generic successor generator
            String currentState = current.row + "," + current.col;
            List<Successor> successors = getSuccessors(currentState);
            
            for (Successor succ : successors) {
                String newKey = succ.getRow() + "," + succ.getCol();
                if (!visited.contains(newKey)) {
                    int newCost = current.cost + succ.stepCost;
                    frontier.add(new Node(succ.getRow(), succ.getCol(), newCost, current, succ.action));
                }
            }
        }
        
        return new UCSResult(null, nodesExpanded); // No path found
    }
    
    private static class UCSResult {
        Node solution;
        int nodesExpanded;
        
        UCSResult(Node solution, int nodesExpanded) {
            this.solution = solution;
            this.nodesExpanded = nodesExpanded;
        }
    }
}
