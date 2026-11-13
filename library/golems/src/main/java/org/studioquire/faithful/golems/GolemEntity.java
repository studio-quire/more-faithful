package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.Stacks;

/**
 * An iron worker. Made, it stands where it is; an empty hand has it follow you, as a wolf does (with no leaps to catch up), and stay again. A tool handed to it sets its job, the tool's
 * kind, at the place it stands, its post, over its area: the rectangle of boundary poles round its post, or a square round it ({@link Area}). An empty hand takes the tool back. What a
 * job does is its {@link Worker}'s loop and its {@link Job}.
 * <p>
 * Its work goes at half a player's speed at the same thing ({@link #EXCHANGE}), and it fights at a skeleton's or a zombie's rate. Iron: it doesn't burn, drown, choke in a wall or mind cactus, and
 * isn't knocked back, but a fall, lava and blows hurt it. 20 hearts, mended an iron ingot at a time, by its owner or by itself from its chest, and drawn cracked as it
 * loses them.
 */
public class GolemEntity extends MobEntity {
	/** The exchange: a golem takes this many times as long as a player at anything. */
	public static final int EXCHANGE = 2;
	public static final int MAX_HEALTH = 40;
	/** What an iron ingot mends: a quarter. */
	public static final int MEND = 10;
	/** Slots of what it carries, past its tool. */
	public static final int SLOTS = 9;
	/** Its box: a player's (the user, 2026-11-09), its arms drawn outside it as on every beta biped. */
	public static final float WIDTH = 0.6F, HEIGHT = 1.8F;
	/** Where it looks and reaches from: a player's eye. */
	public static final float EYE = 1.62F;
	/** How far it can reach from its eye to the nearest part of what it works on: a player's, beta's 4. */
	public static final float REACH = 4.0F;
	/**
	 * How near it walks up to its work, as a person would: it stands within this of what it works on wherever it can, and uses the rest of its reach only where it can't get closer
	 * (the user, 2026-11-09, after first having a reach of 2 for everything).
	 */
	public static final float CLOSE = 2.0F;
	/**
	 * Its clank: the latch at the end of the door sound launchers give beta (a later recording under beta's name), without the swing that leads in (the user, 2026-11-10, who heard the whole door in all its sounds), the mod's own short
	 * clip cut by {@code library/golems/tools/cut_clank.py}. Hurt, handed a tool or relieved of it, mended, a spare taken.
	 */
	static final String CLANK = "golems.clank";
	/** The whole door, closing, where a longer sound suits: made from the iron, and falling apart. */
	static final String DOOR = "random.door_close";
	/** A dry click, beta's lever and dispenser: told to follow or stay. */
	private static final String CLICK = "random.click";
	private static final String TOOL_BREAK = "random.break";
	private static final int TRACK_TOOL = 16, TRACK_WORKING = 17, TRACK_HEALTH = 18;
	/** A mob's {@code movementSpeed} of 1.0 is a player's walk: working it goes at half that, a zombie's pace; following, it keeps up with you. */
	private static final float WORK_PACE = 1.0F / EXCHANGE;
	/** Following, a tamed wolf's pace, a tenth over a player's walk: at the walk itself it could never close a gap a corner or a detour made, and trailed far behind (2026-11-10). */
	private static final float FOLLOW_PACE = 1.1F;
	/** A swing is 8 ticks on a player. */
	private static final int SWING_TICKS = 8 * EXCHANGE;
	/** How far round itself it looks for a way to whoever it follows, each way. */
	private static final int FOLLOW_RANGE = 24;

	private ItemStack tool;
	private final ItemStack[] slots = new ItemStack[SLOTS];
	/** What it put down itself, so it doesn't take it straight back up. Forgotten as each is picked up or despawns, or with the world. */
	private final List<ItemEntity> putDown = new ArrayList<>();
	private boolean hasPost;
	private int postX, postY, postZ;
	private Area area;
	/** Its own number, saved with it: whose poles are whose ({@link PoleClaims}). */
	private long poleClaim;
	private Worker worker;
	/** Whose heels it's at, by name, or null when it stays. */
	private String following;
	private final Walk walk = new Walk(this, true);
	private boolean aiming, stepping, hurtByIron;
	private float stepX, stepY, stepZ, aimX, aimY, aimZ;
	/** The heading its steps had when it last walked facing its aim instead. */
	private float walkYaw;
	private boolean walkingAimed;
	private int swingTicks;
	private float stepDistance;
	private ItemStack shownTool;
	/** Blocks mined and the like since it was loaded: a test's to read. */
	public int worked;
	/** Of those, how many it did from further than {@link #CLOSE}, and the furthest: a test's to read. */
	public int stretched;
	public double furthest;
	/**
	 * What its gathers left lying, for a long run to print: with nowhere in touch of it to stand, with no way there, gone out of touch (rolled or dropped away), not got into in its steps;
	 * and gathers that ran out of time with something left.
	 */
	public final int[] leftLying = new int[5];
	/** The last piece it did from further than {@link #CLOSE}: what, from where, where it had meant to stand and what living thing was by it; for a test to print. */
	public String lastStretch = "";

	public GolemEntity(World world) {
		super(world);
		setBoundingBoxSpacing(WIDTH, HEIGHT);
		texture = "/assets/golems/textures/mob/golem.png";
		movementSpeed = WORK_PACE;
		health = MAX_HEALTH;
		poleClaim = random.nextLong();
	}

	@Override
	protected void initDataTracker() {
		dataTracker.startTracking(TRACK_TOOL, 0);
		dataTracker.startTracking(TRACK_WORKING, (byte) 0);
		dataTracker.startTracking(TRACK_HEALTH, MAX_HEALTH);
	}

	// What hurts it, and what doesn't.

	/** Hurt by nothing in particular: from fire, cactus, a wall or water, which iron doesn't mind, and from a fall or lava, which it does ({@link #hurtByIron}). */
	@Override
	public boolean damage(Entity source, int amount) {
		if (source == null && !hurtByIron) return false;
		return super.damage(source, amount);
	}

	private void hurt(int amount) {
		hurtByIron = true;
		try {
			damage(null, amount);
		} finally {
			hurtByIron = false;
		}
	}

	/**
	 * Iron is tough: whatever hurts it (blows, arrows, blasts, falls, lava) takes half (the user, 2026-11-10), always, with nothing to wear or mend. The odd half carries over to the
	 * next hurt as beta's armour carries what it rounds away for a player ({@code PlayerEntity.applyDamage}), so a zombie's 5 is two and a half a blow over a fight.
	 */
	@Override
	protected void applyDamage(int amount) {
		int halves = amount + hurtSpill;
		hurtSpill = halves % 2;
		super.applyDamage(halves / 2);
	}

	/** The half point a hurt left over, for the next. */
	private int hurtSpill;

	/** A fall hurts as a player's does, halved as every hurt is. */
	@Override
	protected void onLanding(float fallDistance) {
		hurtByIron = true;
		try {
			super.onLanding(fallDistance);
		} finally {
			hurtByIron = false;
		}
	}

	/** In lava: beta's 4 a touch, and no burning after. */
	@Override
	protected void setOnFire() {
		hurt(4);
	}

	/** Fire or lava in its box: fire it doesn't feel, and lava's hurt is {@link #setOnFire}'s. Lightning comes here too, as for beta's fireproof mobs. */
	@Override
	protected void damage(int amount) {
	}

	@Override
	public boolean canBreatheInWater() {
		return true;
	}

	/** Falling gravel or a piston can put it in a block: iron doesn't choke, and it climbs out in {@link #tick}. */
	@Override
	public boolean isInsideWall() {
		return false;
	}

	/** Too heavy to knock about, or a hit near an edge would throw it off. */
	@Override
	public void applyKnockback(Entity attacker, int amount, double dx, double dz) {
	}

	/** Back on top of the land at its post. */
	@Override
	protected void tickInVoid() {
		int cx = hasPost ? postX : MathHelper.floor(x), cz = hasPost ? postZ : MathHelper.floor(z);
		setPosition(cx + 0.5, Math.max(world.getTopSolidBlockY(cx, cz), 1), cz + 0.5);
		velocityX = velocityY = velocityZ = 0.0;
		fallDistance = 0.0F;
		setPath(null);
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	/** It walks by its own step count, heard on a server too; beta's own step is silent there. And it never tramples a field. */
	@Override
	protected boolean bypassesSteppingEffects() {
		return false;
	}

	@Override
	protected String getRandomSound() {
		return null;
	}

	@Override
	protected String getHurtSound() {
		return CLANK;
	}

	@Override
	protected String getDeathSound() {
		return DOOR;
	}

	/** Its own decisions only: a mob's idle wander would walk it off the job. */
	@Override
	protected void pathingUpdate() {
	}

	// Each tick.

	@Override
	public void tick() {
		// Iron doesn't burn: no fire to count down, and none shown.
		fireTicks = 0;
		super.tick();
		fireTicks = 0;
		// A bow at work is held drawn, not swung (drawsBow).
		if (working() && !holdsBow()) {
			swingTicks = (swingTicks + 1) % SWING_TICKS;
			swingAnimationProgress = swingTicks / (float) SWING_TICKS;
		} else {
			swingTicks = 0;
			swingAnimationProgress = 0.0F;
		}
		if (world.isRemote) return;
		if (super.isInsideWall()) setPosition(x, Math.floor(y) + 1.0, z);
		dataTracker.set(TRACK_HEALTH, health);
		stepSounds();
	}

	@Override
	protected void tickLiving() {
		aiming = false;
		if (!world.isRemote) {
			if (worker != null) {
				pickUp();
				worker.tick();
			} else if (following != null) {
				follow();
			}
		}
		movementSpeed = worker == null && following != null ? FOLLOW_PACE : WORK_PACE;
		// Beta's walking turns a mob at most 30 degrees a tick from the way it faces: walking as it aims, it walks on from the heading its steps had, not from its aim.
		if (walkingAimed) yaw = walkYaw;
		// Beta's walking takes a node as reached from twice its width away, which cuts corners into walls. It reads its width nowhere else here.
		float bodyWidth = width, yawWas = yaw, pitchWas = pitch;
		boolean onPath = hasPath();
		width = 0.2F;
		super.tickLiving();
		width = bodyWidth;
		// Off a path, beta's mob runs its idle look: a glance at the nearest player within 8 blocks now and then, a slow turn otherwise. A golem at work keeps its eyes on its work
		// (the user, 2026-11-10, who saw them turn to face them after each piece): between pieces it holds the gaze it had, on the piece just done; it looks at its next piece as it starts
		// on it, along its way as it walks, at what it steps into; waiting with no work, it holds its heading and looks level. With no job, staying or following, beta's look stands: a glance
		// at its owner then is a mob's.
		if (worker != null && (!onPath || !hasPath())) {
			yaw = yawWas;
			pitch = worker.waiting() ? 0.0F : pitchWas;
		}
		// Beta's walking leaves this on if a path ends mid-jump, and nothing but a new path turns it off.
		if (!hasPath() && !isSubmergedInWater() && !isTouchingLava()) jumping = false;
		// It calls a node reached from over a block away, so it can come to a step with its next node past it and nothing telling it to jump.
		if (hasPath() && horizontalCollision) jumping = true;
		float pathYaw = yaw;
		sidewaysSpeed = 0.0F;
		walkingAimed = false;
		if (aiming) faceAim();
		// Aiming as it walks (an archer stepping back from what it shoots at): it faces its aim and the same steps go the path's way, partly sideways or back, as a player walks
		// backwards facing what they're watching. Beta moves a mob along the way it faces, so otherwise facing its target would walk it there.
		if (aiming && hasPath() && forwardSpeed != 0.0F) {
			walkingAimed = true;
			walkYaw = pathYaw;
			float off = (pathYaw - yaw) * (float) Math.PI / 180.0F, speed = forwardSpeed;
			forwardSpeed = speed * MathHelper.cos(off);
			sidewaysSpeed = -speed * MathHelper.sin(off);
		}
		if (stepping) {
			stepping = false;
			yaw = (float) (Math.atan2(stepZ - z, stepX - x) * 180.0 / Math.PI) - 90.0F;
			forwardSpeed = movementSpeed;
			if (horizontalCollision && stepY > boundingBox.minY + 0.5) jumping = true;
		}
	}

	/**
	 * Its footsteps are beta's for any mob (the user, 2026-11-10, after the first build's one stone step everywhere): the step of what it walks on, a fence's under a fence post and snow's
	 * on a snow layer, nothing in water, at a sixth of the block's volume, one every 1 2/3 blocks walked. Beta's own stepping also tramples farmland, which a golem never does, so it
	 * keeps this count of its own; and it's heard on a server, where beta's own step for a mob isn't sent.
	 */
	private void stepSounds() {
		if (!onGround) return;
		double dx = x - prevX, dz = z - prevZ;
		stepDistance += MathHelper.sqrt(dx * dx + dz * dz) * 0.6F;
		if (stepDistance < 1.0F) return;
		stepDistance -= 1.0F;
		int bx = MathHelper.floor(x), by = MathHelper.floor(boundingBox.minY - 0.2), bz = MathHelper.floor(z);
		int id = world.getBlockId(bx, by, bz);
		if (world.getBlockId(bx, by - 1, bz) == Block.FENCE.id) id = Block.FENCE.id;
		if (id <= 0 || Block.BLOCKS[id] == null) return;
		Block on = Block.BLOCKS[id];
		BlockSoundGroup sound = on.soundGroup;
		if (world.getBlockId(bx, by + 1, bz) == Block.SNOW.id) sound = Block.SNOW.soundGroup;
		else if (on.material.isFluid()) return;
		Sounds.play(world, x, boundingBox.minY, z, sound.getSound(), sound.getVolume() * 0.15F, sound.getPitch());
	}

	// Following and staying.

	/** At your heels: a way to you looked for every half second, and walked at your pace. Too far, or no way, and it gets as near as it can and waits. */
	private void follow() {
		PlayerEntity player = world.getPlayer(following);
		if (player == null || player.dead) {
			walk.stop();
			return;
		}
		double d2 = getSquaredDistance(player);
		if (walk.active()) {
			if (d2 < 6.25) walk.stop();
			else if (walk.tick() != Walk.Status.GOING) walk.stop();
		}
		if ((age % 10) != 0 || d2 < 16.0) return;
		int gx = MathHelper.floor(x), gz = MathHelper.floor(z);
		WalkMap map = WalkMap.get();
		map.search(world, gx, feetY(), gz, gx - FOLLOW_RANGE, gz - FOLLOW_RANGE, gx + FOLLOW_RANGE, gz + FOLLOW_RANGE);
		int best = -1;
		double bestD = d2 - 1.0;
		for (int node = 0; node < map.count(); node++) {
			double dx = map.x(node) + 0.5 - player.x, dy = map.y(node) - player.boundingBox.minY, dz = map.z(node) + 0.5 - player.z;
			double d = dx * dx + dy * dy + dz * dz;
			if (d < bestD) {
				bestD = d;
				best = node;
			}
		}
		if (best < 0) {
			walk.stop();
			return;
		}
		// Still on its way to the same place: no new path, which would only make it check its stride.
		if (walk.active() && walk.toX() == map.x(best) && walk.toY() == map.y(best) && walk.toZ() == map.z(best)) return;
		walk.start(map, best, gx - FOLLOW_RANGE, gz - FOLLOW_RANGE, gx + FOLLOW_RANGE, gz + FOLLOW_RANGE);
	}

	public String following() {
		return following;
	}

	// Being handed things.

	/**
	 * Whether a right click with this in hand trades with it: a tool handed or taken back, or a word to follow or stay. A client's click that does is over at that (none of
	 * beta's use of the hand after it, nor its repeats while the button is held), so what comes into the hand isn't used on the same press. An ingot isn't one: held, it mends
	 * an ingot at a time as beta's held click feeds a wolf, up to whole.
	 */
	public boolean exchanges(ItemStack hand) {
		return hand == null || Golems.jobFor(hand) != null;
	}

	@Override
	public boolean interact(PlayerEntity player) {
		ItemStack hand = player.getHand();
		if (hand == null) {
			if (world.isRemote) return true;
			if (worker != null || tool != null) {
				handBack(player);
			} else {
				following = following == null ? player.name : null;
				walk.stop();
				Sounds.play(world, x, y, z, CLICK, 0.3F, following != null ? 1.0F : 0.8F);
			}
			return true;
		}
		if (hand.itemId == Item.IRON_INGOT.id) {
			if (shownHealth() >= MAX_HEALTH) return false;
			if (world.isRemote) return true;
			if (--hand.count <= 0) player.inventory.main[player.inventory.selectedSlot] = null;
			mend();
			return true;
		}
		String kind = Golems.jobFor(hand);
		if (kind == null) return false;
		if (world.isRemote) return true;
		ItemStack old = tool;
		setTool(Stacks.copy(hand));
		player.inventory.main[player.inventory.selectedSlot] = old;
		startJob(kind);
		return true;
	}

	/** Its tool back to the hand that took it, and what it was carrying down at its feet. It stays where it is. */
	private void handBack(PlayerEntity player) {
		if (tool != null) player.inventory.main[player.inventory.selectedSlot] = tool;
		setTool(null);
		if (worker != null) worker.stop();
		worker = null;
		hasPost = false;
		area = null;
		PoleClaims.release(world, poleClaim);
		following = null;
		dropCarried();
		setWorking(false);
		setPath(null);
		Sounds.play(world, x, y, z, CLANK, 0.4F, 0.8F);
	}

	/** To work where it stands, over the poles round it or the square round it, with a puff and a click at each pole it found, so whoever handed it the tool sees which. */
	private void startJob(String kind) {
		Job last = worker != null ? worker.job() : null;
		if (worker != null) worker.stop();
		following = null;
		walk.stop();
		setPost(MathHelper.floor(x), feetY(), MathHelper.floor(z));
		List<int[]> corners = new ArrayList<>();
		area = Area.find(world, postX, postZ, corners);
		PoleClaims.hold(world, poleClaim, area);
		for (int[] c : corners) {
			world.worldEvent(2000, c[0], c[1], c[2], 4);
			world.worldEvent(1000, c[0], c[1], c[2], 0);
		}
		Job job = Golems.newJob(kind, this);
		// Another tool of the same kind: the job goes on knowing what it knew (the axe, the stumps it has to plant).
		if (last != null && last.kind().equals(kind)) {
			NbtCompound state = new NbtCompound();
			last.write(state);
			job.read(state);
		}
		worker = new Worker(this, job);
		Sounds.play(world, x, y, z, CLANK, 0.5F, 0.9F);
	}

	private void setPost(int x, int y, int z) {
		hasPost = true;
		postX = x;
		postY = y;
		postZ = z;
	}

	public int postX() {
		return postX;
	}

	/** The cell its feet were in when it was put to work: the floor is the block under it. */
	public int postY() {
		return postY;
	}

	public int postZ() {
		return postZ;
	}

	public boolean hasPost() {
		return hasPost;
	}

	public Area area() {
		return area;
	}

	/** Its job's tool class, or null. */
	public String jobName() {
		return worker == null ? null : worker.job().kind();
	}

	public Job job() {
		return worker == null ? null : worker.job();
	}

	public ItemStack tool() {
		return tool;
	}

	public void setTool(ItemStack stack) {
		tool = stack;
		dataTracker.set(TRACK_TOOL, stack == null ? 0 : stack.itemId);
	}

	/** The tool class of a stack, or null if it's no tool. */
	static String toolClass(ItemStack stack) {
		return Golems.toolClass(stack);
	}

	// Wear.

	/** Mining a block wears its tool as it wears a player's: the tool's own {@code postMine}. */
	public void wearOnBlock(int blockId, int x, int y, int z) {
		if (tool == null) return;
		tool.getItem().postMine(tool, blockId, x, y, z, this);
		afterWear();
	}

	/** After any wear: a tool used up breaks, with a player's sound for it. */
	public void afterWear() {
		if (tool == null || tool.count > 0) return;
		setTool(null);
		Sounds.play(world, x, y, z, TOOL_BREAK, 0.8F, 0.8F);
	}

	/** Puts on a spare of its job's kind it's carrying, if any. */
	boolean equipSpare(String kind) {
		for (int i = 0; i < SLOTS; i++) {
			if (slots[i] == null || !kind.equals(toolClass(slots[i]))) continue;
			if (slots[i].count > 1) {
				setTool(slots[i].split(1));
			} else {
				setTool(slots[i]);
				slots[i] = null;
			}
			return true;
		}
		return false;
	}

	// What it carries.

	public ItemStack slot(int i) {
		return slots[i];
	}

	public void setSlot(int i, ItemStack stack) {
		slots[i] = stack == null || stack.count <= 0 ? null : stack;
	}

	public int carriedCount() {
		int n = 0;
		for (ItemStack s : slots) n += s == null ? 0 : s.count;
		return n;
	}

	public boolean carriesAnything() {
		for (ItemStack s : slots) if (s != null) return true;
		return false;
	}

	/** No empty slot: it can't be sure of taking the next thing, so it goes home. */
	public boolean isFull() {
		for (ItemStack s : slots) if (s == null) return false;
		return true;
	}

	boolean hasRoomFor(ItemStack stack) {
		for (ItemStack s : slots) {
			if (s == null || Stacks.canMerge(s, stack) && s.count < s.getMaxCount()) return true;
		}
		return false;
	}

	/** Takes what fits of a stack, onto stacks of the same first, as a player's inventory does. */
	public void store(ItemStack stack) {
		for (ItemStack s : slots) {
			if (stack.count <= 0) return;
			if (s == null || !Stacks.canMerge(s, stack)) continue;
			int moved = Math.min(stack.count, s.getMaxCount() - s.count);
			s.count += moved;
			stack.count -= moved;
		}
		for (int i = 0; i < SLOTS && stack.count > 0; i++) {
			if (slots[i] != null) continue;
			slots[i] = Stacks.copy(stack);
			stack.count = 0;
		}
	}

	/** Where a player picks things up from: its box, a block wider each way. */
	Box pickupBox() {
		return boundingBox.expand(1.0, 0.0, 1.0);
	}

	/** Whether it would take this up: not what it put down itself. */
	boolean wouldTake(ItemEntity item) {
		return !item.dead && item.stack != null && !putDown.contains(item);
	}

	/** Picks up what's lying against it, as a player does. */
	private void pickUp() {
		putDown.removeIf(item -> item.dead);
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, pickupBox())) {
			ItemEntity item = (ItemEntity) o;
			if (!wouldTake(item) || item.pickupDelay > 0) continue;
			int before = item.stack.count;
			store(item.stack);
			if (item.stack.count == before) continue;
			if (item.stack.count <= 0) {
				Golems.pickedUp.accept(item, this);
				item.markDead();
			}
		}
	}

	/** Everything it carries, down at its feet, where it won't take it up again. */
	public void dropCarried() {
		for (int i = 0; i < SLOTS; i++) {
			if (slots[i] != null) putDown(slots[i]);
			slots[i] = null;
		}
	}

	/** Something it carried, down at its feet, where it won't take it up again. */
	public void putDown(ItemStack stack) {
		ItemEntity item = dropItem(stack, 0.5F);
		if (item != null) putDown.add(item);
	}

	/** Killed: its core, its tool and what it was carrying. The iron is gone. */
	@Override
	protected void dropItems() {
		dropItem(new ItemStack(Golems.core.get()), 0.5F);
		if (tool != null) dropItem(tool, 0.5F);
		setTool(null);
		dropCarried();
	}

	// Moving and looking, for the loop.

	/** The cell its feet are in, by what it stands on: on a slab, the cell above it. */
	public int feetY() {
		return MathHelper.floor(boundingBox.minY + 0.5);
	}

	public double eyeY() {
		return boundingBox.minY + EYE;
	}

	/** Beta's own looking (at a player, along its way) from the same eye. */
	@Override
	public float getEyeHeight() {
		return EYE;
	}

	/**
	 * Where its work reaches from: its eye over the middle of the cell it stands in. Places to stand are chosen by what's in reach from their middles, and walking ends anywhere within
	 * 0.4 of one, so reach is the cell's, not the exact spot's.
	 */
	public double reachX() {
		return MathHelper.floor(x) + 0.5;
	}

	public double reachY() {
		return feetY() + EYE;
	}

	public double reachZ() {
		return MathHelper.floor(z) + 0.5;
	}

	/** A step straight at a place, this tick, past beta's walking. */
	void stepToward(float x, float y, float z) {
		stepping = true;
		stepX = x;
		stepY = y;
		stepZ = z;
	}

	void aimAt(float x, float y, float z) {
		aiming = true;
		aimX = x;
		aimY = y;
		aimZ = z;
	}

	private void faceAim() {
		double dx = aimX - x, dy = aimY - eyeY(), dz = aimZ - z;
		yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
	}

	void setWorking(boolean working) {
		if (working() != working) dataTracker.set(TRACK_WORKING, (byte) (working ? 1 : 0));
	}

	boolean working() {
		return dataTracker.getByte(TRACK_WORKING) != 0;
	}

	private boolean holdsBow() {
		int id = dataTracker.getInt(TRACK_TOOL);
		return id > 0 && Item.ITEMS[id] instanceof BowItem;
	}

	/** A bow at work is held drawn, arms up along its look, rather than swung: on either side, from what the golem sends. */
	public boolean drawsBow() {
		return working() && holdsBow();
	}

	boolean inWater() {
		return isInFluid(Material.WATER);
	}

	/** How long its last look for work took, in microseconds, once: 0 if it hasn't looked since this was last asked. For a test to print. */
	public long lastLookMicros() {
		if (worker == null) return 0;
		long t = worker.lastLookMicros;
		worker.lastLookMicros = 0;
		return t;
	}

	/** What it was last doing, for a test to print. */
	/** Times it has stepped back from something too near in a fight: a test's to read. */
	public int backedOff() {
		return worker == null ? 0 : worker.backedOff;
	}

	public String status() {
		return worker == null ? (following != null ? "following " + following : "staying") : worker.describe();
	}

	// Health, as a client sees it.

	public int shownHealth() {
		return world.isRemote ? dataTracker.getInt(TRACK_HEALTH) : health;
	}

	/** An ingot's mending, from its owner's hand or its own chest: a quarter back, with the clank and hearts either way. */
	void mend() {
		health = Math.min(MAX_HEALTH, health + MEND);
		dataTracker.set(TRACK_HEALTH, health);
		Sounds.play(world, x, y, z, CLANK, 0.6F, 1.5F);
		showHearts();
		world.broadcastEntityEvent(this, (byte) 7);
	}

	/** It mends itself from its chest only when a whole ingot's worth is gone, so no ingot is spent on less. */
	public boolean wantsMending() {
		return health <= MAX_HEALTH - MEND;
	}

	/** How cracked it's drawn, 0 to 3: below three-quarters of its health, a half and a quarter. Mending takes the cracks back as it goes. */
	public int cracks() {
		int h = shownHealth();
		return h <= 0 ? 0 : h < MAX_HEALTH / 4 ? 3 : h < MAX_HEALTH / 2 ? 2 : h < MAX_HEALTH * 3 / 4 ? 1 : 0;
	}

	private void showHearts() {
		for (int i = 0; i < 7; i++) {
			world.addParticle("heart", x + random.nextFloat() * width * 2.0F - width, y + 0.5 + random.nextFloat() * height, z + random.nextFloat() * width * 2.0F - width, random.nextGaussian() * 0.02,
					random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void processServerEntityStatus(byte status) {
		if (status == 7) showHearts();
		else super.processServerEntityStatus(status);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public ItemStack getHeldItem() {
		int id = dataTracker.getInt(TRACK_TOOL);
		if (id <= 0) return null;
		if (shownTool == null || shownTool.itemId != id) shownTool = new ItemStack(id, 1, 0);
		return shownTool;
	}

	/** Killed, fallen out of the world or taken away: its poles are let go. Not when its chunk goes, which beta doesn't mark. */
	@Override
	public void markDead() {
		super.markDead();
		if (!world.isRemote) PoleClaims.release(world, poleClaim);
	}

	// Saved with the world.

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		if (tool != null) nbt.put("Tool", tool.writeNbt(new NbtCompound()));
		NbtList list = new NbtList();
		for (int i = 0; i < SLOTS; i++) {
			if (slots[i] == null) continue;
			NbtCompound s = slots[i].writeNbt(new NbtCompound());
			s.putByte("Slot", (byte) i);
			list.add(s);
		}
		nbt.put("Items", list);
		if (following != null) nbt.putString("Following", following);
		if (hasPost) {
			nbt.putInt("PostX", postX);
			nbt.putInt("PostY", postY);
			nbt.putInt("PostZ", postZ);
		}
		if (area != null) {
			NbtCompound a = new NbtCompound();
			area.write(a);
			nbt.put("Area", a);
		}
		nbt.putLong("PoleClaim", poleClaim);
		if (worker != null) {
			nbt.putString("Job", worker.job().kind());
			NbtCompound state = new NbtCompound();
			worker.job().write(state);
			nbt.put("JobState", state);
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		if (nbt.contains("Tool")) setTool(new ItemStack(nbt.getCompound("Tool")));
		NbtList list = nbt.getList("Items");
		for (int i = 0; i < list.size(); i++) {
			NbtCompound s = (NbtCompound) list.get(i);
			int slot = s.getByte("Slot");
			if (slot >= 0 && slot < SLOTS) slots[slot] = new ItemStack(s);
		}
		following = nbt.contains("Following") ? nbt.getString("Following") : null;
		if (nbt.contains("PostX")) setPost(nbt.getInt("PostX"), nbt.getInt("PostY"), nbt.getInt("PostZ"));
		if (nbt.contains("Area")) area = Area.read(nbt.getCompound("Area"));
		Job job = nbt.contains("Job") && hasPost ? Golems.newJob(nbt.getString("Job"), this) : null;
		if (job != null) {
			if (area == null) area = new Area(postX - Area.AROUND_POST, postZ - Area.AROUND_POST, postX + Area.AROUND_POST, postZ + Area.AROUND_POST, false);
			job.read(nbt.getCompound("JobState"));
			worker = new Worker(this, job).loaded();
		}
		if (nbt.contains("PoleClaim")) poleClaim = nbt.getLong("PoleClaim");
		// Back from the disk: what it holds, said again (a world from before the glow had none on record).
		if (world != null && !world.isRemote) PoleClaims.hold(world, poleClaim, area);
	}
}
