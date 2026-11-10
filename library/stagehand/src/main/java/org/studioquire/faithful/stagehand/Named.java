package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Names;

/** Names in a script to the game's things, and back. A mod's have its id in them, {@code water-bottles:water_bottle}; beta's are plain, {@code iron_sword}, and {@code minecraft:iron_sword} serves too. */
final class Named {
	static final String[] ENTITIES = {"Boat", "Chicken", "Cow", "Creeper", "Ghast", "Giant", "Minecart", "Pig", "PigZombie", "PrimedTnt", "Sheep", "Skeleton", "Slime", "Spider", "Squid", "Wolf", "Zombie"};

	private Named() {
	}

	/** A name with what may follow it: {@code wool@14}. */
	static final class Spec {
		final String name;
		final int meta;

		Spec(String word) {
			int at = word.indexOf('@');
			String plain = (at < 0 ? word : word.substring(0, at)).toLowerCase(Locale.ROOT);
			name = plain.startsWith("minecraft:") ? plain.substring("minecraft:".length()) : plain;
			if (at < 0) {
				meta = 0;
				return;
			}
			try {
				meta = Integer.parseInt(word.substring(at + 1));
			} catch (NumberFormatException e) {
				throw new Failure("bad_number", "what follows @ is a number: " + word);
			}
			if (meta < 0) throw new Failure("bad_number", "what follows @ can't be negative: " + word);
		}
	}

	private static Item item(String name) {
		return name.contains(":") ? Names.item(name) : Vanilla.item(name);
	}

	/** The item a script names and the damage it asks for, or the script's error. */
	static Item item(Spec spec) {
		Item item = item(spec.name);
		if (item == null) throw new Failure("unknown_item", "no item called " + spec.name + similar(spec.name, itemNames()));
		int most = mostDamage(item);
		if (spec.meta > most) throw new Failure("bad_number", spec.name + " takes @0 to @" + most + ", not @" + spec.meta);
		return item;
	}

	/**
	 * How much damage an item can carry without breaking the game later. Beta's dyes and slabs read their names from a table by it, unchecked, so a slab of damage 4 crashes a client when it's next
	 * pointed at. A mod's item is the mod's to answer for.
	 */
	private static int mostDamage(Item item) {
		if (item.isDamageable()) return item.getMaxDamage();
		if (item == Item.MAP || Names.of(item) != null) return 32767;
		if (item.id == Block.SLAB.id) return 3;
		return item.hasSubtypes() ? 15 : 0;
	}

	/** The block's id a script names and the meta it asks for, or the script's error. 0 is air. */
	static int block(Spec spec) {
		if (spec.name.equals("air")) {
			if (spec.meta != 0) throw new Failure("bad_number", "air takes no @");
			return 0;
		}
		Block block = spec.name.contains(":") ? Names.block(spec.name) : Vanilla.block(spec.name);
		if (block == null) throw new Failure("unknown_block", "no block called " + spec.name + similar(spec.name, blockNames()));
		// A piston finds its facing in a table of six, unchecked, whenever a neighbour changes, and with the extended bit it wants a head that isn't there.
		int most = block instanceof PistonBlock ? 5 : 15;
		if (spec.meta > most) throw new Failure("bad_number", spec.name + " takes @0 to @" + most + ", not @" + spec.meta);
		return block.id;
	}

	static String name(Item item) {
		String mod = Names.of(item);
		if (mod != null) return mod;
		String vanilla = Vanilla.name(item);
		return vanilla != null ? vanilla : "item#" + item.id;
	}

	static String block(Block block) {
		String mod = Names.of(block);
		if (mod != null) return mod;
		String vanilla = Vanilla.name(block);
		return vanilla != null ? vanilla : "block#" + block.id;
	}

	/** {@code zombie}, {@code pig_zombie} and {@code PigZombie} are all fine; a mod's goes by its whole name. */
	static String entityId(String word) {
		if (word.contains(":")) return word;
		String squashed = word.replace("_", "");
		for (String id : ENTITIES) {
			if (id.equalsIgnoreCase(squashed)) return id;
		}
		throw new Failure("unknown_entity", "no entity called " + word + "; there's " + String.join(", ", ENTITIES).toLowerCase(Locale.ROOT) + ", and mods' by their whole names");
	}

	static String entity(Entity entity) {
		String id = EntityRegistry.getId(entity);
		return id == null ? entity.getClass().getSimpleName() : id;
	}

	static Entity makeEntity(String id, World world) {
		Entity entity = EntityRegistry.create(id, world);
		if (entity == null) throw new Failure("unknown_entity", "no entity called " + id);
		return entity;
	}

	static List<String> itemNames() {
		List<String> all = new ArrayList<>(Vanilla.itemNames());
		all.addAll(Names.items());
		return all;
	}

	static List<String> blockNames() {
		List<String> all = new ArrayList<>(Vanilla.blockNames());
		all.add("air");
		all.addAll(Names.blocks());
		return all;
	}

	private static String similar(String name, List<String> names) {
		String word = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
		List<String> like = new ArrayList<>();
		for (String candidate : names) {
			if (candidate.contains(word) && like.size() < 8) like.add(candidate);
		}
		return like.isEmpty() ? "" : "; perhaps " + String.join(", ", like);
	}
}
