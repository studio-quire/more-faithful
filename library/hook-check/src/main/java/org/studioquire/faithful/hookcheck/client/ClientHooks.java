package org.studioquire.faithful.hookcheck.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.studioquire.faithful.hookcheck.HookCheck;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.Camera;
import org.studioquire.oldfaithful.core.api.client.Hud;
import org.studioquire.oldfaithful.core.api.client.Input;
import org.studioquire.oldfaithful.core.api.client.Keys;
import org.studioquire.oldfaithful.core.api.client.PlayerRender;
import org.studioquire.oldfaithful.core.api.client.Screens;
import org.studioquire.oldfaithful.core.api.client.WorldRender;

/** The client's hooks, listened to. Their switches stay off until {@code HookChecks} turns one on. */
public final class ClientHooks implements FaithfulClientMod {
	public static final int ADDED_KEYS = 6;
	public static KeyBinding first, last;
	public static int firstLayerY, secondLayerY, bottomLayerY;
	public static boolean vetoKey, widenFov, tilt, addTooltip;
	public static float lastFov;
	public static int clicks;
	public static final List<String> TOOLTIP = new ArrayList<>();

	@Override
	public void initClient(Mod mod) {
		Hud.layer(Hud.Corner.TOP_LEFT, f -> {
			HookCheck.heard("hud");
			f.text("hook check", 0xFFFFFF);
			firstLayerY = f.y;
		});
		Hud.layer(Hud.Corner.TOP_LEFT, f -> {
			f.text("second layer", 0xFFFFFF);
			secondLayerY = f.y;
		});
		Hud.layer(Hud.Corner.BOTTOM_RIGHT, f -> {
			f.claim(20, 10);
			bottomLayerY = f.y;
		});
		WorldRender.onSky(v -> HookCheck.heard("sky"));
		WorldRender.afterEntities(v -> HookCheck.heard("afterEntities"));
		WorldRender.afterWorld(v -> HookCheck.heard("afterWorld"));
		Screens.onOpened(e -> {
			HookCheck.heard("screen");
			if (e.screen instanceof OptionsScreen) e.add(new Screens.Button(2, 2, 40, 20, "Hook", () -> clicks++));
		});
		Screens.onTooltip(t -> {
			HookCheck.heard("tooltip");
			TOOLTIP.clear();
			TOOLTIP.addAll(t.lines);
			if (addTooltip) t.lines.add("from a mod");
		});
		first = Keys.register("key.hook-check.first", Keyboard.KEY_K);
		for (int i = 0; i < ADDED_KEYS - 2; i++) Keys.register("key.hook-check.filler" + i, Keyboard.KEY_NONE);
		last = Keys.register("key.hook-check.last", Keyboard.KEY_NONE);
		Keys.onPressed(first, () -> HookCheck.heard("keyPressed"));
		Input.onKey(e -> {
			HookCheck.heard("key");
			if (vetoKey) e.cancel();
		});
		Input.onMouse(e -> HookCheck.heard("mouse"));
		PlayerRender.onPose(p -> {
			HookCheck.heard("pose");
			p.model.head.visible = true;
		});
		Camera.onFov(v -> {
			HookCheck.heard("fov");
			if (widenFov) v.value += 20.0F;
			lastFov = v.value;
		});
		Camera.onRoll(v -> {
			HookCheck.heard("roll");
			if (tilt) v.value += 5.0F;
		});
	}
}
