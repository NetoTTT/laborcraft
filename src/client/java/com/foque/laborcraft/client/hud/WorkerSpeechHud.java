package com.foque.laborcraft.client.hud;

import com.quillui.theme.QuillColors;

import com.quillui.render.QuillRender;
import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;
import com.quillui.theme.QuillThemeData;
import com.foque.laborcraft.client.hud.BubbleSettings;
import com.foque.laborcraft.entity.LaborCraftWorker;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Smooth, close-range speech bubbles for worker notices. Important notices persist in the entity;
 * ambient thoughts are short-lived. Position uses the interpolated entity position and screen-space
 * smoothing so camera movement does not make the bubble snap around.
 */
public final class WorkerSpeechHud implements HudElement {
	private static final double MAX_RANGE = 10.0D;
	private static final int MAX_TEXT_WIDTH = 150;
	private static final Map<UUID, Bubble> BUBBLES = new HashMap<>();

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || client.gui.screen() != null) {
			BUBBLES.clear();
			return;
		}

		Camera camera = client.gameRenderer.mainCamera();
		if (!camera.isInitialized()) {
			BUBBLES.clear();
			return;
		}

		float partialTick = delta.getGameTimeDeltaPartialTick(false);
		float seconds = Math.max(0.001F, delta.getRealtimeDeltaTicks() / 20.0F);
		float fadeStep = 1.0F - (float) Math.exp(-10.0F * seconds);
		float smoothStep = 1.0F - (float) Math.exp(-16.0F * seconds);
		long gameTime = client.level.getGameTime();

		Vec3 forward = new Vec3(camera.forwardVector());
		Vec3 left = new Vec3(camera.leftVector());
		Vec3 up = new Vec3(camera.upVector());
		double verticalHalf = Math.toRadians(camera.getFov()) * 0.5D;
		double aspect = graphics.guiWidth() / (double) Math.max(1, graphics.guiHeight());
		double horizontalHalf = Math.atan(Math.tan(verticalHalf) * aspect);
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();

		Set<UUID> visible = new HashSet<>();
		for (var entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof LaborCraftWorker worker)) {
				continue;
			}

			String noticeKey = worker.workNotice();
			if (noticeKey == null || noticeKey.isBlank()) {
				continue;
			}

			Vec3 position = worker.getPosition(partialTick).add(0.0D, worker.getBbHeight() + 0.55D, 0.0D);
			Vec3 toWorker = position.subtract(camera.position());
			double distance = toWorker.length();
			if (distance > MAX_RANGE || distance < 0.2D) {
				continue;
			}
			if (!BubbleSettings.throughWalls() && occluded(client, camera.position(), position)) {
				continue;
			}

			double z = toWorker.dot(forward);
			if (z <= 0.1D) {
				continue;
			}

			double x = -toWorker.dot(left);
			double y = toWorker.dot(up);
			double normX = x / (z * Math.tan(horizontalHalf));
			double normY = y / (z * Math.tan(verticalHalf));
			if (Math.abs(normX) > 1.05D || Math.abs(normY) > 1.05D) {
				continue;
			}

			float targetX = (float) (screenWidth * 0.5D + normX * screenWidth * 0.5D);
			float targetY = (float) (screenHeight * 0.5D - normY * screenHeight * 0.5D);

			Bubble bubble = BUBBLES.computeIfAbsent(worker.getUUID(), id -> new Bubble(id.hashCode()));
			bubble.setMessage(noticeKey, client.font);
			bubble.color = worker.rarity().color();
			bubble.visible = true;
			bubble.targetAlpha = (float) Mth.clamp((MAX_RANGE - distance) / 2.0D, 0.0D, 1.0D);

			if (!bubble.positioned || Math.abs(targetX - bubble.x) + Math.abs(targetY - bubble.y) > 96.0F) {
				bubble.x = targetX;
				bubble.y = targetY;
				bubble.positioned = true;
			} else {
				bubble.x += (targetX - bubble.x) * smoothStep;
				bubble.y += (targetY - bubble.y) * smoothStep;
			}
			visible.add(worker.getUUID());
		}

		Iterator<Map.Entry<UUID, Bubble>> iterator = BUBBLES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Bubble> entry = iterator.next();
			Bubble bubble = entry.getValue();
			if (!visible.contains(entry.getKey())) {
				bubble.targetAlpha = 0.0F;
			}

			bubble.alpha += (bubble.targetAlpha - bubble.alpha) * fadeStep;
			if (!bubble.visible && bubble.alpha < 0.02F) {
				iterator.remove();
				continue;
			}

			if (bubble.alpha < 0.02F) {
				continue;
			}

			float bob = (float) Math.sin((gameTime + partialTick) * 0.16D + bubble.phase) * 1.4F;
			drawBubble(graphics, bubble, screenWidth, screenHeight, bob);
			bubble.visible = false;
		}
	}

	private static void drawBubble(
			GuiGraphicsExtractor graphics, Bubble bubble, int screenWidth, int screenHeight, float bob
	) {
		int bubbleX = Math.round(bubble.x - bubble.width / 2.0F);
		int bubbleY = Math.round(bubble.y - bubble.height - 13.0F + bob);
		bubbleX = Math.max(4, Math.min(bubbleX, screenWidth - bubble.width - 4));
		bubbleY = Math.max(4, bubbleY);

		float alpha = Mth.clamp(bubble.alpha, 0.0F, 1.0F);
		int border = withAlpha(bubble.color, alpha);
		int text = withAlpha(LaborCraftTheme.currentData().color(QuillThemeData.ColorRole.TEXT), alpha);
		int shadow = withAlpha(QuillColors.argb(102, 0, 0, 0), alpha);
		int background = withAlpha(
				LaborCraftTheme.currentData().color(QuillThemeData.ColorRole.PANEL_BG),
				alpha
		);

		graphics.fill(bubbleX + 1, bubbleY + 1,
				bubbleX + bubble.width + 1, bubbleY + bubble.height + 1, shadow);
		graphics.fill(bubbleX, bubbleY, bubbleX + bubble.width, bubbleY + bubble.height, background);
		QuillRender.border(graphics, bubbleX, bubbleY, bubble.width, bubble.height, border);

		int tailX = Math.round(bubble.x);
		graphics.fill(tailX - 3, bubbleY + bubble.height, tailX + 3, bubbleY + bubble.height + 4, background);
		graphics.fill(tailX - 3, bubbleY + bubble.height, tailX + 3, bubbleY + bubble.height + 1, border);

		for (int index = 0; index < bubble.lines.size(); index++) {
			graphics.text(
					Minecraft.getInstance().font,
					bubble.lines.get(index),
					bubbleX + 6,
					bubbleY + 5 + index * 9,
					text
			);
		}
	}

	private static boolean occluded(Minecraft client, Vec3 from, Vec3 to) {
		ClipContext context = new ClipContext(
				from,
				to,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				client.player
		);
		BlockHitResult hit = client.level.clip(context);
		return hit.getType() != HitResult.Type.MISS;
	}

	public static void clear() {
		BUBBLES.clear();
	}

	private static int withAlpha(int color, float alpha) {
		int original = QuillColors.alpha(color);
		int applied = Mth.clamp((int) (original * alpha), 0, 255);
		return QuillColors.withAlpha(color, applied);
	}

	private static final class Bubble {
		private String key;
		private List<FormattedCharSequence> lines = List.of();
		private int width;
		private int height;
		private int color = QuillColors.rgb(224, 169, 59);
		private float x;
		private float y;
		private float alpha;
		private float targetAlpha;
		private float phase;
		private boolean visible;
		private boolean positioned;

		private Bubble(int phaseSeed) {
			this.phase = (phaseSeed & 0xFFFF) / 65535.0F * (float) (Math.PI * 2.0D);
		}

		private void setMessage(String noticeKey, Font font) {
			if (noticeKey.equals(key)) {
				return;
			}

			key = noticeKey;
			Component message = Component.translatable(noticeKey);
			lines = font.split(message, MAX_TEXT_WIDTH);
			int textWidth = 0;
			for (FormattedCharSequence line : lines) {
				textWidth = Math.max(textWidth, font.width(line));
			}
			width = Math.max(34, textWidth + 12);
			height = lines.size() * 9 + 8;
			alpha = Math.min(alpha, 0.25F);
		}
	}
}
