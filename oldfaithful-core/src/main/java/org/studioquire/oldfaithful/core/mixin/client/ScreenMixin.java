package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.client.Input;
import org.studioquire.oldfaithful.core.api.client.Screens;

/** Screens as they're built and clicked, and the loop that gives them their input. */
@Mixin(Screen.class)
abstract class ScreenMixin {
	@Shadow
	protected List buttons;

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
	@SuppressWarnings("unchecked")
	private void faithful$opened(Minecraft minecraft, int width, int height, CallbackInfo ci) {
		Screens.opened((Screen) (Object) this, width, height, (List<ButtonWidget>) buttons);
	}

	/** Some screens read any id as their own. */
	@WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;buttonClicked(Lnet/minecraft/client/gui/widget/ButtonWidget;)V"))
	private void faithful$click(Screen screen, ButtonWidget button, Operation<Void> clicked) {
		if (button instanceof Screens.Button) ((Screens.Button) button).click();
		else clicked.call(screen, button);
	}

	@WrapOperation(method = "tickInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z", remap = false))
	private boolean faithful$key(Operation<Boolean> next) {
		return Input.nextKey(() -> next.call());
	}

	@WrapOperation(method = "tickInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false))
	private boolean faithful$mouse(Operation<Boolean> next) {
		return Input.nextMouse(() -> next.call());
	}
}
