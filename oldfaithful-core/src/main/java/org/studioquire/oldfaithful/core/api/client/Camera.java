package org.studioquire.oldfaithful.core.api.client;

import java.util.function.Consumer;

import org.studioquire.oldfaithful.core.api.Listeners;

/** The view, on a client. Filters: listeners run in order, each seeing the last's value. Vanilla's own and a settings FOV come first. */
public final class Camera {
	public static final class Value {
		public final float tickDelta;
		public float value;

		Value(float tickDelta, float value) {
			this.tickDelta = tickDelta;
			this.value = value;
		}
	}

	private static final Listeners<Consumer<Value>> FOV = new Listeners<>("Camera.onFov");
	private static final Listeners<Consumer<Value>> ROLL = new Listeners<>("Camera.onRoll");

	private Camera() {
	}

	/** The vertical field of view, in degrees, each frame. Vanilla's is 70, less under water. */
	public static void onFov(Consumer<Value> listener) {
		FOV.add(listener);
	}

	/** A tilt of the view, in degrees, each frame, from 0. */
	public static void onRoll(Consumer<Value> listener) {
		ROLL.add(listener);
	}

	/** The core's. */
	public static float fov(float tickDelta, float fov) {
		if (FOV.isEmpty()) return fov;
		Value value = new Value(tickDelta, fov);
		FOV.each(l -> l.accept(value));
		return value.value;
	}

	/** The core's. */
	public static float roll(float tickDelta) {
		if (ROLL.isEmpty()) return 0.0F;
		Value value = new Value(tickDelta, 0.0F);
		ROLL.each(l -> l.accept(value));
		return value.value;
	}
}
