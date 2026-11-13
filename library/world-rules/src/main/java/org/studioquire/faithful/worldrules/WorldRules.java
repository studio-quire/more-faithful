package org.studioquire.faithful.worldrules;

import org.studioquire.oldfaithful.core.api.Beds;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.ChoiceRule;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.Entities;
import org.studioquire.oldfaithful.core.api.Explosions;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * The first library mod. A rule is one line here, two in the lang file and a hook. They run where the world runs, except that a client that knows bows wear out wears its own as the
 * world does, as beta's client does every tool.
 */
public final class WorldRules implements FaithfulMod {
	/** Beta's shortest wait for the weather to change, for when the rule is taken off. */
	private static final int MIN_RAIN_TIME = 12000;
	/** Later versions' odds. */
	private static final int APPLE_ODDS = 200;
	/** Later versions' bow: an item breaks when its damage passes the most, so 385 shots, the last breaking it. */
	public static final int BOW_MOST_DAMAGE = 384;

	public static BoolRule mobsBreakBlocks;
	public static BoolRule blastsDestroyItems;
	public static BoolRule keepInventory;
	public static BoolRule sleeping;
	public static ChoiceRule weather;
	public static BoolRule naturalMonsters;
	public static ChoiceRule monsterLight;
	public static BoolRule appleDrops;
	public static ChoiceRule fallingBonus;
	public static BoolRule bowsWearOut;
	/** On by default, where the others start as beta: worlds that already exist take it too. */
	public static BoolRule pickupReach;

	/** Whether a bow wears in this world: the rule here, or as the server said. Asked for the bow's most damage at every turn, so off before the rule exists, as beta's. */
	public static boolean bowsWear() {
		return bowsWearOut != null && bowsWearOut.get();
	}

	/**
	 * With Monster Light at None: this monster is about to appear naturally in block light, from a torch, lava or the like. Sky light is left to beta's rolls, or nothing would appear outdoors at
	 * night. Not in the Nether, where lava lights most of what there is.
	 */
	public static boolean lightStops(net.minecraft.entity.LivingEntity entity) {
		if (!monsterLight.get().equals("none") || !(entity instanceof net.minecraft.entity.mob.MonsterEntity) || entity.world.dimension.isNether) return false;
		int x = net.minecraft.util.math.MathHelper.floor(entity.x), y = net.minecraft.util.math.MathHelper.floor(entity.boundingBox.minY), z = net.minecraft.util.math.MathHelper.floor(entity.z);
		return entity.world.getBrightness(net.minecraft.world.LightType.BLOCK, x, y, z) > 0;
	}

	@Override
	public void init(Mod mod) {
		mobsBreakBlocks = mod.rules().bool("mobs_break_blocks", true);
		blastsDestroyItems = mod.rules().bool("blasts_destroy_items", true);
		keepInventory = mod.rules().bool("keep_inventory", false);
		sleeping = mod.rules().bool("sleeping", true);
		weather = mod.rules().choice("weather", "normal", "clear", "rain");
		naturalMonsters = mod.rules().bool("natural_monsters", true);
		monsterLight = mod.rules().choice("monster_light", "beta", "none");
		appleDrops = mod.rules().bool("apple_drops", false);
		fallingBonus = mod.rules().choice("falling_bonus", "beta", "fixed", "off");
		bowsWearOut = mod.rules().bool("bows_wear_out", false);
		pickupReach = mod.rules().bool("pickup_reach", true);
		dispenserFromAnyBow();

		Explosions.onBlocks(e -> {
			if (e.cause().isMob() && !mobsBreakBlocks.get()) e.spareBlocks();
		});
		Players.keepInventory(p -> keepInventory.get());
		Beds.onSleep(s -> {
			if (!sleeping.get()) s.setSpawnOnly();
		});
		Ticks.onWorld(WorldRules::holdWeather);
		Entities.onMonsterSpawning(m -> {
			if (!naturalMonsters.get()) m.cancel();
		});
		Drops.block(WorldRules::apple);
	}

	/**
	 * Beta's dispenser asks for a bow at damage 0, so a worn one would make none. Here any bow does, whatever its wear, rule on or off: off, a bow worn while it was on
	 * still carries its damage, unseen. Vanilla's recipe is taken out after every mod's init, so only its shape's twin answers.
	 */
	private static void dispenserFromAnyBow() {
		Recipes.shaped(new ItemStack(Block.DISPENSER), "###", "#X#", "#R#", '#', Block.COBBLESTONE, 'X', new ItemStack(Item.BOW, 1, -1), 'R', Item.REDSTONE);
		Recipes.modify(r -> r.crafting().removeIf(recipe -> recipe instanceof ShapedRecipe && recipe.getOutput().itemId == Block.DISPENSER.id));
	}

	/** Oak's meta is 0 in its low bits. Leaves taken with shears reach the filter as a drop of their own, whose meta is the emptied cell's, so the stack is what tells them. */
	private static void apple(Drops.BlockDrops drops) {
		if (!appleDrops.get() || drops.block != Block.LEAVES || (drops.meta & 3) != 0) return;
		for (ItemStack stack : drops.stacks) {
			if (stack.itemId == Block.LEAVES.id) return;
		}
		if (drops.world.random.nextInt(APPLE_ODDS) == 0) drops.stacks.add(new ItemStack(Item.APPLE));
	}

	/**
	 * After the world's tick, so after a night's sleep has cleared the weather. Rain is beta's flag and its timer kept from running out: the sky, snow and what a client hears all follow from it.
	 * Thunder has its own cycle, and only shows while it rains, so it needs nothing: none under a clear sky, its usual comings and goings under a wet one.
	 */
	private static void holdWeather(World world) {
		String held = weather.get();
		if (held.equals("normal")) return;
		WorldProperties properties = world.getProperties();
		properties.setRaining(held.equals("rain"));
		if (properties.getRainTime() < 2) properties.setRainTime(MIN_RAIN_TIME);
	}
}
