package com.foque.laborcraft.client.ui.widget;

import com.quillui.render.QuillRender;
import com.quillui.theme.QuillThemeData;
import com.quillui.theme.QuillThemeScope;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public final class LcItemGrid<T> extends AbstractWidget {
	private final Function<T, ItemStack> icon;
	private final ToLongFunction<T> amount;
	private final Consumer<T> onSelect;
	private final int columns;
	private final int rows;
	private final int cell;
	private final int gap;
	private final QuillThemeData theme;
	private List<T> entries = List.of();
	private int focusedSlot = -1;

	public LcItemGrid(
			int x,
			int y,
			int columns,
			int rows,
			int cell,
			int gap,
			Function<T, ItemStack> icon,
			ToLongFunction<T> amount,
			Consumer<T> onSelect
	) {
		super(
				x,
				y,
				columns * cell + (columns - 1) * gap,
				rows * cell + (rows - 1) * gap,
				Component.translatable("laborcraft.ui.itemgrid")
		);
		this.columns = columns;
		this.rows = rows;
		this.cell = cell;
		this.gap = gap;
		this.icon = icon;
		this.amount = amount;
		this.onSelect = onSelect;
		this.theme = QuillThemeScope.current();
	}

	public void setEntries(List<T> entries) {
		this.entries = List.copyOf(entries);
		this.focusedSlot = -1;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		Minecraft minecraft = Minecraft.getInstance();
		int hovered = entryAt(mouseX, mouseY);
		int capacity = this.columns * this.rows;

		for (int index = 0; index < capacity; index++) {
			int slotX = slotX(index);
			int slotY = slotY(index);
			boolean filled = index < entries.size();
			boolean activeSlot = filled && (index == hovered || (isFocused() && index == focusedSlot));

			graphics.fill(slotX, slotY, slotX + cell, slotY + cell, index == hovered
					? theme.color(QuillThemeData.ColorRole.BUTTON_HOVER)
					: theme.color(QuillThemeData.ColorRole.BUTTON_BG));
			QuillRender.border(graphics, slotX, slotY, cell, cell, activeSlot
					? theme.color(QuillThemeData.ColorRole.ACCENT)
					: theme.color(QuillThemeData.ColorRole.BUTTON_BORDER));

			if (!filled) {
				continue;
			}

			ItemStack stack = icon.apply(entries.get(index));
			if (stack.isEmpty()) {
				continue;
			}

			int iconX = slotX + (cell - 16) / 2;
			int iconY = slotY + (cell - 16) / 2;
			ItemStack display = stack.copyWithCount(1);
			long quantity = amount.applyAsLong(entries.get(index));
			graphics.item(display, iconX, iconY);
			graphics.itemDecorations(
					minecraft.font,
					display,
					iconX,
					iconY,
					quantity > 1 ? formatAmount(quantity) : null
			);
		}

		if (hovered >= 0) {
			ItemStack stack = icon.apply(entries.get(hovered));
			if (!stack.isEmpty()) {
				long quantity = amount.applyAsLong(entries.get(hovered));
				graphics.setComponentTooltipForNextFrame(
						minecraft.font,
						tooltip(minecraft, stack, quantity),
						mouseX,
						mouseY
				);
			}
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		int index = entryAt(event.x(), event.y());
		if (index < 0) {
			return;
		}

		setFocused(true);
		focusedSlot = index;
		onSelect.accept(entries.get(index));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage());
	}

	private static List<Component> tooltip(Minecraft minecraft, ItemStack stack, long quantity) {
		List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
		lines.add(Component.translatable("laborcraft.ui.itemgrid.amount", quantity).withStyle(ChatFormatting.GRAY));

		long maxStack = stack.getMaxStackSize();
		if (maxStack > 1 && quantity > maxStack) {
			long stacks = quantity / maxStack;
			long remainder = quantity % maxStack;
			MutableComponent breakdown = remainder > 0
					? Component.translatable("laborcraft.ui.itemgrid.amount.stacks.rest", stacks, maxStack, remainder)
					: Component.translatable("laborcraft.ui.itemgrid.amount.stacks", stacks, maxStack);
			lines.add(breakdown.withStyle(ChatFormatting.DARK_GRAY));
		}

		return lines;
	}

	private static String formatAmount(long amount) {
		if (amount < 1_000) {
			return Long.toString(amount);
		}
		if (amount < 1_000_000) {
			return amount / 1_000 + "k";
		}
		if (amount < 1_000_000_000) {
			return amount / 1_000_000 + "M";
		}
		return amount / 1_000_000_000 + "B";
	}

	private int entryAt(double mouseX, double mouseY) {
		for (int index = 0; index < entries.size(); index++) {
			int slotX = slotX(index);
			int slotY = slotY(index);
			if (mouseX >= slotX && mouseX < slotX + cell && mouseY >= slotY && mouseY < slotY + cell) {
				return index;
			}
		}

		return -1;
	}

	private int slotX(int index) {
		return getX() + (index % columns) * (cell + gap);
	}

	private int slotY(int index) {
		return getY() + (index / columns) * (cell + gap);
	}
}
