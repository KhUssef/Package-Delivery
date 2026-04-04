package ai.proj;

public class DeliverySearch {

    private GenericSearch strategy;
    private String initialState;
    private String trafficString;

    public DeliverySearch(GenericSearch strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(GenericSearch strategy) {
        this.strategy = strategy;
    }

    // Load grid data for the chosen strategy
    public void extract(String initialState, String trafficString) {
        this.initialState = initialState;
        this.trafficString = trafficString;
        this.strategy.extract(initialState, trafficString);
    }

    // Execute the search for a specific store → destination pair
    public String search(String goalState) {
        if (this.strategy == null)
            return "FAIL";

        return this.strategy.search(goalState);
    }

    // Utility wrapper: run search from a specific store to a goal
    public String path(String initialState, String traffic, String storeState, String goalState) {
        this.extract(initialState, traffic);
        this.strategy.setStart(storeState);
        return this.strategy.search(goalState);
    }
}
