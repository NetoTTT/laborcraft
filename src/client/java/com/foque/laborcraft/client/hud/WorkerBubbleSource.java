package com.foque.laborcraft.client.hud;

import com.foque.laborcraft.entity.LaborCraftWorker;
import com.quillui.hud.QuillSpeechBubble;
import com.quillui.hud.QuillSpeechBubbleSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Supplies the worker notices to the generic Quill bubble HUD. */
public final class WorkerBubbleSource implements QuillSpeechBubbleSource {
	private static final double MAX_RANGE = 10.0D;

	@Override
	public List<QuillSpeechBubble> bubbles(float partialTick) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return List.of();
		}

		List<QuillSpeechBubble> bubbles = new ArrayList<>();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof LaborCraftWorker worker)) {
				continue;
			}

			String notice = worker.workNotice();
			if (notice == null || notice.isBlank()) {
				continue;
			}

			Vec3 position = worker.getPosition(partialTick)
					.add(0.0D, worker.getBbHeight() + 0.55D, 0.0D);
			bubbles.add(new QuillSpeechBubble(
					worker.getUUID(),
					position,
					Component.translatable(notice),
					worker.rarity().color(),
					MAX_RANGE,
					BubbleSettings.throughWalls()
			));
		}
		return List.copyOf(bubbles);
	}
}
