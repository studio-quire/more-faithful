package org.studioquire.faithful.harness;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.oldfaithful.core.api.Tags;
import org.studioquire.oldfaithful.core.impl.RecipeTable;

/**
 * Tag names against the vocabulary, and mods' recipes: what they name exists, and no two recipes answer one grid. Each mod recipe is laid out
 * in a real grid and every recipe in the game is asked, so a clash with vanilla's counts too.
 */
final class RecipeRules {
	private static final String TAGS_FILE = System.getProperty("faithful.harness.tags");

	private RecipeRules() {
	}

	private static String owner(ItemStack output) {
		String name = output == null ? null : ModIds.item(output.itemId);
		return name != null && name.indexOf(':') > 0 ? Owners.namespace(name) : "vanilla";
	}

	static void run() throws IOException, ReflectiveOperationException {
		tags();
		@SuppressWarnings("unchecked")
		List<CraftingRecipe> all = CraftingRecipeManager.getInstance().getRecipes();
		Set<String> reported = new HashSet<>();
		for (CraftingRecipe recipe : all) {
			if (!recipe.getClass().getName().startsWith(RecipeTable.class.getName())) continue;
			ItemStack out = recipe.getOutput();
			String mod = owner(out);
			if (out == null || Item.ITEMS[out.itemId] == null) {
				Out.fail(mod, "recipe-names-nothing", "a recipe makes " + (out == null ? "nothing" : "the empty id " + out.itemId));
				continue;
			}
			List<ItemStack> cells = layOut(recipe, mod);
			if (cells == null) continue;
			CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
				@Override
				public boolean canUse(PlayerEntity player) {
					return true;
				}
			}, 3, 3);
			for (int i = 0; i < cells.size(); i++) grid.setStack(i, cells.get(i));
			for (CraftingRecipe other : all) {
				if (other == recipe || !other.matches(grid)) continue;
				ItemStack made = other.getOutput();
				if (made != null && made.itemId == out.itemId && made.count == out.count && made.getDamage() == out.getDamage()) continue;
				String a = ModIds.item(out.itemId), b = made == null ? "?" : ModIds.item(made.itemId);
				if (reported.add(a.compareTo(b) < 0 ? a + b : b + a)) Out.fail(mod + ", " + owner(made), "two-recipes-one-shape", "one grid makes both " + a + " and " + b + ": which you get depends on the recipes' order");
			}
		}
	}

	private static void tags() throws IOException {
		if (TAGS_FILE == null) return;
		Set<String> vocabulary = new HashSet<>();
		for (String line : Files.readAllLines(Paths.get(TAGS_FILE))) {
			if (!line.trim().isEmpty() && !line.startsWith("#")) vocabulary.add(line.trim());
		}
		for (String tag : new ArrayList<>(Tags.names())) {
			if (!vocabulary.contains(tag)) Out.fail(String.join(", ", Tags.who(tag)), "tag-vocabulary", tag + " isn't in library/tags.txt: use the spelling there, or add it in the same commit");
		}
	}

	/** One stack per grid cell, nine of them, or null if an ingredient has nothing to stand for it here. */
	private static List<ItemStack> layOut(CraftingRecipe recipe, String mod) throws ReflectiveOperationException {
		List<ItemStack> cells = new ArrayList<>();
		for (int i = 0; i < 9; i++) cells.add(null);
		String name = ModIds.item(recipe.getOutput().itemId);
		Class<?> type = recipe.getClass();
		if (type.getSimpleName().equals("Shaped")) {
			Object array = field(recipe, "cells");
			int width = (Integer) field(recipe, "width");
			for (int i = 0; i < Array.getLength(array); i++) {
				Object ingredient = Array.get(array, i);
				if (ingredient == null) continue;
				ItemStack sample = sample(ingredient, mod, name);
				if (sample == null) return null;
				cells.set(i % width + i / width * 3, sample);
			}
		} else {
			int i = 0;
			for (Object ingredient : (List<?>) field(recipe, "ingredients")) {
				ItemStack sample = sample(ingredient, mod, name);
				if (sample == null) return null;
				cells.set(i++, sample);
			}
		}
		return cells;
	}

	private static ItemStack sample(Object ingredient, String mod, String recipe) throws ReflectiveOperationException {
		String tag = (String) field(ingredient, "tag");
		if (tag != null) {
			List<ItemStack> items = Tags.items(tag);
			if (items.isEmpty()) {
				Out.note(mod, "recipe-empty-tag", recipe + " asks for " + tag + ", which nothing here fills");
				return null;
			}
			ItemStack s = items.get(0);
			return new ItemStack(s.itemId, 1, Math.max(0, s.getDamage()));
		}
		int item = (Integer) field(ingredient, "item"), damage = (Integer) field(ingredient, "damage");
		if (item <= 0 || item >= Item.ITEMS.length || Item.ITEMS[item] == null) {
			Out.fail(mod, "recipe-names-nothing", recipe + " takes the empty id " + item);
			return null;
		}
		return new ItemStack(item, 1, Math.max(0, damage));
	}

	private static Object field(Object o, String name) throws ReflectiveOperationException {
		Field f = o.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(o);
	}
}
