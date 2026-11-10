package org.studioquire.faithful.explosivearrows.selftest.client;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.particle.ExplosionParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;
import org.studioquire.faithful.explosivearrows.ExplosiveArrows;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.oldfaithful.core.api.client.Renderers;

/** A real shot from a real right click, the client's copy of the arrow on a server, and the pictures. */
public final class ArrowsClientTest extends ClientTest {
	private static final int LIMIT = 60;

	private Item explosive;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	private ExplosiveArrow flying() {
		for (Object e : mc.world.entities) if (e instanceof ExplosiveArrow && !((ExplosiveArrow) e).dead) return (ExplosiveArrow) e;
		return null;
	}

	private double away(ExplosiveArrow arrow) {
		return Math.sqrt(arrow.getSquaredDistance(mc.player.x, mc.player.y, mc.player.z));
	}

	/** West at a stone wall seven blocks off, with explosive arrows first in the hotbar and plain ones after. */
	private void shot() {
		// The bow's slot, picked here: the scene's choice of slot is the world's side's, which a client on a server never hears.
		then(() -> mc.player.inventory.selectedSlot = 0);
		scene("shoot");
		await("the bow in the hand, both arrows beside it", LIMIT, () -> held() != null && held().getItem() == Item.BOW && count(explosive) == 3 && count(Item.ARROW) == 5);
		then(() -> {
			mc.player.yaw = 90.0F;
			mc.player.pitch = 0.0F;
		});
		// For a server to hear where the player looks.
		wait(5);
		then(() -> mc.interactionManager.interactItem(mc.player, mc.world, held()));
		ExplosiveArrow[] seen = {null};
		double[] first = {0.0};
		await("the arrow, in this client's world", LIMIT, () -> {
			seen[0] = flying();
			if (seen[0] != null) first[0] = away(seen[0]);
			return seen[0] != null;
		});
		then(() -> check("it knows its shooter" + (remote() ? ", from the server's word" : ""), seen[0].owner == mc.player));
		wait(2);
		then(() -> check("and flies off, rather than striking them as it sets off: " + String.format("%.1f then %.1f blocks", first[0], away(seen[0])), away(seen[0]) > first[0] + 1.5));
		until("it's spent on the wall", LIMIT, () -> seen[0].dead && flying() == null);
		scene("shot");
		then(() -> check("this client's hotbar agrees: one explosive arrow gone", count(explosive) == 2 && count(Item.ARROW) == 5));
	}

	/** Blast puffs more than 16 blocks from the camera, which beta's own drawing never makes: its particle list read by its type, whatever it's named. */
	private int farPuffs() {
		List<?>[] layers = null;
		try {
			for (Field f : ParticleManager.class.getDeclaredFields()) {
				if (f.getType() != List[].class) continue;
				f.setAccessible(true);
				layers = (List<?>[]) f.get(mc.particleManager);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
		int n = 0;
		for (List<?> layer : layers) {
			for (Object p : layer) {
				if (p instanceof ExplosionParticle && Math.sqrt(((ExplosionParticle) p).getSquaredDistance(mc.camera.x, mc.camera.y, mc.camera.z)) > 16.0) n++;
			}
		}
		return n;
	}

	/** A burst some 34 blocks off, up in the open: beta would draw none of its puffs. */
	private void farBurst() {
		then(() -> check("no far puffs before it", farPuffs() == 0));
		scene("far burst");
		until("its puffs drawn, 34 blocks off" + (remote() ? ", from the server's word" : ""), LIMIT, () -> farPuffs() >= 8);
		then(() -> note("far puffs: " + farPuffs()));
		scene("far burst over");
	}

	private void looks() {
		TranslationStorage names = TranslationStorage.getInstance();
		check("the name", names.get(explosive.getTranslationKey() + ".name").equals("Explosive Arrow"));
		int[] arrow = Renderers.tilePixels(new ItemStack(Item.ARROW)), ours = Renderers.tilePixels(new ItemStack(explosive));
		check("its own picture, not the arrow's", ours.length == 256 && !Arrays.equals(arrow, ours) && Arrays.stream(ours).anyMatch(p -> p >>> 24 != 0));
	}

	/** One in the air in front of the camera, crossing slowly, for a look at how it's drawn; then gone before it lands. */
	private void inFlight() {
		ExplosiveArrow[] arrow = {null};
		then(() -> {
			mc.player.yaw = 0.0F;
			mc.player.pitch = 0.0F;
			arrow[0] = new ExplosiveArrow(mc.world, mc.player.x - 0.3, mc.player.y - 0.2, mc.player.z + 2.5);
			arrow[0].setVelocity(1.0, 0.3, 0.0, 0.08F, 0.0F);
			mc.world.spawnEntity(arrow[0]);
		});
		wait(1);
		picture("in-flight");
		then(() -> arrow[0].markDead());
	}

	private void pictures() {
		scene("row");
		then(() -> mc.player.pitch = 0.0F);
		wait(5);
		picture("hotbar");
		then(() -> mc.setScreen(new InventoryScreen(mc.player)));
		wait(5);
		picture("inventory");
		then(() -> mc.setScreen(null));
		wait(20);
		picture("hand");
		if (!remote()) inFlight();
	}

	@Override
	protected void script() {
		explosive = ExplosiveArrows.explosiveArrow.get();
		if (restarted()) {
			then(() -> check("the client has them back", count(explosive) == 7));
			return;
		}
		then(() -> {
			mc.setScreen(null);
			if (!remote()) looks();
		});
		farBurst();
		shot();
		pictures();
		scene("leave");
	}
}
