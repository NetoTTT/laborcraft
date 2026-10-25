package com.foque.laborcraft.housing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Shared player-facing actions for house claims (used by the bed interaction and commands). */
public final class HomeActions {
	private HomeActions() {
	}

	/**
	 * Claims the room anchored to {@code bedPos}. Returns true only when a new house was created.
	 */
	public static boolean claim(ServerPlayer player, ServerLevel level, BlockPos bedPos, String name) {
		HomeRegistry.ClaimResult result = HomeRegistry.claim(
				level.getServer(),
				level,
				bedPos,
				player.getUUID(),
				name
		);

		if (result.home() == null) {
			Component reason = Component.translatable(
					"laborcraft.housing.reason." + (result.failure() == null ? "invalid" : result.failure())
			);
			player.sendSystemMessage(Component.translatable(
					"laborcraft.command.home.invalid",
					bedPos.getX(),
					bedPos.getY(),
					bedPos.getZ(),
					reason
			));
			return false;
		}

		HomeRegistry.Home home = result.home();
		if (result.failure() == null) {
			player.sendSystemMessage(Component.translatable(
					"laborcraft.command.home.set.success",
					home.name(),
					bedPos.getX(),
					bedPos.getY(),
					bedPos.getZ(),
					home.slots(),
					home.capacity()
			));
			return true;
		}

		if (result.alreadyOwner()) {
			player.sendSystemMessage(Component.translatable(
					"laborcraft.command.home.set.already_owner",
					home.name(),
					bedPos.getX(),
					bedPos.getY(),
					bedPos.getZ(),
					home.slots()
			));
			return false;
		}

		Component owner = home.owner() == null
				? Component.translatable("laborcraft.command.home.unknown_owner")
				: Component.literal(ownerName(player, home));
		player.sendSystemMessage(Component.translatable(
				"laborcraft.command.home.set.claimed",
				home.name(),
				bedPos.getX(),
				bedPos.getY(),
				bedPos.getZ(),
				owner
		));
		return false;
	}

	private static String ownerName(ServerPlayer player, HomeRegistry.Home home) {
		if (home.owner() == null) {
			return "";
		}

		ServerPlayer owner = player.level().getServer() == null
				? null
				: player.level().getServer().getPlayerList().getPlayer(home.owner());
		return owner == null ? home.owner().toString() : owner.getName().getString();
	}
}
