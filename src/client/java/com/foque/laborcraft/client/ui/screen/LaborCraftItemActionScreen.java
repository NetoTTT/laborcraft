package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.LcButton;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class LaborCraftItemActionScreen extends LaborCraftScreen {
	public record Option(
			Component label,
			Component description,
			String command,
			boolean primary,
			Consumer<LaborCraftItemActionScreen> action,
			SoundEvent sound
	) {
		public Option(
				Component label,
				Component description,
				String command,
				boolean primary,
				Consumer<LaborCraftItemActionScreen> action
		) {
			this(label, description, command, primary, action, SoundEvents.EXPERIENCE_ORB_PICKUP);
		}
	}

	private final ItemStack stack;
	private final Component kindLabel;
	private final List<Option> options;
	private final Runnable onBack;
	private final long maxQuantity;
	private long quantity = 1L;
	private LcButton minus;
	private LcButton plus;
	private LcButton all;

	public LaborCraftItemActionScreen(
			ItemStack stack,
			Component kindLabel,
			List<Option> options,
			Runnable onBack
	) {
		this(stack, kindLabel, options, onBack, stack.getCount());
	}

	public LaborCraftItemActionScreen(
			ItemStack stack,
			Component kindLabel,
			List<Option> options,
			Runnable onBack,
			long available
	) {
		super(Component.translatable("laborcraft.test.item.title"), 460, 300);
		this.stack = stack.copy();
		this.kindLabel = kindLabel;
		this.options = List.copyOf(options);
		this.onBack = onBack;
		this.maxQuantity = Math.max(1L, available);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.test.item.subtitle");
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int quantityY = panelY + 132;

		minus = new LcButton(
				x + 200,
				quantityY,
				22,
				22,
				Component.literal("-"),
				LcButton.Variant.SECONDARY,
				() -> changeQuantity(-1)
		);
		minus.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.item.minus"),
				Component.translatable("laborcraft.test.item.quantity.tip"),
				null
		));
		addRenderableWidget(minus);

		plus = new LcButton(
				x + 286,
				quantityY,
				22,
				22,
				Component.literal("+"),
				LcButton.Variant.SECONDARY,
				() -> changeQuantity(1)
		);
		plus.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.item.plus"),
				Component.translatable("laborcraft.test.item.quantity.tip"),
				null
		));
		addRenderableWidget(plus);

		all = new LcButton(
				x + 314,
				quantityY,
				width - 314,
				22,
				Component.translatable("laborcraft.test.item.all"),
				LcButton.Variant.SECONDARY,
				() -> changeQuantity(maxQuantity)
		);
		all.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.item.all"),
				Component.translatable("laborcraft.test.item.all.tip"),
				null
		));
		addRenderableWidget(all);

		int optionY = panelY + 162;
		int visibleOptions = Math.min(options.size(), 3);
		for (int index = 0; index < visibleOptions; index++) {
			Option option = options.get(index);
			LcButton button = new LcButton(
					x,
					optionY + index * 29,
					width,
					24,
					option.label(),
					option.primary() ? LcButton.Variant.PRIMARY : LcButton.Variant.SECONDARY,
					() -> option.action().accept(this)
			);
			if (option.sound() != null) {
				button.clickSound(option.sound());
			}
			button.tip(LcTooltips.of(
					option.label(),
					option.description(),
					option.command() == null ? null : "/" + option.command()
			));
			addRenderableWidget(button);
		}

		LcButton back = new LcButton(
				x,
				panelY + panelHeight() - 50,
				90,
				24,
				Component.translatable("laborcraft.test.item.back"),
				LcButton.Variant.GHOST,
				this::back
		);
		back.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.item.back"),
				Component.translatable("laborcraft.test.item.back.tip"),
				null
		));
		addRenderableWidget(back);

		LcButton close = new LcButton(
				panelX + panelWidth() - 30,
				panelY + 8,
				20,
				20,
				Component.literal("X"),
				LcButton.Variant.GHOST,
				this::back
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.item.close"),
				Component.translatable("laborcraft.test.item.close.tip"),
				null
		));
		addRenderableWidget(close);

		updateQuantityButtons();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int y = panelY + 64;
		int width = panelWidth() - 44;

		graphics.fill(x, y, x + width, y + 60, 0x401B232D);
		LcRender.border(graphics, x, y, width, 60, LcTheme.DIVIDER);
		graphics.item(stack, x + 12, y + 22);
		graphics.text(font, stack.getHoverName(), x + 38, y + 14, LcTheme.TITLE);
		graphics.text(font, kindLabel, x + 38, y + 30, LcTheme.MUTED);

		if (maxQuantity > 1) {
			graphics.text(
					font,
					Component.literal("x" + maxQuantity),
					x + width - 38,
					y + 14,
					LcTheme.MUTED
			);
		}

		graphics.text(
				font,
				Component.translatable("laborcraft.test.item.quantity"),
				x,
				panelY + 138,
				LcTheme.TEXT
		);
		graphics.centeredText(
				font,
				Component.literal(quantity + " / " + maxQuantity),
				x + 260,
				panelY + 138,
				LcTheme.TITLE
		);

		if (mouseX >= x + 12 && mouseX < x + 28 && mouseY >= y + 22 && mouseY < y + 38) {
			graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}

	@Override
	public void onClose() {
		back();
	}

	public long quantity() {
		return quantity;
	}

	public void back() {
		onBack.run();
	}

	public void notify(Component message) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
				Component.empty()
						.append(Component.literal("[LaborCraft] ").withStyle(ChatFormatting.GOLD))
						.append(message.copy().withStyle(ChatFormatting.GRAY))
		);
	}

	private void changeQuantity(long delta) {
		quantity = Math.max(1, Math.min(quantity + delta, maxQuantity));
		updateQuantityButtons();
	}

	private void updateQuantityButtons() {
		if (minus == null || plus == null || all == null) {
			return;
		}

		minus.active = quantity > 1;
		plus.active = quantity < maxQuantity;
		all.active = quantity < maxQuantity;
	}
}
