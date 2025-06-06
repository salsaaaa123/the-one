/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.*;
import java.util.Locale;

/**
 * RandomWalkLevy movement model based on truncated Levy walks.
 * Implements power-law flight length distribution (Pareto) for human-like mobility.
 *
 * Reference: "On the Levy-Walk Nature of Human Mobility"
 */
public class RandomWalkLevy extends MovementModel implements SwitchableMovement {

	private Coord lastWaypoint;
	private double alpha;        // Pareto exponent
	private double scale;        // Pareto minimum value (xMin)
	private double maxDistance;  // Truncation value
	private Pareto pareto;

	public RandomWalkLevy(Settings s) {
		super(s);

		this.alpha = s.contains("alpha") ? s.getDouble("alpha") : 0.3;
		this.scale = s.contains("scale") ? s.getDouble("scale") : 3000.0;
		this.maxDistance = s.contains("maxDistance") ? s.getDouble("maxDistance") : 10000.0;

		this.pareto = new Pareto(this.scale, this.alpha, this.maxDistance, this.rng);
	}

	private RandomWalkLevy(RandomWalkLevy r) {
		super(r);
		this.alpha = r.alpha;
		this.scale = r.scale;
		this.maxDistance = r.maxDistance;
		this.pareto = r.pareto;
	}

	@Override
	public Coord getInitialLocation() {
		double x = rng.nextDouble() * getMaxX();
		double y = rng.nextDouble() * getMaxY();
		Coord c = new Coord(x, y);
		this.lastWaypoint = c;
		return c;
	}

	@Override
	public Path getPath() {
		Path p = new Path(generateSpeed());
		p.addWaypoint(lastWaypoint.clone());

		double maxX = getMaxX();
		double maxY = getMaxY();

		double distance = pareto.sample();
		double angle = rng.nextDouble() * 2 * Math.PI;

		double newX = lastWaypoint.getX() + distance * Math.cos(angle);
		double newY = lastWaypoint.getY() + distance * Math.sin(angle);

		// Clamp to bounds
		newX = Math.max(0, Math.min(newX, maxX));
		newY = Math.max(0, Math.min(newY, maxY));

		Coord newWaypoint = new Coord(newX, newY);
		p.addWaypoint(newWaypoint);
		lastWaypoint = newWaypoint;

		// DEBUG: Uncomment this if you want to see step logs
		// System.out.printf(Locale.US, "Sampled dist: %.2f, From (%.1f, %.1f) → (%.1f, %.1f)\n",
		//     distance, lastWaypoint.getX(), lastWaypoint.getY(), newX, newY);

		return p;
	}

	@Override
	public RandomWalkLevy replicate() {
		return new RandomWalkLevy(this);
	}

	@Override
	public boolean isReady() {
		return true;
	}

	public Coord getLastLocation() {
		return lastWaypoint;
	}

	public void setLocation(Coord wp) {
		this.lastWaypoint = wp;
	}
}
