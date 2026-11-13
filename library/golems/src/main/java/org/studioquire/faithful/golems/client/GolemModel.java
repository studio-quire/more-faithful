package org.studioquire.faithful.golems.client;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Beta's own biped, box for box the one its player and zombie are: what makes it a golem is its skin and how it carries itself. Iron hangs still, so its
 * arms don't sway out and in with breath as a biped's do, and they swing half as far in a walk. A bow at work is held drawn ({@link #drawing}).
 */
final class GolemModel extends BipedEntityModel {
	/**
	 * A bow held drawn, as later versions draw one, since beta has no draw: both arms up along its look, wherever it looks, the left reaching across to the string. Set by the renderer for
	 * each golem it draws.
	 */
	boolean drawing;

	GolemModel() {
		hat.hidden = true;
	}

	@Override
	public void setAngles(float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale) {
		super.setAngles(limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale);
		if (drawing) {
			rightArm.pitch = leftArm.pitch = -(float) Math.PI / 2.0F + head.pitch;
			rightArm.yaw = -0.1F + head.yaw;
			leftArm.yaw = 0.5F + head.yaw;
			rightArm.roll = leftArm.roll = 0.0F;
			return;
		}
		float sway = MathHelper.cos(animationProgress * 0.09F) * 0.05F + 0.05F;
		rightArm.roll -= sway;
		leftArm.roll += sway;
		// A biped's arm swings by this much, and by half of it with something held.
		float half = MathHelper.cos(limbAngle * 0.6662F) * limbDistance * 0.5F;
		rightArm.pitch += half * (rightArmPose ? 0.5F : 1.0F);
		leftArm.pitch -= half * (leftArmPose ? 0.5F : 1.0F);
	}
}
