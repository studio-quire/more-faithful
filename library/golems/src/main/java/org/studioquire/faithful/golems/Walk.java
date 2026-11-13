package org.studioquire.faithful.golems;

import net.minecraft.util.math.MathHelper;

/**
 * A golem walking to a node of a {@link WalkMap} search: beta's own walking along the path, the last of it by eye, and a new search from where it is if beta drops the path on the way
 * (it does, one tick in a hundred). Stuck for six seconds is a failure.
 */
final class Walk {
	enum Status { GOING, ARRIVED, FAILED }

	private final GolemEntity golem;
	private boolean active;
	private int toX, toY, toZ;
	private int minX, minZ, maxX, maxZ;
	private double checkX, checkZ;
	private int checkTicks, stuck, byEye, searches;
	/** Straight across open ground where it can, as following does ({@link WalkMap#straightPathTo}); work walks keep to the grid, place by place. */
	private final boolean straight;

	Walk(GolemEntity golem) {
		this(golem, false);
	}

	Walk(GolemEntity golem, boolean straight) {
		this.golem = golem;
		this.straight = straight;
	}

	private net.minecraft.entity.ai.pathing.Path path(WalkMap map, int node) {
		return straight ? map.straightPathTo(node, golem.x, golem.z) : map.pathTo(node);
	}

	/** Off to a node of the search just made, over the rectangle it was made over. */
	void start(WalkMap map, int node, int minX, int minZ, int maxX, int maxZ) {
		active = true;
		toX = map.x(node);
		toY = map.y(node);
		toZ = map.z(node);
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
		checkX = golem.x;
		checkZ = golem.z;
		checkTicks = stuck = byEye = searches = 0;
		golem.setPath(path(map, node));
	}

	boolean active() {
		return active;
	}

	int toX() {
		return toX;
	}

	int toY() {
		return toY;
	}

	int toZ() {
		return toZ;
	}

	void stop() {
		active = false;
		golem.setPath(null);
	}

	Status tick() {
		if (!active) return Status.ARRIVED;
		double dx = toX + 0.5 - golem.x, dz = toZ + 0.5 - golem.z, d2 = dx * dx + dz * dz;
		if (!golem.hasPath()) {
			// Beta calls a path done within 0.4 of its last place's middle (the width it reads, 0.2, doubled): the rest by eye, inside that place.
			boolean there = MathHelper.floor(golem.x) == toX && MathHelper.floor(golem.z) == toZ;
			if (d2 < 0.16 || (there && ++byEye > 40)) {
				active = false;
				return Status.ARRIVED;
			}
			// Short of the place, the way is found again rather than stepped by eye: beta drops a path one tick in a hundred, and a straight step from a place beside can catch on a
			// corner the path went round and stick there, short of where it meant to stand (it then worked a block from 2.62, beyond its 2, in one leveller's test, 2026-11-10).
			if (there) {
				golem.stepToward(toX + 0.5F, toY, toZ + 0.5F);
				return stuck();
			}
			if (++searches > 20) return fail();
			WalkMap map = WalkMap.get();
			map.search(golem.world, MathHelper.floor(golem.x), golem.feetY(), MathHelper.floor(golem.z), minX, minZ, maxX, maxZ);
			int node = map.node(toX, toY, toZ);
			if (node < 0) return fail();
			golem.setPath(path(map, node));
		}
		return stuck();
	}

	/** Every two seconds: not half a block further on, three times running, and it's stuck. */
	private Status stuck() {
		if (++checkTicks < 40) return Status.GOING;
		checkTicks = 0;
		double mx = golem.x - checkX, mz = golem.z - checkZ;
		checkX = golem.x;
		checkZ = golem.z;
		if (mx * mx + mz * mz > 0.25) {
			stuck = 0;
			return Status.GOING;
		}
		return ++stuck >= 3 ? fail() : Status.GOING;
	}

	private Status fail() {
		stop();
		return Status.FAILED;
	}
}
