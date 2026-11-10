package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.DayClock;

/** The sun's clock, saved only where it differs from the time. In level.dat rather than beside it: a vanilla save loses it with the mods, and all that costs is where the sun is. */
@Mixin(WorldProperties.class)
abstract class WorldPropertiesMixin implements DayClock.Holder {
	@Unique
	private static final String KEY = "FaithfulDayDrift";
	@Unique
	private long faithful$drift;
	@Unique
	private int faithful$part;

	@Override
	public long faithful$drift() {
		return faithful$drift;
	}

	@Override
	public void faithful$drift(long drift) {
		faithful$drift = drift;
	}

	@Override
	public int faithful$part() {
		return faithful$part;
	}

	@Override
	public void faithful$part(int part) {
		faithful$part = part;
	}

	@Inject(method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
	private void faithful$read(NbtCompound nbt, CallbackInfo ci) {
		faithful$drift = nbt.getLong(KEY);
	}

	/** Single player's way through a portal. */
	@Inject(method = "<init>(Lnet/minecraft/world/WorldProperties;)V", at = @At("TAIL"))
	private void faithful$copy(WorldProperties properties, CallbackInfo ci) {
		faithful$drift = ((DayClock.Holder) properties).faithful$drift();
		faithful$part = ((DayClock.Holder) properties).faithful$part();
	}

	@Inject(method = "updateProperties", at = @At("TAIL"))
	private void faithful$write(NbtCompound nbt, NbtCompound playerNbt, CallbackInfo ci) {
		if (faithful$drift != 0) nbt.putLong(KEY, faithful$drift);
	}
}
