package org.studioquire.oldfaithful.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SmeltingRecipeManager;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Tags;
import org.studioquire.oldfaithful.core.impl.content.ContentMixins;
import org.studioquire.oldfaithful.core.impl.content.VanillaStorage;

/** Mods' recipes, and the tables vanilla has no room for: smelting by damage or tag, and fuel. */
public final class RecipeTable {
	private static final int GRID = 3;

	private static final class Ingredient {
		final int item, damage;
		final String tag;

		Ingredient(int item, int damage, String tag) {
			this.item = item;
			this.damage = damage;
			this.tag = tag;
		}

		boolean matches(ItemStack stack) {
			if (stack == null) return false;
			if (tag != null) return Tags.has(tag, stack);
			return stack.itemId == item && (damage == -1 || damage == stack.getDamage());
		}
	}

	private static Ingredient ingredient(Object o) {
		if (o instanceof Item) return new Ingredient(((Item) o).id, 0, null);
		if (o instanceof Block) return new Ingredient(((Block) o).id, -1, null);
		if (o instanceof ItemStack) return new Ingredient(((ItemStack) o).itemId, ((ItemStack) o).getDamage(), null);
		if (o instanceof String) {
			String tag = (String) o;
			Tags.declare(tag);
			return new Ingredient(0, 0, tag);
		}
		throw new IllegalArgumentException("An ingredient is an Item, Block, ItemStack or a tag's name, not " + o);
	}

	private static ItemStack made(CraftingInventory grid, ItemStack output, Recipes.Result result) {
		if (result == null) return output.copy();
		List<ItemStack> inputs = new ArrayList<>();
		for (int i = 0; i < grid.size(); i++) {
			if (grid.getStack(i) != null) inputs.add(grid.getStack(i));
		}
		return result.make(inputs);
	}

	private static final class Shaped implements CraftingRecipe {
		final int width, height;
		final Ingredient[] cells;
		final ItemStack output;
		final Recipes.Result result;

		Shaped(int width, int height, Ingredient[] cells, ItemStack output, Recipes.Result result) {
			this.width = width;
			this.height = height;
			this.cells = cells;
			this.output = output;
			this.result = result;
		}

		private boolean at(CraftingInventory grid, int offsetX, int offsetY, boolean mirrored) {
			for (int x = 0; x < GRID; x++) {
				for (int y = 0; y < GRID; y++) {
					int cx = x - offsetX, cy = y - offsetY;
					Ingredient wanted = cx < 0 || cy < 0 || cx >= width || cy >= height ? null : cells[(mirrored ? width - 1 - cx : cx) + cy * width];
					ItemStack there = grid.getStack(x, y);
					if (wanted == null ? there != null : !wanted.matches(there)) return false;
				}
			}
			return true;
		}

		@Override
		public boolean matches(CraftingInventory grid) {
			for (int x = 0; x <= GRID - width; x++) {
				for (int y = 0; y <= GRID - height; y++) {
					if (at(grid, x, y, false) || at(grid, x, y, true)) return true;
				}
			}
			return false;
		}

		@Override
		public ItemStack craft(CraftingInventory grid) {
			return made(grid, output, result);
		}

		@Override
		public int getSize() {
			return width * height;
		}

		@Override
		public ItemStack getOutput() {
			return output;
		}
	}

	private static final class Shapeless implements CraftingRecipe {
		final List<Ingredient> ingredients;
		final ItemStack output;
		final Recipes.Result result;

		Shapeless(List<Ingredient> ingredients, ItemStack output, Recipes.Result result) {
			this.ingredients = ingredients;
			this.output = output;
			this.result = result;
		}

		@Override
		public boolean matches(CraftingInventory grid) {
			List<Ingredient> left = new ArrayList<>(ingredients);
			for (int y = 0; y < GRID; y++) {
				for (int x = 0; x < GRID; x++) {
					ItemStack there = grid.getStack(x, y);
					if (there == null) continue;
					boolean found = false;
					for (int i = 0; i < left.size() && !found; i++) {
						if (left.get(i).matches(there)) {
							left.remove(i);
							found = true;
						}
					}
					if (!found) return false;
				}
			}
			return left.isEmpty();
		}

		@Override
		public ItemStack craft(CraftingInventory grid) {
			return made(grid, output, result);
		}

		@Override
		public int getSize() {
			return ingredients.size();
		}

		@Override
		public ItemStack getOutput() {
			return output;
		}
	}

	private static final class Smelting {
		final Ingredient input;
		final ItemStack output;

		Smelting(Ingredient input, ItemStack output) {
			this.input = input;
			this.output = output;
		}
	}

	private static final class Fuel {
		final Ingredient input;
		final int ticks;

		Fuel(Ingredient input, int ticks) {
			this.input = input;
			this.ticks = ticks;
		}
	}

	private static final List<Smelting> SMELTING = new ArrayList<>();
	private static final List<Fuel> FUELS = new ArrayList<>();
	private static final List<Consumer<Recipes.Editor>> CHANGES = new ArrayList<>();
	private static boolean addedCrafting;
	private static FurnaceBlockEntity fuelReader;

	private RecipeTable() {
	}

	private static void add(CraftingRecipe recipe) {
		addedCrafting = true;
		@SuppressWarnings("unchecked")
		List<CraftingRecipe> recipes = CraftingRecipeManager.getInstance().getRecipes();
		recipes.add(recipe);
	}

	public static void shaped(ItemStack output, Recipes.Result result, Object[] pattern) {
		List<String> rows = new ArrayList<>();
		int i = 0;
		while (i < pattern.length && pattern[i] instanceof String) rows.add((String) pattern[i++]);
		if (rows.isEmpty() || rows.size() > GRID) throw new IllegalArgumentException("A recipe has one to three rows: " + Arrays.toString(pattern));
		int width = rows.get(0).length();
		for (String row : rows) {
			if (row.length() != width || width == 0 || width > GRID) throw new IllegalArgumentException("A recipe's rows are one to three long, all alike: " + rows);
		}
		Map<Character, Ingredient> keys = new HashMap<>();
		for (; i + 1 < pattern.length; i += 2) {
			if (!(pattern[i] instanceof Character)) throw new IllegalArgumentException("Expected a key character in " + Arrays.toString(pattern));
			keys.put((Character) pattern[i], ingredient(pattern[i + 1]));
		}
		if (i != pattern.length) throw new IllegalArgumentException("A key with no ingredient in " + Arrays.toString(pattern));
		Ingredient[] cells = new Ingredient[width * rows.size()];
		for (int y = 0; y < rows.size(); y++) {
			for (int x = 0; x < width; x++) {
				char c = rows.get(y).charAt(x);
				if (c == ' ') continue;
				cells[x + y * width] = keys.get(c);
				if (cells[x + y * width] == null) throw new IllegalArgumentException("Nothing for '" + c + "' in " + Arrays.toString(pattern));
			}
		}
		add(new Shaped(width, rows.size(), cells, output, result));
	}

	public static void shapeless(ItemStack output, Recipes.Result result, Object[] ingredients) {
		if (ingredients.length == 0 || ingredients.length > GRID * GRID) throw new IllegalArgumentException("A shapeless recipe takes one to nine ingredients");
		List<Ingredient> list = new ArrayList<>();
		for (Object o : ingredients) list.add(ingredient(o));
		add(new Shapeless(list, output, result));
	}

	private static void needsContent(String what) {
		if (!ContentMixins.active()) throw new IllegalStateException(what + " needs the content layer: say \"custom\": {\"faithful\": {\"content\": true}} in fabric.mod.json");
	}

	public static void smelting(Object input, ItemStack output) {
		if (output.count != 1) throw new IllegalArgumentException("A furnace makes one at a time, not " + output.count);
		if (input instanceof Item || input instanceof Block) {
			SmeltingRecipeManager.getInstance().addRecipe(input instanceof Item ? ((Item) input).id : ((Block) input).id, output);
		} else {
			needsContent("Smelting by damage or tag");
			SMELTING.add(new Smelting(ingredient(input), output));
		}
	}

	public static void fuel(Object input, int ticks) {
		needsContent("Fuel");
		if (ticks < 0) throw new IllegalArgumentException("Fuel burns for 0 ticks or more");
		FUELS.add(new Fuel(ingredient(input), ticks));
	}

	public static void modify(Consumer<Recipes.Editor> change) {
		CHANGES.add(change);
	}

	/** What the furnace makes of a stack: a mod's word for it, then vanilla's by id. */
	public static ItemStack smelt(ItemStack input, ItemStack vanilla) {
		for (Smelting s : SMELTING) {
			if (s.input.matches(input)) return s.output;
		}
		return vanilla;
	}

	/** Vanilla's answer is private to a furnace, and needs no world, so one that's never placed answers for everyone. */
	public static int fuelTime(ItemStack stack) {
		needsContent("Reading a fuel");
		if (stack == null) return 0;
		if (fuelReader == null) fuelReader = new FurnaceBlockEntity();
		return ((VanillaStorage.FuelTable) fuelReader).faithful$fuelTime(stack);
	}

	/** The last word on a fuel, or vanilla's. */
	public static int fuelTime(ItemStack stack, int vanilla) {
		for (int i = FUELS.size() - 1; i >= 0; i--) {
			if (FUELS.get(i).input.matches(stack)) return FUELS.get(i).ticks;
		}
		return vanilla;
	}

	private static final class Editor implements Recipes.Editor {
		@Override
		@SuppressWarnings("unchecked")
		public List<CraftingRecipe> crafting() {
			return CraftingRecipeManager.getInstance().getRecipes();
		}

		@Override
		public void removeOutput(Object output) {
			Ingredient wanted = ingredient(output instanceof Item ? new ItemStack((Item) output, 1, -1) : output);
			crafting().removeIf(r -> wanted.matches(r.getOutput()));
		}

		@Override
		public void removeSmelting(Object input) {
			if (input instanceof Item || input instanceof Block) {
				SmeltingRecipeManager.getInstance().getRecipes().remove(input instanceof Item ? ((Item) input).id : ((Block) input).id);
			} else {
				Ingredient wanted = ingredient(input);
				SMELTING.removeIf(s -> s.input.item == wanted.item && s.input.damage == wanted.damage && java.util.Objects.equals(s.input.tag, wanted.tag));
			}
		}
	}

	private static int rank(CraftingRecipe recipe) {
		return recipe instanceof ShapelessRecipe || recipe instanceof Shapeless ? 1 : 0;
	}

	/** After every mod's init: changes, then vanilla's order (the most specific first, shapeless last). The sort is stable, so vanilla's recipes keep theirs. */
	public static void finish() {
		if (!CHANGES.isEmpty()) {
			Editor editor = new Editor();
			for (Consumer<Recipes.Editor> change : CHANGES) change.accept(editor);
			CHANGES.clear();
		}
		if (addedCrafting) {
			new Editor().crafting().sort((a, b) -> rank(a) != rank(b) ? rank(a) - rank(b) : b.getSize() - a.getSize());
		}
	}
}
