package org.studioquire.faithful.golems;

import java.util.List;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** The ground a golem works: x and z from min to max, inclusive, at any height. Marked out by boundary poles, or a square round its post. */
public final class Area {
	/** With no poles round it: this far from its post, each way. */
	public static final int AROUND_POST = 16;
	/** How far from its post its poles may be, each way. */
	public static final int POLE_RANGE = 48;

	public final int minX, minZ, maxX, maxZ;
	/** Marked by poles, as opposed to the square round its post. */
	public final boolean marked;

	Area(int minX, int minZ, int maxX, int maxZ, boolean marked) {
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
		this.marked = marked;
	}

	/** The poles round a post, or the square round it; the poles found go in {@code corners}, if given. */
	public static Area find(World world, int postX, int postZ, List<int[]> corners) {
		Area poles = BoundaryPole.rectangleAround(world, postX, postZ, POLE_RANGE, corners);
		return poles != null ? poles : new Area(postX - AROUND_POST, postZ - AROUND_POST, postX + AROUND_POST, postZ + AROUND_POST, false);
	}

	public boolean contains(int x, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
	}

	void write(NbtCompound nbt) {
		nbt.putInt("MinX", minX);
		nbt.putInt("MinZ", minZ);
		nbt.putInt("MaxX", maxX);
		nbt.putInt("MaxZ", maxZ);
		nbt.putBoolean("Marked", marked);
	}

	static Area read(NbtCompound nbt) {
		return new Area(nbt.getInt("MinX"), nbt.getInt("MinZ"), nbt.getInt("MaxX"), nbt.getInt("MaxZ"), nbt.getBoolean("Marked"));
	}

	@Override
	public String toString() {
		return minX + " " + minZ + " to " + maxX + " " + maxZ + (marked ? " (poles)" : " (round its post)");
	}
}
