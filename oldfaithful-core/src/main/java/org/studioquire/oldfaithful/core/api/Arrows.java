package org.studioquire.oldfaithful.core.api;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Which items are arrows, and what each flies as, for anything that shoots one other than beta's own bow and dispenser: a mod's archer, a mod's launcher. Beta's arrow is here from the
 * start; a mod's arrow says what it flies as, in its {@code init}:
 * <pre>{@code
 * Arrows.flies(myArrow.get(), MyArrowEntity::new);
 * }</pre>
 * The entity is made as beta's bow makes one, at the shooter's eye along its look ({@code new ArrowEntity(world, shooter)}); the shooter then sets its flight, spawns it and plays its sound.
 * Beta's bow and dispenser don't ask here: a mod's arrow that they should shoot patches them itself, as Explosive Arrows does.
 */
public final class Arrows {
	/** Makes the arrow an item flies as, from where the shooter's eye is and the way it looks. */
	public interface Factory {
		ArrowEntity make(World world, LivingEntity shooter);
	}

	private static final Map<Integer, Factory> FLIES = new HashMap<>();

	static {
		FLIES.put(Item.ARROW.id, ArrowEntity::new);
	}

	private Arrows() {
	}

	/** This item is an arrow, and flies as what the factory makes. */
	public static void flies(Item item, Factory factory) {
		FLIES.put(item.id, factory);
	}

	public static boolean isArrow(ItemStack stack) {
		return stack != null && stack.count > 0 && FLIES.containsKey(stack.itemId);
	}

	/** The arrow this stack flies as, made but not yet in the world, or null if it isn't an arrow. Takes nothing from the stack. */
	public static ArrowEntity make(ItemStack stack, World world, LivingEntity shooter) {
		Factory factory = stack == null ? null : FLIES.get(stack.itemId);
		return factory == null ? null : factory.make(world, shooter);
	}
}
