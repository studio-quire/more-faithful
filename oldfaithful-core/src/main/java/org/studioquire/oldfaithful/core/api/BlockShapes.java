package org.studioquire.oldfaithful.core.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Render types for a mod's block shapes. A block's {@code getRenderType} returns what {@link #type} gave; on the client, {@code Renderers.blockShape} says how that name is drawn.
 * Vanilla's render types stop at 17. The numbers mean something only inside this game: they never leave it and aren't saved, which is why the name is what's shared.
 */
public final class BlockShapes {
	public static final int FIRST_TYPE = 18;
	private static final Map<String, Integer> TYPES = new HashMap<>();

	private BlockShapes() {
	}

	/** The render type of a shape, by a full name: call it in common code, even before anything has drawn it. */
	public static synchronized int type(String name) {
		return TYPES.computeIfAbsent(name, n -> FIRST_TYPE + TYPES.size());
	}
}
