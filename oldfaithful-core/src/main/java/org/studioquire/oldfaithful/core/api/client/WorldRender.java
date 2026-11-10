package org.studioquire.oldfaithful.core.api.client;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import org.studioquire.oldfaithful.core.api.Listeners;

/**
 * Drawing into the world, on a client, in a world, on a server or not. All are notices, once each eye: an anaglyph frame has two.
 * The matrices are the camera's, with the camera at the origin: subtract {@link View#x} and the others from a world position.
 * What a listener changes in GL's state, it puts back.
 */
public final class WorldRender {
	public static final class View {
		public final Minecraft minecraft;
		public final float tickDelta;
		/** The camera, where this frame draws it. */
		public final double x, y, z;

		View(Minecraft minecraft, float tickDelta) {
			this.minecraft = minecraft;
			this.tickDelta = tickDelta;
			LivingEntity camera = minecraft.camera;
			x = camera.lastTickX + (camera.x - camera.lastTickX) * tickDelta;
			y = camera.lastTickY + (camera.y - camera.lastTickY) * tickDelta;
			z = camera.lastTickZ + (camera.z - camera.lastTickZ) * tickDelta;
		}
	}

	private static final Listeners<Consumer<View>> SKY = new Listeners<>("WorldRender.onSky");
	private static final Listeners<Consumer<View>> ENTITIES = new Listeners<>("WorldRender.afterEntities");
	private static final Listeners<Consumer<View>> WORLD = new Listeners<>("WorldRender.afterWorld");

	private WorldRender() {
	}

	/** After the sky, before the terrain. Only at Far and Normal render distances, where beta draws one. */
	public static void onSky(Consumer<View> listener) {
		SKY.add(listener);
	}

	/** After the solid world's entities and block entities, before particles and water. */
	public static void afterEntities(Consumer<View> listener) {
		ENTITIES.add(listener);
	}

	/** After everything in the world, clouds included, before the hand. */
	public static void afterWorld(Consumer<View> listener) {
		WORLD.add(listener);
	}

	private static void fire(Listeners<Consumer<View>> listeners, Minecraft minecraft, float tickDelta) {
		if (listeners.isEmpty()) return;
		View view = new View(minecraft, tickDelta);
		listeners.each(l -> l.accept(view));
	}

	/** The core's. */
	public static void sky(Minecraft minecraft, float tickDelta) {
		fire(SKY, minecraft, tickDelta);
	}

	/** The core's. */
	public static void entities(Minecraft minecraft, float tickDelta) {
		fire(ENTITIES, minecraft, tickDelta);
	}

	/** The core's. */
	public static void world(Minecraft minecraft, float tickDelta) {
		fire(WORLD, minecraft, tickDelta);
	}
}
