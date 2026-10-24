package com.foque.laborcraft.job;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/**
 * Per-job equipment rules. Today only the farmer has a tool requirement (hoe); future jobs add
 * their own entries here (pickaxe for the miner, sword/shield for security, armor slots etc.).
 */
public final class JobEquipment {
	private static final String FARMER = "laborcraft:farmer";

	private JobEquipment() {
	}

	/** Empty stacks (removing the tool) are always accepted; jobs without a rule accept anything. */
	public static boolean validTool(String jobId, ItemStack stack) {
		if (stack.isEmpty() || jobId == null || jobId.isBlank()) {
			return true;
		}

		if (FARMER.equals(jobId)) {
			return stack.is(ItemTags.HOES);
		}

		return true;
	}

	public static Component toolName(String jobId) {
		if (FARMER.equals(jobId)) {
			return Component.translatable("laborcraft.tool.hoe");
		}

		return Component.translatable("laborcraft.tool.any");
	}
}
