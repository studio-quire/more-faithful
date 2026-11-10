package org.studioquire.faithful.harness.client;

import net.minecraft.client.Minecraft;
import org.studioquire.faithful.harness.Probe;
import org.studioquire.faithful.harness.Scenario;
import org.studioquire.faithful.harness.test.client.ClientTests;
import org.studioquire.faithful.harness.test.client.WithoutTests;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;

/** The client's end of a harness run, in the start-up test's world. */
public final class ProbeWorldCheck implements WorldCheck {
	private Monkey monkey;
	private ClientTests tests;
	private WithoutTests without;

	@Override
	public void start(Minecraft mc, SelfTest results) {
		Probe.report();
		ClientRules.run(mc);
		if (!mc.world.isRemote) Scenario.runAll(mc.world, mc.player);
		if (mc.world.isRemote && Boolean.getBoolean("faithful.harness.monkey")) monkey = new Monkey(Probe.state);
		tests = ClientTests.start(results);
		without = WithoutTests.start(results);
		results.pass("the probe ran");
	}

	@Override
	public boolean tick(Minecraft mc) {
		if (tests != null && !tests.tick(mc)) return false;
		if (without != null && !without.tick(mc)) return false;
		return monkey == null || monkey.tick(mc);
	}
}
