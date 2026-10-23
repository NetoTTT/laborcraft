package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.ui.widget.LcScrollList;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.network.OpenStationPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Tela do Posto de Trabalho. Aba Trabalho: empregados do jogador e a vaga do posto com as acoes.
 * Aba Config: modo de area (raio livre ou zona) — o desenho do mapa entra depois.
 */
public final class LaborCraftStationScreen extends LaborCraftScreen {
	private enum Tab {
		WORK,
		CONFIG
	}

	private final OpenStationPayload station;
	private Tab tab = Tab.WORK;
	private LcScrollList<OpenStationPayload.Summary> workers;
	private int selectedIndex = -1;
	private LcButton primary;
	private LcButton secondary;

	public LaborCraftStationScreen(OpenStationPayload station) {
		super(Component.translatable("laborcraft.ui.station.title"), 460, 360);
		this.station = station;
	}

	@Override
	protected Component subtitle() {
		return Component.translatable(
				"laborcraft.ui.station.subtitle",
				station.pos().getX(),
				station.pos().getY(),
				station.pos().getZ()
		);
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;

		LcButton workTab = new LcButton(x, panelY + 54, 92, 20, Component.translatable("laborcraft.ui.station.tab.work"),
				tab == Tab.WORK ? LcButton.Variant.PRIMARY : LcButton.Variant.GHOST, () -> switchTab(Tab.WORK));
		LcButton configTab = new LcButton(x + 98, panelY + 54, 92, 20, Component.translatable("laborcraft.ui.station.tab.config"),
				tab == Tab.CONFIG ? LcButton.Variant.PRIMARY : LcButton.Variant.GHOST, () -> switchTab(Tab.CONFIG));
		addRenderableWidget(workTab);
		addRenderableWidget(configTab);

		if (tab == Tab.WORK) {
			buildWorkTab(x, width);
		} else {
			buildConfigTab(x, width);
		}
	}

	private void switchTab(Tab next) {
		if (tab == next) {
			return;
		}

		tab = next;
		clearWidgets();
		init();
	}

	private void buildWorkTab(int x, int width) {
		int listWidth = 208;
		int listY = panelY + 96;

		workers = new LcScrollList<>(
				x,
				listY,
				listWidth,
				190,
				36,
				this::drawWorker,
				(summary, index) -> {
					selectedIndex = index;
				}
		);
		workers.replaceItems(station.workers());
		addRenderableWidget(workers);

		int buttonsY = panelY + panelHeight() - 50;
		primary = new LcButton(x, buttonsY, 170, 24, Component.empty(), LcButton.Variant.PRIMARY, this::primaryAction);
		secondary = new LcButton(x + 178, buttonsY, 120, 24, Component.empty(), LcButton.Variant.GHOST, this::secondaryAction);
		LcButton close = new LcButton(x + width - 96, buttonsY, 96, 24,
				Component.translatable("laborcraft.ui.worker.close"), LcButton.Variant.GHOST, this::onClose);
		addRenderableWidget(primary);
		addRenderableWidget(secondary);
		addRenderableWidget(close);
		updateButtons();
	}

	private void buildConfigTab(int x, int width) {
		int buttonsY = panelY + panelHeight() - 50;
		LcButton close = new LcButton(x + width - 96, buttonsY, 96, 24,
				Component.translatable("laborcraft.ui.worker.close"), LcButton.Variant.GHOST, this::onClose);
		addRenderableWidget(close);
	}

	private void updateButtons() {
		if (primary == null || secondary == null) {
			return;
		}

		OpenStationPayload.Summary selected = selectedWorker();
		boolean bound = station.bound() != null;

		if (bound) {
			primary.setMessage(Component.translatable("laborcraft.ui.station.unbind"));
			primary.active = true;
			primary.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.station.unbind"),
					Component.translatable("laborcraft.ui.station.unbind.tip"),
					"/" + LaborCraftCommands.STATION_UNBIND + " " + posString()
			));
		} else {
			primary.setMessage(selected == null
					? Component.translatable("laborcraft.ui.station.bind.none")
					: Component.translatable("laborcraft.ui.station.bind.selected", selected.name()));
			primary.active = selected != null && !selected.boundElsewhere();
			primary.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.station.bind"),
					selected != null && selected.boundElsewhere()
							? Component.translatable("laborcraft.ui.station.bind.bound_elsewhere")
							: Component.translatable("laborcraft.ui.station.bind.tip"),
					selected == null ? null : "/" + LaborCraftCommands.STATION_BIND + " " + posString() + " " + selected.id()
			));
		}

		secondary.active = bound;
		if (!bound) {
			secondary.setMessage(Component.translatable("laborcraft.ui.station.idle"));
			secondary.tip(null);
			return;
		}

		boolean paused = station.bound() != null && station.bound().idle();
		secondary.setMessage(Component.translatable(paused
				? "laborcraft.ui.station.resume"
				: "laborcraft.ui.station.recall"));
		secondary.tip(LcTooltips.of(
				Component.translatable(paused ? "laborcraft.ui.station.resume" : "laborcraft.ui.station.recall"),
				Component.translatable(paused
						? "laborcraft.ui.station.resume.tip"
						: "laborcraft.ui.station.recall.tip"),
				"/" + (paused ? LaborCraftCommands.STATION_RESUME : LaborCraftCommands.STATION_RECALL) + " " + posString()
		));
	}

	private void primaryAction() {
		if (station.bound() != null) {
			LaborCraftUiActions.run(LaborCraftCommands.STATION_UNBIND + " " + posString());
			return;
		}

		OpenStationPayload.Summary selected = selectedWorker();
		if (selected != null) {
			LaborCraftUiActions.run(LaborCraftCommands.STATION_BIND + " " + posString() + " " + selected.id());
		}
	}

	private void secondaryAction() {
		if (station.bound() == null) {
			return;
		}

		boolean paused = station.bound().idle();
		LaborCraftUiActions.run(
				(paused ? LaborCraftCommands.STATION_RESUME : LaborCraftCommands.STATION_RECALL) + " " + posString()
		);
	}

	private String posString() {
		return station.pos().getX() + " " + station.pos().getY() + " " + station.pos().getZ();
	}

	private OpenStationPayload.Summary selectedWorker() {
		if (selectedIndex < 0 || selectedIndex >= station.workers().size()) {
			return null;
		}

		return station.workers().get(selectedIndex);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int width = panelWidth() - 44;

		if (tab == Tab.WORK) {
			drawWorkTab(graphics, x, width);
		} else {
			drawConfigTab(graphics, x, width);
		}
	}

	private void drawWorkTab(GuiGraphicsExtractor graphics, int x, int width) {
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.station.workers", station.workers().size()),
				x,
				panelY + 82,
				LcTheme.TEXT
		);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.station.slot", station.bound() == null ? 0 : 1),
				x + 222,
				panelY + 82,
				LcTheme.TEXT
		);

		int cardX = x + 222;
		int cardY = panelY + 96;
		int cardWidth = width - 222;

		graphics.fill(cardX, cardY, cardX + cardWidth, cardY + 190, 0x401B232D);
		LcRender.border(graphics, cardX, cardY, cardWidth, 190,
				station.bound() == null ? LcTheme.DIVIDER : LcTheme.ACCENT);

		OpenStationPayload.Summary bound = station.bound();
		if (bound == null) {
			graphics.text(font, Component.translatable("laborcraft.ui.station.vacant"), cardX + 12, cardY + 12, LcTheme.MUTED);
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.station.vacant.tip"),
					cardX + 12,
					cardY + 32,
					cardWidth - 24,
					LcTheme.MUTED
			);
			return;
		}

		graphics.text(font, Component.literal(bound.name()), cardX + 12, cardY + 12, LcTheme.TITLE);
		graphics.text(
				font,
				LcWorkerText.join(
						java.util.List.of(
								LcWorkerText.rank(bound.rankId()),
								LcWorkerText.job(bound.jobId())
						),
						LcWorkerText.SEPARATOR
				),
				cardX + 12,
				cardY + 28,
				LcTheme.TEXT
		);
		graphics.text(font, stateLabel(bound), cardX + 12, cardY + 44, LcTheme.ACCENT);
		graphics.text(
				font,
				Component.translatable(bound.taskKey()),
				cardX + 12,
				cardY + 60,
				LcTheme.MUTED
		);
	}

	private void drawConfigTab(GuiGraphicsExtractor graphics, int x, int width) {
		int boxY = panelY + 96;
		graphics.fill(x, boxY, x + width, boxY + 190, 0x401B232D);
		LcRender.border(graphics, x, boxY, width, 190, LcTheme.DIVIDER);

		graphics.text(font, Component.translatable("laborcraft.ui.station.mode"), x + 12, boxY + 12, LcTheme.MUTED);
		Component mode = station.zone() == null
				? Component.translatable("laborcraft.ui.station.mode.free")
				: Component.translatable("laborcraft.ui.station.mode.zone", station.zone().width(), station.zone().depth());
		graphics.text(font, mode, x + 12, boxY + 28, LcTheme.TITLE);

		graphics.textWithWordWrap(
				font,
				Component.translatable("laborcraft.ui.station.config.soon"),
				x + 12,
				boxY + 52,
				width - 24,
				LcTheme.MUTED
		);

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.station.config.commands"),
				x + 12,
				boxY + 84,
				LcTheme.MUTED
		);
	}

	private void drawWorker(
			GuiGraphicsExtractor graphics,
			OpenStationPayload.Summary worker,
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
		boolean bound = station.bound() != null && station.bound().id().equals(worker.id());

		graphics.fill(
				x,
				y,
				x + width,
				y + height - 2,
				selected ? LcTheme.SELECTED_BG : bound ? LcTheme.PRIMARY_BG : hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG
		);
		LcRender.border(
				graphics,
				x,
				y,
				width,
				height - 2,
				selected || bound ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER
		);

		graphics.text(font, Component.literal(worker.name()), x + 10, y + 6, LcTheme.TITLE);

		Component detail = LcWorkerText.join(
				java.util.List.of(
						LcWorkerText.rank(worker.rankId()),
						LcWorkerText.job(worker.jobId())
				),
				LcWorkerText.SEPARATOR
		);
		graphics.text(font, detail, x + 10, y + 20, LcTheme.TEXT);

		if (worker.boundElsewhere()) {
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.station.bound_elsewhere"),
					x + width - 10 - font.width(Component.translatable("laborcraft.ui.station.bound_elsewhere")),
					y + 6,
					LcTheme.DANGER
			);
		}
	}

	private static Component stateLabel(OpenStationPayload.Summary summary) {
		return Component.translatable("laborcraft.ui.worker.state." + summary.state().toLowerCase(Locale.ROOT));
	}
}
