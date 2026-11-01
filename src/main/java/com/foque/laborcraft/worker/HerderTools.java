package com.foque.laborcraft.worker;

import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.animal.Animal;
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

	public static ItemStack requiredFor(Map<HerderAnimals.Species, List<Animal>> herd) {
		if (HerderAnimals.has(herd, HerderAnimals.Species.SHEEP)) {
			return new ItemStack(Items.SHEARS);
		}
		return ItemStack.EMPTY;
	}

	/** Same item, ignoring damage, so a damaged tool still counts. */
	public static boolean matches(ItemStack stack, ItemStack required) {
		return !required.isEmpty() && !stack.isEmpty() && stack.getItem() == required.getItem();
	}
}
