package org.studioquire.oldfaithful.core.api;

import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.impl.SoundNet;

/**
 * A mod's sounds are files: {@code assets/<modid>/sounds/<path>.ogg} (or {@code .wav}) plays as {@code <modid>.<path>} with the slashes as dots, and
 * numbered files are variants of one sound, as vanilla's are: {@code block/hum1.ogg} and {@code block/hum2.ogg} are both {@code my-mod.block.hum}, picked at random.
 * Under {@code sounds/music/} a file joins the game's background music; under {@code sounds/streaming/}, it's for a jukebox or {@code world.playStreaming}, as
 * {@code <modid>.<name>} (a {@code MusicDiscItem} names it). The files stay in the jar. A client's game with no such file just plays nothing.
 */
public final class Sounds {
	private Sounds() {
	}

	/**
	 * Plays a sound at a place for everyone near it. {@code world.playSound} does that in single player and on a client for itself, but goes nowhere on a server; this
	 * works on all three. Players without the core hear nothing. {@code volume} above 1 is heard further, as vanilla's; {@code pitch} 1 is as recorded. In code that runs on both
	 * sides (a block's {@code onUse}), call it only where {@code !world.isRemote}, or the player who did it hears it twice.
	 */
	public static void play(World world, double x, double y, double z, String name, float volume, float pitch) {
		SoundNet.play(world, x, y, z, name, volume, pitch);
	}

	/**
	 * A block's sounds, for {@code setSoundGroup}. {@code step} plays when walked on and placed, {@code broken} when broken; give vanilla's names ({@code step.stone}) or a mod's,
	 * with no fixed prefix. Volume and pitch apply to both.
	 */
	public static BlockSoundGroup group(String step, String broken, float volume, float pitch) {
		return new ModGroup(step, broken, volume, pitch);
	}

	private static final class ModGroup extends BlockSoundGroup {
		private final String step, broken;

		ModGroup(String step, String broken, float volume, float pitch) {
			super(step, volume, pitch);
			this.step = step;
			this.broken = broken;
		}

		@Override
		public String getSound() {
			return step;
		}

		@Override
		public String getBreakSound() {
			return broken;
		}
	}
}
