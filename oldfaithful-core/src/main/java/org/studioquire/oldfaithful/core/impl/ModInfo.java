package org.studioquire.oldfaithful.core.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;

/** A mod that says something in its fabric.mod.json's {@code "faithful"} object, for the hello. */
public final class ModInfo {
	public final String id;
	public final String version;
	public final boolean content;
	public final boolean match;
	public final int network;

	public ModInfo(String id, String version, boolean content, boolean match, int network) {
		this.id = id;
		this.version = version;
		this.content = content;
		this.match = match;
		this.network = network;
	}

	/** Whether a peer must have this mod, at this network version. */
	public boolean mustMatch() {
		return content || match;
	}

	static List<ModInfo> installed() {
		List<ModInfo> mods = new ArrayList<>();
		for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
			CustomValue v = c.getMetadata().getCustomValue("faithful");
			if (v == null || v.getType() != CustomValue.CvType.OBJECT) continue;
			CustomValue.CvObject o = v.getAsObject();
			mods.add(new ModInfo(c.getMetadata().getId(), c.getMetadata().getVersion().getFriendlyString(), flag(o, "content"), flag(o, "match"),
					o.containsKey("network") ? o.get("network").getAsNumber().intValue() : 1));
		}
		mods.sort(Comparator.comparing(m -> m.id));
		return mods;
	}

	private static boolean flag(CustomValue.CvObject o, String key) {
		return o.containsKey(key) && o.get(key).getAsBoolean();
	}
}
