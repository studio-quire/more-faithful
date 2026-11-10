package org.studioquire.faithful.apicheck;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.studioquire.faithful.apicheck.server.Scenes;
import org.studioquire.oldfaithful.core.api.Armor;
import org.studioquire.oldfaithful.core.api.BlockEntities;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.BlockShapes;
import org.studioquire.oldfaithful.core.api.Containers;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.ItemStorage;
import org.studioquire.oldfaithful.core.api.Lookups;
import org.studioquire.oldfaithful.core.api.EntitySpawnData;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.SyncedBlockEntity;
import org.studioquire.oldfaithful.core.api.Tags;
import org.studioquire.oldfaithful.core.api.Tools;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** One of everything the content APIs offer, built as a mod would build it, with no mixins of its own. */
public final class ApiCheck implements FaithfulMod {
	/** Which features run: {@code all}, {@code none}, or {@code other}, for the tests of what worldgen moves. */
	private static final String WORLDGEN = System.getProperty("apicheck.worldgen", "all");

	public static final class MachineBlock extends BlockWithEntity {
		public MachineBlock(int id, int texture) {
			super(id, texture, Material.METAL);
			setHardness(2.0F);
		}

		@Override
		protected BlockEntity createBlockEntity() {
			return new MachineEntity();
		}

		@Override
		public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
			Containers.open(player, "api-check:kiln", (MachineEntity) world.getBlockEntity(x, y, z));
			return true;
		}
	}

	/** A capability of this mod's own, for {@code Lookups}: mods may make types as the core does. */
	public interface Charge {
		int charge();

		/** Returns how much went in. */
		int add(int amount);
	}

	public static final Lookups.Type<Charge> CHARGE = Lookups.type("api-check:charge", Charge.class);

	/** A value clients see, an inventory, and a charge. */
	public static final class MachineEntity extends BlockEntity implements SyncedBlockEntity, Inventory, Charge {
		public int value;
		public int reads;
		public int heat;
		private final ItemStack[] slots = new ItemStack[2];
		private int charge;

		public void setValue(int v) {
			value = v;
			BlockEntities.sync(this);
		}

		@Override
		public void writeSync(NbtCompound nbt) {
			nbt.putInt("Value", value);
		}

		@Override
		public void readSync(NbtCompound nbt) {
			value = nbt.getInt("Value");
			reads++;
		}

		@Override
		public void writeNbt(NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putInt("Value", value);
			nbt.putInt("Charge", charge);
		}

		@Override
		public void readNbt(NbtCompound nbt) {
			super.readNbt(nbt);
			value = nbt.getInt("Value");
			charge = nbt.getInt("Charge");
		}

		@Override
		public int size() {
			return slots.length;
		}

		@Override
		public ItemStack getStack(int slot) {
			return slots[slot];
		}

		@Override
		public ItemStack removeStack(int slot, int amount) {
			if (slots[slot] == null) return null;
			ItemStack taken = slots[slot].split(amount);
			if (slots[slot].count == 0) slots[slot] = null;
			return taken;
		}

		@Override
		public void setStack(int slot, ItemStack stack) {
			slots[slot] = stack;
		}

		@Override
		public String getName() {
			return "Test kiln";
		}

		@Override
		public int getMaxCountPerStack() {
			return 64;
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			return world.getBlockEntity(x, y, z) == this;
		}

		@Override
		public int charge() {
			return charge;
		}

		@Override
		public int add(int amount) {
			int in = Math.min(amount, 1000 - charge);
			charge += in;
			return in;
		}
	}

	public static final class Kiln extends ScreenHandler {
		private final Inventory inventory;
		public int heat;
		private int sent = -1;

		public Kiln(PlayerEntity player, Inventory inventory, NbtCompound data) {
			this.inventory = inventory;
			addSlot(new Slot(inventory, 0, 56, 17));
			addSlot(new Slot(inventory, 1, 56, 53));
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 9; column++) addSlot(new Slot(player.inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
			for (int column = 0; column < 9; column++) addSlot(new Slot(player.inventory, column, 8 + column * 18, 142));
		}

		@Override
		public void sendContentUpdates() {
			super.sendContentUpdates();
			int now = inventory instanceof MachineEntity ? ((MachineEntity) inventory).heat : heat;
			if (now != sent) {
				for (Object listener : listeners) ((ScreenHandlerListener) listener).onPropertyUpdate(this, 0, now);
				sent = now;
			}
		}

		@Override
		public void setProperty(int id, int value) {
			if (id == 0) heat = value;
		}

		@Override
		public boolean canUse(PlayerEntity player) {
			return inventory.canPlayerUse(player);
		}
	}

	public static final class Ore extends Block {
		public Ore(int id, int texture) {
			super(id, texture, Material.STONE);
			setHardness(3.0F);
		}
	}

	public static final class Post extends Block {
		public Post(int id, int texture) {
			super(id, texture, Material.WOOD);
			setHardness(1.0F);
			setOpacity(0);
			setSoundGroup(Sounds.group("api-check.ping", "api-check.machine.hum", 1.0F, 1.0F));
		}

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public boolean isFullCube() {
			return false;
		}

		@Override
		public int getRenderType() {
			return BlockShapes.type("api-check:post");
		}
	}

	public static final class Thing extends Item {
		public Thing(int id, int texture) {
			super(id);
			setTextureId(texture);
		}

		public Thing subtypes() {
			setHasSubtypes(true);
			return this;
		}
	}

	public static final class Critter extends AnimalEntity implements EntitySpawnData {
		public String note = "";

		public Critter(World world) {
			super(world);
			texture = "/assets/api-check/textures/mob/critter.png";
			setBoundingBoxSpacing(0.9F, 0.9F);
		}

		@Override
		protected void initDataTracker() {
			dataTracker.startTracking(16, (byte) 0);
		}

		public void setMood(int mood) {
			dataTracker.set(16, (byte) mood);
		}

		public int mood() {
			return dataTracker.getByte(16);
		}

		@Override
		public void writeSpawnData(NbtCompound nbt) {
			nbt.putString("Note", note);
		}

		@Override
		public void readSpawnData(NbtCompound nbt) {
			note = nbt.getString("Note");
		}

		@Override
		public void writeNbt(NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putString("Note", note);
		}

		@Override
		public void readNbt(NbtCompound nbt) {
			super.readNbt(nbt);
			note = nbt.getString("Note");
		}
	}

	/** Vanilla's chain of instanceofs would send this as a plain boat. */
	public static final class Raft extends BoatEntity implements EntitySpawnData {
		public int planks;

		public Raft(World world) {
			super(world);
		}

		@Override
		public void writeSpawnData(NbtCompound nbt) {
			nbt.putInt("Planks", planks);
		}

		@Override
		public void readSpawnData(NbtCompound nbt) {
			planks = nbt.getInt("Planks");
		}
	}

	public static final Tools.Tier COPPER = Tools.tier("api-check:copper", 15, 5.0F, 180, 1);
	public static final Tools.Tier TIN = Tools.tier("api-check:tin", 5, 3.0F, 60, 0);

	public static BlockEntry<MachineBlock> machine;
	public static BlockEntry<Ore> ore;
	public static BlockEntry<Ore> otherOre;
	public static BlockEntry<Ore> marker;
	public static BlockEntry<Post> post;
	public static ItemEntry<Thing> ingot, dust, coke;
	public static ItemEntry<Tools.Pickaxe> pick, weakPick;
	public static ItemEntry<Tools.Shears> clippers;
	public static ItemEntry<ArmorItem> helmet;

	@Override
	public void content(Content c) {
		machine = c.block("machine", s -> new MachineBlock(s.id(), s.texture("machine"))).standIn(Block.IRON_BLOCK);
		ore = c.block("ore", s -> new Ore(s.id(), s.texture("ore"))).standIn(Block.STONE);
		otherOre = c.block("other_ore", s -> new Ore(s.id(), s.texture("ore"))).standIn(Block.STONE);
		marker = c.block("marker", s -> new Ore(s.id(), s.texture("ore"))).standIn(Block.BEDROCK);
		post = c.block("post", s -> new Post(s.id(), s.texture("post"))).standIn(Block.FENCE);
		ingot = c.item("ingot", s -> new Thing(s.id(), s.texture("ingot")));
		dust = c.item("dust", s -> new Thing(s.id(), s.texture("dust")).subtypes());
		coke = c.item("coke", s -> new Thing(s.id(), s.texture("coke")));
		pick = c.item("copper_pickaxe", Tools.pickaxe(COPPER, "pick"));
		weakPick = c.item("tin_pickaxe", Tools.pickaxe(TIN, "weak_pick"));
		clippers = c.item("clippers", Tools.shears(COPPER, "clippers"));
		helmet = c.item("helmet", s -> {
			ArmorItem item = new ArmorItem(s.id(), 2, Armor.texture("api-check:steel"), 0);
			item.setTextureId(s.texture("helmet"));
			return item;
		});
		c.blockEntity("machine", MachineEntity.class);
		c.entity("critter", Critter.class);
		c.entity("raft", Raft.class).tracking(160, 5).sendVelocity();
		c.screen("kiln", Kiln::new);
		c.alias("oldmod:critter", "critter");
		c.alias("oldmod:machine", "machine");
	}

	@Override
	public void init(Mod mod) {
		Tags.add("c:ingots/test", ingot.get());
		Lookups.register(ItemStorage.TYPE, DispenserBlockEntity.class, (dispenser, side) -> side == 0 ? null : ItemStorage.of(dispenser));
		Lookups.register(CHARGE, MachineEntity.class, (machine, side) -> side == 5 ? null : machine);
		Tools.requires(ore.get(), "pickaxe", 15);
		Tools.requires(otherOre.get(), "pickaxe", 15);

		Recipes.shaped(new ItemStack(pick.get()), "III", " S ", " S ", 'I', "c:ingots/test", 'S', Item.STICK);
		Recipes.shaped(new ItemStack(coke.get(), 2), "D ", " D", 'D', new ItemStack(dust.get(), 1, 3));
		Recipes.shapeless(new ItemStack(dust.get(), 2, 3), ore.get(), Item.FLINT);
		Recipes.shapeless(new ItemStack(Item.STICK, 1), "c:ingots/test", "c:ingots/iron");
		Recipes.smelting(ore.get(), new ItemStack(ingot.get()));
		Recipes.smelting(new ItemStack(dust.get(), 1, 3), new ItemStack(Item.GOLD_INGOT));
		Recipes.smelting("c:ingots/test", new ItemStack(Item.DIAMOND));
		Recipes.fuel(coke.get(), 1000);
		Recipes.fuel(new ItemStack(dust.get(), 1, 5), 50);
		Recipes.modify(r -> {
			r.removeOutput(Block.BOOKSHELF);
			r.removeSmelting(Block.CACTUS);
		});

		if (!WORLDGEN.equals("none")) Worldgen.ore("api-check:a_other", otherOre.get(), 5, 6, 5, 50);
		if (WORLDGEN.equals("all")) {
			Worldgen.ore("api-check:ore", ore.get(), 6, 8, 5, 60);
			Worldgen.chunkBuilt("api-check:marker", (cells, random) -> cells.set(3 + random.nextInt(4), 1, 3, marker.get().id, 2));
			Worldgen.chunkBuilt("api-check:nether", Worldgen.NETHER, (cells, random) -> cells.set(5, 1, 5, marker.get().id));
			Worldgen.chunkBuilt("api-check:sky", Worldgen.SKY, (cells, random) -> cells.set(5, 1, 5, marker.get().id));
		}

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) Scenes.register();
	}
}
