package ai.proj;

import java.util.*;

public abstract class GenericSearch {
    protected int rows;
    protected int cols;
    protected int numDestinations;
    protected int numStores;
    protected int[][][] traffic;
    protected int[][] destinations;
    protected int[][] stores;
    protected int[][] tunnels;
    protected int numTunnels;
    // Selected start store coordinates (set by planner/search wrapper)
    protected int startRow = -1;
    protected int startCol = -1;
    
    // Global action order (tiebreaker order) - must be used by all search algorithms
    protected String[] tieBreakerOrder = {"up", "down", "left", "right", "tunnel"};

    public abstract String search(String goalState);

    // Set the starting store position for algorithms that should not loop over stores
    public void setStart(String storeState) {
        String[] parts = storeState.split(",");
        this.startRow = Integer.parseInt(parts[0]);
        this.startCol = Integer.parseInt(parts[1]);
    }
    
    /**
     * Generic successor generator that all search algorithms must use.
     * Expands actions in the order specified by tieBreakerOrder.
     * Returns (newState, actionName, stepCost) tuples.
     * 
     * @param state Current state as "row,col"
     * @return List of Successor objects containing new state, action name, and step cost
     */
    protected List<Successor> getSuccessors(String state) {
        List<Successor> successors = new ArrayList<>();
        String[] coords = state.split(",");
        int r = Integer.parseInt(coords[0]);
        int c = Integer.parseInt(coords[1]);
        
        // Generate successors in tieBreakerOrder
        for (String action : tieBreakerOrder) {
            switch (action) {
                case "up":
                    // Move up: check if valid and not blocked
                    if (r - 1 >= 0 && traffic[r - 1][c][1] > 0) {
                        int cost = traffic[r - 1][c][1];
                        successors.add(new Successor((r - 1) + "," + c, "up", cost));
                    }
                    break;
                    
                case "down":
                    // Move down: check if valid and not blocked
                    if (r + 1 < rows && traffic[r][c][1] > 0) {
                        int cost = traffic[r][c][1];
                        successors.add(new Successor((r + 1) + "," + c, "down", cost));
                    }
                    break;
                    
                case "left":
                    // Move left: check if valid and not blocked
                    if (c - 1 >= 0 && traffic[r][c - 1][0] > 0) {
                        int cost = traffic[r][c - 1][0];
                        successors.add(new Successor(r + "," + (c - 1), "left", cost));
                    }
                    break;
                    
                case "right":
                    // Move right: check if valid and not blocked
                    if (c + 1 < cols && traffic[r][c][0] > 0) {
                        int cost = traffic[r][c][0];
                        successors.add(new Successor(r + "," + (c + 1), "right", cost));
                    }
                    break;
                    
                case "tunnel":
                    // Check all tunnels for teleportation
                    for (int i = 0; i < numTunnels; i++) {
                        int r1 = tunnels[i * 2][0];
                        int c1 = tunnels[i * 2][1];
                        int r2 = tunnels[i * 2 + 1][0];
                        int c2 = tunnels[i * 2 + 1][1];
                        
                        // If at tunnel entrance 1, can teleport to entrance 2
                        if (r == r1 && c == c1) {
                            int manhattanCost = Math.abs(r1 - r2) + Math.abs(c1 - c2);
                            successors.add(new Successor(r2 + "," + c2, "tunnel", manhattanCost));
                        }
                        // If at tunnel entrance 2, can teleport to entrance 1
                        else if (r == r2 && c == c2) {
                            int manhattanCost = Math.abs(r1 - r2) + Math.abs(c1 - c2);
                            successors.add(new Successor(r1 + "," + c1, "tunnel", manhattanCost));
                        }
                    }
                    break;
            }
        }
        
        return successors;
    }
    
    /**
     * Helper class to represent a successor state with action and cost
     */
    protected static class Successor {
        String newState;  // "row,col" format
        String action;    // Canonical action name: "up", "down", "left", "right", "tunnel"
        int stepCost;     // Cost of taking this action
        
        Successor(String newState, String action, int stepCost) {
            this.newState = newState;
            this.action = action;
            this.stepCost = stepCost;
        }
        
        int getRow() {
            return Integer.parseInt(newState.split(",")[0]);
        }
        
        int getCol() {
            return Integer.parseInt(newState.split(",")[1]);
        }
    }
    
    public void extract(String initialState, String trafficString) {
        // Parse initialState: m;n;P;S;CustomerX_1,CustomerY_1,...;TunnelX_1,TunnelY_1,TunnelX_1,TunnelY_1,...;
        String[] parts = initialState.split(";");
        
        // Extract dimensions and counts
        this.rows = Integer.parseInt(parts[0]);
        this.cols = Integer.parseInt(parts[1]);
        this.numDestinations = Integer.parseInt(parts[2]);
        this.numStores = Integer.parseInt(parts[3]);
        
        // Extract customer/destination locations
        this.destinations = new int[numDestinations][2];
        if (parts.length > 4 && !parts[4].isEmpty()) {
            String[] destCoords = parts[4].split(",");
            for (int i = 0; i < numDestinations; i++) {
                this.destinations[i][0] = Integer.parseInt(destCoords[i * 2]);
                this.destinations[i][1] = Integer.parseInt(destCoords[i * 2 + 1]);
            }
        }
        
        this.stores = new int[numStores][2];
        // Extract store locations (assuming they come after destinations, before tunnels)
        if (parts.length > 5 && !parts[5].isEmpty()) {
            String[] destCoords = parts[5].split(",");
            for (int i = 0; i < numStores; i++) {
                this.stores[i][0] = Integer.parseInt(destCoords[i * 2]);
                this.stores[i][1] = Integer.parseInt(destCoords[i * 2 + 1]);
            }
        }
        


        // Extract tunnel locations (pairs of entrances) - everything after parts[5] are tunnels
        if (parts.length > 6) {
            // Collect all tunnel parts (from index 6 onwards)
            StringBuilder tunnelBuilder = new StringBuilder();
            for (int i = 6; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    if (tunnelBuilder.length() > 0) {
                        tunnelBuilder.append(',');
                    }
                    tunnelBuilder.append(parts[i]);
                }
            }
            
            if (tunnelBuilder.length() > 0) {
                String[] tunnelCoords = tunnelBuilder.toString().split(",");
                this.numTunnels = tunnelCoords.length / 4;
                this.tunnels = new int[numTunnels * 2][2];
                for (int i = 0; i < numTunnels; i++) {
                    this.tunnels[i * 2][0] = Integer.parseInt(tunnelCoords[i * 4]);
                    this.tunnels[i * 2][1] = Integer.parseInt(tunnelCoords[i * 4 + 1]);
                    this.tunnels[i * 2 + 1][0] = Integer.parseInt(tunnelCoords[i * 4 + 2]);
                    this.tunnels[i * 2 + 1][1] = Integer.parseInt(tunnelCoords[i * 4 + 3]);
                }
            } else {
                this.numTunnels = 0;
                this.tunnels = new int[0][2];
            }
        } else {
            this.numTunnels = 0;
            this.tunnels = new int[0][2];
        }
        // Initialize traffic 3D array
        this.traffic = new int[rows][cols][2];
        
        // Parse trafficString: SrcX_1,SrcY_1,DstX_1,DstY_1,Traffic_1;...
        if (trafficString != null && !trafficString.isEmpty()) {
            String[] trafficEntries = trafficString.split(";");
            for (String entry : trafficEntries) {
                if (!entry.isEmpty()) {
                    String[] coords = entry.split(",");
                    int srcX = Integer.parseInt(coords[0]);
                    int srcY = Integer.parseInt(coords[1]);
                    int dstX = Integer.parseInt(coords[2]);
                    int dstY = Integer.parseInt(coords[3]);
                    int trafficLevel = Integer.parseInt(coords[4]);
                    
                    // Determine direction: 0 for horizontal (right), 1 for vertical (down)
                    if (dstX == srcX && dstY == srcY + 1) {
                        // Moving right
                        traffic[srcX][srcY][0] = trafficLevel;
                    } else if (dstX == srcX + 1 && dstY == srcY) {
                        // Moving down
                        traffic[srcX][srcY][1] = trafficLevel;
                    }
                }
            }
        }
    }
    public void extract (String initialState, String trafficString, boolean print){
        extract(initialState, trafficString);
        if (print){
            System.out.println("Extraction complete: " + rows + "x" + cols + ", Destinations: " + numDestinations + ", Stores: " + numStores + ", Tunnels: " + numTunnels);
            for(int i=0; i<numDestinations; i++) {
                System.out.println("Destination " + i + ": (" + destinations[i][0] + "," + destinations[i][1] + ")");
            }
            for(int i=0; i<numTunnels; i++) {
                System.out.println("Tunnel " + i + ": (" + tunnels[i*2][0] + "," + tunnels[i*2][1] + ") <-> (" + tunnels[i*2+1][0] + "," + tunnels[i*2+1][1] + ")");
            }
            for(int r=0; r<rows; r++) {
                for(int c=0; c<cols; c++) {
                    System.out.println("Traffic at (" + r + "," + c + "): Right=" + traffic[r][c][0] + ", Down=" + traffic[r][c][1]);
                }
            }
            for(int i=0; i<numStores; i++) {
                System.out.println("Store " + i + ": (" + stores[i][0] + "," + stores[i][1] + ")");
            }
        }
    }
}
