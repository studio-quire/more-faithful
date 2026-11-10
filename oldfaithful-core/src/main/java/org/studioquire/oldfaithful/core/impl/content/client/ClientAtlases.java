package org.studioquire.oldfaithful.core.impl.content.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.client.texture.TextureManager;
import org.lwjgl.opengl.GL11;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;
import org.studioquire.oldfaithful.core.impl.content.Atlases;

/**
 * Mods' atlases on the client: 256x256 textures of their own, stitched from mods' PNGs through the texture pack, so a pack can
 * replace a tile at the same path. GL names stay the same across a pack switch, because chunk display lists have them baked in, and
 * they wrap (GL_REPEAT), because item and particle UVs run past 1.0 for an atlas above 0. A missing PNG is a visible checker.
 */
public final class ClientAtlases {
	private static final int TILE = 16, ATLAS = 256;
	private static Minecraft minecraft;
	private static int[] names = new int[0];
	/** What's on the atlases, for reading: GL has the same under {@link #names}. */
	private static BufferedImage[] images = new BufferedImage[0];
	private static final List<Animated> ANIMATED = new ArrayList<>();
	/** Tiles missing a PNG, for the harness. */
	public static final List<String> MISSING = new ArrayList<>();

	private ClientAtlases() {
	}

	/** A strip of 16x16 frames, one tile. */
	private static final class Animated extends DynamicTexture {
		final int atlas;
		final byte[][] frames;
		int frame;
		int wait;

		Animated(int index, byte[][] frames) {
			super(index & 0xFF);
			this.atlas = index >> 8;
			this.frames = frames;
		}

		@Override
		public void tick() {
			if (++wait < 2) return;
			wait = 0;
			frame = (frame + 1) % frames.length;
			System.arraycopy(frames[frame], 0, pixels, 0, pixels.length);
		}

		@Override
		public void bind(TextureManager manager) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, names[atlas]);
		}
	}

	public static void load(Minecraft mc) {
		minecraft = mc;
		int count = Atlases.count();
		if (count == 0) return;
		names = new int[count + 1];
		images = stitch(count);
		for (int a = 1; a <= count; a++) names[a] = mc.textureManager.load(images[a]);
		for (Animated t : ANIMATED) mc.textureManager.addDynamicTexture(t);
		System.out.println("[faithful-core] " + Atlases.tiles().size() + " tiles in " + count + " atlases");
	}

	/** After a pack switch: new pictures under the same names. */
	public static void reload(TextureManager manager) {
		if (minecraft == null || names.length == 0) return;
		images = stitch(names.length - 1);
		for (int a = 1; a < names.length; a++) manager.load(images[a], names[a]);
	}

	private static BufferedImage[] stitch(int count) {
		BufferedImage[] images = new BufferedImage[count + 1];
		for (int a = 1; a <= count; a++) images[a] = new BufferedImage(ATLAS, ATLAS, BufferedImage.TYPE_INT_ARGB);
		BufferedImage terrain = read("/terrain.png");
		MISSING.clear();
		boolean first = ANIMATED.isEmpty();
		for (Atlases.Tile t : Atlases.tiles()) {
			BufferedImage to = images[t.index >> 8];
			int x = (t.index & 15) * TILE, y = (t.index >> 4 & 15) * TILE;
			if (t.kind.equals("vanilla")) {
				int tile = Integer.parseInt(t.name), size = terrain == null ? TILE : terrain.getWidth() / 16;
				if (terrain != null) copy(terrain, tile % 16 * size, tile / 16 * size, size, to, x, y);
				continue;
			}
			String path = "/assets/" + t.mod + "/textures/" + t.kind + "/" + t.name + ".png";
			BufferedImage from = read(path);
			if (from == null) {
				MISSING.add(path);
				System.out.println("[faithful-core] WARN No texture at " + path);
				checker(to, x, y);
				continue;
			}
			int size = from.getWidth();
			copy(from, 0, 0, size, to, x, y);
			if (first && from.getHeight() >= size * 2) ANIMATED.add(new Animated(t.index, frames(from, size)));
		}
		return images;
	}

	private static byte[][] frames(BufferedImage strip, int size) {
		int n = strip.getHeight() / size;
		byte[][] frames = new byte[n][TILE * TILE * 4];
		for (int f = 0; f < n; f++) {
			for (int py = 0; py < TILE; py++) {
				for (int px = 0; px < TILE; px++) {
					int argb = strip.getRGB(px * size / TILE, f * size + py * size / TILE), at = (py * TILE + px) * 4;
					frames[f][at] = (byte) (argb >> 16);
					frames[f][at + 1] = (byte) (argb >> 8);
					frames[f][at + 2] = (byte) argb;
					frames[f][at + 3] = (byte) (argb >> 24);
				}
			}
		}
		return frames;
	}

	/** Square, to 16 pixels by nearest: tiles here are 16, whatever a pack's sheets are. */
	private static void copy(BufferedImage from, int fx, int fy, int size, BufferedImage to, int x, int y) {
		for (int py = 0; py < TILE; py++) {
			for (int px = 0; px < TILE; px++) to.setRGB(x + px, y + py, from.getRGB(fx + px * size / TILE, fy + py * size / TILE));
		}
	}

	private static void checker(BufferedImage to, int x, int y) {
		for (int py = 0; py < TILE; py++) {
			for (int px = 0; px < TILE; px++) to.setRGB(x + px, y + py, (px / 8 + py / 8) % 2 == 0 ? 0xFFFF00FF : 0xFF000000);
		}
	}

	private static BufferedImage read(String path) {
		try (InputStream in = minecraft.texturePacks.selected.getResource(path)) {
			return in == null ? null : ImageIO.read(in);
		} catch (IOException e) {
			System.out.println("[faithful-core] WARN Could not read " + path + ": " + e);
			return null;
		}
	}

	/** A tile's pixels, ARGB: 16 across from a mod's atlas, at the pack's size from {@code sheet} for atlas 0. */
	public static int[] pixels(int atlas, int tile, String sheet) {
		if (atlas > 0 && atlas < images.length) return images[atlas].getRGB(tile % 16 * TILE, tile / 16 * TILE, TILE, TILE, null, 0, TILE);
		int[] whole = ClientNet.minecraft.textureManager.getColors(sheet);
		int width = (int) Math.sqrt(whole.length), size = width / 16;
		int[] pixels = new int[size * size];
		for (int y = 0; y < size; y++) System.arraycopy(whole, (tile / 16 * size + y) * width + tile % 16 * size, pixels, y * size, size);
		return pixels;
	}

	/** The GL name of an atlas, by a texture index's top bits; {@code vanilla} for atlas 0. */
	public static int name(int atlas, int vanilla) {
		return atlas > 0 && atlas < names.length ? names[atlas] : vanilla;
	}

	/** Binds the atlas of a mod's texture, or does nothing for vanilla's. */
	public static boolean bind(int atlas) {
		if (atlas <= 0 || atlas >= names.length) return false;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, names[atlas]);
		return true;
	}

	public static void bindTerrain() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/terrain.png"));
	}
}
