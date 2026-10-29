package com.foque.laborcraft.client.ui.screen;

import static com.quillui.theme.QuillThemeData.ColorRole.DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.ItemStackWidget;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenHiringPayload;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.text.QuillTextStyle;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillListView;
import com.quillui.widget.QuillNodes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Hiring offers with stats, selectable rows and a buy action. */
public final class LaborCraftHiringScreen extends QuillScreen {
	private final OpenHiringPayload offer;
	private OpenHiringPayload.Offer selected;

	public LaborCraftHiringScreen(OpenHiringPayload offer) {
		super(Component.translatable("laborcraft.ui.hire.title"), 460, 360);
		this.offer = offer;
		this.selected = offer.offers().isEmpty() ? null : offer.offers().get(0);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.hire.subtitle");
	}

	@Override
	protected void buildContent() {
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(stats());
		root.add(text(Component.translatable("laborcraft.ui.hire.offers")));

		if (offer.offers().isEmpty()) {
			root.add(text(Component.translatable("laborcraft.ui.hire.no_offers"), MUTED));
		}
		else {
			QuillListView<OpenHiringPayload.Offer> list = new QuillListView<>(
					0, 0, 400, 146, 52,
					this::offerRow,
					(offerRow, index) -> {
						selected = offerRow;
						refreshContent();
					}
			);
			list.replaceItems(offer.offers());
			list.setSelected(selected);
			root.add(QuillNodes.flexible(control(list), 1));
		}

		root.add(note());
		root.add(actions());

		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillFlexBox stats() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.hire.balance"),
				text(Component.literal(offer.balanceText()))
		)));
		row.add(QuillNodes.flexible(new QuillCardBox(
				Component.translatable("laborcraft.ui.hire.workers"),
				text(workersValue())
		)));
		return row;
	}

	private Component workersValue() {
		if (offer.limit() <= 0) {
			return Component.translatable(
					"laborcraft.ui.hire.workers.unlimited",
					offer.owned() + offer.pending()
			);
		}
		return Component.translatable(
				"laborcraft.ui.hire.workers.value",
				offer.owned() + offer.pending(),
				offer.limit()
		);
	}

	private QuillLayoutNode offerRow(OpenHiringPayload.Offer candidate, int index) {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(QuillNodes.widget(
				new ItemStackWidget(new ItemStack(Items.WHEAT), 16),
				16,
				16
		));

		QuillFlexBox identity = QuillNodes.column(2);
		QuillFlexBox nameRow = QuillNodes.row(6);
		nameRow.add(control(label(Component.literal(candidate.name())).title()));
		nameRow.add(control(label(LcWorkerText.rankTitle(candidate.rankId()))
				.style(QuillTextStyle.of(LcWorkerText.rarityColor(candidate.rarityId())))));
		identity.add(nameRow);

		List<Component> details = new ArrayList<>();
		details.add(LcWorkerText.rarity(candidate.rarityId()));
		details.add(LcWorkerText.job(""));
		details.add(LcWorkerText.aptitude(candidate.aptitude(), JobTypes.FARMER.id()));
		identity.add(control(label(LcWorkerText.join(details, LcWorkerText.SEPARATOR)).muted()));

		List<Component> boosts = LcWorkerText.boosts(candidate.boosts());
		if (!boosts.isEmpty()) {
			identity.add(control(label(LcWorkerText.join(boosts, LcWorkerText.SEPARATOR)).accent()));
		}
		row.add(QuillNodes.flexible(identity));

		QuillFlexBox price = QuillNodes.column(2);
		price.crossAxisAlignment(QuillCrossAxisAlignment.END);
		price.add(control(label(candidate.free()
				? Component.translatable("laborcraft.ui.hire.free")
				: Component.literal(candidate.priceText())).title()));
		if (candidate.upkeep() > 0L) {
			price.add(control(label(Component.translatable(
					"laborcraft.ui.hire.upkeep",
					Component.literal(candidate.upkeepText())
			)).muted()));
		}
		row.add(price);
		return row;
	}

	private QuillLayoutNode note() {
		boolean blocked = offer.atLimit() || selected == null || !offer.canAfford(selected);
		Component text;
		if (offer.offers().isEmpty()) {
			text = Component.translatable("laborcraft.ui.hire.no_offers");
		}
		else if (offer.atLimit()) {
			text = Component.translatable("laborcraft.ui.hire.limit");
		}
		else if (selected == null || !offer.canAfford(selected)) {
			text = Component.translatable("laborcraft.ui.hire.insufficient");
		}
		else {
			text = Component.translatable("laborcraft.ui.hire.note");
		}
		return text(text, blocked ? DANGER : MUTED);
	}

	private QuillFlexBox actions() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);

		QuillButton buy = new QuillButton(
				0, 0, 280, 24,
				Component.translatable("laborcraft.ui.hire.buy"),
				QuillButton.Variant.PRIMARY,
				this::buySelected
		).enabled(selected != null && !offer.atLimit() && offer.canAfford(selected));
		if (selected != null) {
			buy.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.hire.buy"),
					Component.translatable("laborcraft.ui.hire.buy.tip"),
					"/" + LaborCraftCommands.WORKER_BUY + " " + selected.id()
			));
		}
		row.add(QuillNodes.flexible(control(buy, 24)));

		QuillButton close = new QuillButton(
				0, 0, 76, 24,
				Component.translatable("laborcraft.ui.worker.close"),
				QuillButton.Variant.GHOST,
				this::onClose
		);
		close.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.worker.close"),
				Component.translatable("laborcraft.ui.worker.close.tip"),
				null
		));
		row.add(QuillNodes.widget(close, 76, 24));
		return row;
	}

	private void buySelected() {
		if (selected == null) {
			return;
		}
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_BUY + " " + selected.id());
	}
}
