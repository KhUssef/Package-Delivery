package ai.proj;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class PlannerController {
    private final DeliveryPlanner planner = new DeliveryPlanner();
    private GenericSearch currentStrategy = new IterativeDeepening();

    // Strategy helpers
    private GenericSearch choose(String name) {
        if (name == null || name.isBlank()) return currentStrategy;
        switch (name.toUpperCase()) {
            case "BFS": return new BFS();
            case "DFS": return new DFS();
            case "UCS": return new UniformCost();
            case "ASTAR1": return new AStar(1);
            case "ASTAR2": return new AStar(2);
            case "ITERATIVEDEEPENING": return new IterativeDeepening();
            case "GREEDY1": return new Greedy(1);
            case "GREEDY2": return new Greedy(2);
            default: return currentStrategy;
        }
    }

    // Generate/regenerate
    @PostMapping("/grid/generate")
    public ResponseEntity<String> generate() {
        planner.generateGrid();
        return ResponseEntity.ok(planner.getInitialState());
    }

    @PostMapping("/grid/regenerate")
    public ResponseEntity<String> regenerate() {
        planner.regenerateGrid();
        return ResponseEntity.ok(planner.getInitialState());
    }

    // Getters
    @GetMapping("/grid/initialState")
    public ResponseEntity<String> getInitialState() { return ResponseEntity.ok(planner.getInitialState()); }

    @GetMapping("/grid/traffic")
    public ResponseEntity<String> getTraffic() { return ResponseEntity.ok(planner.getTrafficString()); }

    @GetMapping("/grid/stores")
    public ResponseEntity<int[][]> getStores() { return ResponseEntity.ok(planner.getStores()); }

    @GetMapping("/grid/destinations")
    public ResponseEntity<int[][]> getDestinations() { return ResponseEntity.ok(planner.getDestinations()); }

    @GetMapping("/grid/tunnels")
    public ResponseEntity<int[][]> getTunnels() { return ResponseEntity.ok(planner.getTunnels()); }

    @GetMapping("/grid/size")
    public ResponseEntity<String> getSize() { return ResponseEntity.ok(planner.getRows() + "," + planner.getCols()); }

    @GetMapping("/grid/counts")
    public ResponseEntity<String> getCounts() { return ResponseEntity.ok(planner.getNumStores() + "," + planner.getNumDestinations()); }

    // Strategy
    @PostMapping("/strategy")
    public ResponseEntity<String> setStrategy(@RequestParam(name = "name", required = false) String name) {
        currentStrategy = choose(name);
        return ResponseEntity.ok("OK");
    }

    // Planning: full deliveries
    @PostMapping("/plan")
    public ResponseEntity<String> plan(@RequestParam(name = "strategy", required = false) String strategy) {
        GenericSearch s = choose(strategy);
        String result = planner.plan(s);
        return ResponseEntity.ok(result);
    }

    // Planning: single goal (x,y)
    @PostMapping("/plan/goal")
    public ResponseEntity<String> planForGoal(@RequestParam("goal") String goal,
                                              @RequestParam(name = "strategy", required = false) String strategy) {
        GenericSearch s = choose(strategy);
        String result = planner.planForGoal(s, goal);
        return ResponseEntity.ok(result);
    }

    // Benchmark: run plan() multiple times and return average CPU/RAM/time
    @PostMapping("/benchmark")
    public ResponseEntity<String> benchmark(@RequestParam(name = "strategy", required = false) String strategy,
                                            @RequestParam(name = "runs", defaultValue = "5") int runs) {
        String strategyName = (strategy == null || strategy.isBlank()) ? "ITERATIVEDEEPENING" : strategy.toUpperCase();
        String result = planner.benchmark(strategyName, runs);
        return ResponseEntity.ok(result);
    }
}
