package org.studioquire.faithful.golems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SecondaryBlockItem;
import org.studioquire.faithful.golems.server.ServerEffects;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Tools;

public final class Golems implements FaithfulMod {
	public static ItemEntry<GolemCore> core;
	public static BlockEntry<BoundaryPole> pole, poleLit;
	/** Where a chunk's boundary poles are, two bytes each: x and z within the chunk, then height. */
	static Attachment<net.minecraft.world.chunk.Chunk, byte[]> poles;

	/** The jobs, by the tool class that sets them ({@code Tools.Tool.toolClass}). A job is a class and a line in {@link #init}. */
	private static final Map<String, Function<GolemEntity, Job>> JOBS = new LinkedHashMap<>();
	/** A bow's class, which the core's tools don't have: any {@code BowItem}, so a mod's bow that is one sets the same job and serves as a spare. */
	static final String BOW = "bow";

	/** An item taken in, shown flying to the golem where a player would see it: set by the client in single player, by the server's side on a server. */
	public static BiConsumer<ItemEntity, Entity> pickedUp = (item, golem) -> {
	};

	@Override
	public void content(Content c) {
		core = c.item("golem_core", s -> new GolemCore(s.id(), s.texture("golem_core"))).standIn(Item.DIAMOND);
		// In the hand and the inventory the flat icon of a pole leaning as beta's stick does: a rod 2 pixels thick drawn as a block would hardly show.
		pole = c.block("boundary_pole", s -> new BoundaryPole(s.id(), s.texture("boundary_pole_side"), s.texture("boundary_pole_top"), s.texture("boundary_pole_bits"), false)).standIn(Block.FENCE).item(s -> {
			Item item = new SecondaryBlockItem(s.id(), pole.get());
			item.setTextureId(s.texture("boundary_pole"));
			item.setTranslationKey("golems.boundary_pole");
			return item;
		});
		// A pole a golem is using: lit, as beta's redstone torch is a second block, with no item of its own.
		poleLit = c.block("boundary_pole_lit", s -> new BoundaryPole(s.id(), s.texture("boundary_pole_side_lit"), s.texture("boundary_pole_top_lit"), s.texture("boundary_pole_bits"), true)).standIn(Block.FENCE).noItem();
		c.entity("golem", GolemEntity.class);
	}

	@Override
	public void init(Mod mod) {
		Recipes.shaped(new ItemStack(core.get()), "RGR", "GDG", "RGR", 'R', Item.REDSTONE, 'G', Item.GOLD_INGOT, 'D', Item.DIAMOND);
		// A pole: redstone over an iron ingot, as the pole's red head over its iron. Four, for a rectangle's corners; beta has nothing of these two alone.
		Recipes.shaped(new ItemStack(pole.get(), 4), "R", "I", 'R', Item.REDSTONE, 'I', Item.IRON_INGOT);
		poles = mod.attachment("boundary_poles", Attachment.chunk(Codec.BYTES)).persist();
		PoleClaims.init();
		job("pickaxe", PickaxeJob::new);
		job("hoe", HoeJob::new);
		job("sword", SwordJob::new);
		job("axe", AxeJob::new);
		job(BOW, BowJob::new);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) pickedUp = ServerEffects.pickup();
	}

	/** A job for the tool class given: handing a golem a tool of that class sets it to work at it. */
	public static void job(String toolClass, Function<GolemEntity, Job> factory) {
		JOBS.put(toolClass, factory);
	}

	/** The tool class this stack would set a golem to, or null if it sets none. */
	public static String jobFor(ItemStack stack) {
		String kind = toolClass(stack);
		return kind != null && JOBS.containsKey(kind) ? kind : null;
	}

	/** The tool class of a stack, or null if it's no tool. */
	static String toolClass(ItemStack stack) {
		if (stack == null) return null;
		if (stack.getItem() instanceof BowItem) return BOW;
		Tools.Tool tool = Tools.of(stack.getItem());
		return tool == null ? null : tool.toolClass();
	}

	static Job newJob(String toolClass, GolemEntity golem) {
		Function<GolemEntity, Job> factory = JOBS.get(toolClass);
		return factory == null ? null : factory.apply(golem);
	}
}
