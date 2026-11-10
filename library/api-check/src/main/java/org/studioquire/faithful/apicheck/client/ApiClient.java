package org.studioquire.faithful.apicheck.client;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PigEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.studioquire.faithful.apicheck.ApiCheck;
import org.studioquire.faithful.apicheck.ApiCheck.Critter;
import org.studioquire.faithful.apicheck.ApiCheck.MachineEntity;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.BlockShape;
import org.studioquire.oldfaithful.core.api.client.Client;
import org.studioquire.oldfaithful.core.api.client.ContainerScreens;
import org.studioquire.oldfaithful.core.api.client.Hud;
import org.studioquire.oldfaithful.core.api.client.Renderers;

public final class ApiClient implements FaithfulClientMod {
	static int critterFrames;
	static int machineFrames;
	static World enteredFrom, enteredTo;
	static int worldChanges;
	static Hud.Corner chosenCorner = Hud.Corner.TOP_LEFT, drawnCorner;

	public static final class KilnScreen extends HandledScreen {
		public KilnScreen(ScreenHandler handler) {
			super(handler);
		}

		@Override
		protected void drawBackground(float delta) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/gui/furnace.png"));
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			drawTexture((width - backgroundWidth) / 2, (height - backgroundHeight) / 2, 0, 0, backgroundWidth, backgroundHeight);
		}
	}

	private static final class CritterRenderer extends LivingEntityRenderer {
		CritterRenderer() {
			super(new PigEntityModel(), 0.7F);
		}

		@Override
		public void render(LivingEntity entity, double x, double y, double z, float yaw, float pitch) {
			critterFrames++;
			super.render(entity, x, y, z, yaw, pitch);
		}
	}

	private static final class MachineRenderer extends BlockEntityRenderer {
		@Override
		public void render(BlockEntity blockEntity, double x, double y, double z, float tickDelta) {
			machineFrames++;
		}
	}

	@Override
	public void initClient(Mod mod) {
		Renderers.entity(Critter.class, new CritterRenderer());
		Renderers.blockEntity(MachineEntity.class, new MachineRenderer());
		Renderers.blockShape("api-check:post", BlockShape.boxes(new float[] {0.3F, 0.0F, 0.3F, 0.7F, 0.6F, 0.7F}, new float[] {0.1F, 0.6F, 0.1F, 0.9F, 1.0F, 0.9F}));
		ContainerScreens.register("api-check:kiln", (player, handler, inventory, data) -> new KilnScreen(handler));
		Client.onWorldChanged((from, to) -> {
			if (worldChanges++ == 0) {
				enteredFrom = from;
				enteredTo = to;
			}
		});
		Hud.layer(() -> chosenCorner, frame -> drawnCorner = frame.corner);
	}
}
