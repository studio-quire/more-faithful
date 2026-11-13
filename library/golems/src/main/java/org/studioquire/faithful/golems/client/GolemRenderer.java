package org.studioquire.faithful.golems.client;

import net.minecraft.client.render.entity.UndeadEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.studioquire.faithful.golems.GolemEntity;

/**
 * Beta's biped with something in its hand, the way a skeleton holds its bow, drawn at a player's size. A hurt golem is drawn cracked: the same model again over its skin, with the
 * cracks for how hurt it is ({@link GolemEntity#cracks}), each stage's picture whole, opaque where it's cracked and clear elsewhere, so beta's alpha test is all it needs.
 */
final class GolemRenderer extends UndeadEntityRenderer {
	/** A player's own: 0.9375 of the model, so 0.94 across and 1.88 tall, the arms outside a box 0.6 wide as a player's and a zombie's are. */
	private static final float SCALE = 0.9375F;
	private static final String[] CRACKS = {"/assets/golems/textures/mob/golem_cracks_1.png", "/assets/golems/textures/mob/golem_cracks_2.png",
			"/assets/golems/textures/mob/golem_cracks_3.png"};

	private final GolemModel cracked = new GolemModel();

	GolemRenderer() {
		super(new GolemModel(), 0.5F);
		setDecorationModel(cracked);
	}

	@Override
	protected void applyScale(LivingEntity entity, float tickDelta) {
		GL11.glScalef(SCALE, SCALE, SCALE);
	}

	@Override
	public void render(LivingEntity entity, double x, double y, double z, float yaw, float tickDelta) {
		entityModel.rightArmPose = cracked.rightArmPose = entity.getHeldItem() != null;
		((GolemModel) entityModel).drawing = cracked.drawing = ((GolemEntity) entity).drawsBow();
		super.render(entity, x, y, z, yaw, tickDelta);
	}

	/**
	 * A bow held as a bow, as Still Faithful holds a player's and a skeleton's ({@code BowPose}): on beta's held-tool path, upright in the fist, shifted so the fist
	 * closes on its middle and turned a little in, where beta lays any item that isn't a tool out flat, across the face of a golem drawing it. Always: a golem has no beta look to keep.
	 */
	@Override
	protected void renderMore(LivingEntity entity, float tickDelta) {
		ItemStack held = entity.getHeldItem();
		if (held == null || !(held.getItem() instanceof BowItem)) {
			super.renderMore(entity, tickDelta);
			return;
		}
		GL11.glPushMatrix();
		entityModel.rightArm.transform(0.0625F);
		GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
		GL11.glTranslatef(0.0F, -0.0625F, 0.3125F);
		GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
		float scale = 0.625F;
		GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
		GL11.glScalef(scale, -scale, scale);
		GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
		dispatcher.heldItemRenderer.renderItem(entity, held);
		GL11.glPopMatrix();
	}

	@Override
	protected boolean bindTexture(LivingEntity entity, int layer, float tickDelta) {
		int stage = layer == 0 ? ((GolemEntity) entity).cracks() : 0;
		if (stage == 0) return false;
		bindTexture(CRACKS[stage - 1]);
		return true;
	}

	/** The red of a blow is laid over its body once; the cracks' model over it again would make it darker on a cracked golem. */
	@Override
	protected boolean bindDecorationTexture(LivingEntity entity, int layer, float tickDelta) {
		return false;
	}
}
