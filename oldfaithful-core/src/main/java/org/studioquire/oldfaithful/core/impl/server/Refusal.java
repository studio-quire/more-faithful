package org.studioquire.oldfaithful.core.impl.server;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.server.network.ConnectionListener;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.studioquire.oldfaithful.core.mixin.server.ConnectionListenerAccessor;

/**
 * A dedicated server the core turned down stops. With someone at its console (a start.bat double-clicked, a terminal) the window would close
 * with the reason in it, so there it first closes its listener, so nobody can join and wait at Logging in, and waits for Enter. Without a
 * terminal (input from a pipe or a file: a service, the tests, most container setups) it never waits. A container or host panel that gives
 * the server a terminal counts as a console: there it waits, port closed, until Enter or the panel's stop command, which ends the wait.
 */
public final class Refusal {
	/** Set as the core closes the listener: its thread's accept then fails, which is no fault to print. */
	public static volatile boolean listenerClosed;

	private Refusal() {
	}

	/**
	 * Waits for a line at an interactive console, the listener closed first. {@code console} is beta's own reader of the console, which reads
	 * one more line (or the end of the input, or an error) and then ends, the server having stopped: waiting for it to end is waiting for Enter,
	 * with no second reader taking lines from it.
	 */
	public static void holdForConsole(ConnectionListener listener, Thread console) {
		if (console == null || !console.isAlive() || !interactive()) return;
		close(listener);
		System.out.println("Press Enter to close.");
		try {
			console.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Whether this process reads from and writes to a terminal: Java 22 has a console even when they're redirected, and says by isTerminal. */
	private static boolean interactive() {
		Console console = System.console();
		if (console == null) return false;
		try {
			return (Boolean) Console.class.getMethod("isTerminal").invoke(console);
		} catch (NoSuchMethodException e) {
			// Before Java 22 there's a console only when both ends are a terminal.
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	/** No more joining; anyone who got in while the server was starting is told, rather than left at Logging in. */
	private static void close(ConnectionListener listener) {
		if (listener == null) return;
		ConnectionListenerAccessor open = (ConnectionListenerAccessor) listener;
		listenerClosed = true;
		listener.open = false;
		try {
			open.faithful$socket().close();
		} catch (IOException e) {
			System.out.println("[faithful-core] WARN Could not close the server's port: " + e);
		}
		try {
			open.faithful$thread().join(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		for (Object pending : new ArrayList<>(open.faithful$pending())) ((ServerLoginNetworkHandler) pending).disconnect("The server couldn't start.");
	}
}
