package org.studioquire.oldfaithful.core.api;

/**
 * A library mod, listed in its fabric.mod.json under the {@code faithful:mod} entrypoint. The core calls it once at a
 * defined point, on both sides, after the game's own blocks and items exist. The API is 0.x and can still change between
 * releases (docs/library-authoring.md).
 */
public interface FaithfulMod {
	/** Declare names and factories. Runs for every mod before anything is built. */
	default void content(Content content) {
	}

	/** Every mod has declared: see the names and generate from them. */
	default void lateContent(Content content) {
	}

	/** Everything exists: hooks, recipes, rules, channels. */
	default void init(Mod mod) {
	}
}
