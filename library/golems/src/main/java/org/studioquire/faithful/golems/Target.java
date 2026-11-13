package org.studioquire.faithful.golems;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** A piece of work: a block, or something that moves about (an entity), and how much it's wanted. Higher ranks come first; among equal ranks, the nearest. */
public final class Target {
	/** The block, or the block an entity stood in when it was found. */
	public final int x, y, z;
	/** For work that moves, or null. */
	public final Entity entity;
	public final int rank;

	public Target(int x, int y, int z, int rank) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.entity = null;
		this.rank = rank;
	}

	public Target(Entity entity, int rank) {
		this.x = MathHelper.floor(entity.x);
		this.y = MathHelper.floor(entity.boundingBox.minY);
		this.z = MathHelper.floor(entity.z);
		this.entity = entity;
		this.rank = rank;
	}

	/** Where it is now, its middle. */
	public double centerX() {
		return entity != null ? entity.x : x + 0.5;
	}

	public double centerY() {
		return entity != null ? (entity.boundingBox.minY + entity.boundingBox.maxY) / 2.0 : y + 0.5;
	}

	public double centerZ() {
		return entity != null ? entity.z : z + 0.5;
	}

	/** The same work, for remembering what it gave up on: a block's place, or an entity's id. */
	long key() {
		if (entity != null) return -1L - entity.id;
		return ((long) (x + 0x2000000) << 34) | ((long) (z + 0x2000000) << 8) | (y & 0xFF);
	}

	@Override
	public String toString() {
		return entity != null ? entity.getClass().getSimpleName() + " at " + x + " " + y + " " + z : x + " " + y + " " + z + " rank " + rank;
	}
}
