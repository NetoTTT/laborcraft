package com.foque.laborcraft.client.ui.widget;

import com.quillui.theme.QuillColors;

import com.quillui.render.QuillRender;
import com.quillui.theme.QuillThemeData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Measured inventory board: the player's 9x3 grid plus hotbar, the worker's 3x3 backpack and the
 * tool slot. It paints the slot surfaces and maps a mouse position back to a slot; the owning
 * screen keeps the transfer logic and selection state.
 */
public final class WorkerInventoryBoard extends AbstractWidget {
	public enum Side {
		PLAYER,
		WORKER
	}

	public record SlotHit(Side side, int index) {
	}

	public interface StackSource {
		ItemStack stack(Side side, int index);
	}

	private static final int SLOT = 32;
	private static final int STEP = 34;
	private static final int COLUMNS = 9;
	private static final int TOOL_SLOT = 9;
	private static final int HOTBAR_GAP = 10;
	private static final int WORKER_GAP = 20;
	private static final int PLAYER_WIDTH = COLUMNS * STEP;
	private static final int WORKER_OFFSET = PLAYER_WIDTH + WORKER_GAP;
	private static final int WORKER_WIDTH = 3 * STEP + 8 + SLOT;
	private static final int LABEL_HEIGHT = 14;
	private static final int GRID_HEIGHT = 3 * STEP + HOTBAR_GAP + SLOT;
	private static final int HINT_GAP = 8;

	private final StackSource source;
	private final Component playerLabel;
	private final Component workerLabel;
	private final Component toolLabel;
	private final Component toolName;
	private final Component hint;
	private final QuillThemeData theme;
	private SlotHit selected;

	public WorkerInventoryBoard(
			StackSource source,
			Component playerLabel,
			Component workerLabel,
			Component toolLabel,
			Component toolName,
			Component hint,
			QuillThemeData theme
	) {
		super(
				0,
				0,
				WORKER_OFFSET + WORKER_WIDTH,
				LABEL_HEIGHT + GRID_HEIGHT + HINT_GAP + 34,
				Component.empty()
		);
		this.source = source;
		this.playerLabel = playerLabel;
		this.workerLabel = workerLabel;
		this.toolLabel = toolLabel;
		this.toolName = toolName;
		this.hint = hint;
		this.theme = theme == null ? QuillThemeData.DEFAULT : theme;
	}

	public void select(SlotHit hit) {
		selected = hit;
	}

	public SlotHit selected() {
		return selected;
	}

	/** Preferred board size; used when the screen wants the grid to keep its metrics. */
	public static int preferredWidth() {
		return WORKER_OFFSET + WORKER_WIDTH;
	}

	@Override
	protected void extractWidgetRenderState(
			GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta
	) {
		Font font = Minecraft.getInstance().font;
		int left = getX();
		int top = getY();
		int gridTop = top + LABEL_HEIGHT;
		int workerX = left + WORKER_OFFSET;

		graphics.enableScissor(left, top, left + getWidth(), top + getHeight());
		try {
			text(font, graphics, playerLabel, left, top, QuillThemeData.ColorRole.MUTED);
			text(font, graphics, workerLabel, workerX, top, QuillThemeData.ColorRole.MUTED);
			text(
					font,
					graphics,
					toolLabel,
					workerX + 3 * STEP + 8,
					top,
					QuillThemeData.ColorRole.MUTED
			);

			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < COLUMNS; column++) {
					drawSlot(
							graphics,
							font,
							Side.PLAYER,
							9 + row * COLUMNS + column,
							left + column * STEP,
							gridTop + row * STEP,
							mouseX,
							mouseY
					);
				}
			}
			for (int column = 0; column < COLUMNS; column++) {
				drawSlot(
						graphics,
						font,
						Side.PLAYER,
						column,
						left + column * STEP,
						gridTop + 3 * STEP + HOTBAR_GAP,
						mouseX,
						mouseY
				);
			}

			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					drawSlot(
							graphics,
							font,
							Side.WORKER,
							row * 3 + column,
							workerX + column * STEP,
							gridTop + row * STEP,
							mouseX,
							mouseY
					);
				}
			}
			drawSlot(
					graphics,
					font,
					Side.WORKER,
					TOOL_SLOT,
					workerX + 3 * STEP + 8,
					gridTop,
					mouseX,
					mouseY
			);

			text(
					font,
					graphics,
					toolName,
					workerX + 3 * STEP + 8,
					gridTop + SLOT + 3,
					QuillThemeData.ColorRole.ACCENT
			);

			int hintX = workerX;
			int hintY = gridTop + GRID_HEIGHT + HINT_GAP;
			int hintWidth = Math.max(20, left + getWidth() - hintX);
			List<FormattedCharSequence> lines = font.split(hint, hintWidth);
			for (int index = 0; index < lines.size(); index++) {
				int lineY = hintY + index * 11;
				if (lineY + 9 > top + getHeight()) {
					break;
				}
				graphics.text(
						font,
						lines.get(index),
						hintX,
						lineY,
						theme.color(QuillThemeData.ColorRole.MUTED),
						false
				);
			}
		}
		finally {
			graphics.disableScissor();
		}
	}

	private void drawSlot(
			GuiGraphicsExtractor graphics,
			Font font,
			Side side,
			int index,
			int x,
			int y,
			int mouseX,
			int mouseY
	) {
		boolean isSelected = selected != null
				&& selected.side() == side
				&& selected.index() == index;
		boolean isHovered = mouseX >= x && mouseX < x + SLOT
				&& mouseY >= y && mouseY < y + SLOT;

		int background = isSelected
				? theme.color(QuillThemeData.ColorRole.SELECTED_BG)
				: isHovered
						? theme.color(QuillThemeData.ColorRole.BUTTON_HOVER)
						: theme.color(QuillThemeData.ColorRole.BUTTON_BG);
		int border = isSelected
				? theme.color(QuillThemeData.ColorRole.ACCENT)
				: isHovered
						? theme.color(QuillThemeData.ColorRole.TITLE)
						: theme.color(QuillThemeData.ColorRole.BUTTON_BORDER);
		QuillRender.box(graphics, x, y, SLOT, SLOT, background, border);

		ItemStack stack = source.stack(side, index);
		if (stack.isEmpty()) {
			return;
		}

		graphics.item(stack, x + 8, y + 8);
		if (stack.getCount() > 1) {
			String count = String.valueOf(stack.getCount());
			int textWidth = font.width(count);
			graphics.text(font, count, x + SLOT - textWidth - 2, y + SLOT - 10, QuillColors.rgb(255, 255, 255));
		}
		if (stack.isDamageableItem() && stack.isDamaged()) {
			int max = stack.getMaxDamage();
			int remaining = max - stack.getDamageValue();
			int barWidth = Math.max(1, (int) ((SLOT - 4) * (remaining / (float) max)));
			int color = remaining / (float) max > 0.5F
					? QuillColors.rgb(72, 215, 160)
					: (remaining / (float) max > 0.2F ? QuillColors.rgb(255, 198, 90) : QuillColors.rgb(255, 85, 85));
			graphics.fill(x + 2, y + SLOT - 4, x + SLOT - 2, y + SLOT - 3, QuillColors.rgb(16, 24, 32));
			graphics.fill(x + 2, y + SLOT - 4, x + 2 + barWidth, y + SLOT - 3, color);
		}
	}

	private void text(
			Font font,
			GuiGraphicsExtractor graphics,
			Component message,
			int x,
			int y,
			QuillThemeData.ColorRole role
	) {
		if (message == null || message.getString().isBlank()) {
			return;
		}
		graphics.text(font, message, x, y, theme.color(role), false);
	}

	public SlotHit slotAt(double mouseX, double mouseY) {
		if (mouseX < getX() || mouseX >= getX() + getWidth()
				|| mouseY < getY() || mouseY >= getY() + getHeight()) {
			return null;
		}

		int left = getX();
		int gridTop = getY() + LABEL_HEIGHT;
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				if (inside(mouseX, mouseY, left + column * STEP, gridTop + row * STEP)) {
					return new SlotHit(Side.PLAYER, 9 + row * COLUMNS + column);
				}
			}
		}
		for (int column = 0; column < COLUMNS; column++) {
			if (inside(
					mouseX,
					mouseY,
					left + column * STEP,
					gridTop + 3 * STEP + HOTBAR_GAP
			)) {
				return new SlotHit(Side.PLAYER, column);
			}
		}

		int workerX = left + WORKER_OFFSET;
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				if (inside(mouseX, mouseY, workerX + column * STEP, gridTop + row * STEP)) {
					return new SlotHit(Side.WORKER, row * 3 + column);
				}
			}
		}
		if (inside(mouseX, mouseY, workerX + 3 * STEP + 8, gridTop)) {
			return new SlotHit(Side.WORKER, TOOL_SLOT);
		}
		return null;
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
