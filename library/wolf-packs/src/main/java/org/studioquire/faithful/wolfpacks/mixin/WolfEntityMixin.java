package org.studioquire.faithful.wolfpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.faithful.wolfpacks.Hunt;
import org.studioquire.faithful.wolfpacks.Packs;
import org.studioquire.faithful.wolfpacks.Watch;
import org.studioquire.faithful.wolfpacks.WolfFood;

/**
 * A mixin because the core has no say in how a mob thinks or what an animal eats: a tamed wolf looks to its watch, its pack's fight and its pending healing
 * at the start of its own thinking each tick, and beta's feeding in {@code interact} asks this what it eats and what it heals. A wolf on watch and off its
 * seat isn't beta's follower meanwhile, and walks its last steps back by this.
 */
@Mixin(WolfEntity.class)
abstract class WolfEntityMixin extends AnimalEntity implements Hunt.Holder {
	private WolfEntityMixin(World world) {
		super(world);
	}

	@Unique
	private Hunt wolfpacks$state;
	/** What the feeding in progress is, between beta asking whether it's meat and its heal, in the same call. */
	@Unique
	private FoodItem wolfpacks$fed;
	/** The click in progress held something this wolf eats, so it isn't the owner's sit or stand. */
	@Unique
	private boolean wolfpacks$feeding;

	@Override
	public Hunt wolfpacks$hunt() {
		if (wolfpacks$state == null) wolfpacks$state = new Hunt();
		return wolfpacks$state;
	}

	@Inject(method = "tickLiving", at = @At("HEAD"))
	private void wolfpacks$think(CallbackInfo ci) {
		WolfEntity wolf = (WolfEntity) (Object) this;
		WolfFood.tick(wolf);
		if (!Watch.tick(wolf)) Packs.tick(wolf);
	}

	/**
	 * Beta's tamed wolf with no path walks to its owner, appears beside them when they're far, and sits down where it is when they're away. Not one on watch
	 * and off its seat: it has its monster or its spot to go to. Only the first of the two questions in that method: the second keeps wild wolves' hunting
	 * for wild wolves.
	 */
	@WrapOperation(method = "tickLiving", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;isTamed()Z", ordinal = 0))
	private boolean wolfpacks$notAtHeel(WolfEntity wolf, Operation<Boolean> isTamed) {
		return isTamed.call(wolf) && !Watch.onDuty(wolf);
	}

	/** After beta's thinking: a sitting wolf faces what it's growling at, and one nearly back at its spot walks straight onto it. */
	@Inject(method = "tickLiving", at = @At("TAIL"))
	private void wolfpacks$watches(CallbackInfo ci) {
		WolfEntity wolf = (WolfEntity) (Object) this;
		if (wolf.world.isRemote || !wolf.isTamed()) return;
		if (Watch.settled(wolf)) jumping = false;
		Entity alarm = Watch.growlsAt(wolf);
		if (alarm != null) {
			// As beta turns a sitting wolf's head to a player.
			lookAt(alarm, 10.0F, 20.0F);
			return;
		}
		double[] to = Watch.lastSteps(wolf);
		if (to == null) return;
		double dx = to[0] - x, dz = to[2] - z;
		yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		forwardSpeed = dx * dx + dz * dz < 0.64 ? movementSpeed * 0.5F : movementSpeed;
		sidewaysSpeed = 0.0F;
		jumping = horizontalCollision || isSubmergedInWater();
	}

	/** Beta stands a sitting wolf up when anything hurts it and turns it on what did: one on watch goes back to its spot after (`Watch.hit`). */
	@Inject(method = "damage(Lnet/minecraft/entity/Entity;I)Z", at = @At("HEAD"))
	private void wolfpacks$hitOnItsSpot(Entity source, int amount, CallbackInfoReturnable<Boolean> cir) {
		Watch.hit((WolfEntity) (Object) this, source);
	}

	/**
	 * Beta's feeding goes on when this says meat and the tail shows less than full health. A client on a server always hears no, and leaves it to the server
	 * (`WolfFood`); with the rule off it's beta's question.
	 */
	@WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/FoodItem;isMeat()Z"))
	private boolean wolfpacks$eats(FoodItem food, Operation<Boolean> isMeat, @Local(argsOnly = true) PlayerEntity player) {
		WolfEntity wolf = (WolfEntity) (Object) this;
		wolfpacks$fed = null;
		wolfpacks$feeding = false;
		if (wolf.world.isRemote) return false;
		if (!WolfFood.on()) return isMeat.call(food);
		if (!WolfFood.eats(food)) return false;
		wolfpacks$feeding = true;
		if (!WolfFood.hungry(wolf)) {
			WolfFood.sendHandBack(player);
			return false;
		}
		wolfpacks$fed = food;
		return true;
	}

	/**
	 * Beta's owner check, after food it didn't eat, leads to sitting or standing the wolf. With something it eats in hand that click is only about feeding:
	 * eaten or not, it never sits or stands it. Only where the world runs, where beta's toggle is (a client's does nothing), so any client gets it.
	 */
	@WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Ljava/lang/String;equalsIgnoreCase(Ljava/lang/String;)Z"))
	private boolean wolfpacks$notSitting(String name, String owner, Operation<Boolean> equalsIgnoreCase) {
		boolean feeding = wolfpacks$feeding;
		wolfpacks$feeding = false;
		return !feeding && equalsIgnoreCase.call(name, owner);
	}

	/** Beta heals a raw porkchop's worth for any meat; this, the food's own worth, or Hearty Food's way. */
	@WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;heal(I)V"))
	private void wolfpacks$feed(WolfEntity wolf, int porkchop, Operation<Void> heal) {
		FoodItem food = wolfpacks$fed;
		wolfpacks$fed = null;
		// Eaten: beta returns before its owner check, which would otherwise read this on a later click with an empty hand.
		wolfpacks$feeding = false;
		if (food == null) heal.call(wolf, porkchop);
		else WolfFood.eat(wolf, food.getHealthRestored(), amount -> heal.call(wolf, amount));
	}

	/** Begging, in single player (a client on a server never sets the look it begs by): for fish too, while the rule's on. */
	@WrapOperation(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/FoodItem;isMeat()Z"))
	private boolean wolfpacks$begs(FoodItem food, Operation<Boolean> isMeat) {
		return WolfFood.on() ? WolfFood.eats(food) : isMeat.call(food);
	}
}
