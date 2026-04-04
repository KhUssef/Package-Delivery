package ai.proj;

import java.util.random.RandomGenerator;

public class GridGenerator {

    private int rows;
    private int cols;
    private int numTunnels;
    private int numStores;
    private int numDestinations;
    private int numObstacles;

    private char[][] grid;
    private int[][][] traffic;
    private int[][] tunnels;
    private int[][] stores;
    private int[][] destinations;
    private int[][] obstacles;

    private String initialState;
    private String trafficString;

    public void GenGrid() {

        RandomGenerator rand = RandomGenerator.getDefault();

        this.rows = rand.nextInt(5, 11);
        this.cols = rand.nextInt(5, 11);
        this.numTunnels = rand.nextInt(1, 4);
        this.numStores = rand.nextInt(1, 4);
        this.numDestinations = rand.nextInt(1, 11);

        this.grid = new char[rows][cols];
        this.traffic = new int[rows][cols][2];
        this.tunnels = new int[numTunnels * 2][2];
        this.stores = new int[numStores][2];
        this.destinations = new int[numDestinations][2];

        this.numObstacles = rand.nextInt(1, (rows * cols) / 4);
        this.obstacles = new int[numObstacles][2];

        generateTunnels(rand);
        generateStores(rand);
        generateDestinations(rand);
        normalizeGrid();
        generateObstacles(rand);
        generateTraffic(rand);
        printGrid();

        buildInitialState();
        buildTrafficString();
    }

    // -------------------------
    //   GRID GENERATION STEPS
    // -------------------------

    private void generateTunnels(RandomGenerator rand) {
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
    }

    private void generateStores(RandomGenerator rand) {
        for (int j = 0; j < numStores; j++) {
            int r = rand.nextInt(0, rows);
            int c = rand.nextInt(0, cols);

            while (grid[r][c] != '\u0000') {
                r = rand.nextInt(0, rows);
                c = rand.nextInt(0, cols);
            }

            grid[r][c] = 'S';
            stores[j][0] = r;
            stores[j][1] = c;
        }
    }

    private void generateDestinations(RandomGenerator rand) {
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
    }

    private void normalizeGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '\u0000') {
                    grid[r][c] = '.';
                }
            }
        }
    }

    private void generateObstacles(RandomGenerator rand) {
        for (int i = 0; i < numObstacles; i++) {
            int r = rand.nextInt(0, rows);
            int c = rand.nextInt(0, cols);

            if (r == rows - 1) {
                traffic[r][c][0] = Integer.MAX_VALUE;
            } else if (c == cols - 1) {
                traffic[r][c][1] = Integer.MAX_VALUE;
            } else {
                traffic[r][c][rand.nextInt(0, 2)] = Integer.MAX_VALUE;
            }

            obstacles[i][0] = r;
            obstacles[i][1] = c;
        }
    }

    private void generateTraffic(RandomGenerator rand) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                for (int d = 0; d < 2; d++) {
                    if (traffic[r][c][d] != Integer.MAX_VALUE &&
                        traffic[r][c][d] == 0) {

                        traffic[r][c][d] = rand.nextInt(1, 5);
                    }
                }
            }
        }
    }

    private void printGrid() {
        System.out.println(numDestinations + " " + numStores + " " + numTunnels);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                System.out.print(grid[r][c] + "-");

                if (traffic[r][c][0] == Integer.MAX_VALUE)
                    System.out.print("X");
                else
                    System.out.print(traffic[r][c][0]);

                System.out.print(">");
            }
            System.out.println();

            if (r == rows - 1) break;

            for (int c = 0; c < cols; c++) {
                if (traffic[r][c][1] == Integer.MAX_VALUE)
                    System.out.print("X   ");
                else
                    System.out.print(traffic[r][c][1] + "   ");
            }
            System.out.println();
        }
    }

    private void buildInitialState() {
        StringBuilder sb = new StringBuilder();
        sb.append(rows).append(';')
          .append(cols).append(';')
          .append(numDestinations).append(';')
          .append(numStores).append(';');

        for (int i = 0; i < numDestinations; i++)
            sb.append(destinations[i][0]).append(',')
              .append(destinations[i][1]).append(',');

        sb.deleteCharAt(sb.length() - 1);
        sb.append(';');

        for (int i = 0; i < numStores; i++)
            sb.append(stores[i][0]).append(',')
              .append(stores[i][1]).append(',');

        sb.deleteCharAt(sb.length() - 1);
        sb.append(';');

        for (int i = 0; i < numTunnels; i++) {
            sb.append(tunnels[i * 2][0]).append(',')
              .append(tunnels[i * 2][1]).append(',')
              .append(tunnels[i * 2 + 1][0]).append(',')
              .append(tunnels[i * 2 + 1][1]).append(';');
        }

        this.initialState = sb.toString();
    }

    private void buildTrafficString() {
        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                if (c + 1 < cols) {
                    int t = traffic[r][c][0] == Integer.MAX_VALUE ? 0 : traffic[r][c][0];
                    sb.append(r).append(',').append(c).append(',')
                      .append(r).append(',').append(c + 1).append(',')
                      .append(t).append(';');
                }

                if (r + 1 < rows) {
                    int t = traffic[r][c][1] == Integer.MAX_VALUE ? 0 : traffic[r][c][1];
                    sb.append(r).append(',').append(c).append(',')
                      .append(r + 1).append(',').append(c).append(',')
                      .append(t).append(';');
                }
            }
        }

        this.trafficString = sb.toString();
    }

    // -------------------------
    //          GETTERS
    // -------------------------

    public String getInitialState() { return initialState; }
    public String getTrafficString() { return trafficString; }
    public int[][] getStores() { return stores; }
    public int[][] getDestinations() { return destinations; }
    public char[][] getGrid() { return grid; }
    public int[][] getTunnels() { return tunnels; }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
