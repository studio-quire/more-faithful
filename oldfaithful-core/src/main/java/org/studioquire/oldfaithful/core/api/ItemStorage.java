package org.studioquire.oldfaithful.core.api;

import java.util.function.Predicate;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.impl.content.VanillaStorage;

/**
 * Something that holds items, seen from one side. Every {@code Inventory} block entity is found as one with no work, vanilla's chests (a double chest as one),
 * furnaces and dispensers among them, all their slots from every side: what a side means is for the mod reaching in to decide. Stacks
 * going in or coming out are the caller's; ones given aren't kept or changed. What {@code simulate} says holds for the rest of that tick. Needs the content layer.
 * <p>
 * To limit what a block lets through, implement this on its block entity: that's found ahead of the {@code Inventory} it may also be, and refusing is just {@code insert} handing the stack back and
 * {@code extract} returning null. To answer by side, or to hide an inventory altogether, register a provider ({@link Lookups}).
 */
public interface ItemStorage {
	Lookups.Type<ItemStorage> TYPE = Lookups.type("faithful-core:item_storage", ItemStorage.class, VanillaStorage::find);

	int size();

	/** A copy of what's in a slot, or null. */
	ItemStack peek(int slot);

	/** Puts in what fits. Returns what didn't, or null if all did. */
	ItemStack insert(ItemStack stack, boolean simulate);

	/** Takes up to {@code amount} from a slot. Returns what came out, or null. */
	ItemStack extract(int slot, int amount, boolean simulate);

	/** A plain view of an inventory: every slot, both ways. */
	static ItemStorage of(Inventory inventory) {
		return new VanillaStorage(inventory);
	}

	/** Moves up to {@code amount} items of one kind: the first stack here the other side takes any of. Returns how many moved. */
	static int move(ItemStorage from, ItemStorage to, int amount) {
		return move(from, to, amount, stack -> true);
	}

	/** The same, passing over stacks the filter turns down. */
	static int move(ItemStorage from, ItemStorage to, int amount, Predicate<ItemStack> filter) {
		for (int slot = 0; slot < from.size(); slot++) {
			ItemStack offered = from.extract(slot, amount, true);
			if (offered == null || !filter.test(offered)) continue;
			ItemStack refused = to.insert(offered, true);
			int fits = offered.count - (refused == null ? 0 : refused.count);
			if (fits <= 0) continue;
			ItemStack taken = from.extract(slot, fits, false);
			if (taken == null) continue;
			int moved = taken.count;
			ItemStack left = to.insert(taken, false);
			if (left != null) {
				moved -= left.count;
				ItemStack lost = from.insert(left, false);
				if (lost != null) System.out.println("[faithful-core] WARN " + lost.count + " of " + lost.getItem() + " lost: " + to.getClass().getName() + " took less than it said it would");
			}
			return moved;
		}
		return 0;
	}
}
