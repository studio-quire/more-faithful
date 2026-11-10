package org.studioquire.faithful.itemrepair;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.itemrepair.server.ShowRepairs;
import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Stacks;

/**
 * Later versions' repair in the crafting grid: two of the same item that wears out, at least one of them worn, make one with the uses both had left, up to new, and a twentieth of a new one's
 * on top with Repair Bonus. One recipe per item, shapeless, so the core's grid, sort and harness see an ordinary recipe whose result is worked out.
 * <p>
 * No content and no {@code match}: nothing on the wire changes, so either side may lack it. A client without it on a server with it is shown the server's result
 * ({@link ShowRepairs}); a client with it on a server without it shows none, since the server would make none.
 */
public final class ItemRepair implements FaithfulMod {
	/** Later versions' bonus: 5% of a new item's durability. */
	private static final int BONUS_PERCENT = 5;

	public static BoolRule bonus;
	/** Carries nothing; its presence on the other side says that side repairs. */
	public static Channel presence;

	/** Results this process worked out, by identity: on a server, what {@link ShowRepairs} looks for in a crafting result slot. */
	private static final Set<ItemStack> MADE = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public void init(Mod mod) {
		bonus = mod.rules().bool("repair_bonus", false);
		presence = mod.channel("presence", 1);
		for (Item item : Item.ITEMS) {
			if (item == null || !item.isDamageable()) continue;
			ItemStack any = new ItemStack(item, 1, -1);
			Recipes.shapeless(new ItemStack(item), ItemRepair::repair, any, any.copy());
		}
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) ShowRepairs.register();
	}

	/** The uses an item has left: beta's break when the damage passes the most, so at the most there's one. */
	private static int usesLeft(ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamage() + 1;
	}

	/** The damage two of one item come to, mended into one. */
	public static int repairedDamage(ItemStack a, ItemStack b, boolean withBonus) {
		int max = a.getMaxDamage();
		int uses = usesLeft(a) + usesLeft(b) + (withBonus ? max * BONUS_PERCENT / 100 : 0);
		return Math.max(0, max + 1 - uses);
	}

	/**
	 * Both inputs are the same item, as the recipe matched. A stack of several in a cell isn't a repair, nor are two new ones. The first in reading order is the one kept, its stack data
	 * included, at the new damage.
	 */
	private static ItemStack repair(List<ItemStack> inputs) {
		if (inputs.size() != 2) return null;
		ItemStack first = inputs.get(0), second = inputs.get(1);
		if (first.count != 1 || second.count != 1 || first.getDamage() == 0 && second.getDamage() == 0) return null;
		// An item that wears only under a world's rule (World Rules' bows) has no most damage where it's off, and mends only where it wears.
		if (!first.isDamageable()) return null;
		// A client on a server without the mod: the server makes nothing, so show nothing.
		if (!Server.running() && !presence.available()) return null;
		ItemStack made = Stacks.copy(first);
		made.setDamage(repairedDamage(first, second, bonus.get()));
		MADE.add(made);
		return made;
	}

	/** Whether this is a result worked out here, the very stack. */
	public static boolean made(ItemStack stack) {
		return stack != null && MADE.contains(stack);
	}
}
