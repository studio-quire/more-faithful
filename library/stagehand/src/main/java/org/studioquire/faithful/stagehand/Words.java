package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/** What follows a command: plain words, and {@code key=value} options. A step takes what it understands, and whatever's left over is the script's error. */
final class Words {
	private static final double FARTHEST = 30_000_000.0;
	private static final String NUMBER = "-?\\d{1,9}(\\.\\d{1,6})?";

	final List<String> plain = new ArrayList<>();
	final Map<String, String> options = new LinkedHashMap<>();

	Words(String rest) {
		if (rest.isEmpty()) return;
		for (String word : rest.split("\\s+")) {
			int equals = word.indexOf('=');
			if (equals > 0) options.put(word.substring(0, equals).toLowerCase(Locale.ROOT), word.substring(equals + 1));
			else plain.add(word);
		}
	}

	boolean has(int index) {
		return index < plain.size();
	}

	String get(int index) {
		return plain.get(index);
	}

	String next(String what) {
		if (plain.isEmpty()) throw new Failure("missing", "this wants " + what);
		return plain.remove(0);
	}

	String option(String key) {
		return options.remove(key);
	}

	/** Takes this word out if it's there, anywhere. */
	boolean flag(String word) {
		return plain.removeIf(word::equalsIgnoreCase);
	}

	void finished() {
		if (!plain.isEmpty()) throw new Failure("extra_words", "didn't expect " + String.join(" ", plain));
		if (!options.isEmpty()) throw new Failure("extra_options", "didn't expect " + String.join(", ", options.keySet()));
	}

	/** The next word as a whole number in this range. */
	int number(String what, int least, int most) {
		return number(what, next(what), least, most);
	}

	static int number(String what, String word, int least, int most) {
		int value;
		try {
			value = Integer.parseInt(word);
		} catch (NumberFormatException e) {
			throw new Failure("bad_number", what + " is a whole number, not " + word);
		}
		if (value < least || value > most) throw new Failure("bad_number", what + " is from " + least + " to " + most + ", not " + word);
		return value;
	}

	/** {@code 40} or {@code 40t} is ticks, {@code 5s} seconds, {@code 2m} minutes. */
	static int ticks(String what, String word, int most) {
		String digits = word;
		int scale = 1;
		if (word.endsWith("t")) digits = word.substring(0, word.length() - 1);
		else if (word.endsWith("s")) {
			digits = word.substring(0, word.length() - 1);
			scale = 20;
		} else if (word.endsWith("m")) {
			digits = word.substring(0, word.length() - 1);
			scale = 20 * 60;
		}
		long value;
		try {
			value = Long.parseLong(digits) * scale;
		} catch (NumberFormatException e) {
			throw new Failure("bad_number", what + " is a time like 40, 5s or 2m, not " + word);
		}
		if (value < 0 || value > most) throw new Failure("bad_number", what + " is at most " + most + " ticks, not " + word);
		return (int) value;
	}

	static float decimal(String what, String word, float least, float most) {
		if (!word.matches(NUMBER)) throw new Failure("bad_number", what + " is a number, not " + word);
		float value = Float.parseFloat(word);
		if (value < least || value > most) throw new Failure("bad_number", what + " is from " + least + " to " + most + ", not " + word);
		return value;
	}

	static boolean isCoordinate(String word) {
		return word.equals("~") || word.equals("@me") || word.matches("~?" + NUMBER);
	}

	/** A number, or {@code ~} and an offset from where {@code @me} stands. */
	static final class Coordinate {
		final boolean relative;
		final double value;

		Coordinate(String word) {
			relative = word.startsWith("~");
			String digits = relative ? word.substring(1) : word;
			if (relative && digits.isEmpty()) {
				value = 0.0;
				return;
			}
			if (!digits.matches(NUMBER)) throw new Failure("bad_coordinate", "a coordinate is a number, or ~ and a number: not " + word);
			value = Double.parseDouble(digits);
			if (Math.abs(value) > FARTHEST) throw new Failure("bad_coordinate", "the world stops being safe past " + (long) FARTHEST + ": not " + word);
		}

		double from(double base) {
			double result = relative ? base + value : value;
			if (Math.abs(result) > FARTHEST) throw new Failure("bad_coordinate", "the world stops being safe past " + (long) FARTHEST);
			return result;
		}
	}

	/** Three coordinates, or {@code @me} for {@code ~ ~ ~}. */
	static final class Position {
		final Coordinate x, y, z;

		Position(Coordinate x, Coordinate y, Coordinate z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		boolean relative() {
			return x.relative || y.relative || z.relative;
		}

		/** Worked out from {@code @me}'s cell. */
		double[] at(Run run) {
			if (!relative()) return new double[] {x.value, y.value, z.value};
			PlayerEntity me = run.me();
			return new double[] {x.from(MathHelper.floor(me.x)), y.from(MathHelper.floor(me.boundingBox.minY)), z.from(MathHelper.floor(me.z))};
		}
	}

	Position position(String what) {
		String first = next(what);
		if (first.equals("@me")) return new Position(new Coordinate("~"), new Coordinate("~"), new Coordinate("~"));
		return new Position(new Coordinate(first), new Coordinate(next(what)), new Coordinate(next(what)));
	}
}
