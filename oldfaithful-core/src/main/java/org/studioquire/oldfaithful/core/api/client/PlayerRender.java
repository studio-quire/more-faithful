package org.studioquire.oldfaithful.core.api.client;

import java.util.function.Consumer;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import org.studioquire.oldfaithful.core.api.Listeners;

/** How players are drawn, on a client. */
public final class PlayerRender {
	public static final class Pose {
		public final PlayerEntity player;
		/** Its parts' {@code pitch}, {@code yaw}, {@code roll} and {@code visible} are what to change. */
		public final BipedEntityModel model;

		Pose(PlayerEntity player, BipedEntityModel model) {
			this.player = player;
			this.model = model;
		}
	}

	private static final Listeners<Consumer<Pose>> POSE = new Listeners<>("PlayerRender.onPose");
	private static PlayerEntity drawing;

	private PlayerRender() {
	}

	/**
	 * Filter: after vanilla has posed a player's model, each listener in order changes what the last left. Heard for the body and for
	 * each armor model, so a pose holds for armor too, and heard again each frame; the first-person hand isn't heard.
	 */
	public static void onPose(Consumer<Pose> listener) {
		POSE.add(listener);
	}

	public static boolean hasListeners() {
		return !POSE.isEmpty();
	}

	/** The core's. */
	public static void drawing(PlayerEntity player, Runnable work) {
		drawing = player;
		try {
			work.run();
		} finally {
			drawing = null;
		}
	}

	/** The core's. */
	public static void posed(BipedEntityModel model) {
		if (drawing == null) return;
		Pose pose = new Pose(drawing, model);
		POSE.each(l -> l.accept(pose));
	}
}
