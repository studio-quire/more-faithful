package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A script's text as steps, and everything wrong with it. A script with anything wrong doesn't run at all. A line is a command and its words; {@code #} starts a comment line. */
final class Script {
	private static final int MOST_LINES = 1024, LONGEST_LINE = 2000;

	final List<Step> steps = new ArrayList<>();
	final List<Map<String, Object>> errors = new ArrayList<>();

	private Script() {
	}

	static Script parse(String text) {
		Script script = new Script();
		String[] lines = text.split("\\r?\\n", -1);
		if (lines.length > MOST_LINES) {
			script.error(0, "too_long", "a script is at most " + MOST_LINES + " lines");
			return script;
		}
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty() || line.startsWith("#")) continue;
			try {
				if (line.length() > LONGEST_LINE) throw new Failure("too_long", "a line is at most " + LONGEST_LINE + " characters");
				Step step = line(line);
				step.line = i + 1;
				step.text = line;
				script.steps.add(step);
			} catch (Failure failure) {
				script.error(i + 1, failure.code, failure.getMessage());
			}
		}
		if (script.steps.isEmpty() && script.errors.isEmpty()) script.error(0, "empty", "there's nothing in the script");
		return script;
	}

	private void error(int line, String code, String message) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("code", code);
		error.put("message", message);
		if (line > 0) error.put("line", line);
		errors.add(error);
	}

	private static Step line(String line) {
		int space = line.indexOf(' ');
		String command = (space < 0 ? line : line.substring(0, space)).toLowerCase(Locale.ROOT);
		String rest = space < 0 ? "" : line.substring(space + 1).trim();
		if (command.equals("say")) return new Steps.Say(rest);
		Words words = new Words(rest);
		Step step;
		switch (command) {
			case "as": step = new Steps.As(words); break;
			case "tp": step = new Steps.Teleport(words); break;
			case "locate": step = new Steps.Locate(words); break;
			case "give": step = new Steps.Give(words); break;
			case "kit": step = new Steps.Kit(words); break;
			case "heal": step = new Steps.Heal(words); break;
			case "health": step = new Steps.Health(words); break;
			case "clear": step = new Steps.Clear(words); break;
			case "time": step = new Steps.Time(words); break;
			case "weather": step = new Steps.Weather(words); break;
			case "spawn": step = new Steps.Spawn(words); break;
			case "setblock": step = new Steps.SetBlock(words); break;
			case "fill": step = new Steps.Fill(words); break;
			case "wait": step = words.has(0) && words.get(0).equalsIgnoreCase("until") ? new Steps.WaitUntil(words) : new Steps.Wait(words); break;
			case "status": step = new Steps.Status(words); break;
			case "cancel": step = new Steps.Cancel(words); break;
			case "list": step = new Steps.Lists(words); break;
			default:
				throw new Failure("unknown_command", "no command called " + command + "; there's as, tp, locate, give, kit, heal, health, clear, time, weather, spawn, setblock, fill, say, wait, status, cancel, list");
		}
		words.finished();
		return step;
	}
}
