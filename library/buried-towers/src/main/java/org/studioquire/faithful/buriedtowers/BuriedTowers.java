package org.studioquire.faithful.buriedtowers;

import net.minecraft.world.chunk.Chunk;
import org.studioquire.faithful.buriedtowers.plan.Tower;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.ChoiceRule;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** Towers built with the land, and an end to beta's little dungeons in new chunks. {@link Keeper} has how, {@code plan} what they're like, and {@link Towers} is for other mods to find them. */
public final class BuriedTowers implements FaithfulMod {
	/** On each chunk in a site's square: {@code Keeper}'s record of what it knows about the tower there. */
	static Attachment<Chunk, Integer> mark;
	/** Simple, crude as beta's dungeons and smaller, or complex, as the towers were first built: chosen with the world (the user, 2026-11-07). */
	static ChoiceRule style;

	private static boolean warmed;

	/** Once a game, as the first world opens: code once compiled stays so. */
	private static void warmUp() {
		if (warmed) return;
		warmed = true;
		Tower.warmUp();
	}

	@Override
	public void init(Mod mod) {
		mark = mod.attachment("tower", Attachment.chunk(Codec.INT)).persist();
		style = mod.rules().atCreation().choice("style", "simple", "complex");
		Worldgen.onDungeonPlacing(d -> {
			if (d.world().dimension.id == Worldgen.OVERWORLD) d.cancel();
		});
		Worldgen.chunkBuilt("buried-towers:tower", (cells, random) -> Keeper.of(cells.world()).built(cells));
		Worldgen.decorate("buried-towers:tower", (world, random, x, z) -> Keeper.of(world).decorated(x, z));
		Lifecycle.onWorldUnload(Keeper::forget);
		Lifecycle.onServerStart(BuriedTowers::warmUp);
	}
}
