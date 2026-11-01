package com.foque.laborcraft.worker;

import com.foque.laborcraft.area.EnclosureDescriptor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Tool the herder needs for the animals currently in the pen. Today shears for sheep; later a
 * sword for pigs and a bucket for cows. Keeping this in one place lets the brain swap tools without
 * the player.
 */
public final class HerderTools {
	private HerderTools() {
	}

	public static ItemStack requiredFor(ServerLevel level, EnclosureDescriptor pen) {
		if (HerderAnimals.count(level, pen, HerderAnimals.Species.SHEEP) > 0) {
			return new ItemStack(Items.SHEARS);
		}
		return ItemStack.EMPTY;
	}

	/** Same item, ignoring damage, so a damaged tool still counts. */
	public static boolean matches(ItemStack stack, ItemStack required) {
		return !required.isEmpty() && !stack.isEmpty() && stack.getItem() == required.getItem();
	}
}
