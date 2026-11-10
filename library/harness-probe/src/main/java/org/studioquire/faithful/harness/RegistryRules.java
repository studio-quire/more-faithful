package org.studioquire.faithful.harness;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import org.studioquire.oldfaithful.core.api.ChoiceRule;
import org.studioquire.oldfaithful.core.api.Rule;
import org.studioquire.oldfaithful.core.impl.CommandTable;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.CoreNet;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.content.Atlases;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * The harness's second stage, in the game: everything the mods declared, against what a declaration has to be. Duplicates of a channel, a
 * rule or a command already stop the game as it starts, which the task reports by the mod named there.
 */
final class RegistryRules {
	private static final Pattern PATH = Pattern.compile("[a-z0-9_-]+(/[a-z0-9_-]+)*");
	private static final int VANILLA_BLOCKS = 96, VANILLA_ITEMS_END = 359;
	/** Blocks vanilla keeps an item entry for and never gives you: a stack of one is a stand-in no player or vanilla reader can use. */
	private static final Set<Integer> NOT_REALLY_ITEMS = new HashSet<>(Arrays.asList(0, 8, 9, 10, 11, 26, 34, 36, 43, 51, 55, 59, 60, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94));
	/** Stand-ins that would act in a vanilla reader's world: fire that spreads, liquid that flows, a piston's parts. */
	private static final Set<Integer> UNSAFE_STAND_INS = new HashSet<>(Arrays.asList(8, 10, 34, 36, 51));
	private static final Set<String> VANILLA_COMMANDS = new HashSet<>(Arrays.asList("help", "?", "list", "stop", "save-all", "save-on", "save-off", "op", "deop", "ban", "pardon",
			"ban-ip", "pardon-ip", "kick", "tp", "give", "tell", "say", "time", "whitelist", "me", "kill"));

	private final Map<String, Properties> lang = new HashMap<>();
	private int blocks, items;

	private RegistryRules() {
	}

	/** Every rule that can be read on either side; what's in the mods goes to {@code registry.txt} for the task. */
	static void run(Path dir) throws IOException {
		new RegistryRules().check(dir);
	}

	private void check(Path dir) throws IOException {
		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dir.resolve("registry.txt")))) {
			for (int id = 0; id < Block.BLOCKS.length; id++) checkBlock(id, out);
			for (int id = 0; id < Item.ITEMS.length; id++) checkItem(id, out);
			for (CoreNet.ChannelImpl c : CoreNet.channels()) {
				String mod = Owners.namespace(c.name());
				name(mod, Owners.path(c.name()), "the channel " + c.name());
				if (!mod.equals(Core.ID) && !mod.equals("harness-probe")) out.println("channel\t" + c.name());
			}
			for (Rule r : Rules.all()) {
				String mod = Owners.namespace(r.id());
				name(mod, Owners.path(r.id()), "the rule " + r.id());
				String prefix = "rule." + r.id().replace(':', '.');
				text(mod, prefix + ".name", "the rule " + r.id());
				text(mod, prefix + ".hint", "the rule " + r.id());
				if (r instanceof ChoiceRule) {
					for (String v : r.values()) text(mod, prefix + "." + v, "the rule " + r.id());
				}
				out.println("rule\t" + r.id());
			}
			for (String c : CommandTable.names()) {
				String mod = CommandTable.mod(c);
				if (VANILLA_COMMANDS.contains(c)) Out.fail(mod, "command-shadows-vanilla", "/" + c + " comes ahead of the server's own");
				if (!mod.equals("harness-probe")) out.println("command\t" + c + "\t" + mod);
			}
			for (Atlases.Tile t : Atlases.tiles()) {
				if (t.mod != null) checkTile(t);
			}
		}
		try {
			RecipeRules.run();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		Out.info("registry: " + blocks + " blocks, " + items + " items");
	}

	private void name(String mod, String path, String what) {
		if (!PATH.matcher(path).matches()) Out.fail(mod, "names", what + ": a name of lowercase letters, digits, - and _");
	}

	private void checkBlock(int id, PrintWriter out) {
		Block block = Block.BLOCKS[id];
		if (block == null) return;
		String name = ContentRegistry.blockName(id);
		if (name == null) {
			if (id > VANILLA_BLOCKS) Out.fail(Owners.of(block.getClass()), "unregistered-block", block.getClass().getName() + " sits at id " + id + " outside the registry: declare it with Content.block");
			return;
		}
		blocks++;
		String mod = Owners.namespace(name), path = Owners.path(name);
		name(mod, path, "the block " + name);
		if (id < ContentRegistry.FIRST_BLOCK || id >= ContentRegistry.BLOCK_LIMIT || (id & 0xFF) == 0 || id == 2256 || id == 2257) Out.fail(mod, "block-number", name + " holds " + id);
		// A block with no item is never named on screen.
		if (Item.ITEMS[id] != null) text(mod, block.getTranslationKey() + ".name", "the block " + name);

		int standIn = ContentRegistry.STAND_IN[id] & 0xFF, standInMeta = ContentRegistry.STAND_IN[id] >> 8;
		if (standIn != 0 && Block.BLOCKS[standIn] == null) Out.fail(mod, "stand-in", name + "'s stand-in is the empty id " + standIn);
		if (standInMeta > 15) Out.fail(mod, "stand-in", name + "'s stand-in meta is " + standInMeta);
		if (Block.BLOCKS_WITH_ENTITY[standIn]) Out.fail(mod, "stand-in", name + "'s stand-in has a block entity");
		if (UNSAFE_STAND_INS.contains(standIn)) Out.fail(mod, "stand-in", name + "'s stand-in " + standIn + " would act in a vanilla reader's world");

		int atlas = ContentRegistry.BLOCK_ATLAS[id];
		Set<Integer> atlases = new TreeSet<>();
		for (int side = 0; side < 6; side++) {
			for (int meta = 0; meta < 16; meta++) {
				try {
					int texture = block.getTexture(side, meta);
					if (texture < 0) Out.fail(mod, "texture", name + " has no texture for side " + side + ", meta " + meta);
					atlases.add(texture >> 8);
				} catch (RuntimeException e) {
					Out.fail(mod, "texture", name + " threw for side " + side + ", meta " + meta + ": " + e);
				}
			}
		}
		atlases.remove(atlas);
		if (!atlases.isEmpty()) Out.fail(mod, "tiles-in-one-atlas", name + " is drawn from atlas " + atlas + " but has tiles in " + atlases + ": every side and meta must come from Spec.texture or Spec.vanillaTile");

		String item = "-\t-";
		if (Item.ITEMS[id] != null) {
			int standInItem = ContentRegistry.itemStandInId(id);
			item = standInItem + "\t" + ContentRegistry.itemStandInDamage(id);
			if (standInItem == 0 || Item.ITEMS[standInItem] == null) Out.fail(mod, "item-stand-in", name + "'s item has the stand-in " + standInItem + ", which is no item");
			if (NOT_REALLY_ITEMS.contains(standInItem)) {
				Out.fail(mod, "item-stand-in", name + "'s item stands in as block " + standInItem + ", which vanilla never gives as an item: a block with no item of its own (a lit variant, a crop) declares noItem()");
			}
		}
		out.println("block\t" + name + "\t" + id + "\t" + standIn + "\t" + standInMeta + "\t" + item);
	}

	private void checkItem(int id, PrintWriter out) {
		Item item = Item.ITEMS[id];
		if (item == null || id <= VANILLA_ITEMS_END || id == 2256 || id == 2257 || ContentRegistry.modBlock(id)) return;
		String name = ContentRegistry.itemName(id);
		if (name == null) {
			Out.fail(Owners.of(item.getClass()), "unregistered-item", item.getClass().getName() + " sits at id " + id + " outside the registry: declare it with Content.item");
			return;
		}
		items++;
		String mod = Owners.namespace(name), path = Owners.path(name);
		name(mod, path, "the item " + name);
		if (id < ContentRegistry.FIRST_ITEM) Out.fail(mod, "item-number", name + " holds " + id);
		text(mod, item.getTranslationKey() + ".name", "the item " + name);
		int standIn = ContentRegistry.itemStandInId(id);
		if (standIn == 0 || standIn >= ContentRegistry.FIRST_BLOCK || Item.ITEMS[standIn] == null) Out.fail(mod, "item-stand-in", name + "'s stand-in " + standIn + " isn't a vanilla item");
		if (NOT_REALLY_ITEMS.contains(standIn)) Out.fail(mod, "item-stand-in", name + " stands in as block " + standIn + ", which vanilla never gives as an item");
		if (item.getMaxCount() < 1 || item.getMaxCount() > 64) Out.fail(mod, "item-stack-size", name + " stacks to " + item.getMaxCount());
		// The item's texture is the client's: only a block's is asked on a server.
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			try {
				if (item.getTextureId(0) < 0) Out.fail(mod, "texture", name + " has no texture");
			} catch (RuntimeException e) {
				Out.fail(mod, "texture", name + " threw for its texture: " + e);
			}
		}
		out.println("item\t" + name + "\t" + id + "\t" + standIn + "\t" + ContentRegistry.itemStandInDamage(id));
	}

	private void checkTile(Atlases.Tile t) {
		String file = "assets/" + t.mod + "/textures/" + t.kind + "/" + t.name + ".png";
		Optional<Path> path = FabricLoader.getInstance().getModContainer(t.mod).flatMap(m -> m.findPath(file));
		if (!path.isPresent() || !Files.isRegularFile(path.get())) {
			Out.fail(t.mod, "texture", "no " + file);
			return;
		}
		try (InputStream in = Files.newInputStream(path.get())) {
			BufferedImage image = ImageIO.read(in);
			if (image == null) Out.fail(t.mod, "texture", file + " isn't a PNG");
			else if (image.getWidth() != 16 || image.getHeight() % 16 != 0) Out.fail(t.mod, "texture", file + " is " + image.getWidth() + "x" + image.getHeight() + ", not 16x16 or a strip of 16x16 frames");
		} catch (IOException e) {
			Out.fail(t.mod, "texture", file + ": " + e);
		}
	}

	private void text(String mod, String key, String what) {
		String value = lang(mod).getProperty(key);
		if (value == null || value.trim().isEmpty()) Out.fail(mod, "lang", what + " has no " + key + " in assets/" + mod + "/lang/en_US.lang");
	}

	private Properties lang(String mod) {
		return lang.computeIfAbsent(mod, m -> {
			Properties p = new Properties();
			Optional<Path> file = FabricLoader.getInstance().getModContainer(m).flatMap(c -> c.findPath("assets/" + m + "/lang/en_US.lang"));
			if (file.isPresent() && Files.isRegularFile(file.get())) {
				try (InputStream in = Files.newInputStream(file.get())) {
					p.load(in);
				} catch (IOException e) {
					Out.fail(m, "lang", "unreadable: " + e);
				}
			}
			return p;
		});
	}
}
