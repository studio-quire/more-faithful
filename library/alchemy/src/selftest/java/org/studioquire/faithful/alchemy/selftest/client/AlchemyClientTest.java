package org.studioquire.faithful.alchemy.selftest.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.client.util.ScreenScaler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.util.math.MathHelper;
import org.studioquire.faithful.alchemy.Alchemy;
import org.studioquire.faithful.alchemy.Timed;
import org.studioquire.faithful.alchemy.Ward;
import org.studioquire.faithful.alchemy.client.Bottles;
import org.studioquire.faithful.alchemy.selftest.AlchemyTest;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.waterbottles.WaterBottles;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Names;
import org.studioquire.oldfaithful.core.api.client.Renderers;

/**
 * A client's real clicks at the bench and in the inventory, its real right clicks where a server has the last word, real falls, what it hears of the effects, and the HUD in pictures, read back
 * half pip by half pip and bottle by bottle.
 */
public final class AlchemyClientTest extends ClientTest {
	private static final int LIMIT = 40;
	/** The first hotbar slot in each screen: the bench's grid is nine cells and the inventory's four and four of armour. */
	private static final int BENCH_HOTBAR = 37, INVENTORY_HOTBAR = 36;
	/** A drop whose beta damage, {@code ceil(fall - 3)} of the distance it counts, is 5 or 6. */
	private static final double FALL = 8.5;

	private Item rose, weak, plain, strong, glass;
	private int x, feet, z;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private boolean holding(Item item, int count) {
		return held() != null && held().getItem() == item && held().count == count;
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	private void slot(int index, int button, boolean shift) {
		mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, index, button, shift, mc.player);
	}

	/** Takes a whole stack onto the cursor, one into each cell, and whatever's left goes back. */
	private void deal(int from, int... cells) {
		slot(from, 0, false);
		for (int cell : cells) slot(cell, 1, false);
		slot(from, 0, false);
	}

	private void atBench() {
		scene("kit", "gold", 8, "real rose", 2);
		scene("bench");
		await("the gold in the hand", LIMIT, () -> held() != null && held().getItem() == Item.GOLD_INGOT);
		then(() -> mc.interactionManager.interactBlock(mc.player, mc.world, held(), x - 2, feet, z, 1));
		await("the bench to open", LIMIT, () -> mc.player.currentScreenHandler instanceof CraftingScreenHandler);
		then(() -> {
			deal(BENCH_HOTBAR, 2, 4, 6, 8, 2, 4, 6, 8);
			deal(BENCH_HOTBAR + 1, 5, 5);
		});
		wait(5);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("the bench shows a golden rose for the plus", result != null && result.getItem() == rose);
			slot(0, 0, true);
		});
		wait(10);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("a shift-click makes one, as beta's does when the result stays the same size, and shows the next", count(rose) == 1 && result != null && result.getItem() == rose);
			slot(0, 0, true);
		});
		wait(10);
		then(() -> check("and another uses up the grid, the client agreeing", count(rose) == 2 && mc.player.currentScreenHandler.getSlot(0).getStack() == null));
		scene("expect", "rose", 2);
		scene("expect", "gold", 0);
		then(() -> mc.player.closeHandledScreen());
		// Back on the inventory's screen, a server compares what's there with what that screen last sent, from before the bench: the next tick puts that right, and a kit before it would leave strays on the client.
		wait(5);

		scene("kit", "water", 1, "water", 1, "water", 1, "rose", 6);
		scene("bench");
		await("the kit", LIMIT, () -> count(rose) == 6);
		then(() -> mc.interactionManager.interactBlock(mc.player, mc.world, held(), x - 2, feet, z, 1));
		await("the bench to open", LIMIT, () -> mc.player.currentScreenHandler instanceof CraftingScreenHandler);
		then(() -> {
			deal(BENCH_HOTBAR, 9);
			deal(BENCH_HOTBAR + 3, 1, 3, 7);
		});
		wait(5);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("the bench shows a strong potion for a bottle and three roses, spread out", result != null && result.getItem() == strong);
			slot(0, 0, true);
		});
		wait(10);
		then(() -> {
			check("a shift-click takes it, with nothing left in the grid", count(strong) == 1 && count(rose) == 3);
			mc.player.closeHandledScreen();
		});
		scene("expect", "strong", 1);
		scene("expect", "rose", 3);
	}

	/** Goes on from {@link #atBench()}'s kit: two water bottles and three roses. */
	private void inInventory() {
		then(() -> {
			check("the inventory's grid is the current screen", mc.player.currentScreenHandler == mc.player.playerScreenHandler);
			deal(INVENTORY_HOTBAR + 1, 1);
			deal(INVENTORY_HOTBAR + 3, 4);
		});
		wait(5);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("the inventory's grid shows a weak potion", result != null && result.getItem() == weak);
			slot(0, 0, true);
		});
		wait(10);
		then(() -> check("a shift-click takes it", count(weak) == 1 && count(rose) == 2));
		scene("expect", "weak", 1);
		then(() -> {
			deal(INVENTORY_HOTBAR + 2, 2);
			deal(INVENTORY_HOTBAR + 3, 3, 4);
		});
		wait(5);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("and a plain one", result != null && result.getItem() == plain);
			slot(0, 0, false);
			slot(INVENTORY_HOTBAR + 8, 0, false);
		});
		wait(10);
		then(() -> check("by an ordinary click too", mc.player.inventory.main[8] != null && mc.player.inventory.main[8].getItem() == plain && count(rose) == 0));
		scene("expect", "plain", 1);
		scene("expect", "rose", 0);
	}

	private void drink() {
		mc.interactionManager.interactItem(mc.player, mc.world, held());
	}

	private void drinks(String potion, Item item, int to) {
		scene("kit", potion, 1);
		scene("health", 4);
		await("the potion in the hand and the wound known", LIMIT, () -> holding(item, 1) && mc.player.health == 4);
		then(() -> {
			drink();
			if (remote()) check("no guess on a client: it waits for the server's word", holding(item, 1) && mc.player.health == 4);
		});
		until(potion + ": the hand holds the bottle and the health is there", LIMIT, () -> holding(glass, 1) && mc.player.health == to);
		wait(10);
		then(() -> {
			check("and stays that way", holding(glass, 1) && mc.player.health == to);
			check("nothing else drunk", count(item) == 0);
		});
		scene("expect health", to);
	}

	private void notDrunk() {
		scene("kit", "strong", 1);
		scene("health", 20);
		await("the potion in the hand at full health", LIMIT, () -> holding(strong, 1) && mc.player.health == 20);
		ItemStack[] before = {null};
		then(() -> {
			before[0] = held();
			check("at full health the click does nothing, so no bounce of the hand", !mc.interactionManager.interactItem(mc.player, mc.world, before[0]));
		});
		for (int tick = 0; tick < 12; tick++) {
			then(() -> check("the same stack in the hand, tick after tick", held() == before[0]));
		}
		scene("expect", "strong", 1);
		scene("expect health", 20);
	}

	private void disagrees() {
		scene("kit", "weak", 1);
		scene("health", 4);
		await("the potion in the hand and the wound known", LIMIT, () -> holding(weak, 1) && mc.player.health == 4);
		then(() -> {
			mc.player.health = 20;
			drink();
			check("a client that thinks it's whole doesn't stop it", holding(weak, 1));
		});
		until("the server drinks, and says so", LIMIT, () -> holding(glass, 1) && mc.player.health == 10);

		scene("kit", "weak", 1);
		scene("health", 20);
		await("the potion in the hand at full health", LIMIT, () -> holding(weak, 1) && mc.player.health == 20);
		ItemStack[] before = {null};
		then(() -> {
			before[0] = held();
			mc.player.health = 4;
			drink();
		});
		wait(10);
		then(() -> {
			check("a client that thinks it's wounded guesses no drink the server won't make", held() == before[0]);
			mc.player.health = 20;
		});
		scene("expect", "weak", 1);
	}

	private void fromStack() {
		scene("kit", "weak", 3);
		scene("health", 4);
		await("three in the hand and the wound known", LIMIT, () -> holding(weak, 3) && mc.player.health == 4);
		then(this::drink);
		until("three in a slot: two in the hand, and the client hears of the bottle in another slot", LIMIT, () -> holding(weak, 2) && count(glass) == 1);
		scene("expect", "glass", 1);
	}

	/** A ward and a haste potion by real right clicks: what the client hears is what its HUD and its own walking and mining go by. */
	private void effectsHeard() {
		scene("effects", "none", 0, 0, 0, 0);
		scene("kit", "ward", 1, "haste", 1);
		await("the potions in the hand and nothing running", LIMIT, () -> holding(Alchemy.ward.get(0).get(), 1) && !Alchemy.hasted.running(mc.player) && Ward.points(mc.player) == 0);
		then(this::drink);
		until("a weak ward: the client hears a whole set of the weak's toughness, a full bar", LIMIT,
				() -> Ward.wear.get(mc.player) == Alchemy.WARD[0] && Ward.whole.get(mc.player) == Alchemy.WARD[0] && Ward.points(mc.player) == Ward.BAR && holding(glass, 1));
		then(() -> {
			mc.player.inventory.selectedSlot = 1;
			drink();
		});
		until("a weak haste: the client hears two rows of its bottle", LIMIT, () -> Alchemy.hasted.shown.get(mc.player) == 2 && Alchemy.hasted.running(mc.player));
		scene("expect ward", Alchemy.WARD[0]);
		scene("expect haste", 4 * Timed.TICKS_A_MINUTE - 100);
		scene("effects", "none", 0, 0, 0, 0);
		then(() -> mc.player.inventory.selectedSlot = 0);
	}

	/** How far one tick of walking forward from standing pushes the client's own player, put back where it stood. */
	private float walked() {
		double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
		boolean ground = mc.player.onGround;
		mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
		mc.player.onGround = true;
		mc.player.travel(0.0F, 1.0F);
		float pushed = MathHelper.sqrt(mc.player.velocityX * mc.player.velocityX + mc.player.velocityZ * mc.player.velocityZ);
		mc.player.setPosition(px, py, pz);
		mc.player.onGround = ground;
		mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
		return pushed;
	}

	/** On a server, where the client walks and paces its own mining by what it heard. */
	private void hasteOnClient() {
		float[] before = {0, 0};
		scene("effects", "none", 0, 0, 0, 0);
		scene("kit", "gold", 1);
		await("nothing running", LIMIT, () -> !Alchemy.hasted.running(mc.player) && held() != null);
		then(() -> {
			before[0] = walked();
			before[1] = Block.STONE.getHardness(mc.player);
		});
		scene("effects", "none", 0, 0, 3, 0);
		await("haste heard", LIMIT, () -> Alchemy.hasted.running(mc.player));
		then(() -> {
			float walk = walked(), mine = Block.STONE.getHardness(mc.player);
			check("the client walks 20% faster (" + before[0] + " to " + walk + ") and mines 20% faster (" + before[1] + " to " + mine + ")",
					Math.abs(walk / before[0] - Alchemy.HASTE) < 1e-3 && Math.abs(mine / before[1] - Alchemy.HASTE) < 1e-4);
		});
		scene("effects", "none", 0, 0, 0, 0);
	}

	/**
	 * Lifted {@link #FALL} over the plot's floor and let go, as a server sees a client fall. Beta counts a fall's distance up to the move before the one that lands, so its damage here is 5 or 6 by
	 * where the last tick's step falls; a third of either is 2.
	 */
	private void falls(boolean feather, int least, int most) {
		scene("feather", feather ? "on" : "off");
		scene("health", 20);
		await("the feather " + (feather ? "heard" : "gone") + " and the bar full", LIMIT, () -> Alchemy.feathered.running(mc.player) == feather && mc.player.health == 20 && mc.player.onGround);
		then(() -> mc.player.setPosition(mc.player.x, mc.player.y + FALL, mc.player.z));
		await("off the ground", LIMIT, () -> !mc.player.onGround);
		await("and down again", LIMIT * 2, () -> mc.player.onGround);
		wait(10);
		scene("expect health", 20 - most, 20 - least);
		until("a fall of " + FALL + (feather ? " with the feather" : "") + " takes " + least + " to " + most + " as the client sees it", LIMIT,
				() -> mc.player.health >= 20 - most && mc.player.health <= 20 - least);
	}

	@SuppressWarnings("unchecked")
	private static Attachment<PlayerEntity, Integer> endsAt() {
		try {
			return (Attachment<PlayerEntity, Integer>) Class.forName("org.studioquire.faithful.heartyfood.HeartyFood").getField("endsAt").get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** What the ghost hearts draw from, as this client hears it, with Hearty Food beside: a potion moves them with the hearts. */
	private void ghostHearts() {
		Attachment<PlayerEntity, Integer> end = endsAt();
		scene("kit", "weak", 1);
		scene("eat bread", 2);
		await("ghost hearts for the bread, and the potion in the hand", LIMIT, () -> end.get(mc.player) == 10 && mc.player.health == 2 && holding(weak, 1));
		wait(1);
		then(this::drink);
		await("a potion over it: hearts at once, and the ghost hearts on past them by what's coming", LIMIT, () -> holding(glass, 1) && mc.player.health >= 8 && end.get(mc.player) == 16);
		picture("ghost-hearts-after-a-potion");
		scene("health", 20);
		await("the ghost hearts gone at a full bar", LIMIT, () -> mc.player.health == 20 && end.get(mc.player) == 0);
	}

	private static boolean sameShape(int[] a, int[] b) {
		for (int i = 0; i < a.length; i++) if (a[i] >>> 24 != b[i] >>> 24) return false;
		return a.length == b.length;
	}

	/** Each golden thing has its own thing's outline, from the game's atlas. */
	private void gilding() {
		Object[][] pairs = {{new ItemStack(Block.ROSE), rose}, {new ItemStack(Block.DANDELION), Alchemy.goldenDandelion.get()}, {new ItemStack(Item.FEATHER), Alchemy.goldenFeather.get()},
				{Names.block(Alchemy.LUPIN) == null ? null : new ItemStack(Names.block(Alchemy.LUPIN)), Alchemy.goldenLupin.get()}};
		for (Object[] pair : pairs) {
			if (pair[0] == null) continue;
			Item golden = (Item) pair[1];
			check(golden.getTranslationKey() + " has its thing's outline", sameShape(Renderers.tilePixels((ItemStack) pair[0]), Renderers.tilePixels(new ItemStack(golden))));
		}
	}

	private BufferedImage shot(String name) {
		try {
			return ImageIO.read(new File("shots/" + checks().mod() + "/" + name + ".png"));
		} catch (IOException e) {
			return null;
		}
	}

	/** A pixel of the HUD, by GUI units: the middle of that unit's square in the picture. */
	private static int pixel(BufferedImage image, ScreenScaler screen, int gx, int gy) {
		int k = screen.scaleFactor;
		return image.getRGB(gx * k + k / 2, gy * k + k / 2);
	}

	/** A half pip: A worn armour's grey, B blue, . empty, ? anything else. */
	private static char halfPip(int rgb) {
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
		if (r > 0x90 && g > 0x90 && b > 0x90 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 0x20) return 'A';
		if (b > 0x90 && b - r > 0x40) return 'B';
		if (Math.max(r, Math.max(g, b)) < 0x70) return '.';
		return '?';
	}

	/** The armour bar's twenty half pips from the first, each pip's inner half then its outer: the fill's row in beta's sprites, at the middle of each half. */
	private String bar(BufferedImage image) {
		ScreenScaler screen = new ScreenScaler(mc.options, mc.displayWidth, mc.displayHeight);
		int y = screen.getScaledHeight() - 32 + 4;
		StringBuilder halves = new StringBuilder();
		for (int pip = 0; pip < 10; pip++) {
			int left = screen.getScaledWidth() / 2 + 91 - 9 - pip * 8;
			halves.append(halfPip(pixel(image, screen, left + 6, y))).append(halfPip(pixel(image, screen, left + 2, y)));
		}
		return halves.toString();
	}

	private static String expectedBar(int armour, int blue) {
		int total = Math.min(Ward.BAR, armour + blue), worn = total - blue;
		StringBuilder halves = new StringBuilder();
		for (int point = 1; point <= Ward.BAR; point++) halves.append(point <= worn ? 'A' : point <= total ? 'B' : '.');
		return halves.toString();
	}

	/** {@code armour} none, helmet, chest or set, with the worn armour's points; the blue's wear left and whole; haste's and the feather's steps, which draw nothing on the play screen. */
	private void hud(String name, String armour, int armourPoints, int wear, int whole, int haste, int feather) {
		scene("effects", armour, wear, whole, haste, feather);
		await("the client hears it all", LIMIT, () -> Ward.wear.get(mc.player) == wear && Ward.whole.get(mc.player) == (wear > 0 ? whole : 0) && Alchemy.hasted.shown.get(mc.player) == haste
				&& Alchemy.feathered.shown.get(mc.player) == feather && mc.player.getTotalArmorDurability() == armourPoints);
		wait(2);
		picture(name);
		then(() -> {
			BufferedImage image = shot(name);
			if (!check(name + ": the picture reads", image != null)) return;
			int blue = Ward.points(wear, whole);
			if (armourPoints + blue > 0) {
				String got = bar(image);
				check(name + ", the armour bar: " + got, got.equals(expectedBar(armourPoints, blue)));
			}
		});
	}

	private void hudPictures() {
		then(() -> {
			mc.setScreen(null);
			mc.player.inventory.selectedSlot = 0;
			mc.player.pitch = 45.0F;
		});
		scene("kit", "gold", 1);
		scene("health", 20);
		int weak = Alchemy.WARD[0];
		hud("ward-fresh", "none", 0, weak, weak, 0, 0);
		hud("ward-half-worn", "none", 0, weak / 2, weak, 0, 0);
		hud("ward-with-a-helmet", "helmet", 3, weak / 3, weak, 0, 0);
		hud("ward-fresh-over-a-chestplate", "chest", 8, Alchemy.WARD[1], Alchemy.WARD[1], 0, 0);
		hud("ward-worn-over-a-chestplate", "chest", 8, weak / 2, weak, 0, 0);
		hud("ward-over-whole-iron", "set", 20, weak / 4, weak, 0, 0);
		hud("whole-iron-alone", "set", 20, 0, 0, 0, 0);
		hud("timers-running", "chest", 8, weak / 2, weak, 2, 6);
		then(() -> mc.options.guiScale = 3);
		wait(2);
		hud("ward-worn-over-a-chestplate-larger", "chest", 8, weak / 2, weak, 0, 0);
		then(() -> mc.options.guiScale = 0);
		scene("effects", "none", 0, 0, 0, 0);
		then(() -> mc.player.pitch = 0.0F);
	}

	/** Y haste's lemon, L the feather's lavender, . the panel. */
	private static char liquid(int rgb) {
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
		if (r > 0xb0 && g > 0xb0 && b < 0x60) return 'Y';
		if (b > 0xc0 && r > 0x80 && b > g + 0x20) return 'L';
		if (r > 0xb0 && Math.abs(r - g) < 8 && Math.abs(g - b) < 8) return '.';
		return '?';
	}

	/** The first three places in the inventory's row of bottles, read low in each bottle, where liquid stays to the last. */
	private String bottles(BufferedImage image, int count) {
		ScreenScaler screen = new ScreenScaler(mc.options, mc.displayWidth, mc.displayHeight);
		int left = (screen.getScaledWidth() - 176) / 2, top = (screen.getScaledHeight() - 166) / 2;
		int spacing = Bottles.spacing(Math.max(1, count));
		StringBuilder read = new StringBuilder();
		for (int i = 0; i < 3; i++) read.append(liquid(pixel(image, screen, left + Bottles.FIRST_X + i * spacing + 7, top + Bottles.Y + 13)));
		return read.toString();
	}

	/** The inventory with haste and the feather at so many rows of liquid, in that order of drinking. */
	private void inventory(String name, int haste, int feather, int hasteOrder, int featherOrder, String expected) {
		scene("effects", "none", 0, 0, haste, feather, hasteOrder, featherOrder);
		await("the client hears it", LIMIT, () -> Alchemy.hasted.shown.get(mc.player) == haste && Alchemy.feathered.shown.get(mc.player) == feather
				&& Alchemy.hasted.order.get(mc.player) == (haste > 0 ? hasteOrder : 0) && Alchemy.feathered.order.get(mc.player) == (feather > 0 ? featherOrder : 0));
		then(() -> mc.setScreen(new InventoryScreen(mc.player)));
		wait(3);
		picture(name);
		then(() -> {
			BufferedImage image = shot(name);
			if (!check(name + ": the picture reads", image != null)) return;
			String got = bottles(image, (haste > 0 ? 1 : 0) + (feather > 0 ? 1 : 0));
			check(name + ", the bottles: " + got, got.equals(expected));
			mc.setScreen(null);
		});
	}

	private void inventoryPictures() {
		then(() -> {
			mc.setScreen(null);
			mc.player.pitch = 0.0F;
		});
		scene("kit", "gold", 1);
		inventory("inventory-none-running", 0, 0, 0, 0, "...");
		inventory("inventory-haste", 4, 0, 1, 0, "Y..");
		inventory("inventory-feather-then-haste", 3, 6, 2, 1, "LY.");
		inventory("inventory-haste-low", 1, 0, 1, 0, "Y..");
		scene("effects", "none", 0, 0, 0, 0);
	}

	private void pictures() {
		scene("row");
		then(() -> {
			mc.player.inventory.selectedSlot = 8;
			mc.player.pitch = 0.0F;
		});
		wait(5);
		picture("hotbar");
		then(() -> mc.setScreen(new InventoryScreen(mc.player)));
		wait(5);
		picture("inventory");
		then(() -> mc.setScreen(null));
		then(() -> mc.player.inventory.selectedSlot = 0);
	}

	@Override
	protected void script() {
		rose = Alchemy.goldenRose.get();
		weak = Alchemy.weakPotion.get();
		plain = Alchemy.healthPotion.get();
		strong = Alchemy.strongPotion.get();
		glass = WaterBottles.glassBottle.get();
		if (restarted()) {
			then(() -> check("the client has them back", count(weak) == 1 && count(plain) == 1 && count(strong) == 1 && count(rose) == 5));
			if (AlchemyTest.aloneInRun()) until("and hears the blue and the haste again", LIMIT, () -> Ward.wear.get(mc.player) == 30 && Ward.whole.get(mc.player) == 40 && Alchemy.hasted.running(mc.player));
			scene("effects", "none", 0, 0, 0, 0);
			// Peaceful heals a point every 20 ticks (beta's `age % 20 * 12`): in the very tick of the wound, the client would hear of 5 and never of 4.
			scene("no peaceful");
			scene("health", 4);
			await("the wound known", LIMIT, () -> mc.player.health == 4);
			then(() -> {
				mc.player.inventory.selectedSlot = 2;
				drink();
			});
			until("and a potion still drinks", LIMIT, () -> holding(glass, 1) && mc.player.health == 20);
			scene("peaceful again");
			then(() -> mc.player.inventory.selectedSlot = 0);
			return;
		}
		then(() -> {
			x = MathHelper.floor(mc.player.x);
			feet = MathHelper.floor(mc.player.boundingBox.minY);
			z = MathHelper.floor(mc.player.z);
			mc.setScreen(null);
			TranslationStorage names = TranslationStorage.getInstance();
			String[] expected = {"Golden Rose", "Golden Dandelion", "Golden Lupin", "Golden Feather"};
			Item[] golden = {rose, Alchemy.goldenDandelion.get(), Alchemy.goldenLupin.get(), Alchemy.goldenFeather.get()};
			boolean right = names.get(weak.getTranslationKey() + ".name").equals("Weak Health Potion") && names.get(strong.getTranslationKey() + ".name").equals("Strong Health Potion")
					&& names.get(Alchemy.ward.get(1).get().getTranslationKey() + ".name").equals("Ward Potion") && names.get(Alchemy.feather.get(0).get().getTranslationKey() + ".name").equals("Weak Feather Potion")
					&& names.get(Alchemy.haste.get(2).get().getTranslationKey() + ".name").equals("Strong Haste Potion");
			for (int i = 0; i < golden.length; i++) right &= names.get(golden[i].getTranslationKey() + ".name").equals(expected[i]);
			check("the names", right);
			if (!remote()) gilding();
		});
		scene("no peaceful");
		atBench();
		inInventory();
		drinks("weak", weak, 10);
		drinks("plain", plain, 16);
		drinks("strong", strong, 20);
		notDrunk();
		fromStack();
		if (remote()) disagrees();
		effectsHeard();
		if (remote()) hasteOnClient();
		falls(false, 5, 6);
		falls(true, 2, 2);
		scene("feather", "off");
		if (FabricLoader.getInstance().isModLoaded("hearty-food")) ghostHearts();
		scene("peaceful again");
		hudPictures();
		inventoryPictures();
		pictures();
		scene("leave");
	}
}
