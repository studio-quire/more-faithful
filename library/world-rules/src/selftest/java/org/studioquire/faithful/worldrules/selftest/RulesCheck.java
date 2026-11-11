package org.studioquire.faithful.worldrules.selftest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.studioquire.faithful.worldrules.WorldRules;
import org.studioquire.oldfaithful.core.api.ChoiceRule;
import org.studioquire.oldfaithful.core.api.Rule;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.client.RuleSettings;
import org.studioquire.oldfaithful.options.settings.SettingsModsScreen;
import org.studioquire.oldfaithful.options.settings.SettingsScreen;
import org.studioquire.oldfaithful.selftest.LibraryScreens;
import org.studioquire.oldfaithful.selftest.ScreenCapture;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;
import org.studioquire.oldfaithful.selftest.mixin.ScreenAccessor;

/**
 * World Rules in a running single player world and, joined to the real server, as a client sees them. Started by the harness's
 * start-up test. Then the lock: in single player through the page and its confirm screen, and later launches ({@code worldrules.test.phase})
 * find it still there after a reopen, then gone once the harness took the line out of the world's file; on a locked server, as a client sees it.
 * A mod with rules can't be switched off inside the lock: its Enabled, and that of what it depends on (jars in mods-library with nothing in them but a name, which the harness adds).
 */
public final class RulesCheck implements WorldCheck {
	private static final String PHASE = System.getProperty("worldrules.test.phase");
	private SelfTest results;
	private int stage;
	private int wait;
	private ScreenCapture capture;
	private Screen page;
	private String fileBefore;

	@Override
	public void start(Minecraft mc, SelfTest results) {
		this.results = results;
		// The selftest stage has RulesTest instead; this is libraryRulesTest's, which sets the server's rules up for it.
		if (System.getProperty("faithful.test") != null) stage = -1;
		else if (mc.world.isRemote) stage = Boolean.getBoolean("worldrules.test.serverLocked") ? 110 : 100;
		else stage = "reopened".equals(PHASE) ? 300 : "unlocked".equals(PHASE) ? 400 : 0;
	}

	private static String worldText() {
		try {
			return Files.exists(worldFile()) ? new String(Files.readAllBytes(worldFile()), StandardCharsets.UTF_8) : "";
		} catch (IOException e) {
			return "unreadable: " + e;
		}
	}

	/** The next frame of the current screen saved in shots/, with the mouse on a button (or nowhere); true once it's written. */
	private boolean shot(String name, ButtonWidget pointAt) {
		if (capture == null) {
			capture = ScreenCapture.request(new File("shots/" + name + ".png"), pointAt == null ? -1 : pointAt.x + 4, pointAt == null ? -1 : pointAt.y + 4);
			return false;
		}
		if (!capture.done()) return false;
		capture = null;
		return true;
	}

	private static List<String> wrap(Minecraft mc, String text) {
		List<String> lines = new ArrayList<>();
		String line = "";
		for (String word : text.split(" ")) {
			if (!line.isEmpty() && mc.textRenderer.getWidth(line + " " + word) > 300) {
				lines.add(line);
				line = "";
			}
			line = line.isEmpty() ? word : line + " " + word;
		}
		if (!line.isEmpty()) lines.add(line);
		return lines;
	}

	/** The lock's words in the game's font: labels inside a short button, hints in their two lines, the confirm screen's inside 300 of a small window's 320. */
	private void words(Minecraft mc) {
		List<String> problems = new ArrayList<>();
		for (String label : new String[]{RuleSettings.LOCK, RuleSettings.LOCKED}) {
			if (mc.textRenderer.getWidth(label) > 144) problems.add(label);
		}
		for (String hint : new String[]{RuleSettings.LOCK_HINT, RuleSettings.WORLD_LOCKED, RuleSettings.SERVER_LOCKED}) {
			if (wrap(mc, hint).size() > 2) problems.add(hint);
		}
		if (mc.textRenderer.getWidth(RuleSettings.CONFIRM) > 300 || wrap(mc, RuleSettings.CONFIRM_WARNING).size() > 3) problems.add("the confirm screen's lines");
		results.check("the lock's words fit their places" + (problems.isEmpty() ? "" : ": " + problems), problems.isEmpty());
		String all = String.join(" ", RuleSettings.LOCK, RuleSettings.LOCKED, RuleSettings.LOCK_HINT, RuleSettings.WORLD_LOCKED, RuleSettings.SERVER_LOCKED, RuleSettings.CONFIRM, RuleSettings.CONFIRM_WARNING);
		results.check("and they say nothing of how to unlock: " + all, saysNothingOfUnlocking(all));
	}

	private static boolean saysNothingOfUnlocking(String text) {
		String lower = text.toLowerCase(java.util.Locale.ROOT);
		for (String word : new String[]{"file", "propert", "unlock", "edit", "remove", "closed", Rules.LOCK_KEY}) {
			if (lower.contains(word)) return false;
		}
		return true;
	}

	/** What the page really draws with the mouse on a button, which is that button's hint and the rest of the page. */
	private static String drawnOver(Screen page, ButtonWidget button) {
		LibraryScreens.clear();
		page.render(button.x + 4, button.y + 4, 0);
		return LibraryScreens.text();
	}

	private static String libraryList() {
		try {
			Path list = FabricLoader.getInstance().getGameDir().resolve("mods-library").resolve("selection.txt");
			return Files.exists(list) ? new String(Files.readAllBytes(list), StandardCharsets.UTF_8) : "";
		} catch (IOException e) {
			return "unreadable: " + e;
		}
	}

	/** A mod's Enabled in a lock: greyed, saying why when pointed at, and a press changes neither it nor the library's list. */
	private void lockedSwitch(Screen page, String where, String why) {
		ButtonWidget enabled = find(page, "Enabled");
		if (enabled == null) {
			results.check(where + ": the page has Enabled", false);
			return;
		}
		String before = enabled.text, list = libraryList(), drawn = drawnOver(page, enabled);
		press(page, enabled);
		results.check(where + ": Enabled can't be pressed, and shows why: " + before + ", " + drawn, !enabled.active && drawn.contains(why) && saysNothingOfUnlocking(drawn));
		results.check(where + ": a press leaves it and the library's list as they were", enabled.text.equals(before) && libraryList().equals(list));
	}

	/** A page of locked rules: the lock's button says so and can't be pressed, and every rule shows its value and can't be pressed either. */
	private void lockedPage(Screen page, String where) {
		ButtonWidget lock = find(page, RuleSettings.LOCKED), keep = find(page, "Keep Inventory"), weather = find(page, "Weather");
		results.check(where + ": the page reads " + RuleSettings.LOCKED + ", not pressable: " + (lock == null ? null : lock.text + " " + lock.active),
				lock != null && !lock.active && find(page, RuleSettings.LOCK) == null);
		results.check(where + ": the rules show their values and can't be pressed: " + (keep == null || weather == null ? null : keep.text + ", " + weather.text),
				keep != null && weather != null && !keep.active && !weather.active && keep.text.equals("Keep Inventory: OFF") && weather.text.equals("Weather: Normal"));
		if (lock != null) {
			String drawn = drawnOver(page, lock);
			results.check(where + ": the button's hint says the rules are locked, not how to unlock: " + drawn, drawn.contains(RuleSettings.WORLD_LOCKED) && saysNothingOfUnlocking(drawn));
		}
		lockedSwitch(page, where, RuleSettings.WORLD_LOCKED);
	}

	private static Path worldFile() {
		return FabricLoader.getInstance().getGameDir().resolve("saves").resolve("oldfaithful-startup").resolve("faithful-world.properties");
	}

	private static ButtonWidget find(Screen s, String text) {
		for (ButtonWidget b : ((ScreenAccessor) s).oldfaithful$buttons()) {
			if (b.text.startsWith(text)) return b;
		}
		return null;
	}

	private void press(Screen s, ButtonWidget b) {
		((ScreenAccessor) s).oldfaithful$press(b);
	}

	private Screen open(Minecraft mc, Screen s) {
		s.init(mc, 427, 240);
		return s;
	}

	/** A mod's page from the Mods menu, by the start of its name. */
	private Screen pageOf(Minecraft mc, String name) {
		Screen mods = open(mc, SettingsScreen.open(null));
		press(mods, find(mods, name));
		return mc.currentScreen;
	}

	/** A rule's button on its mod's page, turned to the next page when it isn't on this one: a page shares its rules evenly, so where one falls moves as rules are added. */
	private ButtonWidget across(Screen page, String prefix) {
		ButtonWidget b = find(page, prefix), next = find(page, ">");
		if (b != null || next == null) return b;
		press(page, next);
		return find(page, prefix);
	}

	/** A choice's button goes through its words, each taking effect, back to where it started. */
	private void steps(Screen page, String prefix, ChoiceRule rule, String... words) {
		ButtonWidget b = across(page, prefix);
		for (int i = 0; i < words.length; i++) {
			results.check("the button reads " + words[i] + ", the rule " + rule.values().get(i) + ": " + (b == null ? null : b.text),
					b != null && b.text.equals(prefix + words[i]) && rule.get().equals(rule.values().get(i)));
			if (b != null) press(page, b);
		}
		results.check("and it's back at the start: " + (b == null ? null : b.text), b != null && b.text.equals(prefix + words[0]) && rule.get().equals(rule.defaultText()));
	}

	private void give(Minecraft mc) {
		mc.player.inventory.addStack(new ItemStack(Item.DIAMOND, 7));
	}

	private int diamonds(Minecraft mc) {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) {
			if (s != null && s.itemId == Item.DIAMOND.id) n += s.count;
		}
		return n;
	}

	private int itemsNear(Minecraft mc) {
		int n = 0;
		for (Object e : mc.world.getEntities((Entity) null, Box.createCached(mc.player.x - 6, mc.player.y - 6, mc.player.z - 6, mc.player.x + 6, mc.player.y + 6, mc.player.z + 6))) {
			if (e instanceof ItemEntity) n++;
		}
		return n;
	}

	/** World Rules' page, as Options reaches it where no edition is here: the Mods menu comes first, with nothing to go through before it. */
	private Screen modPage(Minecraft mc) {
		Screen mods = open(mc, SettingsScreen.open(null));
		results.check("the settings open straight on the Mods menu: " + mods.getClass().getSimpleName(), mods instanceof SettingsModsScreen);
		press(mods, find(mods, "World Rules..."));
		return mc.currentScreen;
	}

	/** The library pack's list: this run was launched from it, and a switch changes it for the next launch only. */
	private void library(Minecraft mc) {
		Screen page = modPage(mc);
		ButtonWidget rules = find(page, "Enabled");
		results.check("World Rules reads as on, as this launch's list has it: " + (rules == null ? null : rules.text), rules != null && rules.text.equals("Enabled: ON"));
		press(page, rules);
		String text = libraryList();
		results.check("off: the list loses it, and the core too with nothing else on; the button says wait for a restart: " + rules.text,
				!text.contains("world-rules.jar") && !text.contains("faithful-core.jar") && rules.text.equals("Enabled: OFF*"));
		press(page, rules);
		text = libraryList();
		results.check("on: both are back", text.contains("world-rules.jar") && text.contains("faithful-core.jar") && rules.text.equals("Enabled: ON"));
	}

	@Override
	public boolean tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return false;
		}
		switch (stage) {
			case 0: {
				results.check("a new world: every rule at beta's default", WorldRules.mobsBreakBlocks.get() && WorldRules.blastsDestroyItems.get() && !WorldRules.keepInventory.get() && WorldRules.sleeping.get()
						&& WorldRules.weather.get().equals("normal") && WorldRules.naturalMonsters.get());
				results.check("a world nobody touched has no rules file", !Files.exists(worldFile()));
				Screen options = open(mc, new OptionsScreen(null, mc.options));
				results.check("Options has the settings button", find(options, FabricLoader.getInstance().isModLoaded("oldfaithful-options") ? "Old Faithful..." : "Mods...") != null);
				Screen create = open(mc, new CreateWorldScreen(null));
				ButtonWidget rules = find(create, "World Rules...");
				results.check("Create World has World Rules..., clear of beta's buttons and on the screen", rules != null && rules.y + 20 <= 240 && rules.y >= find(create, "Cancel").y + 20);
				press(create, rules);
				results.check("and it opens the rules, without the mod's on and off", find(mc.currentScreen, "Keep Inventory") != null && find(mc.currentScreen, "Enabled") == null);
				library(mc);
				Screen page = modPage(mc);
				ButtonWidget keep = find(page, "Keep Inventory");
				results.check("the rule's page says what's set: " + (keep == null ? null : keep.text), keep != null && keep.text.equals("Keep Inventory: OFF") && keep.active);
				press(page, keep);
				results.check("pressing it writes the world's file and takes effect at once: " + keep.text, keep.text.equals("Keep Inventory: ON") && WorldRules.keepInventory.get() && Files.exists(worldFile()));
				steps(page, "Weather: ", WorldRules.weather, "Normal", "Clear", "Rain or Snow");
				ButtonWidget monsters = across(page, "Natural Monsters");
				results.check("monsters read ON: " + (monsters == null ? null : monsters.text), monsters != null && monsters.text.equals("Natural Monsters: ON"));
				mc.setScreen(null);
				give(mc);
				mc.player.damage(null, 1000);
				stage = 1;
				wait = 5;
				return false;
			}
			case 1: {
				results.check("keep inventory: a death drops nothing", itemsNear(mc) == 0);
				mc.respawnPlayer(false, 0);
				stage = 2;
				wait = 5;
				return false;
			}
			case 2: {
				results.check("keep inventory: the new player has what the old one carried: " + diamonds(mc), diamonds(mc) == 7);
				Rules.change(WorldRules.keepInventory, "false");
				mc.player.damage(null, 1000);
				stage = 3;
				wait = 5;
				return false;
			}
			case 3: {
				results.check("rule off: a death drops what was carried: " + itemsNear(mc), itemsNear(mc) > 0);
				mc.respawnPlayer(false, 0);
				stage = 30;
				wait = 5;
				return false;
			}
			case 30: {
				// Counted now, as after the first death: what was dropped lies at the spawn, and can be picked up again from 40 ticks on.
				results.check("rule off: nothing carried over: " + diamonds(mc), diamonds(mc) == 0);
				stage = 4;
				// The window's close skips the final save, so the reopened world has what the last autosave saw; wait for one, every 40
				// ticks, with the player alive, or the reopen can find them dead on the death screen.
				wait = 40;
				return false;
			}
			case 4: {
				int x = (int) Math.floor(mc.player.x) + 2, y = (int) Math.floor(mc.player.y) + 6, z = (int) Math.floor(mc.player.z) + 2;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 2; dz++) mc.world.setBlock(x + dx, y - 1, z + dz, Block.STONE.id);
				}
				mc.world.setBlock(x, y, z, Block.BED.id, 0);
				mc.world.setBlock(x, y, z + 1, Block.BED.id, 8);
				Rules.change(WorldRules.sleeping, "false");
				mc.world.setTime(18000);
				Block.BED.onUse(mc.world, x, y, z, mc.player);
				Vec3i at = mc.player.getSpawnPos();
				results.check("sleeping off: the bed is the respawn point, and nobody sleeps", at != null && at.x == x && at.y == y && at.z == z + 1 && !mc.player.isSleeping());
				Rules.change(WorldRules.sleeping, "true");
				Rules.change(WorldRules.mobsBreakBlocks, "false");
				CreeperEntity c = new CreeperEntity(mc.world);
				c.setPosition(x + 0.5, y + 1, z + 0.5);
				mc.world.createExplosion(c, x + 0.5, y + 1, z + 0.5, 3.0F);
				results.check("mobs break blocks off: a creeper leaves the bed", mc.world.getBlockId(x, y, z) == Block.BED.id && mc.world.getBlockId(x, y - 1, z) == Block.STONE.id);
				Rules.change(WorldRules.mobsBreakBlocks, "true");
				mc.world.createExplosion(c, x + 0.5, y + 1, z + 0.5, 3.0F);
				results.check("mobs break blocks on: a creeper breaks it", mc.world.getBlockId(x, y - 1, z) != Block.STONE.id);
				Rules.change(WorldRules.blastsDestroyItems, "false");
				// High in the air: these break nothing, and the player's far enough off not to feel them.
				ItemEntity spared = new ItemEntity(mc.world, x + 1.5, y + 16, z + 0.5, new ItemStack(Item.DIAMOND));
				mc.world.spawnEntity(spared);
				mc.world.createExplosion(null, x + 0.5, y + 16, z + 0.5, 3.0F);
				results.check("blasts destroy items off: the item is still there, and thrown", !spared.dead && spared.velocityX > 0);
				Rules.change(WorldRules.blastsDestroyItems, "true");
				spared.setPosition(x + 1.5, y + 16, z + 0.5);
				mc.world.createExplosion(null, x + 0.5, y + 16, z + 0.5, 3.0F);
				results.check("blasts destroy items on: it's gone", spared.dead);
				mc.world.setTime(6000);
				stage = 5;
				return false;
			}
			case 5: {
				if (capture == null) {
					words(mc);
					page = modPage(mc);
					ButtonWidget lock = find(page, RuleSettings.LOCK), enabled = find(page, "Enabled");
					results.check("World Rules' page: Lock Rules beside Enabled in the top row, pressable: " + (lock == null ? null : lock.x + "," + lock.y),
							lock != null && enabled != null && lock.active && lock.y == enabled.y && lock.x == page.width / 2 + 5 && enabled.x == page.width / 2 - 155
									&& find(page, "Mobs Break Blocks").y == enabled.y + 24);
				}
				if (shot("rules-unlocked", find(page, RuleSettings.LOCK))) stage = 6;
				return false;
			}
			case 6: {
				fileBefore = worldText();
				press(page, find(page, RuleSettings.LOCK));
				Screen confirm = mc.currentScreen;
				results.check("Lock Rules asks first, on beta's confirm screen: " + confirm.getClass().getSimpleName(),
						confirm instanceof ConfirmScreen && find(confirm, RuleSettings.CONFIRM_YES) != null && find(confirm, RuleSettings.CONFIRM_NO) != null);
				press(confirm, find(confirm, RuleSettings.CONFIRM_NO));
				ButtonWidget lock = find(page, RuleSettings.LOCK);
				results.check("Cancel: back on the page, nothing locked, the file as it was", mc.currentScreen == page && !Rules.locked()
						&& lock != null && lock.active && worldText().equals(fileBefore));
				press(page, lock);
				stage = 7;
				wait = 3;
				return false;
			}
			case 7: {
				if (shot("lock-confirm", null)) stage = 8;
				return false;
			}
			case 8: {
				press(mc.currentScreen, find(mc.currentScreen, RuleSettings.CONFIRM_YES));
				String text = worldText();
				results.check("Lock: back on the page, the rules locked, the line in the world's file: " + mc.currentScreen.getClass().getSimpleName(),
						mc.currentScreen == page && Rules.locked() && text.contains(Rules.LOCK_KEY + "=true"));
				List<String> rest = new ArrayList<>(java.util.Arrays.asList(text.split("\r?\n")));
				rest.remove(Rules.LOCK_KEY + "=true");
				results.check("and nothing else in the file changed", rest.equals(java.util.Arrays.asList(fileBefore.split("\r?\n"))));
				lockedPage(page, "locked");
				boolean changed = Rules.change(WorldRules.keepInventory, "true") | Rules.change(WorldRules.weather, "rain");
				results.check("a change from anywhere in the game is refused: the rules as they were, the file too",
						!changed && !WorldRules.keepInventory.get() && WorldRules.weather.get().equals("normal") && worldText().equals(text));
				press(page, find(page, "Keep Inventory"));
				results.check("a press on a rule does nothing", !WorldRules.keepInventory.get() && find(page, "Keep Inventory").text.equals("Keep Inventory: OFF"));
				stage = 9;
				return false;
			}
			case 9: {
				if (shot("rules-locked", find(page, RuleSettings.LOCKED))) stage = 10;
				return false;
			}
			case 10: {
				if (!shot("rules-locked-rule", find(page, "Keep Inventory"))) return false;
				mc.setScreen(null);
				return true;
			}
			case 300: {
				// Reopened with the line still in the file: the lock is the world's, and a rule declared since is locked too.
				results.check("reopened: still locked, the line still there", Rules.locked() && worldText().contains(Rules.LOCK_KEY + "=true"));
				lockedPage(modPage(mc), "reopened");
				Rule late = null;
				for (Rule r : Rules.all()) {
					if (r.id().equals("world-rules-selftest:late_rule")) late = r;
				}
				results.check("a rule a mod brought since is at its default, and locked too", late != null && "false".equals(Rules.shown(late)) && !Rules.change(late, "true")
						&& "false".equals(Rules.shown(late)) && !worldText().contains("late_rule"));
				Screen lateMod = pageOf(mc, "World Rules self-test");
				ButtonWidget other = find(lateMod, RuleSettings.LOCKED), lateButton = find(lateMod, "rule.world-rules-selftest.late_rule");
				results.check("and that mod's page shows the lock too, its rule not pressable", other != null && !other.active && lateButton != null && !lateButton.active);
				lockedSwitch(lateMod, "any mod with rules, not only World Rules", RuleSettings.WORLD_LOCKED);
				lockedSwitch(pageOf(mc, "Side Door Base"), "a mod with no rules that one with rules depends on", RuleSettings.WORLD_LOCKED);
				Screen free = pageOf(mc, "Free Mod");
				ButtonWidget freeSwitch = find(free, "Enabled");
				results.check("a mod with no rules and nothing depending on it can still be pressed", freeSwitch != null && freeSwitch.active);
				if (freeSwitch != null) {
					press(free, freeSwitch);
					results.check("and it goes on the list: " + freeSwitch.text, libraryList().contains("free-mod.jar"));
					press(free, freeSwitch);
					results.check("and off again: " + freeSwitch.text, !libraryList().contains("free-mod.jar"));
				}
				mc.setScreen(null);
				// As the pause menu's Save and quit to title does.
				mc.setWorld(null);
				mc.setScreen(null);
				stage = 301;
				wait = 5;
				return false;
			}
			case 301: {
				results.check("on the title screen: nothing's locked here, and the world's file still has the line", !Rules.worldOpen() && !Rules.locked() && worldText().contains(Rules.LOCK_KEY + "=true"));
				library(mc);
				ButtonWidget base = find(pageOf(mc, "Side Door Base"), "Enabled");
				results.check("and what a mod with rules depends on can be pressed too", base != null && base.active);
				mc.setScreen(null);
				return true;
			}
			case 400: {
				// The harness took the line out with the game closed.
				results.check("unlocked by hand: the line gone, the rules unlocked", !Rules.locked() && !worldText().contains(Rules.LOCK_KEY));
				library(mc);
				Screen page = modPage(mc);
				ButtonWidget lock = find(page, RuleSettings.LOCK), keep = find(page, "Keep Inventory");
				results.check("the page has Lock Rules again, and the rules can be pressed", lock != null && lock.active && keep != null && keep.active);
				if (keep != null) {
					press(page, keep);
					results.check("a press changes the rule again: " + keep.text, keep.text.equals("Keep Inventory: ON") && WorldRules.keepInventory.get()
							&& worldText().contains("rule.world-rules.keep_inventory=true"));
					press(page, keep);
					results.check("and back: " + keep.text, keep.text.equals("Keep Inventory: OFF") && !WorldRules.keepInventory.get());
				}
				mc.setScreen(null);
				return true;
			}
			case 110: {
				wait = 40;
				stage = 111;
				return false;
			}
			case 111: {
				if (capture == null) {
					results.check("on a locked server the client hears it's locked", Rules.locked());
					page = modPage(mc);
					ButtonWidget keep = find(page, "Keep Inventory"), weather = find(page, "Weather");
					results.check("its page reads " + RuleSettings.LOCKED + ", not pressable, and the rules aren't either", find(page, RuleSettings.LOCKED) != null
							&& !find(page, RuleSettings.LOCKED).active && keep != null && !keep.active && weather != null && !weather.active);
					lockedSwitch(page, "on a locked server", RuleSettings.SERVER_LOCKED);
				}
				if (!shot("server-locked", find(page, RuleSettings.LOCKED))) return false;
				mc.setScreen(null);
				// An op's /rule, which the server refuses: the harness reads the world's file after.
				mc.player.sendChatMessage("/rule keep_inventory true");
				stage = 112;
				wait = 40;
				return false;
			}
			case 100: {
				wait = 40;
				stage = 101;
				return false;
			}
			case 101: {
				boolean expected = Boolean.getBoolean("faithful.test.keep");
				results.check("on a server the client hears its rules", WorldRules.keepInventory.get() == expected && !WorldRules.sleeping.get());
				Screen page = modPage(mc);
				ButtonWidget keep = find(page, "Keep Inventory");
				results.check("on a server the rule shows the server's value and can't be pressed: " + keep.text, keep.text.equals("Keep Inventory: " + (expected ? "ON" : "OFF")) && !keep.active);
				ButtonWidget weather = find(page, "Weather");
				results.check("and a choice too: " + weather.text, weather.text.equals("Weather: Rain or Snow") && !weather.active);
				mc.player.sendChatMessage("/ruletest kill OldFaithfulTest true");
				stage = 102;
				wait = 40;
				return false;
			}
			case 102: {
				mc.player.respawn();
				mc.setScreen(null);
				stage = 103;
				wait = 40;
				return false;
			}
			case 103: {
				results.check("after a death and a respawn the client has its things back: " + diamonds(mc), diamonds(mc) >= 7);
				mc.player.sendChatMessage("/ruletest kept OldFaithfulTest true");
				stage = 104;
				wait = 20;
				return false;
			}
			default:
				return true;
		}
	}
}
