package org.studioquire.oldfaithful.core.impl.client;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.studioquire.oldfaithful.core.impl.Attachments;

/** Values the server sends: what a client knows of attachments on a server. A holder that isn't here any more is ignored. */
public final class ClientAttachments {
	private ClientAttachments() {
	}

	public static void register() {
		Attachments.channel().onClient(ClientAttachments::heard);
		Attachments.chunkChannel().onClient(ClientAttachments::heard);
	}

	private static void heard(PlayerEntity player, DataInputStream in) throws IOException {
		World world = ClientNet.minecraft.world;
		if (world == null) return;
		Attachments.receive(in, (target, first, second) -> {
			switch (target) {
				case Attachments.TO_SELF:
					return player;
				case Attachments.TO_ENTITY:
					return world instanceof ClientWorld ? ((ClientWorld) world).getEntity(first) : null;
				case Attachments.TO_CHUNK:
					Chunk chunk = world.getChunk(first, second);
					return chunk instanceof EmptyChunk ? null : chunk;
				default:
					throw new IllegalStateException("An attachment for what? " + target);
			}
		});
	}
}
