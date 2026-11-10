package org.studioquire.faithful.stagehand;

import java.util.List;
import java.util.Map;

/** Maps, lists, strings, numbers, booleans and nulls, as JSON that's still readable in a text editor: the outer levels on lines of their own. */
final class Json {
	private static final int INDENTED_LEVELS = 3;

	private Json() {
	}

	static String write(Object value) {
		StringBuilder out = new StringBuilder();
		write(value, out, 0);
		return out.append('\n').toString();
	}

	private static void write(Object value, StringBuilder out, int depth) {
		if (value == null || value instanceof Boolean) {
			out.append(value);
		} else if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) out.append("null");
			else out.append(value);
		} else if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			boolean first = true;
			out.append('{');
			for (Map.Entry<?, ?> e : map.entrySet()) {
				if (!first) out.append(depth < INDENTED_LEVELS ? "," : ", ");
				first = false;
				newline(out, depth + 1);
				string(String.valueOf(e.getKey()), out);
				out.append(": ");
				write(e.getValue(), out, depth + 1);
			}
			if (!first && depth < INDENTED_LEVELS) newline(out, depth);
			out.append('}');
		} else if (value instanceof List) {
			boolean first = true;
			out.append('[');
			for (Object item : (List<?>) value) {
				if (!first) out.append(depth < INDENTED_LEVELS ? "," : ", ");
				first = false;
				newline(out, depth + 1);
				write(item, out, depth + 1);
			}
			if (!first && depth < INDENTED_LEVELS) newline(out, depth);
			out.append(']');
		} else {
			string(String.valueOf(value), out);
		}
	}

	private static void newline(StringBuilder out, int depth) {
		if (depth > INDENTED_LEVELS) return;
		out.append('\n');
		for (int i = 0; i < depth; i++) out.append("  ");
	}

	private static void string(String s, StringBuilder out) {
		out.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': out.append("\\\""); break;
				case '\\': out.append("\\\\"); break;
				case '\n': out.append("\\n"); break;
				case '\r': out.append("\\r"); break;
				case '\t': out.append("\\t"); break;
				default:
					if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
					else out.append(c);
			}
		}
		out.append('"');
	}
}
