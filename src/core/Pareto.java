/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.Random;

/**
 * A random number generator for a truncated Pareto distribution.
 * Suitable for modeling Levy Walks.
 *
 * Pareto distribution:
 *   P(x) = (k * x_m^k) / x^(k+1), for x >= x_m
 *
 * @author Frans Ekman
 * @modified by [Your Name]
 */
public class Pareto {
	private Random rng;
	private double xMin;      // Minimum value (scale parameter)
	private double alpha;     // Exponent (shape parameter)
	private double xMax;      // Truncation upper bound (optional)

	/**
	 * Constructs a new truncated Pareto random number generator.
	 *
	 * @param xMin Minimum value (scale)
	 * @param alpha Exponent (shape)
	 * @param rng Random number generator
	 */
	public Pareto(double xMin, double alpha, Random rng) {
		this(xMin, alpha, Double.POSITIVE_INFINITY, rng);
	}

	/**
	 * Constructs a new Pareto generator with truncation.
	 *
	 * @param xMin Minimum value
	 * @param alpha Exponent
	 * @param xMax Maximum value (truncate if exceeded)
	 * @param rng Random number generator
	 */
	public Pareto(double xMin, double alpha, double xMax, Random rng) {
		this.rng = rng;
		this.xMin = xMin;
		this.alpha = alpha;
		this.xMax = xMax > 0 ? xMax : Double.POSITIVE_INFINITY;
	}

	/**
	 * Sample a value from the Pareto distribution.
	 *
	 * @return A random number between xMin and xMax
	 */
	public double sample() {
		double x;
		do {
			double u = rng.nextDouble();
			x = xMin / Math.pow(u, 1.0 / alpha);
		} while (x > xMax);
		return x;
	}
}
