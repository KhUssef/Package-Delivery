package ai.proj;

import java.util.*;

public class UniformCost extends GenericSearch {
    
    // Inner class to represent a state in the search
    private static class State {
        int row;
        int col;
        
        State(int row, int col) {
            this.row = row;
            this.col = col;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State state = (State) o;
            return row == state.row && col == state.col;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
        
        @Override
        public String toString() {
            return "(" + row + "," + col + ")";
        }
    }
    
    // Priority queue entry that holds state and its accumulated cost
    private static class PQEntry implements Comparable<PQEntry> {
        State state;
        int cost;  // Accumulated cost to reach this state
        
        PQEntry(State state, int cost) {
            this.state = state;
            this.cost = cost;
        }
        
        @Override
        public int compareTo(PQEntry other) {
            return Integer.compare(this.cost, other.cost);
        }
    }
    
    // Result class to hold both cost and path
    private static class SearchResult {
        int cost;
        List<State> path;
        
        SearchResult(int cost, List<State> path) {
            this.cost = cost;
            this.path = path;
        }
    }
    
    @Override
    public String search(String goalState) {
        // Parse the goal state to extract grid dimensions, stores, destinations, and traffic
        String[] parts = goalState.split(";");
        
        int rows = Integer.parseInt(parts[0]);
        int cols = Integer.parseInt(parts[1]);
        int numDestinations = Integer.parseInt(parts[2]);
        int numStores = Integer.parseInt(parts[3]);
        
        // Parse destination coordinates
        String[] destCoords = parts[4].split(",");
        List<State> destinations = new ArrayList<>();
        for (int i = 0; i < destCoords.length - 1; i += 2) {
            int r = Integer.parseInt(destCoords[i]);
            int c = Integer.parseInt(destCoords[i + 1]);
            destinations.add(new State(r, c));
        }
        
        // Parse store coordinates
        String[] storeCoords = parts[5].split(",");
        List<State> stores = new ArrayList<>();
        for (int i = 0; i < storeCoords.length - 1; i += 2) {
            int r = Integer.parseInt(storeCoords[i]);
            int c = Integer.parseInt(storeCoords[i + 1]);
            stores.add(new State(r, c));
        }
        
        // Parse traffic data - store edge costs (bidirectional)
        Map<String, Integer> trafficMap = new HashMap<>();
        for (int i = 7; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            String[] traffic = part.split(",");
            if (traffic.length >= 5) {
                int r1 = Integer.parseInt(traffic[0].trim());
                int c1 = Integer.parseInt(traffic[1].trim());
                int r2 = Integer.parseInt(traffic[2].trim());
                int c2 = Integer.parseInt(traffic[3].trim());
                int cost = Integer.parseInt(traffic[4].trim());
                
                // Add edge in both directions (bidirectional graph)
                String key1 = r1 + "," + c1 + "->" + r2 + "," + c2;
                String key2 = r2 + "," + c2 + "->" + r1 + "," + c1;
                trafficMap.put(key1, cost);
                trafficMap.put(key2, cost);
            }
        }
        
        // Use the first destination as the goal
        State goal = destinations.get(0);
        
        System.out.println("Destinations: " + destinations);
        System.out.println("Stores: " + stores);
        System.out.println("Total edges in traffic map: " + trafficMap.size());
        
        // Find the store with minimum cost to reach the goal
        State bestStore = null;
        int minCost = Integer.MAX_VALUE;
        List<State> bestPath = null;
        
        for (State store : stores) {
            SearchResult result = uniformCostSearch(store, goal, rows, cols, trafficMap);
            System.out.println("Store at " + store + " -> Goal at " + goal + " : Cost = " + 
                             (result.cost == Integer.MAX_VALUE ? "UNREACHABLE" : result.cost));
            
            if (result.cost < minCost) {
                minCost = result.cost;
                bestStore = store;
                bestPath = result.path;
            }
        }
        
        if (bestStore != null && bestPath != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Best store: (").append(bestStore.row).append(",").append(bestStore.col)
              .append(") with cost: ").append(minCost).append("\n");
            sb.append("Path: ");
            for (int i = 0; i < bestPath.size(); i++) {
                sb.append(bestPath.get(i));
                if (i < bestPath.size() - 1) {
                    sb.append(" -> ");
                }
            }
            return sb.toString();
        }
        
        return "No reachable store found";
    }
    
    // UCS algorithm to find minimum cost from start to goal and track the path
    private SearchResult uniformCostSearch(State start, State goal, int rows, int cols, 
                                          Map<String, Integer> trafficMap) {
        
        PriorityQueue<PQEntry> frontier = new PriorityQueue<>();
        Map<State, Integer> costSoFar = new HashMap<>();
        Map<State, State> cameFrom = new HashMap<>();  // To reconstruct the path
        
        frontier.add(new PQEntry(start, 0));
        costSoFar.put(start, 0);
        cameFrom.put(start, null);  // Start has no parent
        
        while (!frontier.isEmpty()) {
            PQEntry current = frontier.poll();
            
            // Goal test
            if (current.state.equals(goal)) {
                // Reconstruct path
                List<State> path = reconstructPath(cameFrom, start, goal);
                return new SearchResult(current.cost, path);
            }
            
            // Skip if we've found a better path to this state already
            if (current.cost > costSoFar.getOrDefault(current.state, Integer.MAX_VALUE)) {
                continue;
            }
            
            // Expand neighbors (up, down, left, right)
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            
            for (int[] dir : directions) {
                int newRow = current.state.row + dir[0];
                int newCol = current.state.col + dir[1];
                
                // Check bounds
                if (newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols) {
                    continue;
                }
                
                // Get edge cost from traffic map
                String edgeKey = current.state.row + "," + current.state.col + "->" + 
                               newRow + "," + newCol;
                
                if (!trafficMap.containsKey(edgeKey)) {
                    continue; // No edge exists (obstacle)
                }
                
                int edgeCost = trafficMap.get(edgeKey);
                
                // Skip if edge is blocked (infinite cost)
                if (edgeCost == Integer.MAX_VALUE) {
                    continue;
                }
                
                State nextState = new State(newRow, newCol);
                int newCost = current.cost + edgeCost;
                
                // Only add to frontier if this path is better
                if (newCost < costSoFar.getOrDefault(nextState, Integer.MAX_VALUE)) {
                    costSoFar.put(nextState, newCost);
                    cameFrom.put(nextState, current.state);  // Track parent
                    frontier.add(new PQEntry(nextState, newCost));
                }
            }
        }
        
        return new SearchResult(Integer.MAX_VALUE, new ArrayList<>()); // Goal not reachable
    }
    
    // Reconstruct the path from start to goal using the cameFrom map
    private List<State> reconstructPath(Map<State, State> cameFrom, State start, State goal) {
        List<State> path = new ArrayList<>();
        State current = goal;
        
        while (current != null) {
            path.add(0, current);  // Add to front of list
            current = cameFrom.get(current);
        }
        
        return path;
    }
}
