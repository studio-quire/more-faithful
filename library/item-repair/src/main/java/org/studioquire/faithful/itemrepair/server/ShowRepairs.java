package org.studioquire.faithful.itemrepair.server;

import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import org.studioquire.faithful.itemrepair.ItemRepair;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * A dedicated server's side, for clients without the mod. Beta's server never sends a crafting result: each client works out its own, and one without the mod works out nothing, so it would
 * show an empty slot that a click still fills (the server takes the click, finds it different and sends the whole screen back). So each tick, a repair in such a client's result slot is sent
 * to it as a slot update. Its click on that then matches the server's, as a vanilla result's would. The client's own grid changes clear the slot there by themselves.
 */
@Environment(EnvType.SERVER)
public final class ShowRepairs {
	/** What was last sent to each player, by identity: a new result is a new stack. */
	private static final Map<PlayerEntity, ItemStack> SENT = new WeakHashMap<>();

	private ShowRepairs() {
	}

	public static void register() {
		Ticks.onServer(ShowRepairs::tick);
	}

	private static void tick() {
		for (PlayerEntity player : Server.players()) {
			if (ItemRepair.presence.available(player)) continue;
			ScreenHandler handler = player.currentScreenHandler;
			for (int i = 0; i < handler.slots.size(); i++) {
				Slot slot = (Slot) handler.slots.get(i);
				if (!(slot instanceof CraftingResultSlot)) continue;
				ItemStack result = slot.getStack();
				boolean sent = SENT.containsKey(player);
				if (result != SENT.get(player) && (ItemRepair.made(result) || sent)) {
					((ServerPlayerEntity) player).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, i, result));
				}
				if (ItemRepair.made(result)) SENT.put(player, result);
				else SENT.remove(player);
				break;
			}
		}
	}
}
