package org.studioquire.faithful.apicheck.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.pack.TexturePack;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.studioquire.faithful.apicheck.ApiCheck;
import org.studioquire.faithful.apicheck.Checks;
import org.studioquire.faithful.apicheck.ApiCheck.Critter;
import org.studioquire.faithful.apicheck.ApiCheck.Kiln;
import org.studioquire.faithful.apicheck.ApiCheck.MachineEntity;
import org.studioquire.faithful.apicheck.ApiCheck.Raft;
import org.studioquire.oldfaithful.core.api.Containers;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.client.Client;
import org.studioquire.oldfaithful.core.api.client.Hud;
import org.studioquire.oldfaithful.core.api.client.Renderers;
import org.studioquire.oldfaithful.core.mixin.client.SoundEntryAccessor;
import org.studioquire.oldfaithful.selftest.ScreenCapture;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;

/** The same content as a real client sees it, on a server through each path, and in single player. */
public final class ApiWorldCheck implements WorldCheck {
	/** Only under libraryApiTest. As one of libraryTest's subjects no one answers /apitest, and its scenes would build on other mods' test plots. */
	private static final boolean OURS = Boolean.getBoolean("apicheck.clients");
	private static final int Y = 100, RED = 0xFFFF0000;
	private SelfTest results;
	private int stage;
	private int wait;
	private int x, y, z;
	private int readsBefore;
	private final List<String> heard = new ArrayList<>();

	@Override
	public void start(Minecraft mc, SelfTest results) {
		if (!OURS) return;
		this.results = results;
		stage = mc.world.isRemote ? 100 : 0;
		mc.world.addEventListener(new Heard());
		soundTables(mc);
		client(mc);
	}

	private void client(Minecraft mc) {
		results.check("Client.minecraft() is the game", Client.minecraft() == mc);
		results.check("entering a world is a world change, on a server too: " + ApiClient.worldChanges, ApiClient.enteredFrom == null && ApiClient.enteredTo == mc.world);
		ApiClient.chosenCorner = Hud.Corner.BOTTOM_LEFT;

		int stone = Renderers.averageColor(Renderers.tilePixels(Block.STONE, 1, 0));
		results.check("a vanilla tile reads from the pack's sheet: " + Integer.toHexString(stone), (stone >> 16 & 255) == (stone & 255) && (stone & 255) > 80);
		results.check("a mod block's tile is its PNG", same(Renderers.tilePixels(ApiCheck.ore.get(), 1, 0), "block/ore.png"));
		results.check("as is its item's", same(Renderers.tilePixels(new ItemStack(ApiCheck.ore.get())), "block/ore.png"));
		results.check("and a mod item's", same(Renderers.tilePixels(new ItemStack(ApiCheck.ingot.get())), "item/ingot.png"));
		results.check("a vanilla item's comes from its sheet", Renderers.averageColor(Renderers.tilePixels(new ItemStack(Item.DIAMOND))) >= 0);
		int[] clear = new int[4];
		results.check("a clear tile has no average", Renderers.averageColor(clear) == -1);
		if (!mc.world.isRemote) packSwitch(mc);
	}

	/** As the texture packs screen does it: a pack's picture at a mod tile's path, the mod's own where it has none, and back. */
	private void packSwitch(Minecraft mc) {
		TexturePack before = mc.texturePacks.selected;
		try {
			File file = FabricLoader.getInstance().getGameDir().resolve("texturepacks/api-check.zip").toFile();
			file.getParentFile().mkdirs();
			BufferedImage red = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int i = 0; i < 256; i++) red.setRGB(i % 16, i / 16, RED);
			try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
				zip.putNextEntry(new ZipEntry("assets/api-check/textures/block/ore.png"));
				ImageIO.write(red, "png", zip);
				zip.closeEntry();
			}
			mc.texturePacks.reload();
			for (Object pack : mc.texturePacks.getAvailable()) {
				if (file.getName().equals(((TexturePack) pack).name)) mc.texturePacks.select((TexturePack) pack);
			}
			results.check("the pack is on", mc.texturePacks.selected != before);
			mc.textureManager.reload();
			int[] ore = Renderers.tilePixels(ApiCheck.ore.get(), 1, 0);
			results.check("a pack's picture replaces a mod's tile at its path", Arrays.stream(ore).allMatch(p -> p == RED));
			results.check("a tile the pack doesn't have stays the mod's", same(Renderers.tilePixels(new ItemStack(ApiCheck.ingot.get())), "item/ingot.png"));
		} catch (IOException e) {
			results.fail("a texture pack couldn't be made", e);
		}
		mc.texturePacks.select(before);
		mc.textureManager.reload();
		results.check("and off again, the mod's own is back", same(Renderers.tilePixels(ApiCheck.ore.get(), 1, 0), "block/ore.png"));
	}

	private static boolean same(int[] pixels, String texture) {
		try (InputStream in = ApiWorldCheck.class.getResourceAsStream("/assets/api-check/textures/" + texture)) {
			BufferedImage image = ImageIO.read(in);
			return pixels.length == 256 && Arrays.equals(pixels, image.getRGB(0, 0, 16, 16, null, 0, 16));
		} catch (IOException e) {
			return false;
		}
	}

	private final class Heard implements GameEventListener {
		@Override
		public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
			heard.add(sound);
		}

		@Override
		public void blockUpdate(int x, int y, int z) {
		}

		@Override
		public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		}

		@Override
		public void addParticle(String particle, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		}

		@Override
		public void notifyEntityAdded(Entity entity) {
		}

		@Override
		public void notifyEntityRemoved(Entity entity) {
		}

		@Override
		public void notifyAmbientDarknessChanged() {
		}

		@Override
		public void playStreaming(String stream, int x, int y, int z) {
		}

		@Override
		public void updateBlockEntity(int x, int y, int z, BlockEntity blockEntity) {
		}

		@Override
		public void worldEvent(PlayerEntity player, int event, int x, int y, int z, int data) {
		}
	}

	private static boolean opens(Sound sound) {
		try (InputStream in = sound.soundFile.openStream()) {
			return in.read() >= 0;
		} catch (IOException e) {
			return false;
		}
	}

	/** Names mean nothing at run time: the manager's three tables, in the order it declares them. */
	private void soundTables(Minecraft mc) {
		List<SoundEntry> tables = new ArrayList<>();
		try {
			for (Field f : SoundManager.class.getDeclaredFields()) {
				if (f.getType() == SoundEntry.class) {
					f.setAccessible(true);
					tables.add((SoundEntry) f.get(mc.soundManager));
				}
			}
		} catch (ReflectiveOperationException e) {
			results.fail("the sound tables", e);
			return;
		}
		SoundEntry sounds = tables.get(0), streaming = tables.get(1), music = tables.get(2);
		Sound ping = sounds.get("api-check.ping");
		results.check("a mod's sound is loaded from its jar: " + (ping == null ? null : ping.soundFile), ping != null && opens(ping));
		List<?> variants = (List<?>) ((SoundEntryAccessor) sounds).faithful$sets().get("api-check.ping");
		results.check("numbered files are variants of one sound", variants != null && variants.size() == 2);
		results.check("a nested path has dots", sounds.get("api-check.machine.hum") != null);
		results.check("vanilla's own tables aren't in the way", music.getSounds() != null && opens(music.getSounds()));
		Sound disc = streaming.get("api-check.disc");
		results.check("a streaming sound", disc != null && opens(disc));
		mc.soundManager.playSound("api-check.ping", (float) mc.player.x, (float) mc.player.y, (float) mc.player.z, 1.0F, 1.0F);
	}

	private void say(Minecraft mc, String command) {
		mc.player.sendChatMessage("/apitest " + command);
	}

	private MachineEntity machine(World w, int i) {
		return (MachineEntity) w.getBlockEntity(x + i, Y, z + 1);
	}

	private <T> T entity(World w, Class<T> type) {
		for (Object e : w.entities) {
			if (type.isInstance(e)) return type.cast(e);
		}
		return null;
	}

	private void next(int ticks) {
		stage++;
		wait = ticks;
	}

	@Override
	public boolean tick(Minecraft mc) {
		if (!OURS) return true;
		if (wait > 0) {
			wait--;
			return false;
		}
				return mc.world.isRemote ? onServer(mc) : singlePlayer(mc);
	}

	private boolean onServer(Minecraft mc) {
		World w = mc.world;
		switch (stage) {
			case 100: {
				x = w.getSpawnPos().x >> 4 << 4;
				z = w.getSpawnPos().z >> 4 << 4;
				if (!w.isPosLoaded(x, Y, z + 1) || machine(w, 0) == null) return false;
				boolean all = true;
				for (int i = 0; i < 14; i++) all &= machine(w, i) != null && machine(w, i).value == 11 + i;
				results.check("block entities came with their chunk: " + machine(w, 0).value + " .. " + machine(w, 13).value, all);
				readsBefore = machine(w, 0).reads;
				say(mc, "one");
				next(20);
				return false;
			}
			case 101:
				results.check("a single block's update: " + machine(w, 0).value, machine(w, 0).value == 21 && machine(w, 0).reads == readsBefore + 1);
				readsBefore = machine(w, 1).reads;
				say(mc, "few");
				next(20);
				return false;
			case 102: {
				boolean all = true;
				for (int i = 1; i <= 3; i++) all &= machine(w, i).value == 30 + i;
				results.check("a 2-9 update reached each: " + machine(w, 1).value + ", " + machine(w, 2).value + ", " + machine(w, 3).value, all);
				results.check("once each: " + (machine(w, 1).reads - readsBefore), machine(w, 1).reads == readsBefore + 1);
				say(mc, "many");
				next(20);
				return false;
			}
			case 103: {
				boolean all = true;
				for (int i = 0; i < 12; i++) all &= machine(w, i).value == 40 + i;
				results.check("a ten-or-more update's chunk part", all);
				say(mc, "common");
				say(mc, "tools");
				say(mc, "spawn");
				next(40);
				return false;
			}
			case 104: {
				Critter critter = entity(w, Critter.class);
				results.check("a mod's mob reached the client", critter != null);
				results.check("with its data: " + (critter == null ? null : critter.note), critter != null && "from the server".equals(critter.note));
				results.check("and its tracked data: " + (critter == null ? null : critter.mood()), critter != null && critter.mood() == 5);
				results.check("in the right place", critter != null && critter.getSquaredDistance(mc.player.x + 2, critter.y, mc.player.z) < 36.0);
				Raft raft = entity(w, Raft.class);
				results.check("a non-living one, a boat subclass, as itself", raft != null && raft.planks == 7);
				mc.player.yaw = -90.0F;
				mc.player.pitch = 20.0F;
				next(20);
				return false;
			}
			case 105:
				results.check("its renderer drew it: " + ApiClient.critterFrames, ApiClient.critterFrames > 0);
				results.check("a block entity's renderer ran: " + ApiClient.machineFrames, ApiClient.machineFrames > 0);
				say(mc, "fill");
				say(mc, "open");
				next(20);
				return false;
			case 106: {
				ScreenHandler handler = mc.player.currentScreenHandler;
				results.check("a modded screen opened on a server", mc.currentScreen instanceof ApiClient.KilnScreen && handler instanceof Kiln);
				ItemStack in = handler.getSlot(0).getStack();
				results.check("its slots came in vanilla's packets: " + in, in != null && in.itemId == ApiCheck.ingot.get().id && in.count == 4);
				mc.interactionManager.clickSlot(handler.syncId, 0, 0, false, mc.player);
				say(mc, "heat 77");
				next(15);
				return false;
			}
			case 107: {
				results.check("a property came through: " + ((Kiln) mc.player.currentScreenHandler).heat, ((Kiln) mc.player.currentScreenHandler).heat == 77);
				say(mc, "clicked");
				next(10);
				return false;
			}
			case 108:
				mc.setScreen(null);
				say(mc, "sound");
				next(15);
				return false;
			case 109:
				results.check("a sound reached the client: " + heard, heard.contains("api-check.ping"));
				corner();
				return true;
			default:
				return true;
		}
	}

	private boolean singlePlayer(Minecraft mc) {
		World w = mc.world;
		int px = (int) Math.floor(mc.player.x), py = (int) Math.floor(mc.player.y), pz = (int) Math.floor(mc.player.z);
		switch (stage) {
			case 0:
				Checks.recipes(results::check);
				Checks.furnace(w, px + 2, 100, pz, results::check);
				Checks.storage(w, px + 2, 101, pz, results::check);
				Checks.tools(w, mc.player, px + 2, 102, pz, (bx, by, bz) -> mc.interactionManager.breakBlock(bx, by, bz, 1), results::check);
				heard.clear();
				Sounds.play(w, mc.player.x, mc.player.y, mc.player.z, "api-check.ping", 1.0F, 1.0F);
				results.check("a sound in single player", heard.contains("api-check.ping"));
				next(2);
				return false;
			case 1: {
				// Shapes in front of the player, the mod's mob and a helmet on, for looking at.
				x = px;
				y = py;
				z = pz;
				for (int dx = -3; dx <= 3; dx++) {
					w.setBlock(px + dx, py - 1, pz + 3, Block.STONE.id);
					w.setBlock(px + dx, py, pz + 3, (dx & 1) == 0 ? ApiCheck.post.get().id : 0);
					w.setBlock(px + dx, py + 1, pz + 3, dx == 0 ? ApiCheck.machine.get().id : 0);
				}
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = 0; dz < 3; dz++) w.setBlock(px + dx, py - 1, pz + dz, Block.STONE.id);
				}
				Critter critter = new Critter(w);
				critter.setPositionAndAngles(px + 1.5, py, pz + 2.5, 180.0F, 0.0F);
				critter.note = "single player";
				w.spawnEntity(critter);
				mc.player.yaw = 0;
				mc.player.pitch = 0;
				mc.player.inventory.selectedSlot = 0;
				mc.player.inventory.main[0] = new ItemStack(ApiCheck.post.get(), 3);
				mc.player.inventory.main[1] = new ItemStack(ApiCheck.pick.get());
				mc.player.inventory.main[2] = new ItemStack(ApiCheck.machine.get());
				mc.player.inventory.main[3] = new ItemStack(ApiCheck.helmet.get());
				mc.player.inventory.armor[3] = new ItemStack(ApiCheck.helmet.get());
				next(60);
				return false;
			}
			case 2:
				mc.setScreen(new net.minecraft.client.gui.screen.Screen() {
				});
				next(5);
				return false;
			case 3:
				ScreenCapture.request(new File("shots/api-world.png"), 0, 0);
				results.check("a shape's type is its own", ApiCheck.post.get().getRenderType() >= 18);
				Critter here = entity(w, Critter.class);
				results.check("the mob is in the world: " + (here == null ? null : here.x + " " + here.y + " " + here.z + " from " + mc.player.x + " " + mc.player.y + " " + mc.player.z), here != null);
				next(10);
				return false;
			case 4:
				mc.setScreen(null);
				mc.options.thirdPerson = true;
				next(20);
				return false;
			case 5:
				mc.setScreen(new net.minecraft.client.gui.screen.Screen() {
				});
				next(5);
				return false;
			case 6:
				ScreenCapture.request(new File("shots/api-armor.png"), 0, 0);
				next(10);
				return false;
			case 7:
				mc.options.thirdPerson = false;
				mc.setScreen(null);
				next(2);
				return false;
			case 8: {
				MachineEntity m = (MachineEntity) w.getBlockEntity(x, y + 1, z + 3);
				m.setStack(0, new ItemStack(ApiCheck.ingot.get(), 4));
				Containers.open(mc.player, "api-check:kiln", m);
				results.check("a modded screen opened directly in single player", mc.currentScreen instanceof ApiClient.KilnScreen && mc.player.currentScreenHandler instanceof Kiln);
				next(10);
				return false;
			}
			case 9: {
				ScreenHandler handler = mc.player.currentScreenHandler;
				mc.interactionManager.clickSlot(handler.syncId, 0, 0, false, mc.player);
				results.check("a click takes the stack", mc.player.inventory.getCursorStack() != null && mc.player.inventory.getCursorStack().count == 4 && handler.getSlot(0).getStack() == null);
				mc.player.inventory.setCursorStack(null);
				ScreenCapture.request(new File("shots/api-kiln.png"), 0, 0);
				next(10);
				return false;
			}
			case 10:
				mc.setScreen(null);
				corner();
				return true;
			default:
				return true;
		}
	}

	private void corner() {
		results.check("a layer draws in the corner chosen since it was added: " + ApiClient.drawnCorner, ApiClient.drawnCorner == Hud.Corner.BOTTOM_LEFT);
	}
}
