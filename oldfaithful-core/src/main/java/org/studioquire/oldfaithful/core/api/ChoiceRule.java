package org.studioquire.oldfaithful.core.api;

/** A world rule of a few named values, read where the rule applies: {@link #get} is the current world's value, one of {@link #values}. */
public interface ChoiceRule extends Rule {
	String get();
}
