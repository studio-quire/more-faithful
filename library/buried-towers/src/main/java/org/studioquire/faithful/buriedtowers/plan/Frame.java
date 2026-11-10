package org.studioquire.faithful.buriedtowers.plan;

/** Local coordinates about the tower's middle, turned by quarters, so a floor is drawn once and comes out facing any way. {@code u} runs east and {@code v} south before turning. */
final class Frame {
	final int cx, cz, turn;

	Frame(int cx, int cz, int turn) {
		this.cx = cx;
		this.cz = cz;
		this.turn = turn & 3;
	}

	int x(int u, int v) {
		switch (turn) {
			case 1: return cx - v;
			case 2: return cx - u;
			case 3: return cx + v;
			default: return cx + u;
		}
	}

	int z(int u, int v) {
		switch (turn) {
			case 1: return cz + u;
			case 2: return cz - v;
			case 3: return cz - u;
			default: return cz + v;
		}
	}

	/** The frame's {@code u} of a real column. */
	int u(int x, int z) {
		switch (turn) {
			case 1: return z - cz;
			case 2: return cx - x;
			case 3: return cz - z;
			default: return x - cx;
		}
	}

	/** The frame's {@code v} of a real column. */
	int v(int x, int z) {
		switch (turn) {
			case 1: return cx - x;
			case 2: return cz - z;
			case 3: return x - cx;
			default: return z - cz;
		}
	}

	/** A direction given in the frame, as a real one. */
	int dir(int d) {
		return d + turn & 3;
	}
}
