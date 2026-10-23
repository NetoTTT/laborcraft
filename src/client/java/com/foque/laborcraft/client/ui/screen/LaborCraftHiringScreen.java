package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.ui.widget.LcScrollList;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenHiringPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LaborCraftHiringScreen extends LaborCraftScreen {
	private final OpenHiringPayload offer;
	private LcScrollList<OpenHiringPayload.Offer> offers;
	private LcButton buy;
	private int selectedIndex;

	public LaborCraftHiringScreen(OpenHiringPayload offer) {
		super(Component.translatable("laborcraft.ui.hire.title"), 460, 360);
		this.offer = offer;
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.hire.subtitle");
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int buttonsY = panelY + panelHeight() - 50;
		int closeWidth = 76;
		int gap = 8;

		offers = new LcScrollList<>(
				x,
				panelY + 142,
				width,
				146,
				52,
				this::drawOffer,
				(selected, index) -> select(index)
		);
		offers.replaceItems(offer.offers());
		addRenderableWidget(offers);

		buy = new LcButton(
				x,
				buttonsY,
				width - closeWidth - gap,
				24,
				Component.translatable("laborcraft.ui.hire.buy"),
				LcButton.Variant.PRIMARY,
				this::buySelected
		);
		addRenderableWidget(buy);

		LcButton close = new LcButton(
				x + width - closeWidth,
				buttonsY,
				closeWidth,
				24,
				Component.translatable("laborcraft.ui.worker.close"),
				LcButton.Variant.GHOST,
				this::onClose
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.worker.close"),
				Component.translatable("laborcraft.ui.worker.close.tip"),
				null
		));
		addRenderableWidget(close);

		addRenderableWidget(closeButton(this::onClose));

		select(offer.offers().isEmpty() ? -1 : 0);
	}

	private void select(int index) {
		selectedIndex = index;
		OpenHiringPayload.Offer selected = selectedOffer();
		if (buy != null) {
			buy.active = selected != null && !offer.atLimit() && offer.canAfford(selected);
			if (selected != null) {
				buy.tip(LcTooltips.of(
						Component.translatable("laborcraft.ui.hire.buy"),
						Component.translatable("laborcraft.ui.hire.buy.tip"),
						"/" + LaborCraftCommands.WORKER_BUY + " " + selected.id()
				));
			}
		}
	}

	private void buySelected() {
		OpenHiringPayload.Offer selected = selectedOffer();
		if (selected == null) {
			return;
		}

		LaborCraftUiActions.run(LaborCraftCommands.WORKER_BUY + " " + selected.id());
	}

	private OpenHiringPayload.Offer selectedOffer() {
		if (selectedIndex < 0 || selectedIndex >= offer.offers().size()) {
			return null;
		}

		return offer.offers().get(selectedIndex);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int width = panelWidth() - 44;
		int gap = 8;
		int statWidth = (width - gap) / 2;
		drawStat(
				graphics,
				x,
				panelY + 64,
				statWidth,
				Component.translatable("laborcraft.ui.hire.balance"),
				Component.literal(offer.balanceText())
		);
		drawStat(
				graphics,
				x + statWidth + gap,
				panelY + 64,
				width - statWidth - gap,
				Component.translatable("laborcraft.ui.hire.workers"),
				workersValue()
		);

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.hire.offers"),
				x,
				panelY + 124,
				LcTheme.TEXT
		);

		OpenHiringPayload.Offer selected = selectedOffer();
		Component note;
		if (offer.offers().isEmpty()) {
			note = Component.translatable("laborcraft.ui.hire.no_offers");
		} else if (offer.atLimit()) {
			note = Component.translatable("laborcraft.ui.hire.limit");
		} else if (selected == null || !offer.canAfford(selected)) {
			note = Component.translatable("laborcraft.ui.hire.insufficient");
		} else {
			note = Component.translatable("laborcraft.ui.hire.note");
		}

		int noteColor = offer.atLimit() || selected == null || !offer.canAfford(selected)
				? LcTheme.DANGER
				: LcTheme.MUTED;
		graphics.textWithWordWrap(font, note, x, panelY + 292, width, noteColor);

		if (offer.offers().isEmpty()) {
			graphics.centeredText(
					font,
					Component.translatable("laborcraft.ui.hire.no_offers"),
					x + width / 2,
					panelY + 206,
					LcTheme.MUTED
			);
		}
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

	private void drawStat(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			Component label,
			Component value
	) {
		graphics.fill(x, y, x + width, y + 46, 0x401B232D);
		LcRender.border(graphics, x, y, width, 46, LcTheme.DIVIDER);
		graphics.text(font, label, x + 12, y + 9, LcTheme.MUTED);
		graphics.text(font, value, x + 12, y + 24, LcTheme.TITLE);
	}

	private void drawOffer(
			GuiGraphicsExtractor graphics,
			OpenHiringPayload.Offer candidate,
			int index,
			int x,
			int y,
			int width,
			int height,
			int mouseX,
			int mouseY,
			boolean hovered
	) {
		boolean selected = index == selectedIndex;
		graphics.fill(
				x,
				y,
				x + width,
				y + height - 2,
				selected ? LcTheme.SELECTED_BG : hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG
		);
		LcRender.border(
				graphics,
				x,
				y,
				width,
				height - 2,
				selected || hovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER
		);
		if (selected) {
			graphics.fill(x, y, x + 2, y + height - 2, LcTheme.ACCENT);
		}

		graphics.item(new ItemStack(Items.WHEAT), x + 9, y + 8);

		int textX = x + 32;
		int textWidth = Math.max(40, width - 32 - 96);
		Component personName = Component.literal(font.plainSubstrByWidth(candidate.name(), textWidth / 2));
		Component rankTitle = LcWorkerText.rankTitle(candidate.rankId());
		graphics.text(font, personName, textX, y + 6, LcTheme.TITLE);
		graphics.text(
				font,
				rankTitle,
				textX + font.width(personName) + 8,
				y + 6,
				LcWorkerText.rarityColor(candidate.rarityId())
		);

		List<Component> details = new ArrayList<>();
		details.add(LcWorkerText.rarity(candidate.rarityId()));
		details.add(LcWorkerText.job(""));
		details.add(LcWorkerText.aptitude(candidate.aptitude(), JobTypes.FARMER.id()));
		graphics.text(
				font,
				LcWorkerText.join(details, LcWorkerText.SEPARATOR),
				textX,
				y + 20,
				LcTheme.TEXT
		);

		List<Component> boostLabels = LcWorkerText.boosts(candidate.boosts());
		if (!boostLabels.isEmpty()) {
			graphics.text(
					font,
					LcWorkerText.join(boostLabels, LcWorkerText.SEPARATOR),
					textX,
					y + 34,
					LcTheme.ACCENT
			);
		}

		int priceX = x + width - 12;
		Component price = candidate.free()
				? Component.translatable("laborcraft.ui.hire.free")
				: Component.literal(candidate.priceText());
		graphics.text(font, price, priceX - font.width(price), y + 7, LcTheme.TITLE);

		if (candidate.upkeep() > 0L) {
			Component upkeep = Component.translatable(
					"laborcraft.ui.hire.upkeep",
					Component.literal(candidate.upkeepText())
			);
			graphics.text(font, upkeep, priceX - font.width(upkeep), y + 22, LcTheme.MUTED);
		}
	}
}
