package org.studioquire.faithful.staticbad;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;

/** What the static stage must catch, all in common code. Never run. */
public final class Mistakes {
	public static Object client() {
		return Minecraft.class;
	}

	public static Object server(MinecraftServer server) {
		return server.worlds;
	}

	/** A method marked for the client is held to the client's side. */
	@Environment(EnvType.CLIENT)
	public static Object markedForTheClient(MinecraftServer server) {
		return server.worlds;
	}

	public static void lwjgl() {
		GL11.glFlush();
	}

	public static byte chunk(Chunk chunk) {
		return chunk.blocks[0];
	}

	public static boolean stacks(ItemStack a, ItemStack b) {
		return a.itemId == b.itemId;
	}
}
