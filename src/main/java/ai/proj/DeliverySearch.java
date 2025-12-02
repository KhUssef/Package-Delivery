package ai.proj;

import java.util.random.RandomGenerator;

public class DeliverySearch {
    private int rows;
    private int cols;
    private int numTunnels;
    private int numStores;
    private int numDestinations;
    private char[][] grid;
    private int[][][] traffic;
    private int[][] tunnels;
    private int[][] stores;
    private int[][] destinations;
    private int[][] obstacles;
    private int numObstacles;
    private GenericSearch searchAlgorithm;
    private String initialState;
    private String trafficString;

    String GenGrid() {

        RandomGenerator rand = RandomGenerator.getDefault();
        this.rows = rand.nextInt(5, 11);
        this.cols = rand.nextInt(5, 11);
        this.numTunnels = rand.nextInt(1, 4);
        this.numStores = rand.nextInt(1, 4);
        System.out.println("Number of stores: " + numStores);
        this.numDestinations = rand.nextInt(1, 11);
        this.grid = new char[rows][cols];
        this.traffic = new int[rows][cols][2];
        this.tunnels = new int[numTunnels * 2][2];
        this.stores = new int[numStores][2];
        this.destinations = new int[numDestinations][2];
        this.numObstacles = rand.nextInt(1, (rows * cols) / 4);
        this.obstacles = new int[numObstacles][2];

        // generate tunnels
        for (int i = 0; i < numTunnels; i++) {
            int r1 = rand.nextInt(0, rows);
            int c1 = rand.nextInt(0, cols);
            while (grid[r1][c1] != '\u0000') {
                r1 = rand.nextInt(0, rows);
                c1 = rand.nextInt(0, cols);
            }
            int r2 = rand.nextInt(0, rows);
            int c2 = rand.nextInt(0, cols);
            while (grid[r2][c2] != '\u0000' || (r1 == r2 && c1 == c2)) {
                r2 = rand.nextInt(0, rows);
                c2 = rand.nextInt(0, cols);
            }
            grid[r1][c1] = 'T';
            grid[r2][c2] = 'T';
            tunnels[i * 2][0] = r1;
            tunnels[i * 2][1] = c1;
            tunnels[i * 2 + 1][0] = r2;
            tunnels[i * 2 + 1][1] = c2;
        }

        // generate stores
        for (int j = 0; j < numStores; j++) {
            int r = rand.nextInt(0, rows);
            int c = rand.nextInt(0, cols);
            while (grid[r][c] != '\u0000') {
                r = rand.nextInt(0, rows);
                c = rand.nextInt(0, cols);
            }
            System.out.println("Store at: " + r + "," + c);
            grid[r][c] = 'S';
            stores[j][0] = r;
            stores[j][1] = c;
        }

        // generate destinations
        for (int k = 0; k < numDestinations; k++) {
            int r = rand.nextInt(0, rows);
            int c = rand.nextInt(0, cols);
            while (grid[r][c] != '\u0000') {
                r = rand.nextInt(0, rows);
                c = rand.nextInt(0, cols);
            }

            grid[r][c] = 'D';
            destinations[k][0] = r;
            destinations[k][1] = c;
        }

        // improve readbility for printed grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '\u0000') {
                    grid[r][c] = '.';
                }
            }
        }
        System.out.println(numDestinations + " " + numStores + " " + numTunnels);

        // generate obstacles
        for (int i = 0; i < this.numObstacles; i++) {
            int r = rand.nextInt(0, rows);
            int c = rand.nextInt(0, cols);
            if (r == rows - 1) {
                traffic[r][c][0] = Integer.MAX_VALUE;
                obstacles[i][0] = r;
                obstacles[i][1] = c;
                continue;
            }
            if (c == cols - 1) {
                traffic[r][c][1] = Integer.MAX_VALUE;
                obstacles[i][0] = r;
                obstacles[i][1] = c;
                continue;
            }
            traffic[r][c][rand.nextInt(0, 2)] = Integer.MAX_VALUE;
            obstacles[i][0] = r;
            obstacles[i][1] = c;
        }

        // generate traffic
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int d = 0; d < 2; d++) {
                    if (traffic[r][c][d] != Integer.MAX_VALUE && traffic[r][c][d] == 0) {
                        traffic[r][c][d] = rand.nextInt(1, 4);
                    }
                }
            }
        }

        // printing generated grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                System.out.print(grid[r][c] + "-");
                if (traffic[r][c][0] == Integer.MAX_VALUE) {
                    System.out.print("X");
                } else {
                    System.out.print(traffic[r][c][0]);
                }
                System.out.print(">");
            }
            System.out.println();
            if (r == rows - 1)
                break;
            for (int c = 0; c < cols; c++) {
                if (traffic[r][c][1] == Integer.MAX_VALUE) {
                    System.out.print("X   ");
                } else {
                    System.out.print(traffic[r][c][1] + "   ");
                }
            }
            System.out.println();
        }

        // building inital state
        StringBuilder sb = new StringBuilder();
        sb.append(rows).append(';').append(cols).append(';').append(numDestinations).append(';').append(numStores)
                .append(';');

        for (int i = 0; i < numDestinations; i++) {
            sb.append(destinations[i][0]).append(',').append(destinations[i][1]).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(';');
        for (int i = 0; i < numStores; i++) {
            sb.append(stores[i][0]).append(',').append(stores[i][1]).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(';');

        // adding tunnels to initial state
        for (int i = 0; i < numTunnels; i++) {
            int r1 = tunnels[i * 2][0];
            int c1 = tunnels[i * 2][1];
            int r2 = tunnels[i * 2 + 1][0];
            int c2 = tunnels[i * 2 + 1][1];
            sb.append(r1).append(',').append(c1).append(',').append(r2).append(',').append(c2).append(';');
        }
        this.initialState = sb.toString();

        sb = new StringBuilder();

        // building traffic string

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c + 1 < cols) {
                    int t = traffic[r][c][0] != Integer.MAX_VALUE ? traffic[r][c][0] : 0;
                    sb.append(r).append(',').append(c).append(',').append(r).append(',').append(c + 1).append(',')
                            .append(t).append(';');
                }
                if (r + 1 < rows) {
                    int t = traffic[r][c][1] != Integer.MAX_VALUE ? traffic[r][c][1] : 0;
                    sb.append(r).append(',').append(c).append(',').append(r + 1).append(',').append(c).append(',')
                            .append(t).append(';');
                }
            }
        }
        this.trafficString = sb.toString();
        return this.initialState + '\n' + this.trafficString;
    }

    void test() {
        this.searchAlgorithm = new AStar();
        this.searchAlgorithm = new DFS();
        this.GenGrid();
        System.out.println(this.initialState);
        System.out.println(this.trafficString);
        this.searchAlgorithm.extract(this.initialState, this.trafficString);
<<<<<<< HEAD
        this.searchAlgorithm = new IterativeDeepening();
        this.searchAlgorithm.extract(initialState, trafficString);
        System.out.println(this.searchAlgorithm.search(new String(this.destinations[0][0] + "," + this.destinations[0][1])));
=======
        System.out.println("DFS: " + this.searchAlgorithm.search(this.initialState + ";" + 0));
>>>>>>> 694ff923ef734aebf9d98e0e27e8cb53dff64791
    }

    String path(String initialState, String traffic, String goalState) {
        return searchAlgorithm.search( goalState);
    }

    String plan(String initialState, String traffic, String strategy, boolean visualize) {
        // if (strategy.equals("BFS")) {
        //     this.searchAlgorithm = new BFS();
        // } else if (strategy.equals("DFS")) {
        //     this.searchAlgorithm = new DFS();
        // } else if (strategy.equals("ASTAR")) {
        //     this.searchAlgorithm = new AStar();
        // }
        // if (strategy.equals("UCS")) {
        //     this.searchAlgorithm = new UCS();
        // }
        // if (strategy.equals("GREEDY")) {
        //     this.searchAlgorithm = new Greedy();
        // }
        // if (strategy.equals("IterativeDeepening")) {
        //     this.searchAlgorithm = new IterativeDeepening();
        // } else {
        //     return "Invalid strategy";
        // }
        for (int i = 0; i < this.numDestinations; i++) {
            String goalState = initialState + ";" + i;
            String path = path(initialState, traffic, goalState);
            if (path.equals("FAIL")) {
                return "FAIL";
            }
            if (visualize) {
                // Visualizer.visualizePath(this.grid, path, this.startRow, this.startCol);
            }
        }
        return "SUCCESS";
    }
}