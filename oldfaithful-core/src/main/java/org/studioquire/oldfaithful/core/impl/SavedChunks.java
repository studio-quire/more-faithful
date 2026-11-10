package org.studioquire.oldfaithful.core.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ChunkStorage;
import net.minecraft.world.chunk.storage.RegionIo;
import net.minecraft.world.storage.WorldStorage;
import org.studioquire.oldfaithful.core.mixin.RegionChunkStorageAccessor;
import org.studioquire.oldfaithful.core.mixin.WorldAccessor;

/** A chunk as it lies on disk, looked at without bringing it into the world. */
public final class SavedChunks {
	private SavedChunks() {
	}

	/** Where a world's dimension keeps its regions, or null if it keeps none: a world with no saves, or a storage of some other kind. */
	private static File dir(World world) {
		WorldStorage storage = ((WorldAccessor) world).faithful$storage();
		if (storage == null) return null;
		ChunkStorage chunks = storage.getChunkStorage(world.dimension);
		return chunks instanceof RegionChunkStorageAccessor ? ((RegionChunkStorageAccessor) chunks).faithful$dir() : null;
	}

	private static DataInputStream open(World world, int chunkX, int chunkZ) {
		File dir = dir(world);
		if (dir == null) return null;
		// Asking RegionIo about a region that isn't there would make the file.
		if (!new File(dir, "region/r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mcr").isFile()) return null;
		return RegionIo.getChunkInputStream(dir, chunkX, chunkZ);
	}

	public static boolean exists(World world, int chunkX, int chunkZ) {
		try (DataInputStream in = open(world, chunkX, chunkZ)) {
			return in != null;
		} catch (IOException e) {
			return false;
		}
	}

	/** The compound an attachment of this name has on disk, or null. A chunk that won't read has none: it's coming back as a new one if it ever loads. */
	public static NbtCompound attachment(World world, int chunkX, int chunkZ, String id) {
		try (DataInputStream in = open(world, chunkX, chunkZ)) {
			if (in == null) return null;
			NbtCompound root = NbtIo.read(in);
			NbtCompound level = root.getCompound("Level");
			NbtElement faithful = ChunkData.find(level, ChunkData.KEY);
			if (!(faithful instanceof NbtCompound)) return null;
			NbtElement attachments = ChunkData.find((NbtCompound) faithful, "Attachments");
			if (!(attachments instanceof NbtCompound)) return null;
			NbtElement value = ChunkData.find((NbtCompound) attachments, id);
			return value instanceof NbtCompound ? (NbtCompound) value : null;
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}
}
