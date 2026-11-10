package org.studioquire.faithful.stagehand;

import java.util.LinkedHashMap;
import java.util.Map;

/** A line of a script. Made when the script is read, so bad input stops it before anything has run; run on the world's thread. */
abstract class Step {
	int line;
	String text;
	/** What it found or did, in the result. */
	final Map<String, Object> data = new LinkedHashMap<>();

	/** Called each tick until it says it's over. A {@link Failure} ends the script. */
	abstract boolean run(Run run);

	final Map<String, Object> report(boolean ok, Map<String, Object> error) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("line", line);
		out.put("command", text);
		out.put("ok", ok);
		if (error != null) out.put("error", error);
		if (!data.isEmpty()) out.put("data", data);
		return out;
	}
}
