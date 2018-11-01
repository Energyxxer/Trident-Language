package com.energyxxer.trident.util;

public class Range {
	public double min,max;
	public Range(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	public boolean contains(double v) {
		return v >= min && v <= max;
	}

	public double clamp(double value) {
		return (value < min) ? min : ((value > max) ? max : value);
	}

	public int clamp(int value) {
		return (value < min) ? (int) min : ((value > max) ? (int) max : value);
	}
	
	public static Range union(Range r1, Range r2) {
		return new Range(Math.min(r1.min, r2.min),Math.max(r1.min, r2.min));
	}
}
