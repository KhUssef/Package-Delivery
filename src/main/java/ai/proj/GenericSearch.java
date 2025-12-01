package ai.proj;

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

    public abstract String search(String initialState, String trafficString, String goalState);
    
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
        
        // Initialize stores array (assuming stores come after tunnels in the format, or extract from grid)
        // For now, initialize empty as the format doesn't explicitly provide store locations
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
