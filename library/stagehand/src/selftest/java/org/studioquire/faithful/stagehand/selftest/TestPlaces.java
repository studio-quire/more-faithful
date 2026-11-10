package org.studioquire.faithful.stagehand.selftest;

import org.studioquire.faithful.stagehand.Places;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** What Buried Towers will be to Stagehand: a mod answering for a kind of place, through its entrypoint alone. Answers relative to where it's asked from. */
public final class TestPlaces implements Places {
	@Override
	public String kind() {
		return "test-place";
	}

	@Override
	public Place find(Query query) {
		if (query.words.contains("none")) return null;
		if (query.words.contains("bad")) throw new IllegalArgumentException("it doesn't take bad");
		int floor = query.number("floor", 0);
		return new Place(Worldgen.OVERWORLD, query.x + floor, query.y, query.z).about("floor", floor);
	}
}
