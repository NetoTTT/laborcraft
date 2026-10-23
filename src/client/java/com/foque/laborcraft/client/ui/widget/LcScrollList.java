package com.foque.laborcraft.client.ui.widget;

import com.foque.laborcraft.client.ui.LcTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class LcScrollList<T> extends AbstractWidget {
	public interface RowRenderer<T> {
		void render(
				GuiGraphicsExtractor graphics,
				T item,
				int index,
				int x,
				int y,
				int width,
				int height,
				int mouseX,
				int mouseY,
				boolean hovered
		);
	}

	public interface ClickHandler<T> {
		void onClick(T item, int index);
	}

	private static final int SCROLLBAR_WIDTH = 4;
	private static final int SCROLLBAR_GAP = 4;

	private final List<T> items = new ArrayList<>();
	private final int rowHeight;
	private final RowRenderer<T> rowRenderer;
	private final ClickHandler<T> clickHandler;
	private double scroll;

	public LcScrollList(
			int x,
			int y,
			int width,
			int height,
			int rowHeight,
			RowRenderer<T> rowRenderer,
			ClickHandler<T> clickHandler
	) {
		super(x, y, width, height, Component.empty());
		this.rowHeight = rowHeight;
		this.rowRenderer = rowRenderer;
		this.clickHandler = clickHandler;
	}

	public void replaceItems(List<T> newItems) {
		items.clear();
		items.addAll(newItems);
		scroll = 0.0D;
	}

	public int size() {
		return items.size();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public T get(int index) {
		return items.get(index);
	}

	private int contentHeight() {
		return items.size() * rowHeight;
	}

	private int maxScroll() {
		return Math.max(0, contentHeight() - height);
	}

	private int scrollbarX() {
		return getX() + width - SCROLLBAR_WIDTH;
	}

	private int listWidth() {
		return Math.max(1, width - (maxScroll() > 0 ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0));
	}

	private int scrollbarThumbHeight() {
		if (maxScroll() <= 0) {
			return 0;
		}

		return Math.max(16, (int) ((float) height * height / contentHeight()));
	}

	private int scrollbarThumbY() {
		int thumbHeight = scrollbarThumbHeight();
		if (thumbHeight <= 0) {
			return getY();
		}

		int travel = height - thumbHeight;
		return getY() + (int) ((float) travel * (float) scroll / maxScroll());
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.enableScissor(getX(), getY(), getX() + listWidth(), getY() + height);

		int offset = Mth.floor(scroll);
		for (int index = 0; index < items.size(); index++) {
			int rowY = getY() + index * rowHeight - offset;
			if (rowY + rowHeight < getY() || rowY > getY() + height) {
				continue;
			}

			boolean hovered = mouseX >= getX()
					&& mouseX < getX() + listWidth()
					&& mouseY >= rowY
					&& mouseY < rowY + rowHeight
					&& isMouseOver(mouseX, mouseY);
			rowRenderer.render(
					graphics,
					items.get(index),
					index,
					getX(),
					rowY,
					listWidth(),
					rowHeight,
					mouseX,
					mouseY,
					hovered
			);
		}

		graphics.disableScissor();

		if (maxScroll() > 0) {
			int barX = scrollbarX();
			graphics.fill(barX, getY(), barX + SCROLLBAR_WIDTH, getY() + height, LcTheme.BUTTON_DISABLED);
			graphics.fill(
					barX,
					scrollbarThumbY(),
					barX + SCROLLBAR_WIDTH,
					scrollbarThumbY() + scrollbarThumbHeight(),
					LcTheme.ACCENT
			);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!active || !visible || !isMouseOver(mouseX, mouseY) || maxScroll() <= 0) {
			return false;
		}

		scroll = Mth.clamp(scroll - scrollY * rowHeight, 0.0D, maxScroll());
		return true;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (!active) {
			return;
		}

		double mouseX = event.x();
		double mouseY = event.y();
		if (maxScroll() > 0 && mouseX >= scrollbarX() - 2.0D) {
			int thumbHeight = scrollbarThumbHeight();
			int travel = height - thumbHeight;
			if (travel > 0) {
				double ratio = Mth.clamp((mouseY - getY() - thumbHeight / 2.0D) / travel, 0.0D, 1.0D);
				scroll = ratio * maxScroll();
			}
			return;
		}

		int index = Mth.floor((mouseY - getY() + scroll) / rowHeight);
		if (index < 0 || index >= items.size()) {
			return;
		}

		playDownSound(Minecraft.getInstance().getSoundManager());
		clickHandler.onClick(items.get(index), index);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
