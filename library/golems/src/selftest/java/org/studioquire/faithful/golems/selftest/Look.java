package org.studioquire.faithful.golems.selftest;

import java.io.File;

/**
 * A run for looking at the golem rather than testing it: only when a {@code golems/look.properties} is found in a folder above the game's (so {@code local/run/golems-look/golems/}, as library/golems/tools/look.sh makes it), as
 * {@link LevelRun}'s file is. The world test and the usual client test then stand aside, and {@code GolemsLookTest} takes its pictures in single player.
 */
public final class Look {
	private Look() {
	}

	public static boolean asked() {
		for (File f = new File(System.getProperty("user.dir")).getAbsoluteFile(); f != null; f = f.getParentFile()) {
			if (new File(f, "golems/look.properties").isFile()) return true;
		}
		return false;
	}
}
