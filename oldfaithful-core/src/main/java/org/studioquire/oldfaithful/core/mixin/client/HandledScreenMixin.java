package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.client.Screens;

/**
 * A stack's tooltip. The trimmed name is vanilla's whole tooltip, drawn one line tall. A changed one is drawn here, in the same
 * place and style, and vanilla is given nothing to draw.
 */
@Mixin(HandledScreen.class)
abstract class HandledScreenMixin extends Screen {
	@Shadow
	protected int backgroundWidth;
	@Shadow
	protected int backgroundHeight;

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/String;trim()Ljava/lang/String;"))
	private String faithful$tooltip(String name, @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY, @Local(ordinal = 0) Slot hovered) {
		if (!Screens.hasTooltipListeners()) return name;
		List<String> lines = Screens.tooltip(hovered.getStack(), name);
		if (lines.size() == 1 && lines.get(0).equals(name)) return name;
		int x = mouseX - (width - backgroundWidth) / 2 + 12, y = mouseY - (height - backgroundHeight) / 2 - 12;
		int widest = 0;
		for (String line : lines) widest = Math.max(widest, textRenderer.getWidth(line));
		if (!lines.isEmpty()) fillGradient(x - 3, y - 3, x + widest + 3, y + lines.size() * 10 + 1, -1073741824, -1073741824);
		for (int i = 0; i < lines.size(); i++) textRenderer.drawWithShadow(lines.get(i), x, y + i * 10, -1);
		return "";
	}
}
