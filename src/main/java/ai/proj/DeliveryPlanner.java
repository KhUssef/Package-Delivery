package ai.proj;

import java.util.ArrayList;
import java.util.List;

public class DeliveryPlanner {

    private GridGenerator generator;
    private DeliverySearch searcher;

    private String initialState;
    private String trafficString;
    private int[][] stores;
    private int[][] destinations;
    private int[][] tunnels;
    private int numStores;
    private int numDestinations;
    private int rows;
    private int cols;

    public DeliveryPlanner() {
        this.generator = new GridGenerator();
    }

    // Getters to expose current grid and metadata (no regeneration)
    public String getInitialState() { return this.initialState; }
    public String getTrafficString() { return this.trafficString; }
    public int[][] getStores() { return this.stores; }
    public int[][] getDestinations() { return this.destinations; }
    public int[][] getTunnels() { return this.tunnels; }
    public int getNumStores() { return this.numStores; }
    public int getNumDestinations() { return this.numDestinations; }
    public int getRows() { return this.rows; }
    public int getCols() { return this.cols; }

    // Optionally set the pre-generated grid data from GridGenerator
    public void loadFromGenerator() {
        if (this.generator != null) {
            this.initialState = generator.getInitialState();
            this.trafficString = generator.getTrafficString();
            this.stores = generator.getStores();
            this.destinations = generator.getDestinations();
            this.tunnels = generator.getTunnels();
            this.rows = generator.getRows();
            this.cols = generator.getCols();
            this.numStores = (this.stores != null) ? this.stores.length : 0;
            this.numDestinations = (this.destinations != null) ? this.destinations.length : 0;
        }
    }

    // Generate a grid using default settings
    public void generateGrid() {
        generator.GenGrid();
        loadFromGenerator();
    }

    // Regenerate a new grid (fresh instance)
    public void regenerateGrid() {
        generateGrid();
    }

    // Pick a search algorithm based on input string
    private GenericSearch chooseStrategy(String strategy) {
        return switch (strategy.toUpperCase()) {
            case "BFS" -> new BFS();
            case "DFS" -> new DFS();
            case "UCS" -> new UniformCost();
            case "ASTAR1" -> new AStar(1);
            case "ASTAR2" -> new AStar(2);
            case "GREEDY1" -> new Greedy(1);
            case "GREEDY2" -> new Greedy(2);
            case "ITERATIVEDEEPENING" -> new IterativeDeepening();
            default -> null;
        };
    }

    /**
     * Main planning function: computes paths using the already-generated grid.
     * This method assumes `initialState`, `trafficString`, `stores`, and `destinations`
     * are already set via the GridGenerator before calling plan.
     */
    public String plan(String strategyName) {

        // Choose search algorithm
        GenericSearch strategy = chooseStrategy(strategyName);
        if (strategy == null) {
            return "Invalid strategy: " + strategyName;
        }

        // Use existing grid; populate if available
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            loadFromGenerator();
        }

        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            return "FAIL: Grid not initialized. Set grid via GridGenerator before planning.";
        }

        this.numStores = stores.length;
        this.numDestinations = destinations.length;

        this.searcher = new DeliverySearch(strategy);

        // Store results
        List<String> fullPlan = new ArrayList<>();

        // Assign each destination a store and compute the path
        for (int dstIndex = 0; dstIndex < numDestinations; dstIndex++) {
            int dstR = destinations[dstIndex][0];
            int dstC = destinations[dstIndex][1];

            // Ask strategy to choose best store automatically for this destination
            String goalState = dstR + "," + dstC;
            String bestPath = searcher.path(initialState, trafficString, goalState);

            if (bestPath == null) {
                return "FAIL";  // no store can reach this destination
            }

            fullPlan.add(bestPath);

            // visualize later if needed
        }

        return "SUCCESS\n" + String.join("\n", fullPlan);
    }

    /**
     * Overload: plan using a provided strategy instance (no selection by name).
     */
    public String plan(GenericSearch strategy) {
        if (strategy == null) {
            return "Invalid strategy: null";
        }

        // Ensure we use already-generated grid data
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            loadFromGenerator();
        }

        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            return "FAIL: Grid not initialized. Set grid via GridGenerator before planning.";
        }

        this.numStores = stores.length;
        this.numDestinations = destinations.length;

        this.searcher = new DeliverySearch(strategy);

        List<String> fullPlan = new ArrayList<>();

        for (int dstIndex = 0; dstIndex < numDestinations; dstIndex++) {
            int dstR = destinations[dstIndex][0];
            int dstC = destinations[dstIndex][1];
            String goalState = dstR + "," + dstC;
            String bestPath = searcher.path(initialState, trafficString, goalState);
            if (bestPath == null || "FAIL".equals(bestPath)) {
                return "FAIL";
            }
            fullPlan.add(bestPath);
        }

        return "SUCCESS\n" + String.join("\n", fullPlan);
    }

    /**
     * Plan deliveries using only a single store (by index).
     * Returns SUCCESS with one line per destination containing the chosen path,
     * or FAIL if any destination is unreachable from the given store.
     */
    public String planForStore(int storeIndex, String strategyName) {
        // Choose search algorithm
        GenericSearch strategy = chooseStrategy(strategyName);
        if (strategy == null) {
            return "Invalid strategy: " + strategyName;
        }

        // Ensure grid data is available (no regeneration here)
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            loadFromGenerator();
        }

        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            return "FAIL: Grid not initialized. Set grid via GridGenerator before planning.";
        }

        if (storeIndex < 0 || storeIndex >= this.stores.length) {
            return "FAIL: Invalid store index";
        }

        this.searcher = new DeliverySearch(strategy);

        List<String> planLines = new ArrayList<>();

        // For each destination, compute the path; ensure it uses the requested store
        for (int dstIndex = 0; dstIndex < this.destinations.length; dstIndex++) {
            int dstR = destinations[dstIndex][0];
            int dstC = destinations[dstIndex][1];
            String goalState = dstR + "," + dstC;

            String result = searcher.path(initialState, trafficString, goalState);

            // Some strategies (e.g., IterativeDeepening) return "storeIndex;path"
            if (result == null || result.equals("FAIL")) {
                return "FAIL";
            }

            // If result encodes the store index, verify it matches
            // Expected format: "<storeIndex>;<moves...>" or raw path for other algorithms
            String[] parts = result.split(";", 2);
            boolean hasStorePrefix = parts.length > 1 && parts[0].matches("\\d+");
            if (hasStorePrefix) {
                int chosenStore = Integer.parseInt(parts[0]);
                if (chosenStore != storeIndex) {
                    // Not using the requested store; treat as unreachable under constraint
                    return "FAIL";
                }
                planLines.add(result);
            } else {
                // Strategy does not specify store; accept path as-is
                planLines.add(result);
            }

            // No visualization hooks per requirements
        }

        return "SUCCESS\n" + String.join("\n", planLines);
    }

    /**
     * Overload: plan for a single store using a provided strategy instance.
     */
    public String planForStore(int storeIndex, GenericSearch strategy) {
        if (strategy == null) {
            return "Invalid strategy: null";
        }

        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            loadFromGenerator();
        }

        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            return "FAIL: Grid not initialized. Set grid via GridGenerator before planning.";
        }

        if (storeIndex < 0 || storeIndex >= this.stores.length) {
            return "FAIL: Invalid store index";
        }

        this.searcher = new DeliverySearch(strategy);

        List<String> planLines = new ArrayList<>();

        for (int dstIndex = 0; dstIndex < this.destinations.length; dstIndex++) {
            int dstR = destinations[dstIndex][0];
            int dstC = destinations[dstIndex][1];
            String goalState = dstR + "," + dstC;

            String result = searcher.path(initialState, trafficString, goalState);
            if (result == null || "FAIL".equals(result)) {
                return "FAIL";
            }

            String[] parts = result.split(";", 2);
            boolean hasStorePrefix = parts.length > 1 && parts[0].matches("\\d+");
            if (hasStorePrefix) {
                int chosenStore = Integer.parseInt(parts[0]);
                if (chosenStore != storeIndex) {
                    return "FAIL";
                }
                planLines.add(result);
            } else {
                planLines.add(result);
            }

            // No visualization hooks per requirements
        }

        return "SUCCESS\n" + String.join("\n", planLines);
    }

    /**
     * Plan for one specific goal coordinate "x,y" choosing best store automatically.
     */
    public String planForGoal(String goalXY, String strategyName) {
        GenericSearch strategy = chooseStrategy(strategyName);
        if (strategy == null) {
            return "Invalid strategy: " + strategyName;
        }
        return planForGoal(strategy, goalXY);
    }

    /**
     * Overload: plan for one goal using provided strategy.
     */
    public String planForGoal(GenericSearch strategy, String goalXY) {
        if (strategy == null) {
            return "Invalid strategy: null";
        }
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            loadFromGenerator();
        }
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            return "FAIL: Grid not initialized. Set grid via GridGenerator before planning.";
        }
        this.searcher = new DeliverySearch(strategy);
        String result = this.searcher.path(this.initialState, this.trafficString, goalXY);
        return result == null ? "FAIL" : result;
    }

    // Utility: extract final cost from search result string (implementation depends on your search)
    private int extractPathCost(String result) {
        // If your search returns something like: "path...;cost=12"
        // you can parse it here.
        // For now, assume UCS/A* return cost as last token.
        try {
            String[] parts = result.split(",");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
