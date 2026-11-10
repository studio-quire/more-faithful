package org.studioquire.oldfaithful.core.api;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;

/**
 * Tools, their tiers, and which blocks want which. One shared order of levels for everyone: wood {@value #LEVEL_WOOD}, stone {@value #LEVEL_STONE}, iron
 * {@value #LEVEL_IRON}, diamond {@value #LEVEL_DIAMOND}, so a mod's tier fits between. A tool has a class ({@code "pickaxe"}, {@code "axe"}, {@code "shovel"},
 * {@code "sword"}, {@code "shears"}, {@code "hoe"}, or a mod's own) and a tier; a block says what it wants on its side, so a mod's pick works on a
 * vanilla block, a vanilla pick on a mod's ore, and mod A's pick on mod B's ore, with neither knowing the other.
 * <pre>{@code
 * static final Tools.Tier MY_TIER = Tools.tier("my-mod:my_tier", 25, 7.0F, 600, 2);  // in content
 * c.item("my_pickaxe", Tools.pickaxe(MY_TIER, "my_pickaxe"));                       // the texture's name
 * Tools.requires(ore.get(), "pickaxe", Tools.LEVEL_IRON);                           // in init
 * }</pre>
 * A subclass of {@link Pickaxe} and the rest is a {@code PickaxeItem}, so anything looking for one finds it. A tool of a mod's own class implements {@link Tool}.
 */
public final class Tools {
	public static final int LEVEL_WOOD = 0, LEVEL_STONE = 10, LEVEL_IRON = 20, LEVEL_DIAMOND = 30;

	/** What vanilla's {@code ToolMaterial} would be, without being an enum. */
	public static final class Tier {
		public final String name;
		public final int level;
		public final float speed;
		public final int durability;
		public final int damage;

		Tier(String name, int level, float speed, int durability, int damage) {
			this.name = name;
			this.level = level;
			this.speed = speed;
			this.durability = durability;
			this.damage = damage;
		}
	}

	/** An item that's a tool here. */
	public interface Tool {
		String toolClass();

		Tier tier();
	}

	private static final Map<String, Tier> TIERS = new HashMap<>();
	public static final Tier WOOD = tier("minecraft:wood", LEVEL_WOOD, 2.0F, 59, 0);
	public static final Tier STONE = tier("minecraft:stone", LEVEL_STONE, 4.0F, 131, 1);
	public static final Tier IRON = tier("minecraft:iron", LEVEL_IRON, 6.0F, 250, 2);
	public static final Tier DIAMOND = tier("minecraft:diamond", LEVEL_DIAMOND, 8.0F, 1561, 3);
	public static final Tier GOLD = tier("minecraft:gold", LEVEL_WOOD, 12.0F, 32, 0);
	private static final Tier SHEARS = tier("minecraft:shears", LEVEL_WOOD, 15.0F, 238, 0);

	/** A block's wishes, one per tool class. */
	private static final class Rule {
		final int level;
		final boolean required, faster;

		Rule(int level, boolean required, boolean faster) {
			this.level = level;
			this.required = required;
			this.faster = faster;
		}
	}

	private static final Map<Integer, Map<String, Rule>> RULES = new HashMap<>();
	private static final Map<Integer, Tool> VANILLA_TOOLS = new HashMap<>();
	private static final BitSet SHEARS_WEAR = new BitSet();

	private static final class VanillaTool implements Tool {
		final String toolClass;
		final Tier tier;

		VanillaTool(String toolClass, Tier tier) {
			this.toolClass = toolClass;
			this.tier = tier;
		}

		@Override
		public String toolClass() {
			return toolClass;
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	private static void vanilla(String toolClass, Item wood, Item stone, Item iron, Item diamond, Item gold) {
		VANILLA_TOOLS.put(wood.id, new VanillaTool(toolClass, WOOD));
		VANILLA_TOOLS.put(stone.id, new VanillaTool(toolClass, STONE));
		VANILLA_TOOLS.put(iron.id, new VanillaTool(toolClass, IRON));
		VANILLA_TOOLS.put(diamond.id, new VanillaTool(toolClass, DIAMOND));
		VANILLA_TOOLS.put(gold.id, new VanillaTool(toolClass, GOLD));
	}

	/** Levels vanilla's {@code PickaxeItem.isSuitableFor} asks for. Not faster for it: obsidian and redstone ore aren't on vanilla's list of what a pick speeds up. */
	private static void vanillaNeeds(int level, Block... blocks) {
		for (Block b : blocks) RULES.computeIfAbsent(b.id, id -> new HashMap<>()).put("pickaxe", new Rule(level, true, false));
	}

	static {
		vanilla("pickaxe", Item.WOODEN_PICKAXE, Item.STONE_PICKAXE, Item.IRON_PICKAXE, Item.DIAMOND_PICKAXE, Item.GOLDEN_PICKAXE);
		vanilla("axe", Item.WOODEN_AXE, Item.STONE_AXE, Item.IRON_AXE, Item.DIAMOND_AXE, Item.GOLDEN_AXE);
		vanilla("shovel", Item.WOODEN_SHOVEL, Item.STONE_SHOVEL, Item.IRON_SHOVEL, Item.DIAMOND_SHOVEL, Item.GOLDEN_SHOVEL);
		vanilla("sword", Item.WOODEN_SWORD, Item.STONE_SWORD, Item.IRON_SWORD, Item.DIAMOND_SWORD, Item.GOLDEN_SWORD);
		vanilla("hoe", Item.WOODEN_HOE, Item.STONE_HOE, Item.IRON_HOE, Item.DIAMOND_HOE, Item.GOLDEN_HOE);
		VANILLA_TOOLS.put(Item.SHEARS.id, new VanillaTool("shears", SHEARS));
		vanillaNeeds(LEVEL_DIAMOND, Block.OBSIDIAN);
		vanillaNeeds(LEVEL_IRON, Block.DIAMOND_BLOCK, Block.DIAMOND_ORE, Block.GOLD_BLOCK, Block.GOLD_ORE, Block.REDSTONE_ORE, Block.LIT_REDSTONE_ORE);
		vanillaNeeds(LEVEL_STONE, Block.IRON_BLOCK, Block.IRON_ORE, Block.LAPIS_BLOCK, Block.LAPIS_ORE);
	}

	private Tools() {
	}

	/**
	 * @param name a full name
	 * @param level on the shared order above
	 * @param speed the multiplier on what it's right for: wood 2, stone 4, iron 6, diamond 8, gold 12
	 * @param durability uses before it breaks
	 * @param damage added to the tool class's own (wood 0, stone 1, iron 2, diamond 3); a sword counts it twice, as vanilla's does, and {@link #sword(Tier, int, String)} says otherwise
	 */
	public static Tier tier(String name, int level, float speed, int durability, int damage) {
		Tier t = new Tier(name, level, speed, durability, damage);
		if (TIERS.putIfAbsent(name, t) != null) throw new IllegalStateException("The tool tier " + name + " is declared twice");
		return t;
	}

	public static class Pickaxe extends PickaxeItem implements Tool {
		private final Tier tier;

		public Pickaxe(int id, Tier tier) {
			super(id, ToolMaterial.IRON);
			this.tier = tier;
			setMaxDamage(tier.durability);
		}

		@Override
		public float getMiningSpeedMultiplier(ItemStack stack, Block block) {
			return super.getMiningSpeedMultiplier(stack, block) > 1.0F ? tier.speed : 1.0F;
		}

		@Override
		public int getAttackDamage(Entity attacked) {
			return 2 + tier.damage;
		}

		@Override
		public String toolClass() {
			return "pickaxe";
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	public static class Axe extends AxeItem implements Tool {
		private final Tier tier;

		public Axe(int id, Tier tier) {
			super(id, ToolMaterial.IRON);
			this.tier = tier;
			setMaxDamage(tier.durability);
		}

		@Override
		public float getMiningSpeedMultiplier(ItemStack stack, Block block) {
			return super.getMiningSpeedMultiplier(stack, block) > 1.0F ? tier.speed : 1.0F;
		}

		@Override
		public int getAttackDamage(Entity attacked) {
			return 3 + tier.damage;
		}

		@Override
		public String toolClass() {
			return "axe";
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	public static class Shovel extends ShovelItem implements Tool {
		private final Tier tier;

		public Shovel(int id, Tier tier) {
			super(id, ToolMaterial.IRON);
			this.tier = tier;
			setMaxDamage(tier.durability);
		}

		@Override
		public float getMiningSpeedMultiplier(ItemStack stack, Block block) {
			return super.getMiningSpeedMultiplier(stack, block) > 1.0F ? tier.speed : 1.0F;
		}

		@Override
		public int getAttackDamage(Entity attacked) {
			return 1 + tier.damage;
		}

		@Override
		public String toolClass() {
			return "shovel";
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	public static class Sword extends SwordItem implements Tool {
		private final Tier tier;
		private final int damage;

		public Sword(int id, Tier tier) {
			this(id, tier, 4 + tier.damage * 2);
		}

		/** @param damage all of it: wood 4, stone 6, iron 8, diamond 10 */
		public Sword(int id, Tier tier, int damage) {
			super(id, ToolMaterial.IRON);
			this.tier = tier;
			this.damage = damage;
			setMaxDamage(tier.durability);
		}

		@Override
		public int getAttackDamage(Entity attacked) {
			return damage;
		}

		@Override
		public String toolClass() {
			return "sword";
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	public static class Hoe extends HoeItem implements Tool {
		private final Tier tier;

		public Hoe(int id, Tier tier) {
			super(id, ToolMaterial.IRON);
			this.tier = tier;
			setMaxDamage(tier.durability);
		}

		@Override
		public String toolClass() {
			return "hoe";
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	/** Takes wool off sheep and leaves off trees, as vanilla's do. The tier gives its durability. */
	public static class Shears extends ShearsItem implements Tool {
		private final Tier tier;

		public Shears(int id, Tier tier) {
			super(id);
			this.tier = tier;
			setMaxDamage(tier.durability);
		}

		@Override
		public String toolClass() {
			return "shears";
		}

		@Override
		public Tier tier() {
			return tier;
		}
	}

	public static Content.ItemFactory<Pickaxe> pickaxe(Tier tier, String texture) {
		return s -> textured(new Pickaxe(s.id(), tier), s, texture);
	}

	public static Content.ItemFactory<Axe> axe(Tier tier, String texture) {
		return s -> textured(new Axe(s.id(), tier), s, texture);
	}

	public static Content.ItemFactory<Shovel> shovel(Tier tier, String texture) {
		return s -> textured(new Shovel(s.id(), tier), s, texture);
	}

	public static Content.ItemFactory<Sword> sword(Tier tier, String texture) {
		return s -> textured(new Sword(s.id(), tier), s, texture);
	}

	/** A sword of its own damage, all of it, for one between what a tier's would be: a tier's damage counts twice on a sword, so 7 between stone's 6 and iron's 8 can only be said here. */
	public static Content.ItemFactory<Sword> sword(Tier tier, int damage, String texture) {
		return s -> textured(new Sword(s.id(), tier, damage), s, texture);
	}

	public static Content.ItemFactory<Hoe> hoe(Tier tier, String texture) {
		return s -> textured(new Hoe(s.id(), tier), s, texture);
	}

	public static Content.ItemFactory<Shears> shears(Tier tier, String texture) {
		return s -> textured(new Shears(s.id(), tier), s, texture);
	}

	private static <T extends Item> T textured(T item, Content.Spec spec, String texture) {
		item.setTextureId(spec.texture(texture));
		return item;
	}

	private static Map<String, Rule> rules(Block block) {
		return RULES.computeIfAbsent(block.id, id -> new HashMap<>());
	}

	/**
	 * This block drops only for a tool of this class at this level or above, and such a tool mines it at its speed. Say it for several classes and any will
	 * do. On a vanilla block this adds to what vanilla's tools already do. Whatever the block's material says, so hands don't harvest it.
	 */
	public static void requires(Block block, String toolClass, int level) {
		rules(block).put(toolClass, new Rule(level, true, true));
	}

	/** A tool of this class mines this block at its speed. What drops it stays the block's material's business. */
	public static void effective(Block block, String toolClass) {
		rules(block).put(toolClass, new Rule(0, false, true));
	}

	/** Shears lose durability cutting this block, as they do on vanilla's leaves. Every other tool wears on whatever it mines, so only shears need telling. */
	public static void wearsShears(Block block) {
		SHEARS_WEAR.set(block.id);
	}

	/** The core's. */
	public static boolean shearsWearOn(int block) {
		return SHEARS_WEAR.get(block);
	}

	/** The tool an item is, or null. */
	public static Tool of(Item item) {
		return item instanceof Tool ? (Tool) item : item == null ? null : VANILLA_TOOLS.get(item.id);
	}

	/** Whether what's in the hand harvests this block: {@code usual} is vanilla's answer, kept for any block with no requirement. */
	public static boolean harvests(ItemStack held, Block block, boolean usual) {
		Map<String, Rule> rules = RULES.get(block.id);
		if (rules == null) return usual;
		Tool tool = held == null ? null : of(held.getItem());
		boolean required = false;
		for (Map.Entry<String, Rule> e : rules.entrySet()) {
			if (!e.getValue().required) continue;
			required = true;
			if (tool != null && tool.toolClass().equals(e.getKey()) && tool.tier().level >= e.getValue().level) return true;
		}
		return !required && usual;
	}

	/** What {@code Item.SHEARS.id} should read as for what's in the hand: anything that is shears is as good. */
	public static int shearsId(ItemStack held, int shears) {
		return held != null && held.getItem() instanceof ShearsItem ? held.itemId : shears;
	}

	/** The speed of what's in the hand on this block: {@code usual} is the item's own answer, and a block's word for its class raises it to the tier's. */
	public static float speed(ItemStack held, Block block, float usual) {
		Map<String, Rule> rules = RULES.get(block.id);
		if (rules == null) return usual;
		Tool tool = of(held.getItem());
		if (tool == null) return usual;
		Rule rule = rules.get(tool.toolClass());
		return rule != null && rule.faster ? Math.max(usual, tool.tier().speed) : usual;
	}
}
