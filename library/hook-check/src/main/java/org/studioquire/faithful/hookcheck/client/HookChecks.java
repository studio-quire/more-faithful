package org.studioquire.faithful.hookcheck.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Box;
import org.studioquire.faithful.hookcheck.HookCheck;
import org.studioquire.oldfaithful.core.api.client.Screens;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.StartupCheck;
import org.studioquire.oldfaithful.selftest.WorldCheck;
import org.studioquire.oldfaithful.selftest.mixin.ScreenAccessor;

/**
 * Every hook in the hidden client: single player (a first launch, and a second with {@code -Dhookcheck.reopened=true} on the same
 * world) and on the real server, where no world-side hook may speak.
 */
public final class HookChecks implements WorldCheck {
	private static final boolean REOPENED = Boolean.getBoolean("hookcheck.reopened");
	private static final int K = 14, E = 8;

	private SelfTest results;
	private int stage;
	private int wait;
	private int x, y, z;

	@Override
	public void start(Minecraft mc, SelfTest results) {
		this.results = results;
		stage = mc.world.isRemote ? 100 : REOPENED ? 50 : 0;
	}

	private void next(int stage, int wait) {
		this.stage = stage;
		this.wait = wait;
	}

	private static Screen open(Minecraft mc, Screen s) {
		s.init(mc, 427, 240);
		return s;
	}

	private static List<ButtonWidget> buttons(Screen s) {
		@SuppressWarnings("unchecked")
		List<ButtonWidget> list = ((ScreenAccessor) s).oldfaithful$buttons();
		return list;
	}

	private static Screens.Button find(Screen s, String text) {
		for (ButtonWidget b : buttons(s)) {
			if (b instanceof Screens.Button && b.text.equals(text)) return (Screens.Button) b;
		}
		return null;
	}

	private int items(Minecraft mc, int item) {
		int n = 0;
		for (Object e : mc.world.getEntities((Entity) null, Box.createCached(mc.player.x - 8, mc.player.y - 8, mc.player.z - 8, mc.player.x + 8, mc.player.y + 8, mc.player.z + 8))) {
			if (e instanceof ItemEntity && ((ItemEntity) e).stack.itemId == item) n += ((ItemEntity) e).stack.count;
		}
		return n;
	}

	private void keysAndScreens(Minecraft mc) {
		results.check("mods' keys join the game's own before options.txt is read: " + mc.options.allKeys.length, mc.options.allKeys.length == 10 + ClientHooks.ADDED_KEYS && mc.options.allKeys[10] == ClientHooks.first);

		KeybindsScreen keys = (KeybindsScreen) open(mc, new KeybindsScreen(null, mc.options));
		List<ButtonWidget> b = buttons(keys);
		Screens.Button forward = find(keys, ">");
		results.check("past fourteen keys the screen pages", forward != null && find(keys, "<") != null);
		int firstY = b.get(0).y;
		results.check("page one shows fourteen", b.get(13).visible && !b.get(14).visible && !b.get(14).active);
		forward.click();
		b = buttons(keys);
		results.check("page two shows the rest in beta's places", !b.get(0).visible && b.get(14).visible && b.get(14).active && b.get(14).y == firstY);
		keys.render(0, 0, 0.0F);

		mc.options.setKeybindKey(10 + ClientHooks.ADDED_KEYS - 1, 44);
		try {
			String text = new String(Files.readAllBytes(FabricLoader.getInstance().getGameDir().resolve("options.txt")));
			results.check("a mod's key is saved with the game's", text.contains("key_key.hook-check.last:44"));
		} catch (java.io.IOException e) {
			results.fail("options.txt", e);
		}

		OptionsScreen options = (OptionsScreen) open(mc, new OptionsScreen(null, mc.options));
		Screens.Button hook = find(options, "Hook");
		results.check("a screen opened gets a mod's widget", hook != null);
		ClientHooks.clicks = 0;
		((ScreenAccessor) options).oldfaithful$click(hook.x + 1, hook.y + 1, 0);
		results.check("and a click on it reaches the mod, not the screen", ClientHooks.clicks == 1);
		open(mc, options);
		results.check("it's there again after a resize rebuilds the screen", find(options, "Hook") != null);

		InventoryScreen inventory = new InventoryScreen(mc.player);
		open(mc, inventory);
		results.check("slot roles read the player's screen", Screens.role(mc.player.playerScreenHandler, (Slot) mc.player.playerScreenHandler.slots.get(0)) == Screens.SlotRole.CRAFT_RESULT
				&& Screens.role(mc.player.playerScreenHandler, (Slot) mc.player.playerScreenHandler.slots.get(1)) == Screens.SlotRole.CRAFT_INPUT
				&& Screens.role(mc.player.playerScreenHandler, (Slot) mc.player.playerScreenHandler.slots.get(5)) == Screens.SlotRole.ARMOR
				&& Screens.role(mc.player.playerScreenHandler, (Slot) mc.player.playerScreenHandler.slots.get(9)) == Screens.SlotRole.PLAYER_MAIN
				&& Screens.role(mc.player.playerScreenHandler, (Slot) mc.player.playerScreenHandler.slots.get(36)) == Screens.SlotRole.HOTBAR);
		Slot slot = (Slot) mc.player.playerScreenHandler.slots.get(36);
		mc.player.inventory.main[0] = new ItemStack(Item.DIAMOND, 1);
		int left = (inventory.width - 176) / 2, top = (inventory.height - 166) / 2;
		ClientHooks.TOOLTIP.clear();
		inventory.render(left + slot.x + 2, top + slot.y + 2, 0.0F);
		results.check("a tooltip is heard with the item's name: " + ClientHooks.TOOLTIP, ClientHooks.TOOLTIP.size() == 1 && ClientHooks.TOOLTIP.get(0).equals("Diamond"));
		ClientHooks.addTooltip = true;
		inventory.render(left + slot.x + 2, top + slot.y + 2, 0.0F);
		ClientHooks.addTooltip = false;
		results.check("and a line added is drawn without a fault", HookCheck.count("tooltip") >= 2);
		mc.player.inventory.main[0] = null;
	}

	private void blocks(Minecraft mc) {
		x = (int) Math.floor(mc.player.x) + 2;
		y = (int) Math.floor(mc.player.y) + 3;
		z = (int) Math.floor(mc.player.z);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 0; dy <= 3; dy++) mc.world.setBlock(x + dx, y + dy, z + dz, dy == 0 ? Block.STONE.id : 0);
			}
		}
		mc.world.setBlock(x, y + 1, z, Block.DIRT.id);
		HookCheck.vetoBreak = true;
		boolean broke = mc.interactionManager.breakBlock(x, y + 1, z, 1);
		HookCheck.vetoBreak = false;
		results.check("single player: a vetoed break leaves the block", !broke && mc.world.getBlockId(x, y + 1, z) == Block.DIRT.id);
		HookCheck.addDrop = true;
		broke = mc.interactionManager.breakBlock(x, y + 1, z, 1);
		HookCheck.addDrop = false;
		results.check("single player: a break's drop filter has the player", broke && HookCheck.lastBlockDrops != null && HookCheck.lastBlockDrops.player == mc.player);
		ItemStack stone = new ItemStack(Block.STONE, 3);
		HookCheck.vetoUse = true;
		boolean used = mc.interactionManager.interactBlock(mc.player, mc.world, stone, x, y, z, 1);
		HookCheck.vetoUse = false;
		results.check("single player: a vetoed use places nothing", !used && mc.world.getBlockId(x, y + 1, z) == 0);
		used = mc.interactionManager.interactBlock(mc.player, mc.world, stone, x, y, z, 1);
		results.check("single player: placing is heard", used && HookCheck.lastPlaced != null && HookCheck.lastPlaced.x == x && HookCheck.lastPlaced.y == y + 1);
		mc.world.setBlock(x, y + 1, z, 0);
	}

	@Override
	public boolean tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return false;
		}
		switch (stage) {
			case 0: {
				results.check("single player: a world load, the save's start and a join were heard: " + HookCheck.summary(),
						HookCheck.count("worldLoad") == 1 && HookCheck.count("serverStart") == 1 && HookCheck.count("joined") == 1);
				results.check("single player: all three ticks run", HookCheck.count("worldTick") > 0 && HookCheck.count("serverTick") > 0 && HookCheck.count("clientTick") > 0);
				results.check("single player: chunks load", HookCheck.count("chunkLoaded") > 0);
				results.check("the HUD's layers stack in a corner, each under the last: " + ClientHooks.firstLayerY + ", " + ClientHooks.secondLayerY,
						HookCheck.count("hud") > 0 && ClientHooks.secondLayerY > ClientHooks.firstLayerY);
				results.check("the frame's stages are heard", HookCheck.count("afterEntities") > 0 && HookCheck.count("afterWorld") > 0);
				results.check("the sky's is, at this distance or not, as beta draws one: " + mc.options.viewDistance, mc.options.viewDistance >= 2 || HookCheck.count("sky") > 0);
				keysAndScreens(mc);
				mc.setScreen(null);
				blocks(mc);
				StartupCheck.pushKey(K, 0, true);
				StartupCheck.pushKey(K, 0, false);
				next(1, 3);
				return false;
			}
			case 1: {
				results.check("a raw key is heard, and a bound key's press", HookCheck.count("key") >= 2 && HookCheck.count("keyPressed") == 1);
				ClientHooks.vetoKey = true;
				StartupCheck.pushKey(E, 0, true);
				StartupCheck.pushKey(E, 0, false);
				next(2, 3);
				return false;
			}
			case 2: {
				results.check("a vetoed key never reaches the game", mc.currentScreen == null);
				ClientHooks.vetoKey = false;
				StartupCheck.pushKey(E, 0, true);
				StartupCheck.pushKey(E, 0, false);
				next(3, 3);
				return false;
			}
			case 3: {
				results.check("the same key goes through without the veto", mc.currentScreen instanceof InventoryScreen);
				mc.setScreen(null);
				ClientHooks.widenFov = true;
				ClientHooks.tilt = true;
				mc.options.thirdPerson = true;
				next(4, 3);
				return false;
			}
			case 4: {
				results.check("a filter's FOV is what's used: " + ClientHooks.lastFov, ClientHooks.lastFov >= 80.0F);
				results.check("the roll filter is asked each frame", HookCheck.count("roll") > 0);
				results.check("a player's pose is heard in third person", HookCheck.count("pose") > 0);
				ClientHooks.widenFov = false;
				ClientHooks.tilt = false;
				mc.options.thirdPerson = false;
				HookCheck.mana.set(mc.player, 7);
				HookCheck.rage.set(mc.player, 9);
				HookCheck.pouch.set(mc.player, 3);
				HookCheck.scratch.set(mc.player, 5);
				mc.player.inventory.addStack(new ItemStack(Item.DIAMOND, 2));
				mc.player.inventory.addStack(new ItemStack(Item.COAL, 2));
				HookCheck.keepFirstStack = true;
				mc.player.damage(null, 1000);
				HookCheck.keepFirstStack = false;
				next(5, 5);
				return false;
			}
			case 5: {
				results.check("single player: a death is heard, and its drops filter", HookCheck.count("died") >= 1 && HookCheck.count("deathDrops") == 1);
				results.check("single player: DROP's stacks fall: " + items(mc, Item.GOLD_INGOT.id), items(mc, Item.GOLD_INGOT.id) == 3);
				results.check("single player: what a filter didn't keep falls", items(mc, Item.COAL.id) == 2 && items(mc, Item.DIAMOND.id) == 0);
				mc.respawnPlayer(false, 0);
				next(6, 5);
				return false;
			}
			case 6: {
				results.check("single player: the respawn hook gave the old and new objects", HookCheck.reborn == mc.player && HookCheck.dead != mc.player);
				results.check("single player: KEEP carried over, RESET didn't, DROP's gone", HookCheck.mana.get(mc.player) == 7 && HookCheck.scratch.get(mc.player) == 5 && !HookCheck.rage.has(mc.player) && !HookCheck.pouch.has(mc.player));
				int diamonds = 0;
				for (ItemStack s : mc.player.inventory.main) {
					if (s != null && s.itemId == Item.DIAMOND.id) diamonds += s.count;
				}
				results.check("single player: the stack a filter kept is on the new player: " + diamonds, diamonds == 2);
				HookCheck.rage.set(mc.player, 9);
				HookCheck.pouch.set(mc.player, 3);
				mc.changeDimension();
				next(7, 5);
				return false;
			}
			case 7: {
				results.check("single player: a dimension change is heard, and attachments stay", HookCheck.count("dimension") == 1 && HookCheck.lastDimension.to == -1 && HookCheck.mana.get(mc.player) == 7 && HookCheck.rage.get(mc.player) == 9);
				results.check("single player: the Nether is another world's load and unload, not a stop", HookCheck.count("worldLoad") == 2 && HookCheck.count("worldUnload") == 1 && HookCheck.count("serverStop") == 0);
				mc.changeDimension();
				next(8, 5);
				return false;
			}
			case 8: {
				results.check("single player: and the way back", HookCheck.count("dimension") == 2 && HookCheck.lastDimension.to == 0 && HookCheck.mana.get(mc.player) == 7);
				mc.world.saveWithLoadingDisplay(true, null);
				results.check("single player: a save is heard", HookCheck.count("worldSave") > 0);
				return true;
			}
			case 50: {
				results.check("reopened: persisted values came back", HookCheck.mana.get(mc.player) == 7 && HookCheck.rage.get(mc.player) == 9 && HookCheck.pouch.get(mc.player) == 3);
				results.check("reopened: one not persisted didn't", !HookCheck.scratch.has(mc.player));
				results.check("reopened: a mod's key kept its binding", ClientHooks.last.code == 44);
				return true;
			}
			case 100: {
				next(101, 40);
				return false;
			}
			case 101: {
				mc.player.sendChatMessage("/hookhi");
				mc.player.sendChatMessage("/hooktest attach OldFaithfulTest");
				next(102, 40);
				return false;
			}
			case 102: {
				results.check("on a server: SELF comes to the player it's on", Integer.valueOf(7).equals(HookCheck.mana.get(mc.player)) && Integer.valueOf(9).equals(HookCheck.rage.get(mc.player)));
				results.check("on a server: NONE doesn't", !HookCheck.pouch.has(mc.player) && !HookCheck.scratch.has(mc.player));
				boolean cow = false;
				for (Object e : mc.world.entities) cow |= e instanceof CowEntity && "kept".equals(HookCheck.tag.get((Entity) e));
				results.check("on a server: TRACKING reaches a client that sees the entity", cow);
				String note = HookCheck.note.get(mc.world.getChunk(mc.world.getSpawnPos().x >> 4, mc.world.getSpawnPos().z >> 4));
				results.check("on a server: TRACKING reaches a client that has the chunk: " + note, "spawn chunk".equals(note));
				mc.player.sendChatMessage("/hooktest kill OldFaithfulTest false");
				next(103, 40);
				return false;
			}
			case 103: {
				mc.player.respawn();
				mc.setScreen(null);
				next(104, 40);
				return false;
			}
			case 104: {
				results.check("on a server: after a respawn the new client player has what it keeps", Integer.valueOf(7).equals(HookCheck.mana.get(mc.player)) && !HookCheck.rage.has(mc.player));
				mc.player.sendChatMessage("/hooktest respawned OldFaithfulTest false");
				next(105, 20);
				return false;
			}
			case 105: {
				mc.player.sendChatMessage("/hooktest dimension OldFaithfulTest");
				next(106, 60);
				return false;
			}
			case 106: {
				results.check("on a server: and after a dimension change", Integer.valueOf(7).equals(HookCheck.mana.get(mc.player)));
				StringBuilder spoke = new StringBuilder();
				for (String hook : HookCheck.WORLD_SIDE) {
					if (HookCheck.count(hook) != 0) spoke.append(hook).append(' ');
				}
				results.check("a client on a server heard no world-side hook: " + spoke, spoke.length() == 0);
				results.check("its own hooks still run: " + HookCheck.summary(), HookCheck.count("clientTick") > 0 && HookCheck.count("hud") > 0);
				return true;
			}
			default:
				return true;
		}
	}
}
