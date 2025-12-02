package ai.proj;

import java.util.*;

public class BFS extends GenericSearch {
    
    // Node class to represent states in the search tree
    private class Node {
        int row;
        int col;
        String path;
        int cost;
        
        public Node(int row, int col, String path, int cost) {
            this.row = row;
            this.col = col;
            this.path = path;
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
        
        String bestPath = null;
        int bestStore = -1;
        int bestCost = Integer.MAX_VALUE;
        int totalNodesExpanded = 0;
        
        // Try all stores and find the best one
        for (int s = 0; s < stores.length; s++) {
            int startR = stores[s][0];
            int startC = stores[s][1];
            
            // Run BFS from this store
            BFSResult result = bfsFromStore(startR, startC, goalR, goalC);
            totalNodesExpanded += result.nodesExpanded;
            
            if (result.path != null && result.cost < bestCost) {
                bestCost = result.cost;
                bestPath = result.path;
                bestStore = s;
            }
        }
        
        if (bestPath == null) {
            return "FAIL;0;" + totalNodesExpanded;
        }
        
        System.out.println(bestStore + ";" + bestPath);
        return rebuild(bestStore + ";" + bestPath) + bestCost + ";" + totalNodesExpanded;
    }
    
    private BFSResult bfsFromStore(int startR, int startC, int goalR, int goalC) {
        Queue<Node> frontier = new LinkedList<>();
        Set<String> explored = new HashSet<>();
        int nodesExpanded = 0;
        
        // Create initial node
        Node initialNode = new Node(startR, startC, "", 0);
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
                return new BFSResult(currentNode.path, currentNode.cost, nodesExpanded);
            }
            
            // Expand node - generate successors
            List<Move> successors = getSuccessors(currentNode.row, currentNode.col);
            
            for (Move move : successors) {
                String newPath = currentNode.path.isEmpty() ? move.dir : currentNode.path + ";" + move.dir;
                int newCost = currentNode.cost + move.cost;
                
                Node successor = new Node(move.r, move.c, newPath, newCost);
                
                if (!explored.contains(successor.getStateKey())) {
                    frontier.add(successor);
                }
            }
        }
        
        // No solution found from this store
        return new BFSResult(null, Integer.MAX_VALUE, nodesExpanded);
    }
    
    private List<Move> getSuccessors(int r, int c) {
        List<Move> moves = new ArrayList<>();
        
        // UP
        if (r - 1 >= 0 && traffic[r - 1][c][1] > 0) {
            int cost = traffic[r - 1][c][1];
            moves.add(new Move(r - 1, c, "U", cost));
        }
        
        // RIGHT
        if (c + 1 < cols && traffic[r][c][0] > 0) {
            int cost = traffic[r][c][0];
            moves.add(new Move(r, c + 1, "R", cost));
        }
        
        // DOWN
        if (r + 1 < rows && traffic[r][c][1] > 0) {
            int cost = traffic[r][c][1];
            moves.add(new Move(r + 1, c, "D", cost));
        }
        
        // LEFT
        if (c - 1 >= 0 && traffic[r][c - 1][0] > 0) {
            int cost = traffic[r][c - 1][0];
            moves.add(new Move(r, c - 1, "L", cost));
        }
        
        // TUNNELS (always last)
        for (int i = 0; i < numTunnels; i++) {
            int r1 = tunnels[2 * i][0], c1 = tunnels[2 * i][1];
            int r2 = tunnels[2 * i + 1][0], c2 = tunnels[2 * i + 1][1];
            
            if (r == r1 && c == c1) {
                int cost = Math.abs(r1 - r2) + Math.abs(c1 - c2);
                moves.add(new Move(r2, c2, "T", cost));
            } else if (r == r2 && c == c2) {
                int cost = Math.abs(r1 - r2) + Math.abs(c1 - c2);
                moves.add(new Move(r1, c1, "T", cost));
            }
        }
        
        return moves;
    }
    
    private String rebuild(String path) {
        String[] moves = path.split(";");
        StringBuilder result = new StringBuilder();
        result.append(this.stores[Integer.parseInt(moves[0])][0])
              .append(',')
              .append(this.stores[Integer.parseInt(moves[0])][1])
              .append(';');
        
        int[] current = {this.stores[Integer.parseInt(moves[0])][0], 
                        this.stores[Integer.parseInt(moves[0])][1]};
        moves = Arrays.copyOfRange(moves, 1, moves.length);
        
        for (String move : moves) {
            switch(move) {
                case "R":
                    current[1] += 1;
                    break;
                case "L":
                    current[1] -= 1;
                    break;
                case "U":
                    current[0] -= 1;
                    break;
                case "D":
                    current[0] += 1;
                    break;
                case "T":
                    // Find the tunnel exit
                    for (int i = 0; i < numTunnels; i++) {
                        if (tunnels[i * 2][0] == current[0] && tunnels[i * 2][1] == current[1]) {
                            current[0] = tunnels[i * 2 + 1][0];
                            current[1] = tunnels[i * 2 + 1][1];
                            break;
                        } else if (tunnels[i * 2 + 1][0] == current[0] && tunnels[i * 2 + 1][1] == current[1]) {
                            current[0] = tunnels[i * 2][0];
                            current[1] = tunnels[i * 2][1];
                            break;
                        }
                    }
                    break;
            }
            result.append(current[0]).append(',').append(current[1]).append(';');
        }
        
        return result.toString();
    }
    
    private static class Move {
        int r, c;
        String dir;
        int cost;
        
        Move(int r, int c, String dir, int cost) {
            this.r = r;
            this.c = c;
            this.dir = dir;
            this.cost = cost;
        }
    }
    
    private static class BFSResult {
        String path;
        int cost;
        int nodesExpanded;
        
        BFSResult(String path, int cost, int nodesExpanded) {
            this.path = path;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
        }
    }
}