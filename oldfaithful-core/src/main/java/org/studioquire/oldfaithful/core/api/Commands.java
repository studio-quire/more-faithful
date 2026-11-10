package org.studioquire.oldfaithful.core.api;

import net.minecraft.entity.player.PlayerEntity;
import org.studioquire.oldfaithful.core.impl.CommandTable;

/**
 * Server commands. There are none in single player: beta only opens chat on a server, and that stays. A mod's commands come ahead
 * of the server's own, and its names are its own to check against the harness's list. A handler's throw is logged and answered,
 * and after five the command is turned off, as with a listener.
 */
public final class Commands {
	public interface Handler {
		/** {@code args} is what followed the name. */
		void run(String[] args, Reply reply);
	}

	public interface Reply {
		void say(String line);

		/** Who typed it, or null for the console. */
		default PlayerEntity player() {
			return null;
		}
	}

	private Commands() {
	}

	/** For the console and ops. {@code help} is the line the server's help prints. */
	public static void register(String name, String help, Handler handler) {
		register(name, help, false, handler);
	}

	/** With {@code everyone} true, any player may type it too, as they may {@code /me}. */
	public static void register(String name, String help, boolean everyone, Handler handler) {
		CommandTable.add(name, help, everyone, handler);
	}
}
