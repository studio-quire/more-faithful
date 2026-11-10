package org.studioquire.faithful.apicheck;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.recipe.SmeltingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.studioquire.faithful.apicheck.ApiCheck.MachineEntity;
import org.studioquire.oldfaithful.core.api.BlockEntities;
import org.studioquire.oldfaithful.core.api.Containers;
import org.studioquire.oldfaithful.core.api.ItemStorage;
import org.studioquire.oldfaithful.core.api.Lookups;
import org.studioquire.oldfaithful.core.api.Recipes;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.api.Tags;
import org.studioquire.oldfaithful.core.api.Tools;

/** What's the same wherever the world runs: on the real server, and in single player. */
public final class Checks {
	public interface Report {
		void check(String what, boolean ok);
	}

	public interface Breaker {
		void breakBlock(int x, int y, int z);
	}

	private Checks() {
	}

	private static ItemStack craft(ItemStack... grid) {
		CraftingInventory inventory = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, 3, 3);
		for (int i = 0; i < grid.length; i++) inventory.setStack(i, grid[i]);
		return CraftingRecipeManager.getInstance().craft(inventory);
	}

	private static ItemStack stack(Item item) {
		return new ItemStack(item);
	}

	public static void recipes(Report r) {
		ItemStack i = stack(ApiCheck.ingot.get()), s = stack(Item.STICK);
		ItemStack made = craft(i, i, i, null, s, null, null, s, null);
		r.check("a tag recipe: three ingots and two sticks make the pick", made != null && made.itemId == ApiCheck.pick.get().id);
		ItemStack dust3 = new ItemStack(ApiCheck.dust.get(), 1, 3), dust4 = new ItemStack(ApiCheck.dust.get(), 1, 4);
		made = craft(dust3, null, null, null, dust3, null);
		r.check("a shaped recipe, by damage", made != null && made.itemId == ApiCheck.coke.get().id && made.count == 2);
		made = craft(null, dust3, null, dust3, null, null);
		r.check("and its mirror image", made != null && made.itemId == ApiCheck.coke.get().id);
		r.check("and not with the wrong damage", craft(dust4, null, null, null, dust3, null) == null);
		made = craft(new ItemStack(ApiCheck.ore.get()), null, null, null, null, null, null, null, stack(Item.FLINT));
		r.check("a shapeless recipe, anywhere in the grid", made != null && made.itemId == ApiCheck.dust.get().id && made.getDamage() == 3 && made.count == 2);
		made = craft(stack(Item.IRON_INGOT), stack(ApiCheck.ingot.get()));
		r.check("a shapeless recipe with two tags", made != null && made.itemId == Item.STICK.id);
		ItemStack plank = new ItemStack(Block.PLANKS), book = stack(Item.BOOK);
		r.check("modify took out a vanilla recipe", craft(plank, plank, plank, book, book, book, plank, plank, plank) == null);
		made = craft(new ItemStack(Block.PLANKS), new ItemStack(Block.PLANKS), null, new ItemStack(Block.PLANKS), new ItemStack(Block.PLANKS), null);
		r.check("and vanilla's others stand", made != null && made.itemId == Block.CRAFTING_TABLE.id);
		r.check("modify took out a vanilla smelting recipe", SmeltingRecipeManager.getInstance().craft(Block.CACTUS.id) == null);
		r.check("a tag has what was added, and vanilla's own", Tags.has("c:ingots/test", i) && Tags.has("c:planks", plank) && !Tags.has("c:planks", i));
		r.check("a tag asked for by a recipe is named", Tags.names().contains("c:ingots/iron"));
	}

	private static ItemStack burn(FurnaceBlockEntity furnace, ItemStack input, ItemStack fuel, int ticks) {
		furnace.setStack(0, input);
		furnace.setStack(1, fuel);
		furnace.setStack(2, null);
		furnace.burnTime = 0;
		furnace.cookTime = 0;
		for (int t = 0; t < ticks; t++) furnace.tick();
		return furnace.getStack(2);
	}

	public static void furnace(World w, int x, int y, int z, Report r) {
		w.setBlock(x, y, z, Block.FURNACE.id);
		FurnaceBlockEntity f = (FurnaceBlockEntity) w.getBlockEntity(x, y, z);
		ItemStack out = burn(f, new ItemStack(ApiCheck.ore.get()), new ItemStack(ApiCheck.coke.get()), 205);
		r.check("a mod's ore smelts on a mod's fuel: " + out, out != null && out.itemId == ApiCheck.ingot.get().id);
		r.check("and the fuel burns for what it said: " + f.fuelTime, f.fuelTime == 1000);
		out = burn(f, new ItemStack(ApiCheck.dust.get(), 1, 3), new ItemStack(Item.COAL), 205);
		r.check("smelting by damage", out != null && out.itemId == Item.GOLD_INGOT.id);
		out = burn(f, new ItemStack(ApiCheck.dust.get(), 1, 4), new ItemStack(Item.COAL), 205);
		r.check("not with another damage", out == null);
		out = burn(f, new ItemStack(ApiCheck.ingot.get()), new ItemStack(Item.COAL), 205);
		r.check("smelting by tag", out != null && out.itemId == Item.DIAMOND.id);
		out = burn(f, new ItemStack(Block.IRON_ORE), new ItemStack(ApiCheck.dust.get(), 1, 5), 10);
		r.check("a fuel by damage burns for its own time: " + f.fuelTime, f.fuelTime == 50);
		f.burnTime = 0;
		w.setBlock(x, y, z, 0);
	}

	private static float seconds(Block block, PlayerEntity p) {
		return block.getHardness(p);
	}

	private static int drops(World w, int x, int y, int z, int item) {
		int n = 0;
		for (Object e : w.getEntities(null, Box.createCached(x - 2, y - 2, z - 2, x + 3, y + 3, z + 3))) {
			if (e instanceof ItemEntity && !((ItemEntity) e).dead && ((ItemEntity) e).stack.itemId == item) {
				n += ((ItemEntity) e).stack.count;
				((ItemEntity) e).markDead();
			}
		}
		return n;
	}

	private static void held(PlayerEntity p, ItemStack stack) {
		p.inventory.main[p.inventory.selectedSlot] = stack;
	}

	public static void tools(World w, PlayerEntity p, int x, int y, int z, Breaker breaker, Report r) {
		int ore = ApiCheck.ore.get().id;
		Block oreBlock = ApiCheck.ore.get();
		held(p, new ItemStack(ApiCheck.pick.get()));
		float modPick = seconds(oreBlock, p);
		r.check("a mod's pick harvests a mod's ore", p.canHarvest(oreBlock));
		r.check("and has its tier's speed: " + p.inventory.getStrengthOnBlock(Block.STONE), Math.abs(p.inventory.getStrengthOnBlock(Block.STONE) - 5.0F) < 0.01F);
		r.check("a mod's pick at level 15 harvests iron ore", p.canHarvest(Block.IRON_ORE));
		r.check("not diamond ore, which wants iron's level", !p.canHarvest(Block.DIAMOND_ORE));
		w.setBlock(x, y, z, ore);
		breaker.breakBlock(x, y, z);
		r.check("mod pick, mod ore: gone, and dropped", w.getBlockId(x, y, z) == 0 && drops(w, x, y, z, ore) == 1);
		r.check("and the pick wore", p.inventory.main[p.inventory.selectedSlot].getDamage() == 1);

		held(p, new ItemStack(Item.IRON_PICKAXE));
		r.check("a vanilla iron pick harvests a mod's ore", p.canHarvest(oreBlock));
		float iron = seconds(oreBlock, p);
		w.setBlock(x, y, z, ore);
		breaker.breakBlock(x, y, z);
		r.check("vanilla pick, mod ore: dropped", drops(w, x, y, z, ore) == 1);
		r.check("the iron pick is faster than the copper one: " + iron + " > " + modPick, iron > modPick);

		held(p, new ItemStack(Item.STONE_PICKAXE));
		r.check("a stone pick doesn't: below the level", !p.canHarvest(oreBlock));
		w.setBlock(x, y, z, ore);
		breaker.breakBlock(x, y, z);
		r.check("nothing dropped for it", drops(w, x, y, z, ore) == 0);

		held(p, new ItemStack(ApiCheck.weakPick.get()));
		r.check("a mod's low tier doesn't either", !p.canHarvest(oreBlock));
		held(p, null);
		r.check("nor does a hand", !p.canHarvest(oreBlock));

		held(p, new ItemStack(Item.WOODEN_PICKAXE));
		r.check("vanilla's own rule stands: a wooden pick and iron ore", !p.canHarvest(Block.IRON_ORE) && p.canHarvest(Block.STONE));
		held(p, new ItemStack(Item.DIAMOND_PICKAXE));
		r.check("and diamond and obsidian", p.canHarvest(Block.OBSIDIAN) && Math.abs(p.inventory.getStrengthOnBlock(Block.OBSIDIAN) - 1.0F) < 0.01F);

		held(p, new ItemStack(ApiCheck.clippers.get()));
		w.setBlock(x, y, z, Block.LEAVES.id);
		breaker.breakBlock(x, y, z);
		r.check("a mod's shears take leaves", drops(w, x, y, z, Block.LEAVES.id) == 1);
		SheepEntity sheep = new SheepEntity(w);
		sheep.setPositionAndAngles(p.x, p.y, p.z, 0.0F, 0.0F);
		sheep.interact(p);
		r.check("and wool", sheep.isSheared());
		held(p, null);
		r.check("a block's sounds are the mod's", ApiCheck.post.get().soundGroup.getSound().equals("api-check.ping"));
	}

	private static int count(Inventory inventory) {
		int n = 0;
		for (int i = 0; i < inventory.size(); i++) {
			if (inventory.getStack(i) != null) n += inventory.getStack(i).count;
		}
		return n;
	}

	public static void storage(World w, int x, int y, int z, Report r) {
		for (int dx = 0; dx < 6; dx++) w.setBlock(x + dx, y, z, 0);
		w.setBlock(x, y, z, Block.CHEST.id);
		w.setBlock(x + 1, y, z, Block.CHEST.id);
		w.setBlock(x + 3, y, z, ApiCheck.machine.get().id);
		w.setBlock(x + 4, y, z, Block.FURNACE.id);
		w.setBlock(x + 5, y, z, Block.DISPENSER.id);
		ItemStorage chest = Lookups.find(ItemStorage.TYPE, w, x + 1, y, z, 1);
		r.check("a double chest is one storage of 54", chest != null && chest.size() == 54);
		ItemStorage machine = Lookups.find(ItemStorage.TYPE, w, x + 3, y, z, Lookups.NO_SIDE);
		r.check("a mod's inventory is found with no work", machine != null && machine.size() == 2);
		ItemStack rest = chest.insert(new ItemStack(ApiCheck.ingot.get(), 70), false);
		r.check("a stack goes in, and over into the next slot", rest == null && chest.peek(0).count == 64 && chest.peek(1).count == 6);
		r.check("simulating moves nothing", chest.extract(0, 5, true).count == 5 && chest.peek(0).count == 64);
		int moved = ItemStorage.move(chest, machine, 10);
		r.check("an item moved between two storages by lookup: " + moved, moved == 10 && machine.peek(0).itemId == ApiCheck.ingot.get().id && machine.peek(0).count == 10 && chest.peek(0).count == 54);
		ItemStorage left = Lookups.find(ItemStorage.TYPE, w, x, y, z, 1);
		r.check("seen from the other half, it's the same chest", left.size() == 54 && left.peek(0).count == 54);
		ItemStorage top = Lookups.find(ItemStorage.TYPE, w, x + 4, y, z, 1), bottom = Lookups.find(ItemStorage.TYPE, w, x + 4, y, z, 0);
		FurnaceBlockEntity furnace = (FurnaceBlockEntity) w.getBlockEntity(x + 4, y, z);
		r.check("a furnace is its three slots from any side", top.size() == 3 && bottom.size() == 3);
		r.check("what goes in takes the first that fits, whatever the side", bottom.insert(new ItemStack(Block.IRON_ORE, 3), false) == null && furnace.getStack(0).count == 3);
		furnace.setStack(2, new ItemStack(Item.IRON_INGOT, 2));
		ItemStack taken = top.extract(2, 5, false);
		r.check("and any slot gives what it holds", taken != null && taken.count == 2 && furnace.getStack(2) == null);
		furnace.setStack(0, null);
		DispenserBlockEntity dispenser = (DispenserBlockEntity) w.getBlockEntity(x + 5, y, z);
		ItemStorage disp = Lookups.find(ItemStorage.TYPE, w, x + 5, y, z, 3);
		r.check("a dispenser's nine", disp.size() == 9 && disp.insert(new ItemStack(Item.ARROW, 5), false) == null && count(dispenser) == 5);

		ApiCheck.Charge charge = Lookups.find(ApiCheck.CHARGE, w, x + 3, y, z, Lookups.NO_SIDE);
		r.check("a mod's own type, by interface", charge != null && charge.add(1500) == 1000 && charge.charge() == 1000);
		r.check("and nothing where there's none", Lookups.find(ApiCheck.CHARGE, w, x, y, z, 1) == null && Lookups.find(ItemStorage.TYPE, w, x + 2, y, z, 1) == null);
		r.check("a provider's null is final: it hides an Inventory on that side", Lookups.find(ItemStorage.TYPE, w, x + 5, y, z, 0) == null);
		r.check("and a block entity that is the type answers by side through one, as beside sees it", Lookups.beside(ApiCheck.CHARGE, w, x + 2, y, z, 5) == charge
				&& Lookups.beside(ApiCheck.CHARGE, w, x + 4, y, z, 4) == null);
		r.check("what a furnace burns, vanilla's and a mod's", Recipes.fuelTime(new ItemStack(Item.COAL)) == 1600 && Recipes.fuelTime(new ItemStack(Block.PLANKS)) == 300
				&& Recipes.fuelTime(new ItemStack(ApiCheck.coke.get())) == 1000 && Recipes.fuelTime(new ItemStack(Block.DIRT)) == 0 && Recipes.fuelTime(null) == 0);
		Containers.Tracked tracked = new Containers.Tracked(null, 3);
		Containers.Tracked.Value big = tracked.add(null), second = tracked.add(null);
		tracked.receive(3, (short) 100000);
		boolean halfway = big.get() == 0;
		tracked.receive(4, (short) (100000 >> 16));
		tracked.receive(5, (short) -7);
		tracked.receive(6, (short) (-7 >> 16));
		r.check("a tracked number past a short arrives whole, and only then: " + big.get() + ", " + second.get(), halfway && big.get() == 100000 && second.get() == -7);
		r.check("and ids that aren't its own are left alone", !tracked.receive(2, 1) && !tracked.receive(7, 1));

		Containers.Slots slots = new Containers.Slots("Test", 2, null);
		ItemStack marked = new ItemStack(ApiCheck.ingot.get(), 40);
		Stacks.data(marked).putString("note", "kept");
		slots.setStack(1, marked);
		NbtCompound saved = new NbtCompound();
		slots.write(saved, "Items");
		Containers.Slots back = new Containers.Slots("Test", 2, null);
		back.read(saved, "Items");
		r.check("slots save and load, stack data and all", back.getStack(0) == null && back.getStack(1).count == 40 && "kept".equals(Stacks.peek(back.getStack(1)).getString("note")));
		BlockEntities.spill(w, x + 2, y + 1, z, back);
		int spilled = 0;
		boolean allKept = true;
		for (Object e : w.getEntities((net.minecraft.entity.Entity) null, Box.createCached(x, y - 1, z - 2, x + 5, y + 4, z + 3))) {
			if (!(e instanceof ItemEntity) || ((ItemEntity) e).stack.itemId != ApiCheck.ingot.get().id) continue;
			ItemStack lying = ((ItemEntity) e).stack;
			spilled += lying.count;
			allKept &= Stacks.peek(lying) != null && "kept".equals(Stacks.peek(lying).getString("note"));
			((ItemEntity) e).markDead();
		}
		r.check("a spill throws it all out, each part with its data: " + spilled, spilled == 40 && allKept && back.getStack(1) == null);
		BlockEntity be = w.getBlockEntity(x + 3, y, z);
		r.check("the machine is what it says", be instanceof MachineEntity);
		for (int dx = 0; dx < 6; dx++) {
			Inventory inv = w.getBlockEntity(x + dx, y, z) instanceof Inventory ? (Inventory) w.getBlockEntity(x + dx, y, z) : null;
			if (inv != null) {
				for (int i = 0; i < inv.size(); i++) inv.setStack(i, null);
			}
			w.setBlock(x + dx, y, z, 0);
		}
	}
}
