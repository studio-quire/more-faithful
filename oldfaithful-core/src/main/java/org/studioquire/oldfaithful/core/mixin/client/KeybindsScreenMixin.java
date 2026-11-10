package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.client.Screens;

/**
 * Beta's screen has room for seven rows of two. Past fourteen keys, each page shows fourteen, in beta's layout, with an arrow either
 * side of Done. With beta's own ten, or up to four more, the layout is beta's. A key that shares its code with another reads in red.
 */
@Mixin(KeybindsScreen.class)
abstract class KeybindsScreenMixin extends Screen {
	@Unique
	private static final int PER_PAGE = 14;
	@Unique
	private static final int ROW = 24;
	/** A key's name when another key is on the same one. */
	@Unique
	private static final int SHARED = 0xFF5555;

	@Shadow
	private GameOptions gameOptions;
	@Unique
	private int faithful$page;

	@Unique
	private int faithful$pages() {
		return (gameOptions.allKeys.length + PER_PAGE - 1) / PER_PAGE;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void faithful$layOut(CallbackInfo ci) {
		if (faithful$pages() <= 1) return;
		for (int i = 0; i < gameOptions.allKeys.length; i++) {
			ButtonWidget key = (ButtonWidget) buttons.get(i);
			boolean here = i / PER_PAGE == faithful$page;
			key.y -= i / PER_PAGE * (PER_PAGE / 2) * ROW;
			key.visible = key.active = here;
		}
		int y = height / 6 + 168;
		buttons.add(new Screens.Button(width / 2 - 124, y, 20, 20, "<", () -> faithful$turn(-1)));
		buttons.add(new Screens.Button(width / 2 + 104, y, 20, 20, ">", () -> faithful$turn(1)));
	}

	@Unique
	private void faithful$turn(int by) {
		faithful$page = (faithful$page + by + faithful$pages()) % faithful$pages();
		buttons.clear();
		init();
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/option/KeybindsScreen;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
	private void faithful$label(KeybindsScreen screen, TextRenderer renderer, String text, int x, int y, int color, Operation<Void> draw, @Local(ordinal = 3) int index) {
		int shown = faithful$shared(index) ? SHARED : color;
		if (faithful$pages() <= 1) {
			draw.call(screen, renderer, text, x, y, shown);
		} else if (index / PER_PAGE == faithful$page) {
			draw.call(screen, renderer, text, x, y - faithful$page * (PER_PAGE / 2) * ROW, shown);
		}
	}

	@Unique
	private boolean faithful$shared(int index) {
		for (int i = 0; i < gameOptions.allKeys.length; i++) {
			if (i != index && gameOptions.allKeys[i].code == gameOptions.allKeys[index].code) return true;
		}
		return false;
	}
}
