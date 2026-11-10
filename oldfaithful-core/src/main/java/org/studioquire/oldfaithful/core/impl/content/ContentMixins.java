package org.studioquire.oldfaithful.core.impl.content;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Weaves the content layer in only if an installed mod declares content, so a game without any runs vanilla's own code. True
 * by absence of code rather than by a check inside it.
 */
public final class ContentMixins implements IMixinConfigPlugin {
	private static final boolean ACTIVE = active();

	public static boolean active() {
		for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
			CustomValue v = c.getMetadata().getCustomValue("faithful");
			if (v == null || v.getType() != CustomValue.CvType.OBJECT) continue;
			CustomValue.CvObject o = v.getAsObject();
			if (o.containsKey("content") && o.get("content").getAsBoolean()) return true;
		}
		return false;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!ACTIVE) return false;
		// Mixins on our editions' mods, for when those are installed.
		if (mixinClassName.contains("LightingCompat")) return FabricLoader.getInstance().isModLoaded("oldfaithful-lighting");
		if (mixinClassName.endsWith("PacketLimitsMixin")) return FabricLoader.getInstance().isModLoaded("purefaithful-hardening");
		return true;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
