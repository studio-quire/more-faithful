package org.studioquire.faithful.stagehand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;

/**
 * A kind of named place a mod can answer for: {@code locate tower}, {@code tp tower nearest floor=3}. A mod offers one under the entrypoint {@code stagehand:places} in its {@code fabric.mod.json}, with
 * {@code compileOnly project(':library:stagehand')} in its {@code build.gradle}. It doesn't depend on Stagehand: a game without Stagehand never loads the class. {@link #find} is called where the world
 * runs, on its thread.
 * <p>
 * Nothing at the place needs to be loaded or built yet. Stagehand loads the chunks before it moves anyone, so they generate then, and it checks the landing itself, moving it only if it isn't safe.
 */
public interface Places {
	/** The word scripts use, in lower case. */
	String kind();

	/** Where it is. Null says there's none, and an {@link IllegalArgumentException}'s message goes back to the script: use it for words you don't take. Both are plain errors there. */
	Place find(Query query);

	/** What a script asked: {@code tp tower nearest floor=3} is the word {@code nearest} and the option {@code floor}, {@code 3}. */
	final class Query {
		/** Where it was asked from: the player's world and cell, or the world's spawn with nobody there. */
		public final World world;
		public final int x, y, z;
		public final List<String> words;
		public final Map<String, String> options;

		Query(World world, int x, int y, int z, List<String> words, Map<String, String> options) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.words = Collections.unmodifiableList(words);
			this.options = Collections.unmodifiableMap(options);
		}

		/** The option as a number, or the fallback if it isn't there. Anything else is the script's error. */
		public int number(String option, int fallback) {
			String value = options.get(option);
			if (value == null) return fallback;
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(option + " takes a number, not " + value);
			}
		}
	}

	/** An answer: the cell a player's feet go in, and in which dimension ({@code Worldgen.OVERWORLD}, {@code Worldgen.NETHER}). */
	final class Place {
		public final int dimension, x, y, z;
		/** Whatever a script might want to read back: which floor, how far down. Numbers, strings and booleans. */
		public final Map<String, Object> about = new LinkedHashMap<>();

		public Place(int dimension, int x, int y, int z) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Place about(String key, Object value) {
			about.put(key, value);
			return this;
		}
	}
}
