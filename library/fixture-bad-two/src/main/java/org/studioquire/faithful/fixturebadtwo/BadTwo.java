package org.studioquire.faithful.fixturebadtwo;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.faithful.fixturebadone.BadOne;

/** The other half of the mistakes that take two mods. */
public final class BadTwo implements FaithfulMod {
	private static final List<String> ON = Arrays.asList(System.getProperty("faithful.bad", "").split(","));

	private static boolean on(String mistake) {
		return ON.contains(mistake);
	}

	public static final class Thing extends Item {
		Thing(int id, int texture) {
			super(id);
			setTextureId(texture);
		}
	}

	@Override
	public void content(Content c) {
	}

	@Override
	public void init(Mod mod) {
		if (on("twice_command")) Commands.register("samecommand", "a planted mistake", (args, reply) -> {
		});
		// Leans on the other mod without declaring it: fine together, a crash alone.
		if (on("undeclared_dependency")) System.out.println(BadOne.on("x"));
		if (on("table_conflict")) Block.STONE.slipperiness = 0.8F;
		if (on("order_matters")) Drops.block(d -> d.stacks.add(new ItemStack(Item.STICK)));
	}
}
