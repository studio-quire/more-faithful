package org.studioquire.oldfaithful.core.api;

/** A veto: once any listener cancels, it stays cancelled, and the listeners after it still hear the event and can read that. */
public abstract class Cancellable {
	private boolean cancelled;

	public final void cancel() {
		cancelled = true;
	}

	public final boolean cancelled() {
		return cancelled;
	}
}
