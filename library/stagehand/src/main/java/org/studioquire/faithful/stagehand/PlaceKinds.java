package org.studioquire.faithful.stagehand;

import java.util.Map;
import java.util.TreeMap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** The kinds of place there are: what needs no mod, the world's spawn and another player, and what mods offer. Asked for when a script first names one. */
final class PlaceKinds {
	private static Map<String, Places> all;

	private PlaceKinds() {
	}

	static synchronized Map<String, Places> all() {
		if (all == null) {
			all = new TreeMap<>();
			all.put("spawn", new Spawn());
			all.put("player", new Player());
			// One mod's broken entrypoint mustn't take the rest with it.
			FabricLoader.getInstance().getEntrypointContainers("stagehand:places", Places.class).forEach(offer -> {
				String mod = offer.getProvider().getMetadata().getId();
				try {
					Places places = offer.getEntrypoint();
					Places had = all.putIfAbsent(places.kind(), places);
					if (had != null) System.out.println("[stagehand] WARN " + mod + " also answers for the place " + places.kind() + ", and is passed over");
				} catch (RuntimeException | LinkageError e) {
					System.out.println("[stagehand] WARN " + mod + "'s places can't be used: " + e);
				}
			});
		}
		return all;
	}

	private static final class Spawn implements Places {
		@Override
		public String kind() {
			return "spawn";
		}

		@Override
		public Place find(Query query) {
			Vec3i spawn = query.world.getSpawnPos();
			return new Place(Worldgen.OVERWORLD, spawn.x, spawn.y, spawn.z);
		}
	}

	private static final class Player implements Places {
		@Override
		public String kind() {
			return "player";
		}

		@Override
		public Place find(Query query) {
			if (query.words.size() != 1) throw new IllegalArgumentException("player takes a name");
			PlayerEntity player = Run.player(query.words.get(0));
			if (player == null) throw new IllegalArgumentException(query.words.get(0) + " isn't here; here: " + Run.names());
			return new Place(player.dimensionId, MathHelper.floor(player.x), MathHelper.floor(player.boundingBox.minY), MathHelper.floor(player.z)).about("name", player.name);
		}
	}
}
