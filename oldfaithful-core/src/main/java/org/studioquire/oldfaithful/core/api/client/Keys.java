package org.studioquire.oldfaithful.core.api.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.studioquire.oldfaithful.core.api.Listeners;

/**
 * Keys a player can rebind: they join the game's own in Controls and in options.txt, which pages past the seven rows beta has room
 * for. Declared in {@code initClient}. The translation key names it in the lang file and in options.txt, so it's the mod's to keep.
 *
 * <p>Two keys on one code both fire, as beta's own do if a player binds them alike: nothing is moved or switched off behind anyone's back, since
 * which mod should lose isn't ours to say. Controls shows every key that shares its code in red, and the player moves one. The harness lists
 * defaults that clash: pick one nobody has.
 */
public final class Keys {
	private static final List<KeyBinding> ADDED = new ArrayList<>();
	private static final Map<KeyBinding, Listeners<Runnable>> PRESSED = new HashMap<>();
	private static boolean sealed;

	private Keys() {
	}

	/** A key with its default: {@code Keyboard.KEY_*}. */
	public static KeyBinding register(String translationKey, int defaultCode) {
		if (sealed) throw new IllegalStateException("The key " + translationKey + " came after the game read its options: declare keys in initClient");
		KeyBinding key = new KeyBinding(translationKey, defaultCode);
		ADDED.add(key);
		return key;
	}

	/** Notice: this key went down in the game, with no screen open. */
	public static void onPressed(KeyBinding key, Runnable listener) {
		PRESSED.computeIfAbsent(key, k -> new Listeners<>("Keys.onPressed " + k.translationKey)).add(listener);
	}

	public static boolean isDown(KeyBinding key) {
		return Keyboard.isKeyDown(key.code);
	}

	/** The core's: before {@code options.txt} is read. */
	public static KeyBinding[] extend(KeyBinding[] vanilla) {
		sealed = true;
		if (ADDED.isEmpty()) return vanilla;
		KeyBinding[] all = Arrays.copyOf(vanilla, vanilla.length + ADDED.size());
		for (int i = 0; i < ADDED.size(); i++) all[vanilla.length + i] = ADDED.get(i);
		return all;
	}

	/** The core's. */
	static boolean hasPressListeners() {
		return !PRESSED.isEmpty();
	}

	/** The core's. */
	static void pressed(int code) {
		for (Map.Entry<KeyBinding, Listeners<Runnable>> e : PRESSED.entrySet()) {
			if (e.getKey().code == code) e.getValue().each(Runnable::run);
		}
	}
}
