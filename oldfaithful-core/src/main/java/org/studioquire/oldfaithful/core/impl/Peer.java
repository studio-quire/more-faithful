package org.studioquire.oldfaithful.core.impl;

import java.util.HashSet;
import java.util.Set;

/** What one connection's other end has said about itself. */
public final class Peer {
	/** The peer spoke the hello, so it may be sent 250 packets. */
	public volatile boolean core;
	/** Both ends have the content layer: stack data rides in slot packets, and ids past 255 in block packets. */
	public volatile boolean content;
	public boolean helloSent;
	public boolean replied;
	/** The login held until the reply comes. */
	public Object heldLogin;
	/** The channels both ends have, by the number the hello gave them. */
	public final Set<Integer> channels = new HashSet<>();
}
