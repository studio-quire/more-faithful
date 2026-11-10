package org.studioquire.oldfaithful.core.api.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import org.studioquire.oldfaithful.core.api.Listeners;
import org.studioquire.oldfaithful.core.mixin.client.SlotAccessor;

/** Screens, on a client. */
public final class Screens {
	/** A button that answers its own click, so no screen has to know it. Its id is the core's. */
	public static final class Button extends ButtonWidget {
		private static int nextId = -7000;
		private final Runnable onClick;
		private final String mod;

		public Button(int x, int y, int width, int height, String text, Runnable onClick) {
			super(nextId--, x, y, width, height, text);
			this.onClick = onClick;
			this.mod = OPENED.speaking();
		}

		/** The core's. */
		public void click() {
			try {
				onClick.run();
			} catch (RuntimeException | LinkageError e) {
				System.out.println("[faithful-core] WARN " + mod + "'s button threw: " + e);
				e.printStackTrace(System.out);
			}
		}
	}

	public static final class Opened {
		public final Screen screen;
		/** In GUI units. */
		public final int width, height;
		private final List<ButtonWidget> buttons;

		Opened(Screen screen, int width, int height, List<ButtonWidget> buttons) {
			this.screen = screen;
			this.width = width;
			this.height = height;
			this.buttons = buttons;
		}

		/** Adds a button below the screen's own in its list, where {@code buttons.get(i)} in a vanilla screen doesn't reach it. */
		public void add(Button button) {
			buttons.add(button);
		}
	}

	public static final class Tooltip {
		public final ItemStack stack;
		/** The first is the item's name, if it has one. Empty, nothing shows. */
		public final List<String> lines = new ArrayList<>();

		Tooltip(ItemStack stack, String name) {
			this.stack = stack;
			if (!name.isEmpty()) lines.add(name);
		}
	}

	public enum SlotRole {
		HOTBAR, PLAYER_MAIN, ARMOR, CONTAINER, CRAFT_INPUT, CRAFT_RESULT, FURNACE_INPUT, FURNACE_FUEL, FURNACE_RESULT
	}

	private static final Listeners<Consumer<Opened>> OPENED = new Listeners<>("Screens.onOpened");
	private static final Listeners<Consumer<Tooltip>> TOOLTIP = new Listeners<>("Screens.onTooltip");
	private static final Map<Class<?>, BiFunction<ScreenHandler, Slot, SlotRole>> ROLES = new HashMap<>();

	private Screens() {
	}

	/**
	 * Notice: a screen has built its widgets. Heard each time it does: a window resize rebuilds them, so widgets added here come
	 * back. Every screen, the main menu's too. Listeners use {@link Opened#add} and look at {@code screen}'s class.
	 */
	public static void onOpened(Consumer<Opened> listener) {
		OPENED.add(listener);
	}

	/** Filter, in order, each seeing the last's lines: what shows when the mouse is on a stack in an inventory. */
	public static void onTooltip(Consumer<Tooltip> listener) {
		TOOLTIP.add(listener);
	}

	/** Roles for the slots of a mod's own screen handler. A null answer falls back to the core's reading. */
	public static void slotRoles(Class<? extends ScreenHandler> handler, BiFunction<ScreenHandler, Slot, SlotRole> roles) {
		ROLES.put(handler, roles);
	}

	/** What a slot is for, so a sorting mod needs to know no screen by name. Anything not known is {@link SlotRole#CONTAINER}. */
	public static SlotRole role(ScreenHandler handler, Slot slot) {
		BiFunction<ScreenHandler, Slot, SlotRole> roles = ROLES.get(handler.getClass());
		SlotRole role = roles == null ? null : roles.apply(handler, slot);
		if (role != null) return role;
		if (slot instanceof CraftingResultSlot) return SlotRole.CRAFT_RESULT;
		if (slot instanceof FurnaceOutputSlot) return SlotRole.FURNACE_RESULT;
		Inventory inventory = ((SlotAccessor) slot).faithful$inventory();
		int index = ((SlotAccessor) slot).faithful$index();
		if (inventory instanceof PlayerInventory) return index < 9 ? SlotRole.HOTBAR : index < 36 ? SlotRole.PLAYER_MAIN : SlotRole.ARMOR;
		if (inventory instanceof CraftingInventory) return SlotRole.CRAFT_INPUT;
		if (inventory instanceof FurnaceBlockEntity) return index == 0 ? SlotRole.FURNACE_INPUT : SlotRole.FURNACE_FUEL;
		return SlotRole.CONTAINER;
	}

	/** The core's. */
	public static void opened(Screen screen, int width, int height, List<ButtonWidget> buttons) {
		if (OPENED.isEmpty()) return;
		Opened event = new Opened(screen, width, height, buttons);
		OPENED.each(l -> l.accept(event));
	}

	public static boolean hasTooltipListeners() {
		return !TOOLTIP.isEmpty();
	}

	/** The core's. */
	public static List<String> tooltip(ItemStack stack, String name) {
		Tooltip event = new Tooltip(stack, name);
		TOOLTIP.each(l -> l.accept(event));
		return event.lines;
	}
}
