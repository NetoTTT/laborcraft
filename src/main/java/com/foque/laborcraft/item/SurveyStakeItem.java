package com.foque.laborcraft.item;

import com.foque.laborcraft.network.OpenStationPayload;
import com.foque.laborcraft.worker.StationRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Estaca de Demarcacao. Clique A, clique B e depois agachar + clique no Posto para aplicar a zona.
 * E o caminho acessivel (e o fallback) do desenho de area que a UI do posto vai oferecer.
 */
public class SurveyStakeItem extends Item {
	private static final Map<UUID, Selection> PENDING = new HashMap<>();

	public SurveyStakeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}

		BlockPos clicked = context.getClickedPos().immutable();
		String dimension = serverLevel.dimension().identifier().toString();

		if (player.isShiftKeyDown()) {
			applyToStation(serverLevel, player, dimension, clicked);
			return InteractionResult.SUCCESS_SERVER;
		}

		Selection selection = PENDING.get(player.getUUID());
		if (selection == null || !selection.dimension().equals(dimension) || selection.to() != null) {
			PENDING.put(player.getUUID(), new Selection(dimension, clicked, null));
			player.sendSystemMessage(Component.translatable(
					"laborcraft.stake.corner_a",
					clicked.getX(),
					clicked.getY(),
					clicked.getZ()
			));
			return InteractionResult.SUCCESS_SERVER;
		}

		PENDING.put(player.getUUID(), new Selection(dimension, selection.from(), clicked));
		int width = Math.abs(clicked.getX() - selection.from().getX()) + 1;
		int depth = Math.abs(clicked.getZ() - selection.from().getZ()) + 1;
		player.sendSystemMessage(Component.translatable("laborcraft.stake.corner_b", width, depth));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static String zoneClipErrorKey(int result) {
		return switch (result) {
			case -2 -> "laborcraft.station.zone.clip.too_complex";
			case -3 -> "laborcraft.station.zone.invalid.too_large";
			case -4 -> "laborcraft.station.zone.invalid.too_far";
			default -> "laborcraft.station.zone.clip.blocked";
		};
	}

	private void applyToStation(ServerLevel level, ServerPlayer player, String dimension, BlockPos clicked) {
		StationRegistry.Station station = StationRegistry.get(level.getServer(), dimension, clicked);
		if (station == null) {
			player.sendSystemMessage(Component.translatable("laborcraft.stake.not_station"));
			return;
		}

		if (!station.isOwner(player.getUUID())) {
			player.sendSystemMessage(Component.translatable("laborcraft.station.not_owner"));
			return;
		}

		Selection selection = PENDING.get(player.getUUID());
		if (selection == null || selection.to() == null || !selection.dimension().equals(dimension)) {
			player.sendSystemMessage(Component.translatable("laborcraft.stake.need_corners"));
			return;
		}

		int result = StationRegistry.applyZone(
				level.getServer(),
				station,
				selection.from(),
				selection.to()
		);
		if (result < 0) {
			player.sendSystemMessage(Component.translatable(zoneClipErrorKey(result)));
			return;
		}

		PENDING.remove(player.getUUID());

		StationRegistry.Station updated = StationRegistry.get(level.getServer(), dimension, clicked);
		OpenStationPayload.open(player, updated);
		player.sendSystemMessage(Component.translatable("laborcraft.station.zone.applied", result));
	}

	public record Selection(String dimension, BlockPos from, BlockPos to) {
	}
}
