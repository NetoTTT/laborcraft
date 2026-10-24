package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.render.StationPreviewManager;
import com.foque.laborcraft.client.render.WorldHighlightManager;
import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.LcButton;

import com.foque.laborcraft.client.ui.widget.LcScrollList;
import com.foque.laborcraft.client.ui.widget.LcAreaMap;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.network.OpenStationPayload;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Tela do Posto de Trabalho. Aba Trabalho: empregados do jogador e a vaga do posto com as acoes.
 * Aba Config: modo de area (raio livre ou zona) — o desenho do mapa entra depois.
 */
public final class LaborCraftStationScreen extends LaborCraftScreen {
	public enum Tab {
		WORK,
		CONFIG,
		STORAGE
	}

	private final OpenStationPayload station;
	private Tab tab = Tab.WORK;
	private LcScrollList<OpenStationPayload.Summary> workers;
	private int selectedIndex = -1;
	private LcButton primary;
	private LcButton secondary;
	private boolean mapOpen;
	private LcAreaMap areaMap;
	private LcButton confirmMapButton;
	private LcButton previewMapButton;
	private LcButton layerButton;
	private LcAreaMap.Layer mapLayer = LcAreaMap.Layer.AUTO;
	private LcScrollList<OpenStationPayload.StorageView> storageList;
	private LcButton storageRoleButton;
	private LcButton storageUnlinkButton;
	private int selectedStorage = -1;
	private BlockPos selectedStoragePos;
	private static final ItemStack EYE_ICON = new ItemStack(Items.ENDER_EYE);

	public LaborCraftStationScreen(OpenStationPayload station) {
		this(station, null, null);
	}

	public LaborCraftStationScreen(OpenStationPayload station, Tab initialTab) {
		this(station, initialTab, null);
	}

	public LaborCraftStationScreen(OpenStationPayload station, Tab initialTab, BlockPos initialStorage) {
		super(Component.translatable("laborcraft.ui.station.title"), 460, 360);
		this.station = station;
		this.selectedStoragePos = initialStorage;
		if (initialTab != null) {
			this.tab = initialTab;
		} else if (StationPreviewManager.hasDraft(station.pos())) {
			this.tab = Tab.CONFIG;
			this.mapOpen = true;
		}
	}

	public BlockPos stationPos() {
		return station.pos();
	}

	public Tab activeTab() {
		return tab;
	}

	public BlockPos activeStorage() {
		return selectedStoragePos;
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
		LcButton storageTab = new LcButton(x + 196, panelY + 54, 92, 20, Component.translatable("laborcraft.ui.station.tab.storage"),
				tab == Tab.STORAGE ? LcButton.Variant.PRIMARY : LcButton.Variant.GHOST, () -> switchTab(Tab.STORAGE));
		addRenderableWidget(workTab);
		addRenderableWidget(configTab);
		addRenderableWidget(storageTab);

		switch (tab) {
			case WORK -> buildWorkTab(x, width);
			case CONFIG -> buildConfigTab(x, width);
			case STORAGE -> buildStorageTab(x, width);
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
					// A tela nao se reconstroi sozinha: o clique precisa atualizar os botoes na hora.
					updateButtons();
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

		if (mapOpen) {
			StationPreviewManager.Draft draft = StationPreviewManager.getDraft(station.pos());
			BlockPos savedFrom = station.zone() == null ? null : station.zone().from();
			BlockPos savedTo = station.zone() == null ? null : station.zone().to();
			BlockPos selectionFrom;
			BlockPos selectionTo;
			if (draft != null) {
				selectionFrom = draft.from();
				selectionTo = draft.to();
			} else {
				selectionFrom = savedFrom == null ? null : atStationY(savedFrom);
				selectionTo = savedTo == null ? null : atStationY(savedTo);
			}
			areaMap = createAreaMap(x, width, selectionFrom, selectionTo, false);
			addRenderableWidget(areaMap);
			confirmMapButton = new LcButton(x, buttonsY, 100, 24,
					Component.translatable("laborcraft.ui.station.map.confirm"), LcButton.Variant.PRIMARY, this::confirmZone);
			confirmMapButton.active = areaMap.selectionFitsLimit();
			addRenderableWidget(confirmMapButton);
			LcButton cancel = new LcButton(x + 106, buttonsY, 90, 24,
					Component.translatable("laborcraft.ui.station.map.cancel"), LcButton.Variant.GHOST, this::cancelMap);
			addRenderableWidget(cancel);
			previewMapButton = new LcButton(x + 202, buttonsY, 100, 24,
					Component.translatable("laborcraft.ui.station.map.preview"), LcButton.Variant.GHOST, this::previewZone);
			previewMapButton.active = areaMap.selectionFitsLimit();
			addRenderableWidget(previewMapButton);
		} else {
			areaMap = createAreaMap(x, width, null, null, true);
			addRenderableWidget(areaMap);

			// Compact action in the box header; the bottom row stays for Draw / Clear / Close.
			LcButton preview = new LcButton(x + width - 118, panelY + 102, 106, 20,
					Component.translatable("laborcraft.ui.station.map.preview_saved"), LcButton.Variant.GHOST,
					this::previewSavedZone);
			preview.active = !station.ownAreas().isEmpty();
			addRenderableWidget(preview);

			LcButton draw = new LcButton(x, buttonsY, 132, 24,
					Component.translatable("laborcraft.ui.station.map.draw"), LcButton.Variant.PRIMARY, this::openMap);
			addRenderableWidget(draw);
			if (station.zone() != null) {
				LcButton clear = new LcButton(x + 140, buttonsY, 128, 24,
						Component.translatable("laborcraft.ui.station.map.clear"), LcButton.Variant.DANGER,
						this::clearZone);
				addRenderableWidget(clear);
			}
		}

		addLayerButton(x, width);
	}

	private void addLayerButton(int x, int width) {
		layerButton = new LcButton(x + width - 110, panelY + 54, 110, 20,
				areaMap != null ? areaMap.layerLabel() : Component.empty(),
				LcButton.Variant.GHOST, this::cycleLayer);
		layerButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.station.map.layer"),
				Component.translatable("laborcraft.ui.station.map.layer.tip"),
				null
		));
		addRenderableWidget(layerButton);
	}

	private void cycleLayer() {
		if (areaMap == null) {
			return;
		}
		mapLayer = mapLayer.next();
		areaMap.setLayer(mapLayer);
		layerButton.setMessage(areaMap.layerLabel());
	}

	private void buildStorageTab(int x, int width) {
		int buttonsY = panelY + panelHeight() - 50;
		LcButton close = new LcButton(x + width - 96, buttonsY, 96, 24,
				Component.translatable("laborcraft.ui.worker.close"), LcButton.Variant.GHOST, this::onClose);
		addRenderableWidget(close);

		LcButton detect = new LcButton(x + width - 110, panelY + 102, 106, 20,
				Component.translatable("laborcraft.ui.station.storage.detect"), LcButton.Variant.GHOST,
				this::detectStorage);
		detect.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.station.storage.detect"),
				Component.translatable("laborcraft.ui.station.storage.detect.tip"),
				null
		));
		addRenderableWidget(detect);

		storageList = new LcScrollList<>(
				x + 8,
				panelY + 126,
				width - 16,
				140,
				32,
				this::drawStorageRow,
				(view, index) -> {
					selectedStorage = index;
					selectedStoragePos = view.pos();
					updateStorageButtons();
				}
		);
		storageList.replaceItems(station.storage());
		addRenderableWidget(storageList);

		selectedStorage = -1;
		if (selectedStoragePos != null) {
			for (int index = 0; index < station.storage().size(); index++) {
				if (station.storage().get(index).pos().equals(selectedStoragePos)) {
					selectedStorage = index;
					break;
				}
			}
		}

		storageRoleButton = new LcButton(x, buttonsY, 150, 24, Component.empty(),
				LcButton.Variant.SECONDARY, this::cycleStorageRole);
		storageUnlinkButton = new LcButton(x + 158, buttonsY, 120, 24,
				Component.translatable("laborcraft.ui.station.storage.unlink"), LcButton.Variant.DANGER,
				this::unlinkStorage);
		addRenderableWidget(storageRoleButton);
		addRenderableWidget(storageUnlinkButton);
		updateStorageButtons();
	}

	private OpenStationPayload.StorageView selectedStorageView() {
		if (selectedStorage < 0 || selectedStorage >= station.storage().size()) {
			return null;
		}
		return station.storage().get(selectedStorage);
	}

	private void updateStorageButtons() {
		OpenStationPayload.StorageView view = selectedStorageView();
		if (storageRoleButton != null) {
			storageRoleButton.active = view != null;
			storageRoleButton.setMessage(Component.translatable(
					"laborcraft.ui.station.storage.role",
					Component.translatable("laborcraft.storage.role." + (view == null ? "mixed" : view.role()))
			));
		}
		if (storageUnlinkButton != null) {
			storageUnlinkButton.active = view != null;
		}
	}

	private void detectStorage() {
		LaborCraftUiActions.run(LaborCraftCommands.STATION_STORAGE_SCAN + " " + posString());
	}

	private void unlinkStorage() {
		OpenStationPayload.StorageView view = selectedStorageView();
		if (view == null) {
			return;
		}
		LaborCraftUiActions.run(
				LaborCraftCommands.STATION_STORAGE_UNLINK + " " + posString() + " " + coordinates(view.pos())
		);
	}

	private void cycleStorageRole() {
		OpenStationPayload.StorageView view = selectedStorageView();
		if (view == null) {
			return;
		}
		LaborCraftUiActions.run(
				LaborCraftCommands.STATION_STORAGE_ROLE + " " + posString() + " " + coordinates(view.pos())
						+ " " + nextStorageRole(view.role())
		);
	}

	private static String nextStorageRole(String role) {
		return switch (role == null ? "" : role) {
			case "mixed" -> "output";
			case "output" -> "input";
			case "input" -> "tools";
			default -> "mixed";
		};
	}

	private void highlightStorage(int index) {
		if (index < 0 || index >= station.storage().size()) {
			return;
		}
		WorldHighlightManager.highlight(station.storage().get(index).pos(), 0xFFFFC65A);
		this.onClose();
	}

	private LcAreaMap createAreaMap(int x, int width, BlockPos selectionFrom, BlockPos selectionTo, boolean readOnly) {
		BlockPos savedFrom = station.zone() == null ? null : station.zone().from();
		BlockPos savedTo = station.zone() == null ? null : station.zone().to();
		LcAreaMap map = new LcAreaMap(
				x + 12,
				panelY + 132,
				width - 24,
				120,
				station.pos(),
				station.maxRadius(),
				selectionFrom,
				selectionTo,
				savedFrom,
				savedTo,
				station.bound() != null ? station.bound().id() : null,
				station.bound() != null ? station.bound().name() : null,
				station.ownAreas(),
				station.areas(),
				readOnly
		);
		map.setLayer(mapLayer);
		return map;
	}

	private BlockPos atStationY(BlockPos pos) {
		return new BlockPos(pos.getX(), station.pos().getY(), pos.getZ());
	}

	@Override
	protected void clearWidgets() {
		if (areaMap != null) {
			areaMap.close();
			areaMap = null;
		}
		super.clearWidgets();
	}

	@Override
	public void removed() {
		if (areaMap != null) {
			areaMap.close();
			areaMap = null;
		}
		super.removed();
	}

	@Override
	public void onClose() {
		if (areaMap != null) {
			areaMap.close();
			areaMap = null;
		}
		super.onClose();
	}

	private void openMap() {
		mapOpen = true;
		clearWidgets();
		init();
	}

	private void cancelMap() {
		StationPreviewManager.clear(station.pos());
		mapOpen = false;
		clearWidgets();
		init();
	}

	private void confirmZone() {
		if (areaMap == null || !areaMap.selectionFitsLimit()) {
			return;
		}
		BlockPos from = areaMap.from();
		BlockPos to = areaMap.to();
		String command = LaborCraftCommands.STATION_ZONE_SET + " " + posString()
				+ " " + coordinates(from) + " " + coordinates(to);
		LaborCraftUiActions.run(command);
		StationPreviewManager.clear(station.pos());
	}

	private void previewZone() {
		if (areaMap == null || !areaMap.selectionFitsLimit()) {
			return;
		}
		BlockPos from = areaMap.from();
		BlockPos to = areaMap.to();
		StationPreviewManager.startPreview(
				station.pos(),
				from,
				to,
				station.verticalRadius(),
				station.areas(),
				station.otherPosts()
		);
		this.onClose();
	}

	private void previewSavedZone() {
		StationPreviewManager.startSavedPreview(
				station.pos(),
				station.ownAreas(),
				station.verticalRadius()
		);
		this.onClose();
	}

	private void clearZone() {
		LaborCraftUiActions.run(LaborCraftCommands.STATION_ZONE_CLEAR + " " + posString());
	}

	private void updateMapButton() {
		if (confirmMapButton != null && areaMap != null) {
			confirmMapButton.active = areaMap.selectionFitsLimit();
		}
		if (previewMapButton != null && areaMap != null) {
			previewMapButton.active = areaMap.selectionFitsLimit();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (tab == Tab.STORAGE && storageList != null) {
			int index = storageList.rowAt(event.x(), event.y());
			if (index >= 0) {
				int rowY = storageList.rowTop(index);
				int eyeX = storageList.getX() + storageList.rowWidth() - 24;
				int eyeY = rowY + 6;
				if (event.x() >= eyeX && event.x() < eyeX + 18
						&& event.y() >= eyeY && event.y() < eyeY + 18) {
					highlightStorage(index);
					return true;
				}
			}
		}

		boolean handled = super.mouseClicked(event, doubleClick);
		updateMapButton();
		return handled;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = super.mouseReleased(event);
		updateMapButton();
		return handled;
	}

	private static String coordinates(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
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

		switch (tab) {
			case WORK -> drawWorkTab(graphics, x, width);
			case CONFIG -> drawConfigTab(graphics, x, width);
			case STORAGE -> drawStorageTab(graphics, x, width);
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

		if (station.workers().isEmpty()) {
			graphics.textWithWordWrap(
					font,
					Component.translatable("laborcraft.ui.station.workers.empty"),
					x + 10,
					panelY + 168,
					188,
					LcTheme.MUTED
			);
		}

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
		if (layerButton != null && areaMap != null) {
			layerButton.setMessage(areaMap.layerLabel());
		}

		int boxY = panelY + 96;
		graphics.fill(x, boxY, x + width, boxY + 190, 0x401B232D);
		LcRender.border(graphics, x, boxY, width, 190, LcTheme.DIVIDER);

		if (mapOpen) {
			graphics.text(font, Component.translatable("laborcraft.ui.station.map.title"),
					x + 12, boxY + 8, LcTheme.TITLE);
			graphics.text(font, Component.translatable("laborcraft.ui.station.map.hint"),
					x + 12, boxY + 22, LcTheme.MUTED);

			if (areaMap != null) {
				if (areaMap.hasSelection()) {
					int blocks = areaMap.selectionWidth() * areaMap.selectionDepth();
					int color = areaMap.selectionFitsLimit() ? LcTheme.ACCENT : LcTheme.DANGER;
					graphics.text(font, Component.translatable(
							"laborcraft.ui.station.map.size",
							areaMap.selectionWidth(), areaMap.selectionDepth(), blocks,
							station.pos().getY() - station.verticalRadius(),
							station.pos().getY() + station.verticalRadius()
					), x + 12, boxY + 162, color);
				} else {
					graphics.text(font, Component.translatable("laborcraft.ui.station.map.no_selection"),
							x + 12, boxY + 162, LcTheme.MUTED);
				}
				if (confirmMapButton != null) {
					confirmMapButton.active = areaMap.selectionFitsLimit();
				}
				if (previewMapButton != null) {
					previewMapButton.active = areaMap.selectionFitsLimit();
				}
			}

			graphics.text(font, Component.translatable("laborcraft.ui.station.map.legend"),
					x + 12, boxY + 176, LcTheme.MUTED);
		} else {
			graphics.text(font, Component.translatable("laborcraft.ui.station.mode"), x + 12, boxY + 8, LcTheme.MUTED);
			int diameter = station.maxRadius() * 2 + 1;
			Component mode = station.zone() == null
					? Component.translatable("laborcraft.ui.station.mode.free_radius", diameter, diameter)
					: Component.translatable("laborcraft.ui.station.mode.zone", station.zone().width(), station.zone().depth());
			graphics.text(font, mode, x + 12, boxY + 22, LcTheme.TITLE);

			graphics.text(font, Component.translatable("laborcraft.ui.station.config.vertical",
					station.verticalRadius()), x + 12, boxY + 162, LcTheme.MUTED);
			graphics.text(font, Component.translatable("laborcraft.ui.station.map.legend"),
					x + 12, boxY + 176, LcTheme.MUTED);
		}
	}

	private void drawStorageTab(GuiGraphicsExtractor graphics, int x, int width) {
		int boxY = panelY + 96;
		graphics.fill(x, boxY, x + width, boxY + 190, 0x401B232D);
		LcRender.border(graphics, x, boxY, width, 190, LcTheme.DIVIDER);

		graphics.text(font, Component.translatable(
				"laborcraft.ui.station.storage.title",
				station.storage().size()
		), x + 12, boxY + 8, LcTheme.TITLE);

		if (station.storage().isEmpty()) {
			graphics.centeredText(
					font,
					Component.translatable("laborcraft.ui.station.storage.empty"),
					x + width / 2,
					boxY + 82,
					LcTheme.MUTED
			);
		}

		graphics.text(font, Component.translatable("laborcraft.ui.station.storage.hint"),
				x + 12, boxY + 176, LcTheme.MUTED);
	}

	private void drawStorageRow(
			GuiGraphicsExtractor graphics,
			OpenStationPayload.StorageView view,
			int index,
			int x,
			int y,
			int width,
			int height,
			int mouseX,
			int mouseY,
			boolean hovered
	) {
		boolean selected = index == selectedStorage;
		int background = selected
				? LcTheme.SELECTED_BG
				: (hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG);
		graphics.fill(x, y, x + width, y + height - 2, background);
		LcRender.border(graphics, x, y, width, height - 2, selected ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);

		Component role = Component.translatable("laborcraft.storage.role." + view.role());
		int roleColor = switch (view.role()) {
			case "output" -> 0xFF48D7A0;
			case "input" -> 0xFF4C8BF5;
			case "tools" -> 0xFFFFC65A;
			default -> LcTheme.TEXT;
		};
		graphics.text(font, role, x + 8, y + 5, roleColor);

		if (view.valid()) {
			graphics.text(
					font,
					Component.translatable(view.blockKey()),
					x + 16 + font.width(role),
					y + 5,
					LcTheme.TITLE
			);
		} else {
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.station.storage.invalid"),
					x + 16 + font.width(role),
					y + 5,
					LcTheme.DANGER
			);
		}

		graphics.text(
				font,
				Component.literal(
						view.pos().getX() + ", " + view.pos().getY() + ", " + view.pos().getZ()
								+ " · " + view.distance() + " bl"
				),
				x + 8,
				y + 18,
				LcTheme.MUTED
		);

		int eyeX = x + width - 24;
		int eyeY = y + 6;
		boolean eyeHovered = mouseX >= eyeX && mouseX < eyeX + 18 && mouseY >= eyeY && mouseY < eyeY + 18;
		graphics.fill(eyeX, eyeY, eyeX + 18, eyeY + 18, eyeHovered ? 0x66354250 : 0x44111820);
		LcRender.border(graphics, eyeX, eyeY, 18, 18, eyeHovered ? LcTheme.ACCENT : 0xFF354250);
		graphics.item(EYE_ICON, eyeX + 1, eyeY + 1);
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
