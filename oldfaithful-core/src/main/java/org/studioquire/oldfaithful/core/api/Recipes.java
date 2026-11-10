package org.studioquire.oldfaithful.core.api;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import org.studioquire.oldfaithful.core.impl.RecipeTable;

/**
 * Recipes, from {@code init}. An ingredient is what vanilla's recipes take, or a tag: an {@code Item} (damage 0), a {@code Block} (any damage),
 * an {@code ItemStack} (its damage, or any at -1), or a tag's name as a {@code String}. Both sides must add the same recipes, so add them in
 * common code; a client shows a result and the server decides.
 * <pre>{@code
 * Recipes.shaped(new ItemStack(pick), "III", " S ", " S ", 'I', "c:ingots/iron", 'S', Item.STICK);
 * Recipes.shapeless(new ItemStack(dust, 2), ore, Item.FLINT);
 * Recipes.smelting(new ItemStack(ore, 1, 3), new ItemStack(ingot));
 * Recipes.fuel("c:planks", 300);
 * Recipes.modify(r -> r.removeOutput(Block.BOOKSHELF));
 * }</pre>
 */
public final class Recipes {
	/**
	 * A result worked out from what's in the grid, for one a stack can't say: a stack carrying another's data over. {@code inputs} are the grid's stacks in reading order, empty cells left out; don't change
	 * them. Return a new stack, {@code Stacks.copy} of an input if its data should come along, or null for no result.
	 */
	public interface Result {
		ItemStack make(List<ItemStack> inputs);
	}

	/** Changes to what everyone has added; see {@link #modify}. */
	public interface Editor {
		/** Every crafting recipe, vanilla's and mods'. Remove or add here for anything the calls below can't say. */
		List<CraftingRecipe> crafting();

		/** Every crafting recipe that makes this {@code Item}, {@code Block} or {@code ItemStack} (at its damage, or any at -1). */
		void removeOutput(Object output);

		/** The smelting recipe of this input, by the id for a plain {@code Item} or {@code Block}, or as it was added. */
		void removeSmelting(Object input);
	}

	private Recipes() {
	}

	/** Rows of up to three characters, then pairs of a {@code char} and an ingredient. A mirror image matches too, as with vanilla's. A space is an empty cell. */
	public static void shaped(ItemStack output, Object... pattern) {
		RecipeTable.shaped(output, null, pattern);
	}

	public static void shapeless(ItemStack output, Object... ingredients) {
		RecipeTable.shapeless(output, null, ingredients);
	}

	/** As {@link #shaped(ItemStack, Object...)}, with the result made when it's crafted; {@code output} is what the recipe says it makes. */
	public static void shaped(ItemStack output, Result result, Object... pattern) {
		RecipeTable.shaped(output, result, pattern);
	}

	/** As {@link #shapeless(ItemStack, Object...)}, with the result made when it's crafted; {@code output} is what the recipe says it makes. */
	public static void shapeless(ItemStack output, Result result, Object... ingredients) {
		RecipeTable.shapeless(output, result, ingredients);
	}

	/**
	 * What a furnace makes of an input, one item at a time: {@code output}'s count must be 1, since vanilla's furnace adds one to the stack. An
	 * {@code Item} or {@code Block} goes in vanilla's own table and matches any damage. An {@code ItemStack} with a damage, or a tag, needs the content layer.
	 */
	public static void smelting(Object input, ItemStack output) {
		RecipeTable.smelting(input, output);
	}

	/** How long an ingredient burns, in ticks (a coal's 1600, a plank's 300). A later word replaces vanilla's; 0 stops something burning. Needs the content layer. */
	public static void fuel(Object input, int ticks) {
		RecipeTable.fuel(input, ticks);
	}

	/** Runs once every mod's {@code init} is done, in mod id order, so it sees everything: vanilla's recipes, and those of mods it doesn't know. */
	public static void modify(Consumer<Editor> change) {
		RecipeTable.modify(change);
	}

	/** How long this stack burns in a furnace, in ticks, vanilla's fuels and mods' alike; 0 if it doesn't. In common code, with a null stack too. Needs the content layer. */
	public static int fuelTime(ItemStack stack) {
		return RecipeTable.fuelTime(stack);
	}

	/** What the furnace makes of this stack, or null. */
	public static ItemStack smeltingResult(ItemStack input) {
		return RecipeTable.smelt(input, input == null ? null : net.minecraft.recipe.SmeltingRecipeManager.getInstance().craft(input.itemId));
	}
}
