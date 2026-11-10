package org.studioquire.faithful.harness.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.studioquire.faithful.harness.Out;
import org.studioquire.oldfaithful.options.settings.SettingsRegistry;

/** What only a client knows: keys, and the settings screen, which logs a setting registered twice. */
final class ClientRules {
	private ClientRules() {
	}

	static void run(Minecraft mc) {
		Map<Integer, List<String>> byCode = new TreeMap<>();
		for (KeyBinding key : mc.options.allKeys) byCode.computeIfAbsent(key.code, k -> new ArrayList<>()).add(key.translationKey);
		for (Map.Entry<Integer, List<String>> e : byCode.entrySet()) {
			if (e.getValue().size() > 1) Out.note("keys", "duplicate-default-key", Keyboard.getKeyName(e.getKey()) + ": " + e.getValue());
		}
		SettingsRegistry.get();
	}
}
