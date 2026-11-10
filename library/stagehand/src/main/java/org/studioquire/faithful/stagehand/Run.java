package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** A script on its way through the game: its steps in order, as far as each tick lets them go, and its result on disk whenever anything changes. */
final class Run {
	/** A script can't hold the tick for long, however many instant lines it has. */
	private static final int MOST_STEPS_A_TICK = 64;
	/** Past this, a result that can't be written is given up on: about a minute. */
	private static final int MOST_WRITE_TRIES = 1200;

	final String id;
	private final Script script;
	private final long started;
	private final List<Map<String, Object>> done = new ArrayList<>();
	private int at;
	private String state = "running";
	private Map<String, Object> error;
	private long finished;
	private boolean dirty = true, wasPaused;
	private int writeTries;
	private boolean logged;

	/** Who {@code @me} is, once {@code as} has said. */
	String as;
	Side side;

	Run(String id, Script script, long started) {
		this.id = id;
		this.script = script;
		this.started = started;
		if (!script.errors.isEmpty()) {
			state = "failed";
			error = new LinkedHashMap<>(script.errors.get(0));
			finished = started;
		}
	}

	/** True when it's over and written. */
	boolean tick(Side here) {
		side = here;
		if (state.equals("running")) advance();
		boolean paused = here.paused();
		if (paused != wasPaused && state.equals("running")) dirty = true;
		wasPaused = paused;
		if (dirty) {
			if (Inbox.write(id, result())) {
				dirty = false;
			} else if (++writeTries >= MOST_WRITE_TRIES) {
				System.out.println("[stagehand] WARN can't write the result of " + id);
				dirty = false;
			}
		}
		boolean over = !state.equals("running") && !dirty;
		if (over && !logged) {
			logged = true;
			System.out.println("[stagehand] " + id + ": " + state + ", " + done.size() + " of " + script.steps.size() + " steps");
		}
		return over;
	}

	private void advance() {
		for (int count = 0; count < MOST_STEPS_A_TICK; count++) {
			if (at >= script.steps.size()) {
				end("done", null, null);
				return;
			}
			Step step = script.steps.get(at);
			boolean over;
			try {
				over = step.run(this);
			} catch (Failure failure) {
				fail(step, failure.code, failure.getMessage());
				return;
			} catch (RuntimeException | LinkageError | StackOverflowError e) {
				System.out.println("[stagehand] WARN line " + step.line + " of " + id + " threw: " + e);
				e.printStackTrace(System.out);
				fail(step, "threw", e.toString());
				return;
			}
			if (!over) return;
			done.add(step.report(true, null));
			dirty = true;
			at++;
		}
	}

	private void fail(Step step, String code, String message) {
		Map<String, Object> problem = new LinkedHashMap<>();
		problem.put("code", code);
		problem.put("message", message);
		problem.put("line", step.line);
		done.add(step.report(false, problem));
		state = "failed";
		error = problem;
		finished = System.currentTimeMillis();
		dirty = true;
	}

	void end(String how, String code, String message) {
		if (!state.equals("running")) return;
		state = how;
		finished = System.currentTimeMillis();
		dirty = true;
		if (code != null) {
			error = new LinkedHashMap<>();
			error.put("code", code);
			error.put("message", message);
		}
	}

	private Map<String, Object> result() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", id);
		out.put("state", state);
		out.put("ok", state.equals("done"));
		out.put("where", side.where());
		if (state.equals("running") && side.paused()) out.put("paused", true);
		out.put("started_ms", started);
		if (finished != 0) out.put("finished_ms", finished);
		if (error != null) out.put("error", error);
		if (script.errors.size() > 1) out.put("errors", script.errors);
		if (state.equals("running") && at < script.steps.size()) out.put("waiting_on", script.steps.get(at).text);
		out.put("steps", done);
		int notRun = script.steps.size() - done.size();
		if (notRun > 0 && !state.equals("running")) out.put("not_run", notRun);
		return out;
	}

	// Who and where. Each looks afresh: a respawn makes a new player, and in single player a change of dimension a new world.

	static PlayerEntity player(String name) {
		for (PlayerEntity p : Server.players()) {
			if (p.name.equalsIgnoreCase(name)) return p;
		}
		return null;
	}

	static List<String> names() {
		List<String> names = new ArrayList<>();
		for (PlayerEntity p : Server.players()) names.add(p.name);
		return names;
	}

	/** {@code @me}: whoever {@code as} named, or else the only player here. */
	PlayerEntity me() {
		if (as != null) {
			PlayerEntity p = player(as);
			if (p == null) throw new Failure("no_player", as + " isn't here; here: " + names());
			return p;
		}
		List<PlayerEntity> all = Server.players();
		if (all.isEmpty()) throw new Failure("no_player", "nobody is in the world");
		if (all.size() > 1) throw new Failure("which_player", "there's more than one player here, " + names() + ", so say which with: as <name>");
		return all.get(0);
	}

	PlayerEntity meOrNull() {
		try {
			return me();
		} catch (Failure nobody) {
			return null;
		}
	}

	/** The world asked for by {@code dim=}, or else {@code @me}'s, or with nobody here the overworld. */
	World world(Integer dimension) {
		if (dimension != null) {
			World world = Server.world(dimension);
			if (world == null) throw new Failure("dimension_not_running", Move.dimension(dimension) + " isn't running here: single player runs only the one the player is in");
			return world;
		}
		PlayerEntity p = meOrNull();
		if (p != null) return p.world;
		World overworld = Server.world(Worldgen.OVERWORLD);
		if (overworld == null) throw new Failure("no_world", "no world is running");
		return overworld;
	}

	Places.Place find(Places places, List<String> words, Map<String, String> options) {
		PlayerEntity p = meOrNull();
		World world = world(null);
		int x, y, z;
		if (p != null) {
			x = MathHelper.floor(p.x);
			y = MathHelper.floor(p.boundingBox.minY);
			z = MathHelper.floor(p.z);
		} else {
			Vec3i spawn = world.getSpawnPos();
			x = spawn.x;
			y = spawn.y;
			z = spawn.z;
		}
		Places.Place place;
		try {
			place = places.find(new Places.Query(world, x, y, z, words, options));
		} catch (IllegalArgumentException e) {
			throw new Failure("bad_place", places.kind() + ": " + e.getMessage());
		}
		if (place == null) throw new Failure("no_such_place", "no " + places.kind() + " answers that");
		return place;
	}
}
