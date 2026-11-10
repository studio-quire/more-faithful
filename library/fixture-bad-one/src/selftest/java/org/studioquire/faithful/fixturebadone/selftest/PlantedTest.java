package org.studioquire.faithful.fixturebadone.selftest;

import org.studioquire.faithful.harness.test.WorldTest;

/** A test that fails or says nothing, on word from `libraryBadTest`. */
public final class PlantedTest extends WorldTest {
	@Override
	protected void script() {
		String bad = System.getProperty("faithful.bad", "");
		if (bad.contains("selftest_silent")) return;
		check("what a mod's test says reaches the harness", !bad.contains("selftest_fails"));
	}
}
