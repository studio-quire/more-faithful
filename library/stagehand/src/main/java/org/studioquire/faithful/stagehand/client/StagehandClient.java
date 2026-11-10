package org.studioquire.faithful.stagehand.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.studioquire.faithful.stagehand.Move;
import org.studioquire.faithful.stagehand.Side;
import org.studioquire.faithful.stagehand.Stagehand;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Ticks;
import org.studioquire.oldfaithful.core.api.Worldgen;
import org.studioquire.oldfaithful.core.api.client.Client;

/**
 * Single player's side. The client's tick rather than the world's, because it goes on in a menu: alt-tabbing away, as anyone talking to a session does, pauses the game, and a script still has to run
 * and answer, or say why it can't.
 */
public final class StagehandClient implements FaithfulClientMod, Side {
	@Override
	public void initClient(Mod mod) {
		Stagehand.side(this);
		Ticks.onClient(Stagehand::tick);
	}

	@Override
	public String where() {
		return "single player";
	}

	@Override
	public Refusal refusal() {
		Minecraft mc = Client.minecraft();
		if (mc.world == null) return new Refusal("no_world", "the game is at its menus, with no world open");
		if (mc.world.isRemote) return new Refusal("on_a_server", "this game is joined to a server, and a script runs where the world runs: Stagehand has to be on the server, and the script in its folder");
		return null;
	}

	@Override
	public boolean paused() {
		return Client.minecraft().paused;
	}

	@Override
	public boolean netherAllowed(PlayerEntity player) {
		return true;
	}

	@Override
	public void changeDimension(PlayerEntity player, int dimension, double x, double y, double z) {
		Minecraft mc = Client.minecraft();
		if (player.currentScreenHandler != player.playerScreenHandler) mc.player.closeHandledScreen();
		// The game scales the position by 8 on the way, and starts the new world's chunks around the result.
		double scale = dimension == Worldgen.NETHER ? 8.0 : 1.0 / 8.0;
		player.setPositionAndAnglesKeepPrevAngles(x * scale, y, z * scale, player.yaw, player.pitch);
		Move.crossing = true;
		try {
			mc.changeDimension();
		} finally {
			Move.crossing = false;
		}
	}

	@Override
	public void place(PlayerEntity player, double x, double y, double z, float yaw, float pitch) {
		player.setPositionAndAnglesKeepPrevAngles(x, y, z, yaw, pitch);
		player.prevYaw = yaw;
		player.prevPitch = pitch;
		player.world.updateEntity(player, false);
	}

	@Override
	public void setTime(World world, long time) {
		world.setTime(time);
	}
}
