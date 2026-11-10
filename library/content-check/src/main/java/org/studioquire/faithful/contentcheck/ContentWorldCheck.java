package org.studioquire.faithful.contentcheck;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;

/** The same content as a real client sees it: in single player, and on a server through each of the block packets. */
public final class ContentWorldCheck implements WorldCheck {
	private SelfTest results;
	private int stage;
	private int wait;
	private int crowdWait;

	@Override
	public void start(Minecraft mc, SelfTest results) {
		this.results = results;
		this.stage = mc.world.isRemote ? 100 : 0;
	}

	@Override
	public boolean tick(Minecraft mc) {
		if (wait > 0) {
			wait--;
			return false;
		}
		if (mc.player == null) {
			results.check("still connected, at stage " + stage, false);
			return true;
		}
		World w = mc.world;
		int x = (int) Math.floor(mc.player.x), z = (int) Math.floor(mc.player.z), y = 100;
		switch (stage) {
			case 0: {
				int solid = ContentCheck.solid.get().id;
				w.setBlock(x + 2, y, z, solid, 9);
				results.check("single player: a mod block reads back", w.getBlockId(x + 2, y, z) == solid && w.getBlockMeta(x + 2, y, z) == 9);
				mc.player.inventory.addStack(new ItemStack(ContentCheck.thing.get(), 2));
				stage = 1;
				wait = 20;
				return false;
			}
			case 1: {
				// A wall of mod and vanilla blocks in front of the player, mod items in the hand and on the ground: looked at by eye.
				w.setBlock(x + 2, y, z, 0);
				int px = x, py = (int) Math.floor(mc.player.y), pz = z;
				int solid = ContentCheck.solid.get().id, see = ContentCheck.see.get().id;
				for (int dx = -3; dx <= 3; dx++) {
					for (int dy = 0; dy < 4; dy++) {
						int id = (dx + dy & 1) == 0 ? solid : dy == 2 ? see : Block.STONE.id;
						w.setBlock(px + dx, py + dy - 1, pz + 3, id);
					}
				}
				mc.player.yaw = 0;
				mc.player.pitch = 0;
				mc.player.inventory.selectedSlot = 0;
				mc.player.inventory.main[0] = new ItemStack(ContentCheck.solid.get(), 3);
				mc.player.inventory.main[1] = new ItemStack(ContentCheck.thing.get(), 3);
				mc.player.inventory.main[2] = new ItemStack(Block.STONE, 3);
				w.spawnEntity(new net.minecraft.entity.ItemEntity(w, px - 1, py + 0.5, pz + 1.5, new ItemStack(ContentCheck.thing.get(), 1)));
				w.spawnEntity(new net.minecraft.entity.ItemEntity(w, px + 1, py + 0.5, pz + 1.5, new ItemStack(ContentCheck.solid.get(), 1)));
				stage = 2;
				wait = 60;
				return false;
			}
			case 2:
				mc.setScreen(new net.minecraft.client.gui.screen.Screen() {
				});
				stage = 3;
				wait = 5;
				return false;
			case 3:
				org.studioquire.oldfaithful.selftest.ScreenCapture.request(new java.io.File("shots/content-world.png"), 0, 0);
				w.setTime(13200);
				stage = 4;
				wait = 10;
				return false;
			case 4:
				mc.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(mc.player));
				stage = 5;
				wait = 5;
				return false;
			case 5:
				org.studioquire.oldfaithful.selftest.ScreenCapture.request(new java.io.File("shots/content-dusk.png"), 0, 0);
				stage = 6;
				wait = 10;
				return false;
			case 6:
				mc.setScreen(null);
				return true;
			case 100: {
				mc.player.sendChatMessage("/contest one");
				mc.player.sendChatMessage("/contest few");
				mc.player.sendChatMessage("/contest many");
				mc.player.sendChatMessage("/contest stack");
				stage = 101;
				wait = 60;
				return false;
			}
			case 101: {
				net.minecraft.util.math.Vec3i s = w.getSpawnPos();
				int solid = ContentCheck.solid.get().id, see = ContentCheck.see.get().id;
				results.check("the place packet's block arrived whole: " + w.getBlockId(s.x, 100, s.z), w.getBlockId(s.x, 100, s.z) == solid && w.getBlockMeta(s.x + 3, 100, s.z) == 3);
				results.check("a single block update: " + w.getBlockId(s.x, 106, s.z) + "/" + w.getBlockMeta(s.x, 106, s.z), w.getBlockId(s.x, 106, s.z) == solid && w.getBlockMeta(s.x, 106, s.z) == 5);
				boolean few = true;
				for (int i = 0; i < 5; i++) few &= w.getBlockId(s.x + i, 107, s.z) == see && w.getBlockMeta(s.x + i, 107, s.z) == i + 1;
				results.check("a multi-block update", few);
				boolean many = true;
				for (int i = 0; i < 40; i++) many &= w.getBlockId(s.x + i % 8, 108, s.z + i / 8) == solid && w.getBlockMeta(s.x + i % 8, 108, s.z + i / 8) == 3;
				results.check("a partial chunk", many);
				ItemStack data = null;
				for (ItemStack st : mc.player.inventory.main) {
					if (st != null && st.itemId == ContentCheck.thing.get().id && st.getDamage() == 2) data = st;
				}
				results.check("a stack's data came over the wire", data != null && "from the server".equals(Stacks.data(data).getString("note")));
				int big = -1;
				for (int i = 0; i < 36; i++) {
					ItemStack st = mc.player.inventory.main[i];
					if (st != null && st.getDamage() == ContentScenes.BIG_DAMAGE) big = i;
				}
				ItemStack bigStack = big < 0 ? null : mc.player.inventory.main[big];
				results.check("a stack too big for the wire is still itself on a client: " + bigStack, bigStack != null && bigStack.itemId == ContentCheck.thing.get().id);
				results.check("and the client knows its data stayed behind", bigStack != null && Stacks.withheld(bigStack));
				if (bigStack == null) return true;
				mc.interactionManager.clickSlot(0, big < 9 ? 36 + big : big, 0, false, mc.player);
				stage = 102;
				wait = 10;
				return false;
			}
			case 102:
				mc.interactionManager.clickSlot(0, ContentScenes.BIG_SLOT, 0, false, mc.player);
				stage = 103;
				wait = 20;
				return false;
			case 103:
				mc.player.sendChatMessage("/contest big");
				mc.player.sendChatMessage("/contest crowd");
				stage = 104;
				return false;
			case 104: {
				// What that one packet carried.
				int whole = 0, withheld = 0;
				for (ItemStack st : mc.player.inventory.main) {
					if (st == null || st.getDamage() != ContentScenes.CROWD_DAMAGE) continue;
					if (Stacks.withheld(st)) withheld++;
					else if (Stacks.wireBytes(st) > 15000) whole++;
				}
				if (whole + withheld == 0 && crowdWait++ < 400) return false;
				results.check("one packet carries what it safely can, and the rest of its stacks go without their data: " + whole + " whole, " + withheld + " not", whole > 0 && withheld > 0 && whole + withheld == 36);
				wait = 40;
				stage = 105;
				return false;
			}
			case 105:
				mc.player.sendChatMessage("/contest bigchest");
				crowdWait = 0;
				stage = 106;
				return false;
			case 106:
				if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
					wait = 60; // what passes the send queue's limit is a kick on the server's next tick, and a moment to reach here
					stage = 107;
				} else if (crowdWait++ > 400) {
					results.check("a double chest of big stacks opens", false);
					return true;
				}
				return false;
			case 107: {
				int filled = 0;
				for (Object slot : mc.player.currentScreenHandler.slots) {
					ItemStack st = ((Slot) slot).getStack();
					if (st != null && st.getDamage() == ContentScenes.CROWD_DAMAGE) filled++;
				}
				results.check("a double chest of stacks near the cap keeps the connection, with every stack there: " + filled + " of " + mc.player.currentScreenHandler.slots.size() + " in " + mc.player.currentScreenHandler.getClass().getSimpleName() + ", first " + ((Slot) mc.player.currentScreenHandler.slots.get(0)).getStack(), filled == 90);
				mc.setScreen(null);
				return true;
			}
			default:
				return true;
		}
	}
}
