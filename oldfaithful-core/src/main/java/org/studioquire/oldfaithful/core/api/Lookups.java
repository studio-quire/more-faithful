package org.studioquire.oldfaithful.core.api;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;

/**
 * Finding what's at a position by what it can do, so two mods meet without knowing each other. A {@link Type} names a capability: the
 * core's is {@link ItemStorage#TYPE}, and mods may make their own.
 * <p>
 * Who answers, first match only: a provider for the block, then one for its block entity's class (the nearest in its class's line), then the block entity itself if it implements the
 * type's interface, then the type's own fallback ({@code ItemStorage} finds every {@code Inventory} that way). <b>A provider's answer is final</b>: null means "nothing here", so it hides a
 * block entity that implements the type, or an {@code Inventory} from {@code ItemStorage}, and it's how a block entity answers by side: {@code (entity, side) -> side == 0 ? null : entity}.
 * <p>
 * {@code side} is always the target's own face, the one being touched, whoever asks: vanilla's 0 down, 1 up, 2 north, 3 south, 4 west, 5 east, or {@link #NO_SIDE} when none applies. A
 * neighbour on your side {@code s} is touched on its face {@code s ^ 1}, and {@link #beside} does that sum. The answer is for this moment: look again rather than keep it. It never loads a chunk.
 * Only where the world runs is it worth doing: a remote client's block entities hold what's been synced, no more.
 */
public final class Lookups {
	public static final int NO_SIDE = -1;
	private static final int[] DX = {0, 0, 0, 0, -1, 1}, DY = {-1, 1, 0, 0, 0, 0}, DZ = {0, 0, -1, 1, 0, 0};

	public interface Provider<T> {
		T find(World world, int x, int y, int z, int side);
	}

	public interface EntityProvider<T, E extends BlockEntity> {
		T find(E blockEntity, int side);
	}

	public static final class Type<T> {
		public final String name;
		public final Class<T> api;
		private final Provider<T> fallback;
		private final Map<Integer, Provider<T>> byBlock = new HashMap<>();
		private final Map<Class<?>, EntityProvider<T, BlockEntity>> byEntity = new HashMap<>();

		Type(String name, Class<T> api, Provider<T> fallback) {
			this.name = name;
			this.api = api;
			this.fallback = fallback;
		}

		private T find(World world, int x, int y, int z, int side) {
			Provider<T> block = byBlock.get(world.getBlockId(x, y, z));
			if (block != null) return block.find(world, x, y, z, side);
			BlockEntity be = world.getBlockEntity(x, y, z);
			if (be != null) {
				for (Class<?> c = be.getClass(); c != BlockEntity.class; c = c.getSuperclass()) {
					EntityProvider<T, BlockEntity> provider = byEntity.get(c);
					if (provider != null) return provider.find(be, side);
				}
				if (api.isInstance(be)) return api.cast(be);
			}
			return fallback != null ? fallback.find(world, x, y, z, side) : null;
		}
	}

	private Lookups() {
	}

	public static <T> Type<T> type(String name, Class<T> api) {
		return new Type<>(name, api, null);
	}

	/** With what to try last: how a core type adapts vanilla's blocks. */
	public static <T> Type<T> type(String name, Class<T> api, Provider<T> fallback) {
		return new Type<>(name, api, fallback);
	}

	/** Answers for one block, by its id. Final, null included. */
	public static <T> void register(Type<T> type, net.minecraft.block.Block block, Provider<T> provider) {
		type.byBlock.put(block.id, provider);
	}

	/** Answers for a block entity class and its subclasses. Final, null included. */
	@SuppressWarnings("unchecked")
	public static <T, E extends BlockEntity> void register(Type<T> type, Class<E> blockEntity, EntityProvider<T, E> provider) {
		type.byEntity.put(blockEntity, (EntityProvider<T, BlockEntity>) provider);
	}

	/** What's at a position, touched on its face {@code side}, or null. */
	public static <T> T find(Type<T> type, World world, int x, int y, int z, int side) {
		return world.isPosLoaded(x, y, z) ? type.find(world, x, y, z, side) : null;
	}

	/** What's next to {@code x, y, z} on that block's {@code side}, touched on the face toward it. */
	public static <T> T beside(Type<T> type, World world, int x, int y, int z, int side) {
		return find(type, world, x + DX[side], y + DY[side], z + DZ[side], side ^ 1);
	}
}
