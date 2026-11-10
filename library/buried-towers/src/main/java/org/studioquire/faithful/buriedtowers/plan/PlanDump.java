package org.studioquire.faithful.buriedtowers.plan;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * A tower's cells and floors in a file for {@code tools/tower_view.py}. Run as a program it draws a tower on made-up land, with no game: the fast way to look at a floor while it's being designed. The
 * test driver writes the same file from what really stood in a world.
 */
public final class PlanDump {
	/** {@code seed x z climate groundHeight file [SIMPLE|COMPLEX]}, simple if not said. */
	public static void main(String[] args) throws IOException {
		long seed = Long.parseLong(args[0]);
		int x = Integer.parseInt(args[1]), z = Integer.parseInt(args[2]);
		Climate climate = Climate.valueOf(args[3]);
		int land = Integer.parseInt(args[4]);
		Noise roll = new Noise(seed);
		Ground ground = (ax, az) -> land + (int) ((roll.at(ax, 0, az, 19) - 0.5) * 10);
		Tower t = new Tower(seed, x, z, climate, ground, args.length > 6 ? Style.valueOf(args[6]) : Style.SIMPLE);
		Path file = Paths.get(args[5]);
		Files.createDirectories(file.toAbsolutePath().getParent());
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file), 1 << 16)) {
			write(t, (cx, cy, cz) -> {
				int c = t.plan.get(cx, cy, cz);
				if (c != Plan.KEEP) return c;
				if (cy == ground.top(cx, cz) - 1) return Blocks.GRASS;
				if (cy == ground.top(cx, cz) - 2) return Blocks.DIRT;
				if (cy < ground.top(cx, cz)) return Blocks.STONE;
				return cy < Ground.SEA ? Blocks.WATER : Blocks.AIR;
			}, out);
		}
	}

	/** What's standing in a cell, however it's come by. */
	public interface Cells {
		int get(int x, int y, int z);
	}

	public static void write(Tower t, Cells cells, OutputStream stream) throws IOException {
		DataOutputStream out = new DataOutputStream(stream);
		out.writeInt(t.plan.ox);
		out.writeInt(t.plan.oz);
		out.writeInt(t.plan.size);
		out.writeInt(t.cx);
		out.writeInt(t.cz);
		out.writeInt(t.radius);
		byte[] ids = new byte[Plan.HEIGHT], metas = new byte[Plan.HEIGHT];
		for (int x = t.plan.ox; x < t.plan.ox + t.plan.size; x++) {
			for (int z = t.plan.oz; z < t.plan.oz + t.plan.size; z++) {
				for (int y = 0; y < Plan.HEIGHT; y++) {
					int c = cells.get(x, y, z);
					ids[y] = (byte) Blocks.id(c);
					metas[y] = (byte) Blocks.meta(c);
				}
				out.write(ids);
				out.write(metas);
			}
		}
		StringBuilder json = new StringBuilder();
		json.append("{\"seed\":").append(t.seed).append(",\"style\":\"").append(t.style).append('"').append(",\"climate\":\"").append(t.climate).append("\",\"purpose\":\"").append(t.purpose).append("\",\"groundY\":").append(t.groundY);
		json.append(",\"gate\":[").append(t.gateX).append(',').append(t.gateZ).append("],\"gap\":").append(t.gapUnder);
		if (t.hidden != null) {
			json.append(",\"hidden\":{\"room\":[");
			for (int i = 0; i < 6; i++) json.append(i > 0 ? "," : "").append(t.hidden[i]);
			json.append("],\"chest\":[").append(t.hiddenChest[0]).append(',').append(t.hiddenChest[1]).append(',').append(t.hiddenChest[2]).append("]}");
		}
		if (t.cart != null) {
			int[] a = t.cart.start(), b = t.cart.end();
			json.append(",\"cart\":{\"start\":[").append(a[0]).append(',').append(a[1]).append(',').append(a[2]).append("],\"end\":[").append(b[0]).append(',').append(b[1]).append(',').append(b[2]);
			json.append("],\"lever\":[").append(t.cart.leverX).append(',').append(t.cart.leverY).append(',').append(t.cart.leverZ).append("],\"from\":").append(t.cart.up.index).append(",\"to\":").append(t.cart.low.index).append('}');
		}
		json.append(",\"floors\":[");
		boolean first = true;
		for (Floor f : t.floors) {
			if (!first) json.append(',');
			first = false;
			json.append("{\"index\":").append(f.index).append(",\"kind\":\"").append(f.kindName()).append("\",\"palette\":\"").append(f.pal.name).append("\",\"y\":").append(f.y).append(",\"h\":").append(f.h);
			json.append(",\"stand\":").append(f.stand).append(",\"leave\":").append(f.leave).append(",\"safe\":[").append(f.safeX).append(',').append(f.safeY).append(',').append(f.safeZ).append(']');
			json.append(",\"down\":").append(f.out == null ? "null" : "\"" + f.out.kind + "\"");
			if (f.out != null) json.append(",\"mouth\":[").append(f.out.ux).append(',').append(f.out.uz).append("],\"landing\":[").append(f.out.lx).append(',').append(f.out.lz).append("],\"to\":").append(f.out.low.index);
			json.append(",\"notes\":").append(f.notes.size()).append('}');
		}
		json.append("],\"fixtures\":{");
		first = true;
		for (Map.Entry<Long, String> e : t.plan.fixtures.entrySet()) {
			if (!first) json.append(',');
			first = false;
			json.append('"').append(Plan.keyX(e.getKey())).append(',').append(Plan.keyY(e.getKey())).append(',').append(Plan.keyZ(e.getKey())).append("\":\"").append(e.getValue()).append('"');
		}
		json.append("}}");
		byte[] text = json.toString().getBytes(StandardCharsets.UTF_8);
		out.writeInt(text.length);
		out.write(text);
		out.flush();
	}

	private PlanDump() {
	}
}
