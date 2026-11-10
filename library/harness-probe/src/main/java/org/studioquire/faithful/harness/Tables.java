package org.studioquire.faithful.harness;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.recipe.SmeltingRecipeManager;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.EntitySpawnGroup;
import org.studioquire.faithful.harness.mixin.BlockEntityAccessor;
import org.studioquire.faithful.harness.mixin.EntityRegistryAccessor;
import org.studioquire.faithful.harness.mixin.FireBlockAccessor;
import org.studioquire.faithful.harness.mixin.ToolItemAccessor;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * Vanilla's tables as this combination left them, a line per property, so the task can say who changed what: one mod alone against the
 * core alone, and everything together against the sum of those.
 */
final class Tables {
	private Tables() {
	}

	private static String material(Material m) throws IllegalAccessException {
		Field[] fields = Material.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			if (Modifier.isStatic(fields[i].getModifiers()) && fields[i].get(null) == m) return String.valueOf(i);
		}
		return "mod";
	}

	private static String stack(ItemStack s) {
		return s == null ? "-" : ModIds.item(s.itemId) + "x" + s.count + ":" + s.getDamage();
	}

	static void write(Path file) throws IOException, IllegalAccessException {
		Map<String, String> t = new TreeMap<>();
		FireBlockAccessor fire = (FireBlockAccessor) Block.FIRE;
		for (int id = 1; id < 256; id++) {
			Block b = Block.BLOCKS[id];
			String k = "block." + id + ".";
			t.put(k + "exists", String.valueOf(b != null));
			t.put(k + "fire.burn", String.valueOf(fire.burn()[id]));
			t.put(k + "fire.spread", String.valueOf(fire.spread()[id]));
			if (b == null) continue;
			t.put(k + "class", b.getClass().getName());
			t.put(k + "hardness", String.valueOf(b.getHardness()));
			t.put(k + "resistance", String.valueOf(b.getBlastResistance(null)));
			t.put(k + "slipperiness", String.valueOf(b.slipperiness));
			t.put(k + "material", material(b.material));
			t.put(k + "opacity", String.valueOf(Block.BLOCKS_LIGHT_OPACITY[id]));
			t.put(k + "luminance", String.valueOf(Block.BLOCKS_LIGHT_LUMINANCE[id]));
			t.put(k + "randomTick", String.valueOf(Block.BLOCKS_RANDOM_TICK[id]));
			t.put(k + "opaque", String.valueOf(Block.BLOCKS_OPAQUE[id]));
			t.put(k + "withEntity", String.valueOf(Block.BLOCKS_WITH_ENTITY[id]));
			t.put(k + "allowVision", String.valueOf(Block.BLOCKS_ALLOW_VISION[id]));
			t.put(k + "ignoreMetaUpdate", String.valueOf(Block.BLOCKS_IGNORE_META_UPDATE[id]));
			t.put(k + "drops", b.getDroppedItemId(0, new Random(1)) + "x" + b.getDroppedItemCount(new Random(1)));
		}
		for (int id = 256; id <= 359; id++) item(t, id);
		item(t, 2256);
		item(t, 2257);
		for (int id = 1; id < 256; id++) {
			if (Item.ITEMS[id] != null) t.put("item." + id + ".maxCount", String.valueOf(Item.ITEMS[id].getMaxCount()));
		}
		Map<String, Integer> recipes = new TreeMap<>();
		for (Object r : CraftingRecipeManager.getInstance().getRecipes()) {
			CraftingRecipe recipe = (CraftingRecipe) r;
			ItemStack out = recipe.getOutput();
			if (out == null || out.itemId < ContentRegistry.FIRST_BLOCK) recipes.merge("recipe." + recipe.getClass().getSimpleName() + "." + stack(out) + ".size" + recipe.getSize(), 1, Integer::sum);
		}
		recipes.forEach((k, v) -> t.put(k, String.valueOf(v)));
		for (Object e : SmeltingRecipeManager.getInstance().getRecipes().entrySet()) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
			t.put("smelting." + ModIds.item((Integer) entry.getKey()), stack((ItemStack) entry.getValue()));
		}
		for (Map.Entry<String, Class<?>> e : EntityRegistryAccessor.ids().entrySet()) {
			if (e.getKey().indexOf(':') < 0) t.put("entity." + e.getKey(), e.getValue().getName());
		}
		for (Map.Entry<String, Class<?>> e : BlockEntityAccessor.ids().entrySet()) {
			if (e.getKey().indexOf(':') < 0) t.put("blockEntity." + e.getKey(), e.getValue().getName());
		}
		biomes(t);

		List<String> lines = new ArrayList<>();
		t.forEach((k, v) -> lines.add(k + "=" + v));
		Files.write(file, lines);
	}

	private static void item(Map<String, String> t, int id) {
		Item item = Item.ITEMS[id];
		String k = "item." + id + ".";
		t.put(k + "exists", String.valueOf(item != null));
		if (item == null) return;
		t.put(k + "class", item.getClass().getName());
		t.put(k + "maxCount", String.valueOf(item.getMaxCount()));
		t.put(k + "maxDamage", String.valueOf(item.getMaxDamage()));
		if (item instanceof ToolItem) {
			ToolItemAccessor tool = (ToolItemAccessor) item;
			List<Integer> on = new ArrayList<>();
			for (Block b : tool.effective()) on.add(b == null ? 0 : b.id);
			t.put(k + "tool.effective", on.toString());
			t.put(k + "tool.speed", String.valueOf(tool.speed()));
		}
	}

	private static void biomes(Map<String, String> t) throws IllegalAccessException {
		for (Field f : Biome.class.getFields()) {
			if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Biome.class) continue;
			Biome biome = (Biome) f.get(null);
			for (SpawnGroup g : SpawnGroup.values()) {
				List<?> list = biome.getSpawnableEntities(g);
				if (list == null) continue;
				List<String> entries = new ArrayList<>();
				for (Object o : list) entries.add(((EntitySpawnGroup) o).clazz.getName() + "x" + ((EntitySpawnGroup) o).amount);
				Collections.sort(entries);
				t.put("biome." + biome.name + "." + g.name(), entries.toString());
			}
		}
	}
}
