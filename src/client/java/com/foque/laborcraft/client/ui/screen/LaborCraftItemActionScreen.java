package com.foque.laborcraft.client.ui.screen;

import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.ItemStackWidget;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillNodes;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** Item action sheet with quantity controls and option buttons. */
public final class LaborCraftItemActionScreen extends QuillScreen {
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
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(itemCard());
		root.add(quantityRow());

		int visible = Math.min(options.size(), 3);
		for (int index = 0; index < visible; index++) {
			Option option = options.get(index);
			QuillButton button = new QuillButton(
					0, 0, 300, 24,
					option.label(),
					option.primary() ? QuillButton.Variant.PRIMARY : QuillButton.Variant.SECONDARY,
					() -> option.action().accept(this)
			);
			if (option.sound() != null) {
				button.clickSound(option.sound());
			}
			button.tip(QuillTooltips.of(
					option.label(),
					option.description(),
					option.command() == null ? null : "/" + option.command()
			));
			root.add(control(button, 24));
		}

		root.add(backRow());
		setContent(root);
	}

	private QuillLayoutNode itemCard() {
		QuillFlexBox body = QuillNodes.row(QuillSpacing.SPACE_2);
		body.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		body.add(QuillNodes.widget(new ItemStackWidget(stack, 16), 16, 16));

		QuillFlexBox text = QuillNodes.column(2);
		text.add(control(label(stack.getHoverName()).title()));
		text.add(control(label(kindLabel).muted()));
		body.add(QuillNodes.flexible(text));

		if (maxQuantity > 1) {
			body.add(text(Component.literal("x" + maxQuantity), MUTED));
		}
		return new QuillCardBox(null, body);
	}

	private QuillFlexBox quantityRow() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(text(Component.translatable("laborcraft.test.item.quantity")));
		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		row.add(QuillNodes.widget(quantityButton(
				"-",
				"laborcraft.test.item.minus",
				-1
		), 22, 22));
		row.add(text(Component.literal(quantity + " / " + maxQuantity)));
		row.add(QuillNodes.widget(quantityButton(
				"+",
				"laborcraft.test.item.plus",
				1
		), 22, 22));
		row.add(QuillNodes.widget(allButton(), 92, 22));
		return row;
	}

	private QuillButton quantityButton(String label, String tipKey, long delta) {
		QuillButton button = new QuillButton(
				0, 0, 22, 22,
				Component.literal(label),
				QuillButton.Variant.SECONDARY,
				() -> changeQuantity(delta)
		);
		button.active = delta < 0 ? quantity > 1 : quantity < maxQuantity;
		button.tip(LcTooltips.of(
				Component.translatable(tipKey),
				Component.translatable("laborcraft.test.item.quantity.tip"),
				null
		));
		return button;
	}

	private QuillButton allButton() {
		QuillButton button = new QuillButton(
				0, 0, 92, 22,
				Component.translatable("laborcraft.test.item.all"),
				QuillButton.Variant.SECONDARY,
				() -> changeQuantity(maxQuantity)
		);
		button.active = quantity < maxQuantity;
		button.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.item.all"),
				Component.translatable("laborcraft.test.item.all.tip"),
				null
		));
		return button;
	}

	private QuillFlexBox backRow() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		QuillButton back = new QuillButton(
				0, 0, 90, 24,
				Component.translatable("laborcraft.test.item.back"),
				QuillButton.Variant.GHOST,
				this::back
		);
		back.tip(QuillTooltips.of(
				Component.translatable("laborcraft.test.item.back"),
				Component.translatable("laborcraft.test.item.back.tip"),
				null
		));
		row.add(QuillNodes.widget(back, 90, 24));
		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		return row;
	}

	private void changeQuantity(long delta) {
		quantity = Math.max(1, Math.min(quantity + delta, maxQuantity));
		refreshContent();
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
}
