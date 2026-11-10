package org.studioquire.oldfaithful.core.api.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.oldfaithful.core.impl.content.client.ClientContent;

/** The client's half of {@code Containers}: what a declared screen looks like. A {@code HandledScreen} subclass, usually. */
public final class ContainerScreens {
	public interface Factory {
		Screen create(PlayerEntity player, ScreenHandler handler, Inventory inventory, NbtCompound data);
	}

	private ContainerScreens() {
	}

	/** From {@code initClient}. {@code name} is the full name, {@code ns:my_screen}, where {@code Content.screen} takes the short one. */
	public static void register(String name, Factory factory) {
		ClientContent.screen(name, factory);
	}
}
