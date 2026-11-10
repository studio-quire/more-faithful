package org.studioquire.oldfaithful.core.api;

/** What a mod gets from the core. Names it declares through this are namespaced by its id, so they can't collide. */
public interface Mod {
	String id();

	/** This mod's world rules. */
	RuleSet rules();

	/** A network channel, {@code <mod id>:<name>}; the protocol number is compared at the hello. */
	Channel channel(String name, int protocol);

	/** Data on players, entities or chunks, {@code <mod id>:<name>}, in {@code init}. See {@link Attachment}. */
	<H, T> Attachment<H, T> attachment(String name, Attachment.Spec<H, T> spec);
}
