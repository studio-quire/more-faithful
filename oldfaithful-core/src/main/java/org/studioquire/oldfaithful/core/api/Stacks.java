package org.studioquire.oldfaithful.core.api;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.studioquire.oldfaithful.core.impl.content.StackDataHolder;

/**
 * Data on one stack, which b1.7.3 has no room for. It survives copying and splitting, is saved with the stack, goes over the wire
 * in window and slot packets, and is part of a stack's identity: stacks with different data don't merge. For anything big, keep it per
 * world and put an id in the stack's damage, as maps do.
 *
 * <p>Past {@link #MAX_WIRE_BYTES} a stack's data doesn't go on the wire, and past 256 KB in one packet, or with 512 KB in the send queue, the rest of its stacks'
 * data doesn't either. The stack still goes, as the item it is. The data stays where the world runs, saved and used as ever, and nothing
 * a client sends ever replaces it. So on a client on a server the stack has none: {@link #withheld} says so, and its tooltip tells the
 * player. A mod with a budget measures against {@link #wireBytes}.
 */
public final class Stacks {
	/** The most data a stack can have and still reach a client whole. */
	public static final int MAX_WIRE_BYTES = 16 * 1024;

	private Stacks() {
	}

	/** The stack's data, made if there's none; change it in place. */
	public static NbtCompound data(ItemStack stack) {
		StackDataHolder holder = (StackDataHolder) (Object) stack;
		NbtCompound data = holder.faithful$data();
		if (data == null) {
			data = new NbtCompound();
			holder.faithful$setData(data);
		}
		return data;
	}

	/** The stack's data, or null. */
	public static NbtCompound peek(ItemStack stack) {
		return ((StackDataHolder) (Object) stack).faithful$data();
	}

	/** How much of the wire this stack's data would take: 0 if it has none. */
	public static int wireBytes(ItemStack stack) {
		return org.studioquire.oldfaithful.core.impl.content.StackData.size(stack);
	}

	/** On a client on a server: this stack has data too big to be sent, so what's here isn't all of it. Don't read that as an empty stack. */
	public static boolean withheld(ItemStack stack) {
		return org.studioquire.oldfaithful.core.impl.content.StackData.withheld(peek(stack));
	}

	public static void clear(ItemStack stack) {
		((StackDataHolder) (Object) stack).faithful$setData(null);
	}

	/** Whether two stacks may become one: the same item, damage and data. The only way mods should compare stacks. */
	public static boolean canMerge(ItemStack a, ItemStack b) {
		return a != null && b != null && a.itemId == b.itemId && (!a.hasSubtypes() || a.getDamage() == b.getDamage()) && sameData(a, b);
	}

	public static boolean sameData(ItemStack a, ItemStack b) {
		return org.studioquire.oldfaithful.core.impl.content.StackData.same(peek(a), peek(b));
	}

	/** A copy of the stack, data included. */
	public static ItemStack copy(ItemStack stack) {
		return stack == null ? null : stack.copy();
	}
}
