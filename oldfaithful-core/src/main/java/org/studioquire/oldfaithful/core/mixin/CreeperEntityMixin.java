package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.studioquire.oldfaithful.core.impl.Flight;

/** A creeper's flight from what frightens it (`Creepers`) lasts from tick to tick and belongs to it alone. Never saved. */
@Mixin(CreeperEntity.class)
abstract class CreeperEntityMixin implements Flight.Holder {
	@Unique
	private Flight faithful$running;

	@Override
	public Flight faithful$flight() {
		return faithful$running;
	}

	@Override
	public void faithful$setFlight(Flight flight) {
		faithful$running = flight;
	}
}
