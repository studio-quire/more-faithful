package org.studioquire.faithful.towercheck.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;
import org.studioquire.faithful.buriedtowers.Towers;

/** What the driver knows of the mod, here so a run without it never loads a class that names it. */
final class Survey {
	private Survey() {
	}

	/** The towers whose squares fall in a square of chunks, a line each: x z climate purpose floors. */
	static void sites(World world, int x0, int z0, int size, Path file) throws IOException {
		if (!TowerCheck.TOWERS) {
			Files.write(file, new byte[0]);
			return;
		}
		List<String> lines = new ArrayList<>();
		for (Towers.Found t : Towers.within(world, x0, z0, size)) {
			StringBuilder floors = new StringBuilder();
			for (Towers.FoundFloor f : t.floors) floors.append(f.kind.replace(' ', '_')).append('>').append(f.wayDown == null ? "-" : f.wayDown.replace(' ', '_')).append(' ');
			lines.add(t.x + " " + t.z + " " + t.climate + " " + t.purpose + " " + t.groundY + " " + t.gateX + " " + t.gateZ + " " + floors.toString().trim());
		}
		Files.write(file, lines);
	}

	/** Where the sites of a square of squares would stand, kept {@code margin} from their squares' edges, a line each: x z. */
	static void places(World world, int rx0, int rz0, int squares, int margin, Path file) throws IOException {
		List<String> lines = new ArrayList<>();
		if (TowerCheck.TOWERS) for (int[] p : Towers.places(world, rx0, rz0, squares, margin)) lines.add(p[0] + " " + p[1]);
		Files.write(file, lines);
	}

	/** For each square of a square of squares, its site and a digest of its tower as drawn, a line each: rx rz x z digest, or rx rz none. */
	static void plans(World world, int rx0, int rz0, int squares, Path file) throws IOException {
		List<String> lines = new ArrayList<>();
		if (TowerCheck.TOWERS) {
			for (int rx = rx0; rx < rx0 + squares; rx++) {
				for (int rz = rz0; rz < rz0 + squares; rz++) {
					java.security.MessageDigest digest;
					try {
						digest = java.security.MessageDigest.getInstance("SHA-256");
					} catch (java.security.NoSuchAlgorithmException e) {
						throw new IllegalStateException(e);
					}
					java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
					String at = Towers.planDump(world, rx * 24 * 16, rz * 24 * 16, bytes);
					StringBuilder hex = new StringBuilder();
					for (byte b : digest.digest(bytes.toByteArray())) hex.append(String.format("%02x", b));
					lines.add(rx + " " + rz + " " + (at.equals("none") ? "none" : at + " " + hex.substring(0, 24)));
				}
			}
		}
		Files.write(file, lines);
	}

	static void report(World world, int x0, int z0, int size, Path file) throws IOException {
		if (!TowerCheck.TOWERS) Files.write(file, new byte[0]);
		else Files.write(file, Towers.report(world, x0, z0, size));
	}

	/** A tower's cells as they stand in the world, in the form {@code tools/tower_view.py} reads, from the world. */
	static String tower(World world, int x, int z, Path file) throws IOException {
		if (!TowerCheck.TOWERS) return "none";
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file), 1 << 16)) {
			return Towers.dump(world, x, z, out);
		}
	}
}
