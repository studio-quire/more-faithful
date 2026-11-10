package org.studioquire.faithful.buriedtowers;

import org.studioquire.faithful.stagehand.Places;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * Towers, for Stagehand: {@code locate tower}, {@code tp tower nearest floor=3}. Only Stagehand loads this, through its entrypoint, so a game without Stagehand never touches it. Floors count from the
 * top as a tower does, 0 being the top one; no floor asked means just outside the gate. A floor's {@code way} says how you go on down.
 */
public final class TowerPlaces implements Places {
	@Override
	public String kind() {
		return "tower";
	}

	@Override
	public Place find(Query query) {
		for (String word : query.words) {
			if (!word.equals("nearest")) throw new IllegalArgumentException("tower takes nearest, not " + word);
		}
		Towers.Found tower = Towers.nearest(query.world, query.x, query.z);
		if (tower == null) return null;
		Place place;
		if (query.options.containsKey("floor")) {
			int index = query.number("floor", 0);
			if (index < 0 || index >= tower.floors.size()) throw new IllegalArgumentException("this tower's floors are 0 to " + (tower.floors.size() - 1));
			Towers.FoundFloor floor = tower.floors.get(index);
			place = new Place(Worldgen.OVERWORLD, floor.safeX, floor.safeY, floor.safeZ).about("floor", index).about("kind", floor.kind);
			if (floor.wayDown != null) place.about("way", floor.wayDown).about("way_x", floor.wayX).about("way_z", floor.wayZ);
		} else {
			place = new Place(Worldgen.OVERWORLD, tower.gateX, tower.groundY + 1, tower.gateZ);
		}
		return place.about("floors", tower.floors.size()).about("climate", tower.climate).about("built_for", tower.purpose).about("tower_x", tower.x).about("tower_z", tower.z);
	}
}
