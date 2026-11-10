package org.studioquire.oldfaithful.core.api;

/** A world rule, read where the rule applies: {@link #get} is the current world's value. */
public interface BoolRule extends Rule {
	boolean get();

	boolean defaultValue();
}
