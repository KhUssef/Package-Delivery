package ai.proj;

import java.util.*;

public class BFS extends GenericSearch {
    
    // Node class to represent states in the search tree
    private class Node {
        int row;
        int col;
        List<String> actions;  // List of canonical action names
        int cost;
        
        public Node(int row, int col, List<String> actions, int cost) {
            this.row = row;
            this.col = col;
            this.actions = new ArrayList<>(actions);
            this.cost = cost;
        }
        
        public String getStateKey() {
            return row + "," + col;
        }
    }
    
    @Override
    public String search(String goalState) {
        // Parse goal state - it contains the coordinates of the destination
        String[] coords = goalState.split(",");
        int goalR = Integer.parseInt(coords[0]);
        int goalC = Integer.parseInt(coords[1]);
        
        // Single-store search: start coordinates set via GenericSearch.setStart
        int startR = (startRow >= 0 ? startRow : stores[0][0]);
        int startC = (startCol >= 0 ? startCol : stores[0][1]);

        BFSResult result = bfsFromStore(startR, startC, goalR, goalC);
        if (result.actions == null) {
            return "FAIL;0;" + result.nodesExpanded;
        }
        return startR + "," + startC + ";" + String.join(",", result.actions) + ";" + result.cost + ";" + result.nodesExpanded;
    }
    
    private BFSResult bfsFromStore(int startR, int startC, int goalR, int goalC) {
        Queue<Node> frontier = new LinkedList<>();
        Set<String> explored = new HashSet<>();
        int nodesExpanded = 0;
        
        // Create initial node
        Node initialNode = new Node(startR, startC, new ArrayList<>(), 0);
        frontier.add(initialNode);
        
        while (!frontier.isEmpty()) {
            Node currentNode = frontier.poll();
            
            // Check if we've already explored this state
            String stateKey = currentNode.getStateKey();
            if (explored.contains(stateKey)) {
                continue;
            }
            
            explored.add(stateKey);
            nodesExpanded++;
            
            // Goal test: reached target destination
            if (currentNode.row == goalR && currentNode.col == goalC) {
                return new BFSResult(currentNode.actions, currentNode.cost, nodesExpanded);
            }
            
            // Expand node - generate successors using generic method
            String currentState = currentNode.row + "," + currentNode.col;
            List<Successor> successors = getSuccessors(currentState);
            
            for (Successor succ : successors) {
                String newKey = succ.getRow() + "," + succ.getCol();
                if (!explored.contains(newKey)) {
                    List<String> newActions = new ArrayList<>(currentNode.actions);
                    newActions.add(succ.action);
                    int newCost = currentNode.cost + succ.stepCost;
                    Node successor = new Node(succ.getRow(), succ.getCol(), newActions, newCost);
                    frontier.add(successor);
                }
            }
        }
        
        // No solution found from this store
        return new BFSResult(null, Integer.MAX_VALUE, nodesExpanded);
    }
    
    private static class BFSResult {
        List<String> actions;
        int cost;
        int nodesExpanded;
        
        BFSResult(List<String> actions, int cost, int nodesExpanded) {
            this.actions = actions;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
        }
    }
}