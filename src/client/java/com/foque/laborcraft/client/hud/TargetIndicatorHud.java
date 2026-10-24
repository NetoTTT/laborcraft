package com.foque.laborcraft.client.hud;

import com.foque.laborcraft.client.render.WorldHighlightManager;
import com.foque.laborcraft.client.ui.LcRender;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * FPS-style off-screen indicator for the active world highlight: when the target leaves the camera
 * frustum, a badge with the chest icon, an outward chevron and the distance appears on a ring
 * around the crosshair. Purely client-side and reusable for other highlight sources later.
 */
public final class TargetIndicatorHud implements HudElement {
	private static final int BADGE = 11;
	private static final double MAX_RANGE = 96.0D;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		BlockPos target = WorldHighlightManager.target();
		if (target == null || client.player == null || client.level == null) {
			return;
		}
		if (WorldHighlightManager.remainingSeconds() <= 0) {
			return;
		}

		Camera camera = client.gameRenderer.mainCamera();
		if (!camera.isInitialized()) {
			return;
		}

		Vec3 toTarget = Vec3.atCenterOf(target).subtract(camera.position());
		double distance = toTarget.length();
		if (distance < 0.5D || distance > MAX_RANGE) {
			return;
		}

		Vec3 forward = new Vec3(camera.forwardVector());
		Vec3 left = new Vec3(camera.leftVector());
		Vec3 up = new Vec3(camera.upVector());
		double z = toTarget.dot(forward);
		double x = -toTarget.dot(left);
		double y = toTarget.dot(up);

		double verticalHalf = Math.toRadians(camera.getFov()) * 0.5D;
		double aspect = graphics.guiWidth() / (double) Math.max(1, graphics.guiHeight());
		double horizontalHalf = Math.atan(Math.tan(verticalHalf) * aspect);

		if (z > 0.0D) {
			double screenX = x / (z * Math.tan(horizontalHalf));
			double screenY = y / (z * Math.tan(verticalHalf));
			if (Math.abs(screenX) < 0.98D && Math.abs(screenY) < 0.98D) {
				return;
			}
		}

		double dx = x;
		double dy = -y;
		if (Math.abs(dx) < 1.0E-4D && Math.abs(dy) < 1.0E-4D) {
			dy = 1.0D;
		}
		double length = Math.hypot(dx, dy);
		double ux = dx / length;
		double uy = dy / length;

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		double radius = Mth.clamp(Math.min(width, height) * 0.30D, 60.0D, 180.0D);
		double px = Mth.clamp(width / 2.0D + ux * radius, 24.0D, width - 24.0D);
		double py = Mth.clamp(height / 2.0D + uy * radius, 24.0D, height - 24.0D);

		int color = WorldHighlightManager.color();
		int cx = (int) px;
		int cy = (int) py;

		graphics.fill(cx - BADGE, cy - BADGE, cx + BADGE, cy + BADGE, 0xCC101820);
		LcRender.border(graphics, cx - BADGE, cy - BADGE, BADGE * 2, BADGE * 2, color);
		ItemStack icon = WorldHighlightManager.icon();
		if (!icon.isEmpty()) {
			graphics.item(icon, cx - 8, cy - 8);
		}

		float angle = (float) Math.atan2(uy, ux);
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) cx, (float) cy);
		graphics.pose().rotate(angle);
		for (int step = 0; step < 6; step++) {
			int half = 5 - step;
			graphics.fill(13 + step, -half, 14 + step, half + 1, color);
		}
		graphics.pose().popMatrix();

		Component label = Component.literal(Math.round(distance) + " m");
		int textWidth = client.font.width(label);
		int labelY = cy + BADGE + 5;
		if (labelY + 9 > height) {
			labelY = cy - BADGE - 13;
		}
		graphics.fill(
				cx - textWidth / 2 - 3,
				labelY - 2,
				cx + textWidth / 2 + 3,
				labelY + 9,
				0xAA101820
		);
		graphics.centeredText(client.font, label, cx, labelY, color);
	}
}
