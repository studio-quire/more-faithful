package org.studioquire.oldfaithful.core.impl.content;

import java.util.List;

/** Implemented by the game's {@code ScreenHandler}: who's watching it, which only its subclasses can see. */
public interface ListenerAccess {
	List<?> faithful$listeners();
}
