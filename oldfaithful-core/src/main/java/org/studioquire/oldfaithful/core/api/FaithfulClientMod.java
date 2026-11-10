package org.studioquire.oldfaithful.core.api;

/** The client side of a library mod, under the {@code faithful:client} entrypoint; runs after every {@link FaithfulMod}. */
public interface FaithfulClientMod {
	default void initClient(Mod mod) {
	}
}
