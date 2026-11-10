package org.studioquire.faithful.itemrepair.selftest;

import java.util.Arrays;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.itemrepair.ItemRepair;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.content.ContentMixins;

/** The recipe where the world runs, through the game's own recipe table: which pairs mend, the sums, the cap, the bonus. The client agreeing with the server is {@code RepairClientTest}. */
public final class RepairTest extends WorldTest {
	private static ItemStack worn(Item item, int damage) {
		return new ItemStack(item, 1, damage);
	}

	private static ItemStack crafted(int side, ItemStack... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, side, side);
		for (int i = 0; i < cells.length; i++) grid.setStack(i, cells[i]);
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private static boolean is(ItemStack made, Item item, int damage) {
		return made != null && made.getItem() == item && made.count == 1 && made.getDamage() == damage;
	}

	private static String said(ItemStack made) {
		return made == null ? "nothing" : made.count + " of " + made.getItem().getTranslationKey() + " at " + made.getDamage();
	}

	private void sums() {
		// Uses left are the most less the damage, plus one: at the most a tool has its last use.
		ItemStack made = crafted(2, worn(Item.WOODEN_PICKAXE, 50), worn(Item.WOODEN_PICKAXE, 40));
		check("two wooden picks with 10 and 20 uses left make one with 30, at damage 30: " + said(made), is(made, Item.WOODEN_PICKAXE, 30));
		made = crafted(2, worn(Item.WOODEN_PICKAXE, 59), worn(Item.WOODEN_PICKAXE, 59));
		check("two on their last use make one with two: " + said(made), is(made, Item.WOODEN_PICKAXE, 58));
		made = crafted(2, worn(Item.IRON_PICKAXE, 10), worn(Item.IRON_PICKAXE, 20));
		check("no better than new: " + said(made), is(made, Item.IRON_PICKAXE, 0));
		made = crafted(2, new ItemStack(Item.WOODEN_PICKAXE), worn(Item.WOODEN_PICKAXE, 30));
		check("a new one with a worn one mends, as in later versions: " + said(made), is(made, Item.WOODEN_PICKAXE, 0));
		made = crafted(2, worn(Item.DIAMOND_PICKAXE, 1000), worn(Item.DIAMOND_PICKAXE, 1200));
		check("diamond, 562 and 362 left, no bonus: " + said(made), is(made, Item.DIAMOND_PICKAXE, 638));
		made = crafted(3, null, worn(Item.DIAMOND_PICKAXE, 1000), null, null, null, null, null, null, worn(Item.DIAMOND_PICKAXE, 1200));
		check("at the bench, cells apart: " + said(made), is(made, Item.DIAMOND_PICKAXE, 638));

		for (Item item : new Item[] {Item.STONE_SWORD, Item.GOLDEN_AXE, Item.LEATHER_BOOTS, Item.CHAIN_CHESTPLATE, Item.DIAMOND_HELMET, Item.SHEARS, Item.FISHING_ROD, Item.FLINT_AND_STEEL}) {
			int max = item.getMaxDamage(), a = max / 2, b = max - 3;
			made = crafted(2, worn(item, a), worn(item, b));
			int expected = Math.max(0, max + 1 - (max - a + 1) - (max - b + 1));
			check(item.getTranslationKey() + " mends: " + said(made), is(made, item, expected));
		}
	}

	private void refused() {
		check("two new ones make nothing", crafted(2, new ItemStack(Item.IRON_SWORD), new ItemStack(Item.IRON_SWORD)) == null);
		check("nor a wooden pick with a stone one", crafted(2, worn(Item.WOODEN_PICKAXE, 30), worn(Item.STONE_PICKAXE, 30)) == null);
		check("nor a pick with an axe", crafted(2, worn(Item.IRON_PICKAXE, 30), worn(Item.IRON_AXE, 30)) == null);
		check("nor iron boots with an iron helmet", crafted(2, worn(Item.IRON_BOOTS, 30), worn(Item.IRON_HELMET, 30)) == null);
		check("nor three", crafted(2, worn(Item.WOODEN_PICKAXE, 50), worn(Item.WOODEN_PICKAXE, 50), worn(Item.WOODEN_PICKAXE, 50)) == null);
		check("nor one alone", crafted(2, worn(Item.WOODEN_PICKAXE, 50)) == null);
		// Beta's tools never stack, but an item that wears out may (stackable while new), and a give command can make anything.
		check("two in one cell isn't a repair", crafted(2, new ItemStack(Item.WOODEN_PICKAXE, 2, 50)) == null);
		check("nor two to a cell twice", crafted(2, new ItemStack(Item.WOODEN_PICKAXE, 2, 50), new ItemStack(Item.WOODEN_PICKAXE, 2, 50)) == null);
		check("dye's damage is its colour, so two dyes aren't mended", crafted(2, new ItemStack(Item.DYE, 1, 4), new ItemStack(Item.DYE, 1, 4)) == null);
		// A worn bow mending where World Rules' Bows Wear Out is on, and not where it's off, is World Rules' test.
		check("nor two new bows", crafted(2, new ItemStack(Item.BOW), new ItemStack(Item.BOW)) == null);
	}

	/** Stack data exists only with the content layer, which a set without content leaves off. */
	private void data() {
		if (!ContentMixins.active()) return;
		ItemStack first = worn(Item.IRON_SWORD, 200), second = worn(Item.IRON_SWORD, 100);
		Stacks.data(first).putString("mark", "first");
		Stacks.data(second).putString("mark", "second");
		ItemStack made = crafted(2, first, second);
		check("the first in reading order is the one kept, its data with it: " + said(made), is(made, Item.IRON_SWORD, 49) && made != first
				&& Stacks.peek(made) != null && "first".equals(Stacks.peek(made).getString("mark")));
		check("and the inputs are as they were", first.getDamage() == 200 && second.getDamage() == 100 && first.count == 1);
		made = crafted(2, second, first);
		check("the other way round, the other's", made != null && "second".equals(Stacks.peek(made).getString("mark")));
	}

	private void bonus() {
		check("the bonus is off by default", !ItemRepair.bonus.defaultValue());
		boolean was = ItemRepair.bonus.get();
		Rules.change(ItemRepair.bonus, "true");
		ItemStack made = crafted(2, worn(Item.DIAMOND_PICKAXE, 1000), worn(Item.DIAMOND_PICKAXE, 1200));
		check("with the bonus, a twentieth of 1561 more, 78: " + said(made), is(made, Item.DIAMOND_PICKAXE, 560));
		made = crafted(2, worn(Item.WOODEN_PICKAXE, 59), worn(Item.WOODEN_PICKAXE, 59));
		check("two wooden ones on their last use, 2 more: " + said(made), is(made, Item.WOODEN_PICKAXE, 56));
		check("still nothing for two new ones", crafted(2, new ItemStack(Item.IRON_SWORD), new ItemStack(Item.IRON_SWORD)) == null);
		Rules.change(ItemRepair.bonus, String.valueOf(was));
	}

	private void scenes() {
		// The kit for the client's clicks: two worn diamond picks in the first hotbar slots, the rest emptied.
		scene("kit", a -> {
			Arrays.fill(player.inventory.main, null);
			player.inventory.main[0] = worn(Item.DIAMOND_PICKAXE, 1000);
			player.inventory.main[1] = worn(Item.DIAMOND_PICKAXE, 1200);
			player.currentScreenHandler.sendContentUpdates();
		});
		scene("bonus", a -> Rules.change(ItemRepair.bonus, a[0]));
		scene("expect result", a -> {
			ItemStack result = player.playerScreenHandler.getSlot(0).getStack();
			check("where the world runs, the inventory's grid shows " + a[0] + ": " + said(result), is(result, Item.DIAMOND_PICKAXE, Integer.parseInt(a[0])));
		});
		scene("expect taken", a -> {
			int picks = 0, damage = -1;
			for (ItemStack s : player.inventory.main) {
				if (s != null && s.getItem() == Item.DIAMOND_PICKAXE) {
					picks++;
					damage = s.getDamage();
				}
			}
			boolean gridEmpty = true;
			for (int i = 1; i <= 4; i++) gridEmpty &= player.playerScreenHandler.getSlot(i).getStack() == null;
			check("where the world runs, one pick at " + a[0] + " and an empty grid: " + picks + " at " + damage, picks == 1 && damage == Integer.parseInt(a[0]) && gridEmpty);
		});
		scene("clear", a -> {
			Arrays.fill(player.inventory.main, null);
			player.currentScreenHandler.sendContentUpdates();
		});
	}

	@Override
	protected void script() {
		scenes();
		sums();
		refused();
		data();
		bonus();
	}
}
