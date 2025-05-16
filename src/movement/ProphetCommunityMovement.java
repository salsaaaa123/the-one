/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Community-based movement model tailored for the Prophet paper simulation.
 * Nodes have a home community and probabilistic movement towards
 * their home, a gathering place, or other areas, based on their
 * current location.
 *
 * Areas are numbered 1-11 for Communities C1-C11 and 12 for Gathering Place G.
 * This version manages location state internally using lastWaypoint.
 * NOTE: Home area is assigned randomly in this version, not based on host
 * index.
 */
public class ProphetCommunityMovement extends MovementModel {

    /** Area number for the Gathering Place (G) */
    public static final int GATHERING_AREA = 12;
    private static final int NUM_COMMUNITIES = 11; // Total communities C1-C11
    private static final int TOTAL_AREAS = NUM_COMMUNITIES + 1; // Total areas C1-C11 + G

    private Coord lastWaypoint; // The node's location after completing the previous path, managed internally
    private int homeArea; // The node's home community (1-11). Assigned randomly in this version.
    private int currentArea; // The area the node is currently considered to be in (1-12). Based on
                             // lastWaypoint.

    /**
     * Constructor for the movement model.
     * Reads settings, assigns a random home area, and sets initial location state.
     * 
     * @param settings The settings object containing parameters
     */
    public ProphetCommunityMovement(Settings settings) {
        super(settings);
        // Initialize homeArea: randomly assign one of the community areas (1-11)
        // *** NOTE: This assigns home areas RANDOMLY, NOT 10 nodes per community as in
        // the paper detail ***
        this.homeArea = rng.nextInt(NUM_COMMUNITIES) + 1; // Random integer from 1 to 11

        // Initial location state. getInitialLocation() will be called by framework
        // later.
        // Initialize lastWaypoint to null here.
        this.lastWaypoint = null; // Will be set by getInitialLocation()

        // currentArea needs to be consistent with the initial location set by
        // getInitialLocation
        // Set it to the home area initially.
        this.currentArea = this.homeArea; // Assume starts in home area (consistent with getInitialLocation)
    }

    /**
     * Copy constructor for replication.
     * 
     * @param rwp The model to replicate
     */
    protected ProphetCommunityMovement(ProphetCommunityMovement rwp) {
        super(rwp);
        this.homeArea = rwp.homeArea;
        this.currentArea = rwp.currentArea; // Copy current area state
        // Copy the lastWaypoint Coord object (handle null)
        this.lastWaypoint = (rwp.lastWaypoint != null) ? rwp.lastWaypoint.clone() : null;
    }

    /**
     * Returns a possible (random) initial placement for a host, ensuring it's
     * within its home area initially. This method is called by the framework
     * when a new host is created.
     * It also sets the initial lastWaypoint and currentArea state for the model.
     * 
     * @return Random position on the map within the home area
     */
    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        // Initial location is within the home area (using the randomly assigned
        // homeArea)
        Coord initialCoord = getCoordInArea(this.homeArea);

        // Set the initial internal state of the model
        this.lastWaypoint = initialCoord; // Set the initial lastWaypoint
        this.currentArea = this.homeArea; // Ensure current area is set to home initially

        return initialCoord; // Return the coordinate for the framework to place the host
    }

    /**
     * Generates a new path based on the probabilistic model.
     * Selects a destination area, then a coordinate within that area.
     * Uses the internally stored lastWaypoint as the path start.
     * 
     * @return The generated Path
     */
    @Override
    public Path getPath() {
        assert lastWaypoint != null
                : "lastWaypoint is not initialized! getInitialLocation() should be called first by framework.";
        assert rng != null : "MovementModel not initialized!"; // Should be initialized by super

        Path p;
        // Generate speed using the base class method
        double speed = generateSpeed();

        // Add current location (our lastWaypoint) as the start of the path --- USING
        // INTERNAL STATE ---
        p = new Path(speed);
        p.addWaypoint(lastWaypoint.clone()); // Use lastWaypoint as the starting point for the new path

        // Determine the node's current area based on our last known location
        // (lastWaypoint)
        // We use getAreaFromCoord to be sure it's consistent with the grid mapping.
        this.currentArea = getAreaFromCoord(lastWaypoint); // Calculate current area from lastWaypoint. Requires
                                                           // getAreaFromCoord.

        // Choose the next destination area based on current area and home area
        int nextArea = chooseNextArea(this.currentArea, this.homeArea); // Pass current and home areas

        // Get a random coordinate within the chosen destination area
        Coord nextCoord = getCoordInArea(nextArea); // Requires getCoordInArea method

        // Add the destination coordinate to the path (path length 1)
        p.addWaypoint(nextCoord);

        // *** IMPORTANT STATE UPDATE ***
        // Update our internal state to reflect the *destination* of the path we just
        // generated.
        // When getPath is called again, this nextCoord will become the new start
        // (lastWaypoint).
        this.currentArea = nextArea; // Update currentArea to the destination area
        this.lastWaypoint = nextCoord; // Update lastWaypoint to the destination coordinate

        return p; // Return the path for the framework to use
    }

    /**
     * Creates a replicate of this movement model.
     * 
     * @return A new ProphetCommunityMovement instance with the same state
     */
    @Override
    public ProphetCommunityMovement replicate() {
        return new ProphetCommunityMovement(this);
    }

    /**
     * Chooses the next area number (1-12) based on current area,
     * home area, and probabilities from Table I.
     * 
     * @param currentArea The area the node is currently in (1-12)
     * @param homeArea    The node's home area (1-11)
     * @return The number of the chosen next area (1-12)
     */
    protected int chooseNextArea(int currentArea, int homeArea) {
        double probability = rng.nextDouble();

        if (currentArea == homeArea) {
            // Currently at Home Area (probability 0.8 to Gathering Place, 0.2 Elsewhere)
            if (probability <= 0.8) {
                return GATHERING_AREA; // Go to Gathering Place (Area 12)
            } else {
                // Go Elsewhere (any area EXCEPT homeArea and GATHERING_AREA)
                return chooseElsewhereArea(homeArea, GATHERING_AREA); // Pass excluded areas
            }
        } else if (currentArea == GATHERING_AREA) {
            // Assuming "Elsewhere" logic applies from Gathering if not at Home
            // Probability 0.9 to Home Area, 0.1 Elsewhere
            if (probability <= 0.9) {
                return homeArea; // Go to Home Area
            } else {
                // Go Elsewhere (any area EXCEPT currentArea and homeArea)
                return chooseElsewhereArea(currentArea, homeArea); // Pass excluded areas
            }
        } else { // Currently Elsewhere (not at Home Area, and not at Gathering Place)
            // Probability 0.9 to Home Area, 0.1 Elsewhere
            if (probability <= 0.9) {
                return homeArea; // Go to Home Area
            } else {
                // Go Elsewhere (any area EXCEPT currentArea and homeArea)
                return chooseElsewhereArea(currentArea, homeArea); // Pass excluded areas
            }
        }
    }

    /**
     * Chooses a random area number from 1 to TOTAL_AREAS, excluding the specified
     * areas.
     * Used for the "Elsewhere" destination logic.
     * 
     * @param excludedArea1 The first area number to exclude
     * @param excludedArea2 The second area number to exclude
     * @return A random area number from the remaining valid options (1-TOTAL_AREAS)
     */
    protected int chooseElsewhereArea(int excludedArea1, int excludedArea2) {
        List<Integer> possibleAreas = new ArrayList<>();
        for (int i = 1; i <= TOTAL_AREAS; i++) { // Use TOTAL_AREAS constant. Iterate through all potential areas 1-12
            if (i != excludedArea1 && i != excludedArea2) { // Exclude the two specified areas
                possibleAreas.add(i);
            }
        }
        if (possibleAreas.isEmpty()) {
            System.err.println("Warning: No 'Elsewhere' areas available! Ex1=" + excludedArea1 + ", Ex2="
                    + excludedArea2 + ". Falling back to home area.");
            return homeArea; // Fallback - ideally should not happen with 12 areas total
        }
        Collections.shuffle(possibleAreas, rng); // Shuffle the list using the model's RNG
        return possibleAreas.get(0); // Take the first element after shuffling
    }

    /**
     * Returns a random coordinate within the specified area number's bounds.
     * Maps area numbers (1-12) to the grid positions (C1-C11 and G)
     * from Figure 2 in the paper.
     * 
     * @param areaNumber The number of the area (1-12)
     * @return A random Coord within the specified area
     */
    protected Coord getCoordInArea(int areaNumber) {
        double x, y;
        double maxX = getMaxX();
        double maxY = getMaxY();

        double xQuarter = maxX / 4.0;
        double yThird = maxY / 3.0;

        switch (areaNumber) {
            case 1:
                x = rng.nextDouble() * xQuarter;
                y = rng.nextDouble() * yThird + 2.0 * yThird;
                return new Coord(x, y);
            case 2:
                x = rng.nextDouble() * xQuarter + xQuarter;
                y = rng.nextDouble() * yThird + 2.0 * yThird;
                return new Coord(x, y);
            case 3:
                x = rng.nextDouble() * xQuarter + 2.0 * xQuarter;
                y = rng.nextDouble() * yThird + 2.0 * yThird;
                return new Coord(x, y);
            case 4:
                x = rng.nextDouble() * xQuarter + 3.0 * xQuarter;
                y = rng.nextDouble() * yThird + 2.0 * yThird;
                return new Coord(x, y);
            case 5:
                x = rng.nextDouble() * xQuarter;
                y = rng.nextDouble() * yThird + yThird;
                return new Coord(x, y);
            case 6:
                x = rng.nextDouble() * xQuarter + xQuarter;
                y = rng.nextDouble() * yThird + yThird;
                return new Coord(x, y);
            case 7:
                x = rng.nextDouble() * xQuarter + 2.0 * xQuarter;
                y = rng.nextDouble() * yThird + yThird;
                return new Coord(x, y);
            case 8:
                x = rng.nextDouble() * xQuarter + 3.0 * xQuarter;
                y = rng.nextDouble() * yThird + yThird;
                return new Coord(x, y);
            case 9:
                x = rng.nextDouble() * xQuarter;
                y = rng.nextDouble() * yThird;
                return new Coord(x, y);
            case 10:
                x = rng.nextDouble() * xQuarter + xQuarter;
                y = rng.nextDouble() * yThird;
                return new Coord(x, y);
            case 11:
                x = rng.nextDouble() * xQuarter + 2.0 * xQuarter;
                y = rng.nextDouble() * yThird;
                return new Coord(x, y);
            case GATHERING_AREA:
                x = rng.nextDouble() * xQuarter + 3.0 * xQuarter;
                y = rng.nextDouble() * yThird;
                return new Coord(x, y);
            default:
                System.err.println("Warning: Requested coordinate for invalid area number: " + areaNumber
                        + ". Returning random coord.");
                return new Coord(rng.nextDouble() * maxX, rng.nextDouble() * maxY);
        }
    }

    /**
     * Determines the area number (1-12) that a given coordinate falls into.
     * This is the reverse mapping of getCoordInArea.
     * 
     * @param c The coordinate
     * @return The area number (1-12) or -1 if outside expected bounds
     */
    protected int getAreaFromCoord(Coord c) {
        double x = c.getX();
        double y = c.getY();
        double maxX = getMaxX();
        double maxY = getMaxY();

        double xQuarter = maxX / 4.0;
        double yThird = maxY / 3.0;

        int area = -1;

        if (y >= 2.0 * yThird && y <= maxY) {
            if (x >= 0 && x < xQuarter)
                area = 1;
            else if (x >= xQuarter && x < 2.0 * xQuarter)
                area = 2;
            else if (x >= 2.0 * xQuarter && x < 3.0 * xQuarter)
                area = 3;
            else if (x >= 3.0 * xQuarter && x <= maxX)
                area = 4;
        } else if (y >= yThird && y < 2.0 * yThird) {
            if (x >= 0 && x < xQuarter)
                area = 5;
            else if (x >= xQuarter && x < 2.0 * xQuarter)
                area = 6;
            else if (x >= 2.0 * xQuarter && x < 3.0 * xQuarter)
                area = 7;
            else if (x >= 3.0 * xQuarter && x <= maxX)
                area = 8;
        } else if (y >= 0 && y < yThird) {
            if (x >= 0 && x < xQuarter)
                area = 9;
            else if (x >= xQuarter && x < 2.0 * xQuarter)
                area = 10;
            else if (x >= 2.0 * xQuarter && x < 3.0 * xQuarter)
                area = 11;
            else if (x >= 3.0 * xQuarter && x <= maxX)
                area = GATHERING_AREA;
        }

        if (area == -1) {
            System.err.println("Warning: Coordinate " + c + " did not map to any area!");
        }
        return area;
    }

    // Note: generateSpeed() without parameters will use the default speed range
    // from settings.
    // If the paper specifies a fixed range (10-30 m/s), you might need to add a
    // helper
    // or ensure your settings for speed range are set accordingly.
    // double generateSpeed(double min, double max) { return rng.nextDouble() * (max
    // - min) + min; }
}