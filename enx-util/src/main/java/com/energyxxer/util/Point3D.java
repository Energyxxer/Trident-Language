package com.energyxxer.util;

public class Point3D {
	public int x;
	public int y;
	public int z;
	public Point3D(int x, int y, int z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Point3D() {
		this.x = this.y = this.z = 0;
	}
	@Override
	public String toString() {
		return "[" + x + "," + y + "," + z + "]";
	}
}
