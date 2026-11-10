package org.studioquire.faithful.buriedtowers.plan;

/** A kind of floor: how tall it comes, how deep it belongs, who'd have built it, and how to draw one. */
abstract class FloorKind {
	/** Short, medium and tall, for how a tower mixes them. */
	enum Size {
		SHORT, MEDIUM, TALL
	}

	final String name;
	final Size size;
	final int minH, maxH;
	final double minDepth, maxDepth;

	FloorKind(String name, Size size, int minH, int maxH, double minDepth, double maxDepth) {
		this.name = name;
		this.size = size;
		this.minH = minH;
		this.maxH = maxH;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
	}

	/** What's underfoot, ways' cells included: asked before anything's drawn. */
	Mat underfoot(Floor f) {
		return f.pal.floor;
	}

	/** The underside of the floor above. */
	Mat overhead(Floor f) {
		return f.pal.ceiling;
	}

	/** How likely in this tower. Zero never. */
	double weight(Tower t) {
		return 1;
	}

	/** The raised walk's height over the floor's lowest cell, if it has one: asked before anything's drawn, as ways are placed by it. */
	int standOffset(int h) {
		return 0;
	}

	/** Where the way on starts, measured the same way, if not from where you come and go. */
	int leaveOffset(int h) {
		return standOffset(h);
	}

	/** Whether a way down into this floor stops at its walking level, on a stage of its own over what's below, rather than standing on the floor's lowest cell. */
	boolean landsOnStage() {
		return false;
	}

	/** Whether a place is over the open part of the floor, for a stage's rails and posts. */
	boolean openAt(Floor f, int x, int z) {
		return false;
	}

	/** Whether there's lava let into the floor, so the room below keeps wood out of its ceiling, and a ladder's post into the room isn't a log. */
	boolean hotUnderfoot() {
		return false;
	}

	/** The top of the water it stands in, measured the same way, or -1. */
	int waterOffset(int h) {
		return -1;
	}

	/** Whether a way may take this column of the floor: a floor with a heart it needs kept whole says no there. */
	boolean allows(Floor f, int x, int z) {
		return true;
	}

	/** How far from the middle the floor's idea stands, which ways keep off when they can. */
	double heart(Floor f) {
		return 0;
	}

	/** Whether what's under the raised walk is a hollow sealed off from the room, where nothing's put: a spawner there would be out of sight and out of reach. */
	boolean hollowUnder() {
		return false;
	}

	/** The monster that's taken the floor over, or null: every spawner {@link Guards} leaves on it is of that kind. */
	String takenBy() {
		return null;
	}

	/** Whether a way that wants the tall room's walls can come down here. */
	boolean openWalls() {
		return false;
	}

	abstract void draw(Floor f);

	/** After the ways are drawn, before anything's walked or settled: for what has to fit round them, like water over the floor. */
	void afterWays(Floor f) {
	}
}
