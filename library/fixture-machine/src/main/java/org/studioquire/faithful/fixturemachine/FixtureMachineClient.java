package org.studioquire.faithful.fixturemachine;

import org.lwjgl.input.Keyboard;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.Keys;

public final class FixtureMachineClient implements FaithfulClientMod {
	@Override
	public void initClient(Mod mod) {
		Keys.onPressed(Keys.register("key.fixture-machine.lamp", Keyboard.KEY_F), () -> {
		});
	}
}
