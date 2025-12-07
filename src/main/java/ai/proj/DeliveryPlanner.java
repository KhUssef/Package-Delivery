package ai.proj;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * DeliveryPlanner orchestrates generating a grid (via GridGenerator),
 * choosing a search strategy, and planning deliveries (assigning stores to goals).
 *
 * This variant includes ONE consistent performance measurement wrapper for
 * plan(String strategyName) using:
 *  - JVM warm-up
 *  - forced GC before measurement
 *  - wall-clock time (nanoTime)
 *  - per-thread CPU time (ThreadMXBean)
 *  - RAM delta using Runtime totalMemory/freeMemory
 *
 * Note: other planning methods (plan(GenericSearch), planForStore, planForGoal)
 * are left uninstrumented to keep measurements focused on plan() as requested.
 */
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
     * Entry point: plan using strategy name, then delegate to instrumented overload.
     */
    public String plan(String strategyName) {
        GenericSearch strategy = chooseStrategy(strategyName);
        if (strategy == null) {
            return "Invalid strategy: " + strategyName;
        }
        return plan(strategy);   // delegating to the instrumented version
    }

    /**
     * Main planning function: computes paths using the already-generated grid.
     * No instrumentation here; use benchmark() for CPU/RAM measurement.
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

            String bestPath = null;
            int bestCost = Integer.MAX_VALUE;

            // Evaluate each store by running the strategy from that store
            for (int s = 0; s < numStores; s++) {
                String storeState = stores[s][0] + "," + stores[s][1];
                String result = searcher.path(initialState, trafficString, storeState, goalState);
                if (result == null || result.startsWith("FAIL")) continue;
                int cost = extractResultCost(result);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestPath = result;
                }
            }

            if (bestPath == null) {
                return "FAIL";  // no store can reach this destination
            }

            fullPlan.add(bestPath);
        }

        return "SUCCESS\n" + String.join("\n", fullPlan);
    }

    /**
     * Benchmark a strategy by running plan() multiple times and computing average CPU/RAM/time.
     * @param strategyName Name of the strategy (BFS, DFS, UCS, ASTAR1, etc.)
     * @param runs Number of runs to average over
     * @return Benchmark result string with averages
     */
    public String benchmark(String strategyName, int runs) {
        if (runs < 1) runs = 1;

        // Ensure grid is loaded
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            loadFromGenerator();
        }
        if (this.initialState == null || this.trafficString == null || this.stores == null || this.destinations == null) {
            return "FAIL: Grid not initialized.";
        }

        // Warmup: run a few times without measurement
        int warmupRuns = 3;
        for (int i = 0; i < warmupRuns; i++) {
            GenericSearch warmupStrategy = chooseStrategy(strategyName);
            if (warmupStrategy == null) return "Invalid strategy: " + strategyName;
            plan(warmupStrategy);
        }

        // Force GC before measurement
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        long totalWallNs = 0;
        long totalCpuNs = 0;
        long totalRamDelta = 0;
        String lastResult = null;

        for (int i = 0; i < runs; i++) {
            // Create fresh strategy instance each run
            GenericSearch strategy = chooseStrategy(strategyName);
            if (strategy == null) return "Invalid strategy: " + strategyName;

            System.gc();
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            long ramBefore = usedRam();
            long wallBefore = System.nanoTime();
            long cpuBefore = threadCpu();

            lastResult = plan(strategy);

            long cpuAfter = threadCpu();
            long wallAfter = System.nanoTime();
            long ramAfter = usedRam();

            totalWallNs += (wallAfter - wallBefore);
            totalCpuNs += (cpuAfter - cpuBefore);
            totalRamDelta += (ramAfter - ramBefore);
        }

        double avgWallMs = (totalWallNs / (double) runs) / 1_000_000.0;
        double avgCpuMs = (totalCpuNs / (double) runs) / 1_000_000.0;
        double avgRamKB = (totalRamDelta / (double) runs) / 1024.0;

        String summary = String.format(
            "Benchmark [%s] over %d runs:\n  avgWallMs=%.3f\n  avgCpuMs=%.3f\n  avgRamDeltaKB=%.1f",
            strategyName, runs, avgWallMs, avgCpuMs, avgRamKB
        );

        System.out.println(summary);

        return summary + "\n\nLast result:\n" + lastResult;
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
            String storeState = stores[storeIndex][0] + "," + stores[storeIndex][1];
            String result = searcher.path(initialState, trafficString, storeState, goalState);

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
            String storeState = stores[storeIndex][0] + "," + stores[storeIndex][1];
            String result = searcher.path(initialState, trafficString, storeState, goalState);
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
        }

        return "SUCCESS\n" + String.join("\n", planLines);
    }

    /**
     * Plan for one specific goal coordinate "x,y" choosing best store automatically.
     * This method is uninstrumented (measurement only done by plan()).
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

        // Choose best store automatically for this goal
        String bestResult = null;
        int bestCost = Integer.MAX_VALUE;
        for (int s = 0; s < this.stores.length; s++) {
            String storeState = stores[s][0] + "," + stores[s][1];
            String result = this.searcher.path(this.initialState, this.trafficString, storeState, goalXY);
            if (result == null || result.startsWith("FAIL")) continue;
            int cost = extractResultCost(result);
            if (cost < bestCost) { bestCost = cost; bestResult = result; }
        }

        return bestResult == null ? "FAIL" : bestResult;
    }

    // Utility: extract final cost from search result string (implementation depends on your search)
    private int extractResultCost(String result) {
        // Expected format: x,y;plan;cost;nodesExpanded
        try {
            String[] parts = result.split(";");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
            return Integer.MAX_VALUE;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    // -------------------------
    // Helper methods for measurement
    // -------------------------
    private long usedRam() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private long threadCpu() {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        if (mx.isCurrentThreadCpuTimeSupported()) {
            return mx.getCurrentThreadCpuTime();
        }
        return 0L;
    }
}
