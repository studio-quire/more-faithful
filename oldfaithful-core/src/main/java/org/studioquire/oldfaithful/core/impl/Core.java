package org.studioquire.oldfaithful.core.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.ChoiceRule;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.RuleSet;
import org.studioquire.oldfaithful.core.impl.client.ClientAttachments;
import org.studioquire.oldfaithful.core.impl.content.ContentMixins;
import org.studioquire.oldfaithful.core.impl.content.ContentNet;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientContent;
import org.studioquire.oldfaithful.core.impl.content.server.ServerContent;

/** The core's own start: runs each library mod's entrypoints, once, in mod id order, on either side. */
public final class Core {
	public static final String ID = "faithful-core";
	/** A test-only salt: shuffles the order listeners run in, to show nothing leans on it. */
	private static final String ORDER_SALT = System.getProperty("faithful.test.orderSalt", "");

	private static final List<ModInfo> MODS = ModInfo.installed();
	private static boolean started;
	private static String currentMod = ID;

	private Core() {
	}

	public static List<ModInfo> mods() {
		return MODS;
	}

	public static String currentMod() {
		return currentMod;
	}

	/** What listeners are ordered by: the mod id, unless a test is salting it. */
	public static String orderKey(String mod) {
		if (ORDER_SALT.isEmpty()) return mod;
		try {
			StringBuilder key = new StringBuilder();
			for (byte b : MessageDigest.getInstance("SHA-256").digest((ORDER_SALT + mod).getBytes(StandardCharsets.UTF_8))) key.append(b);
			return key.toString();
		} catch (NoSuchAlgorithmException e) {
			return mod;
		}
	}

	private static final class ModImpl implements Mod, RuleSet {
		private final String id;

		ModImpl(String id) {
			this.id = id;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public RuleSet rules() {
			return this;
		}

		@Override
		public Channel channel(String name, int protocol) {
			return CoreNet.channel(id + ":" + name, protocol);
		}

		@Override
		public <H, T> Attachment<H, T> attachment(String name, Attachment.Spec<H, T> spec) {
			return Attachments.declare(id + ":" + name, spec);
		}

		@Override
		public BoolRule bool(String key, boolean defaultValue) {
			return Rules.declare(id + ":" + key, false, defaultValue);
		}

		@Override
		public ChoiceRule choice(String key, String... values) {
			return Rules.declare(id + ":" + key, false, values);
		}

		@Override
		public RuleSet atCreation() {
			return new RuleSet() {
				@Override
				public BoolRule bool(String key, boolean defaultValue) {
					return Rules.declare(id + ":" + key, true, defaultValue);
				}

				@Override
				public ChoiceRule choice(String key, String... values) {
					return Rules.declare(id + ":" + key, true, values);
				}

				@Override
				public RuleSet atCreation() {
					return this;
				}
			};
		}
	}

	/** Called when the game has its own blocks and items and hasn't loaded a world yet. */
	public static synchronized void start() {
		if (started) return;
		started = true;
		FabricLoader loader = FabricLoader.getInstance();
		boolean client = loader.getEnvironmentType() == EnvType.CLIENT;
		CoreNet.startSide(client);
		Rules.start();
		Attachments.start();
		if (client) ClientAttachments.register();
		SoundNet.start();
		if (ContentMixins.active()) {
			ContentNet.start(client ? new ClientContent() : new ServerContent());
			if (client) ClientContent.start();
		}
		List<EntrypointContainer<FaithfulMod>> mods = sorted(loader.getEntrypointContainers("faithful:mod", FaithfulMod.class));
		for (EntrypointContainer<FaithfulMod> c : mods) {
			String id = c.getProvider().getMetadata().getId();
			run(id, () -> c.getEntrypoint().content(new ContentRegistry.ModContent(id)));
		}
		for (EntrypointContainer<FaithfulMod> c : mods) {
			String id = c.getProvider().getMetadata().getId();
			run(id, () -> c.getEntrypoint().lateContent(new ContentRegistry.ModContent(id)));
		}
		ContentRegistry.build();
		for (EntrypointContainer<FaithfulMod> c : mods) {
			run(c.getProvider().getMetadata().getId(), () -> c.getEntrypoint().init(new ModImpl(c.getProvider().getMetadata().getId())));
		}
		RecipeTable.finish();
		if (client) {
			for (EntrypointContainer<FaithfulClientMod> c : sorted(loader.getEntrypointContainers("faithful:client", FaithfulClientMod.class))) {
				run(c.getProvider().getMetadata().getId(), () -> c.getEntrypoint().initClient(new ModImpl(c.getProvider().getMetadata().getId())));
			}
		}
		Rules.meet();
		currentMod = ID;
		CoreNet.seal();
	}

	private static void run(String mod, Runnable init) {
		currentMod = mod;
		try {
			init.run();
		} catch (RuntimeException | LinkageError e) {
			throw new IllegalStateException("The mod " + mod + " failed while starting: " + e, e);
		}
	}

	private static <T> List<EntrypointContainer<T>> sorted(List<EntrypointContainer<T>> containers) {
		List<EntrypointContainer<T>> list = new ArrayList<>(containers);
		list.sort(Comparator.comparing(c -> orderKey(c.getProvider().getMetadata().getId())));
		return list;
	}
}
