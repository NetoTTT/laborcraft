package com.foque.laborcraft.client.ui.screen;

import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.INFO;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;
import static com.quillui.theme.QuillThemeData.ColorRole.SUCCESS;
import static com.quillui.theme.QuillThemeData.ColorRole.TITLE;

import com.foque.laborcraft.client.render.StationPreviewManager;
import com.foque.laborcraft.client.render.WorldHighlightManager;
import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.WorkerListFilter;
import com.foque.laborcraft.client.ui.widget.LcAreaMap;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.network.OpenStationPayload;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.theme.QuillThemeData;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillLabel;
import com.quillui.widget.QuillListView;
import com.quillui.widget.QuillNodes;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Workstation window rebuilt on the measured Quill tree.
 *
 * <p>Work: employee list with real Pay/highlight actions, the post slot card and the bind/recall
 * bar. Config: work area summary plus the interactive map. Storage: linked containers with role
 * and unlink actions. The legacy {@code LaborCraftStationScreen} stays available until this screen
 * has been smoke-tested in game.</p>
 */
public final class LaborCraftStationV2Screen extends QuillScreen {
	public enum Tab {
		WORK,
		CONFIG,
		STORAGE
	}

	private static final int PANEL_WIDTH = 480;
	private static final int PANEL_HEIGHT = 400;
	private static final int WORKER_LIST_WIDTH = 208;
	private static final int MAP_HEIGHT = 170;
	private static final ItemStack EYE_ICON = new ItemStack(Items.ENDER_EYE);

	private static WorkerListFilter pendingFilter;
	private static UUID pendingWorker;
	private static BlockPos pendingStorage;

	private final OpenStationPayload station;
	private Tab tab;
	private WorkerListFilter filter;
	private UUID selectedWorkerId;
	private BlockPos selectedStoragePos;
	private boolean mapOpen;
	private LcAreaMap areaMap;
	private QuillListView<OpenStationPayload.Summary> workerList;
	private QuillListView<OpenStationPayload.StorageView> storageList;
	private LcAreaMap.Layer mapLayer = LcAreaMap.Layer.AUTO;
	private QuillLabel selectionStatus;
	private QuillButton confirmMapButton;
	private QuillButton previewMapButton;
	private int workerScroll;
	private int storageScroll;

	public LaborCraftStationV2Screen(OpenStationPayload station) {
		this(station, null, null);
	}

	public LaborCraftStationV2Screen(OpenStationPayload station, Tab initialTab) {
		this(station, initialTab, null);
	}

	public LaborCraftStationV2Screen(OpenStationPayload station, Tab initialTab, BlockPos initialStorage) {
		super(Component.translatable("laborcraft.ui.station.title"), PANEL_WIDTH, PANEL_HEIGHT);
		this.station = station;
		this.tab = initialTab == null ? Tab.WORK : initialTab;
		this.filter = pendingFilter == null ? WorkerListFilter.ALL : pendingFilter;
		this.selectedWorkerId = pendingWorker;
		this.selectedStoragePos = initialStorage == null ? pendingStorage : initialStorage;
		this.mapOpen = StationPreviewManager.hasDraft(station.pos());

		pendingFilter = null;
		pendingWorker = null;
		pendingStorage = null;
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
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(QuillNodes.tabs(
				theme(),
				6,
				List.of(
						Component.translatable("laborcraft.ui.station.tab.work"),
						Component.translatable("laborcraft.ui.station.tab.config"),
						Component.translatable("laborcraft.ui.station.tab.storage")
				),
				tab.ordinal(),
				index -> switchTab(Tab.values()[index])
		));

		switch (tab) {
			case WORK -> {
				root.add(workHeader());
				root.add(QuillNodes.flexible(workBody(), 1));
				root.add(workActions());
			}
			case CONFIG -> {
				if (mapOpen) {
					areaMap = createAreaMap(false);
					root.add(configMapHeader());
					root.add(configMap());
				}
				else {
					root.add(configSummary());
					root.add(QuillNodes.flexible(QuillNodes.spacer()));
				}
				root.add(configActions());
			}
			case STORAGE -> {
				root.add(storageHeader());
				root.add(QuillNodes.flexible(storageBody(), 1));
				root.add(storageActions());
			}
		}

		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private void switchTab(Tab next) {
		if (tab == next) {
			return;
		}
		tab = next;
		refreshContent();
	}

	// --- Work tab ---------------------------------------------------------------------------

	private QuillFlexBox workHeader() {
		QuillFlexBox header = QuillNodes.row(QuillSpacing.SPACE_2);
		header.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		header.add(control(label(Component.translatable(
				"laborcraft.ui.station.workers",
				station.workers().size()
		))));

		QuillButton filterButton = new QuillButton(
				0, 0, 104, 18,
				Component.translatable(filter.labelKey()),
				QuillButton.Variant.GHOST,
				this::cycleFilter
		);
		filterButton.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.filter.label"),
				Component.translatable("laborcraft.ui.filter.tip"),
				null
		));
		header.add(QuillNodes.widget(filterButton, 104, 18));
		header.add(QuillNodes.flexible(QuillNodes.spacer()));
		header.add(control(label(Component.translatable(
				"laborcraft.ui.station.slot",
				station.bound() == null ? 0 : 1
		))));
		return header;
	}

	private QuillLayoutNode workBody() {
		QuillFlexBox body = QuillNodes.row(QuillSpacing.SPACE_3);
		body.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);

		QuillListView<OpenStationPayload.Summary> list = new QuillListView<>(
				0, 0, WORKER_LIST_WIDTH, 180, 40,
				this::workerRow,
				(summary, index) -> {
					workerScroll = workerList == null ? 0 : workerList.scrollOffset();
					selectedWorkerId = summary.id();
					refreshContent();
				},
				theme()
		);
		list.replaceItems(visibleWorkers());
		list.setSelected(selectedWorker());
		list.setScrollOffset(workerScroll);
		workerList = list;
		body.add(QuillNodes.fixedWidth(WORKER_LIST_WIDTH, control(list)));

		body.add(QuillNodes.flexible(slotCard()));
		return body;
	}

	private QuillCardBox slotCard() {
		QuillFlexBox column = QuillNodes.column(QuillSpacing.SPACE_1);
		OpenStationPayload.Summary bound = station.bound();
		if (bound == null) {
			column.add(control(label(Component.translatable(
					"laborcraft.ui.station.vacant"
			)).muted()));
			column.add(control(label(Component.translatable(
					"laborcraft.ui.station.vacant.tip"
			)).muted()));
			return new QuillCardBox(Component.translatable("laborcraft.ui.station.slot", 0), column);
		}

		column.add(control(label(Component.literal(bound.name())).title()));
		column.add(control(label(LcWorkerText.join(
				List.of(LcWorkerText.rank(bound.rankId()), LcWorkerText.job(bound.jobId())),
				LcWorkerText.SEPARATOR
		))));
		column.add(control(label(stateLabel(bound)).color(bound.idle() ? MUTED : ACCENT)));
		column.add(control(label(Component.translatable(bound.taskKey())).muted()));
		if (bound.hasDebt()) {
			column.add(control(label(Component.translatable(
					"laborcraft.ui.station.debt",
					bound.debtText()
			)).color(DANGER)));
		}
		return new QuillCardBox(Component.translatable("laborcraft.ui.station.slot", 1), column);
	}

	private QuillLayoutNode workerRow(OpenStationPayload.Summary worker, int index) {
		boolean bound = station.bound() != null && station.bound().id().equals(worker.id());
		QuillFlexBox row = QuillNodes.row(6);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);

		QuillFlexBox identity = QuillNodes.column(2);
		identity.add(control(label(Component.literal(worker.name())).title()));
		identity.add(control(label(LcWorkerText.join(
				List.of(LcWorkerText.rank(worker.rankId()), LcWorkerText.job(worker.jobId())),
				LcWorkerText.SEPARATOR
		)).muted()));
		if (bound) {
			identity.add(control(label(stateLabel(worker)).color(ACCENT)));
		}
		row.add(QuillNodes.flexible(identity));

		if (worker.boundElsewhere()) {
			row.add(control(label(Component.translatable(
					"laborcraft.ui.station.bound_elsewhere"
			)).color(DANGER)));
		}

		if (worker.hasDebt()) {
			QuillButton pay = new QuillButton(
					0, 0, 46, 16,
					Component.translatable("laborcraft.ui.employee.pay"),
					QuillButton.Variant.SECONDARY,
					() -> payWorker(worker)
			);
			pay.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.employee.pay"),
					Component.translatable("laborcraft.ui.employee.pay.tip"),
					"/" + LaborCraftCommands.WORKER_PAY_STATION + " " + worker.id() + " " + posString()
			));
			row.add(QuillNodes.widget(pay, 46, 16));
		}

		QuillButton eye = new QuillButton(
				0, 0, 18, 18,
				Component.literal("E"),
				QuillButton.Variant.GHOST,
				() -> highlightWorker(worker.id())
		);
		eye.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.highlight"),
				Component.translatable("laborcraft.ui.station.highlight.tip"),
				null
		));
		row.add(QuillNodes.widget(eye, 18, 18));
		return row;
	}

	private QuillFlexBox workActions() {
		QuillFlexBox bar = QuillNodes.row(QuillSpacing.SPACE_2);
		OpenStationPayload.Summary bound = station.bound();
		OpenStationPayload.Summary selected = selectedWorker();

		Component primaryLabel;
		Runnable primaryAction;
		boolean primaryEnabled;
		Component primaryTip;
		if (bound != null) {
			primaryLabel = Component.translatable("laborcraft.ui.station.unbind");
			primaryAction = () -> LaborCraftUiActions.run(
					LaborCraftCommands.STATION_UNBIND + " " + posString()
			);
			primaryEnabled = true;
			primaryTip = Component.translatable("laborcraft.ui.station.unbind.tip");
		}
		else {
			primaryLabel = selected == null
					? Component.translatable("laborcraft.ui.station.bind")
					: Component.translatable("laborcraft.ui.station.bind.selected", selected.name());
			primaryAction = () -> bind(selected);
			primaryEnabled = selected != null && !selected.boundElsewhere();
			primaryTip = selected != null && selected.boundElsewhere()
					? Component.translatable("laborcraft.ui.station.bind.bound_elsewhere")
					: Component.translatable("laborcraft.ui.station.bind.tip");
		}

		QuillButton primary = new QuillButton(
				0, 0, 170, 24,
				primaryLabel,
				QuillButton.Variant.PRIMARY,
				primaryAction
		).enabled(primaryEnabled);
		primary.tip(QuillTooltips.of(
				Component.translatable(bound == null
						? "laborcraft.ui.station.bind"
						: "laborcraft.ui.station.unbind"),
				primaryTip,
				null
		));
		bar.add(QuillNodes.flexible(QuillNodes.widget(primary, 170, 24), 1));

		boolean paused = bound != null && bound.idle();
		QuillButton secondary = new QuillButton(
				0, 0, 120, 24,
				Component.translatable(bound == null
						? "laborcraft.ui.station.idle"
						: paused
								? "laborcraft.ui.station.resume"
								: "laborcraft.ui.station.recall"),
				QuillButton.Variant.SECONDARY,
				this::secondaryAction
		).enabled(bound != null);
		secondary.tip(QuillTooltips.of(
				Component.translatable(bound == null
						? "laborcraft.ui.station.idle"
						: paused
								? "laborcraft.ui.station.resume"
								: "laborcraft.ui.station.recall"),
				Component.translatable(bound == null
						? "laborcraft.ui.station.recall.tip"
						: paused
								? "laborcraft.ui.station.resume.tip"
								: "laborcraft.ui.station.recall.tip"),
				null
		));
		bar.add(QuillNodes.widget(secondary, 120, 24));
		return bar;
	}

	private void cycleFilter() {
		filter = filter.next();
		pendingFilter = filter;
		workerScroll = 0;
		selectedWorkerId = null;
		refreshContent();
	}

	private List<OpenStationPayload.Summary> visibleWorkers() {
		return station.workers().stream()
				.filter(summary -> filter.matches(summary.state(), summary.hasDebt()))
				.toList();
	}

	private OpenStationPayload.Summary selectedWorker() {
		if (selectedWorkerId == null) {
			return null;
		}
		for (OpenStationPayload.Summary summary : visibleWorkers()) {
			if (summary.id().equals(selectedWorkerId)) {
				return summary;
			}
		}
		return null;
	}

	private void bind(OpenStationPayload.Summary selected) {
		if (selected == null || selected.boundElsewhere()) {
			return;
		}
		pendingFilter = filter;
		pendingWorker = selected.id();
		LaborCraftUiActions.run(
				LaborCraftCommands.STATION_BIND + " " + posString() + " " + selected.id()
		);
	}

	private void payWorker(OpenStationPayload.Summary worker) {
		pendingFilter = filter;
		pendingWorker = worker.id();
		LaborCraftUiActions.run(
				LaborCraftCommands.WORKER_PAY_STATION + " " + worker.id() + " " + posString()
		);
	}

	private void secondaryAction() {
		OpenStationPayload.Summary bound = station.bound();
		if (bound == null) {
			return;
		}
		pendingFilter = filter;
		pendingWorker = bound.id();
		LaborCraftUiActions.run(
				(bound.idle() ? LaborCraftCommands.STATION_RESUME : LaborCraftCommands.STATION_RECALL)
						+ " " + posString()
		);
	}

	private void highlightWorker(UUID id) {
		Minecraft client = Minecraft.getInstance();
		boolean found = WorldHighlightManager.highlightEntity(id, 0xFF48D7A0, EYE_ICON);
		onClose();
		if (!found && client.gui != null) {
			client.gui.hud.setOverlayMessage(
					Component.translatable("laborcraft.ui.highlight.missing"),
					false
			);
		}
	}

	// --- Config tab -------------------------------------------------------------------------

	private QuillFlexBox configMapHeader() {
		QuillFlexBox header = QuillNodes.row(QuillSpacing.SPACE_2);
		header.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		header.add(control(label(Component.translatable(
				"laborcraft.ui.station.map.title"
		)).title()));
		header.add(QuillNodes.flexible(QuillNodes.spacer()));
		header.add(QuillNodes.widget(layerButton(), 110, 18));
		return header;
	}

	private QuillCardBox configMap() {
		QuillFlexBox column = QuillNodes.column(QuillSpacing.SPACE_1);
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.map.hint"
		)).muted()));
		column.add(QuillNodes.fixedHeight(MAP_HEIGHT, control(areaMap)));
		selectionStatus = label(selectionLabel());
		column.add(control(selectionStatus));
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.map.legend"
		)).muted()));
		return new QuillCardBox(null, column);
	}

	private QuillCardBox configSummary() {
		QuillFlexBox column = QuillNodes.column(QuillSpacing.SPACE_1);
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.mode"
		)).muted()));

		int diameter = station.maxRadius() * 2 + 1;
		Component mode = station.zone() == null
				? Component.translatable("laborcraft.ui.station.mode.free_radius", diameter, diameter)
				: Component.translatable(
						"laborcraft.ui.station.mode.zone",
						station.zone().width(),
						station.zone().depth()
				);
		column.add(control(label(mode).color(station.zone() == null ? DANGER : TITLE)));
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.config.map_hint"
		)).muted()));
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.config.vertical",
				station.verticalRadius(),
				station.verticalRadius()
		)).muted()));
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.action",
				station.verticalRadius(),
				station.verticalRadius()
		)).muted()));
		column.add(control(label(Component.translatable(
				"laborcraft.ui.station.map.legend"
		)).muted()));

		if (station.zone() != null) {
			QuillButton preview = new QuillButton(
					0, 0, 130, 20,
					Component.translatable("laborcraft.ui.station.map.preview_saved"),
					QuillButton.Variant.SECONDARY,
					this::previewSavedZone
			);
			preview.enabled(!station.ownAreas().isEmpty());
			preview.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.station.map.preview_saved"),
					Component.translatable("laborcraft.ui.station.map.preview"),
					null
			));
			column.add(QuillNodes.widget(preview, 130, 20));
		}

		return new QuillCardBox(Component.translatable("laborcraft.ui.station.mode"), column);
	}

	private QuillFlexBox configActions() {
		QuillFlexBox bar = QuillNodes.row(QuillSpacing.SPACE_2);

		if (mapOpen) {
			confirmMapButton = new QuillButton(
					0, 0, 100, 24,
					Component.translatable("laborcraft.ui.station.map.confirm"),
					QuillButton.Variant.PRIMARY,
					this::confirmZone
			).enabled(areaMap != null && areaMap.selectionFitsLimit());
			bar.add(QuillNodes.flexible(QuillNodes.widget(confirmMapButton, 100, 24), 1));

			bar.add(QuillNodes.widget(new QuillButton(
					0, 0, 90, 24,
					Component.translatable("laborcraft.ui.station.map.cancel"),
					QuillButton.Variant.GHOST,
					this::cancelMap
			), 90, 24));

			previewMapButton = new QuillButton(
					0, 0, 100, 24,
					Component.translatable("laborcraft.ui.station.map.preview"),
					QuillButton.Variant.SECONDARY,
					this::previewZone
			).enabled(areaMap != null && areaMap.selectionFitsLimit());
			bar.add(QuillNodes.widget(previewMapButton, 100, 24));
		}
		else {
			QuillButton draw = new QuillButton(
					0, 0, 132, 24,
					Component.translatable("laborcraft.ui.station.map.draw"),
					QuillButton.Variant.PRIMARY,
					this::openMap
			);
			bar.add(QuillNodes.flexible(QuillNodes.widget(draw, 132, 24), 1));

			if (station.zone() != null) {
				bar.add(QuillNodes.widget(new QuillButton(
						0, 0, 128, 24,
						Component.translatable("laborcraft.ui.station.map.clear"),
						QuillButton.Variant.DANGER,
						this::clearZone
				), 128, 24));
			}
			else {
				QuillButton maxArea = new QuillButton(
						0, 0, 168, 24,
						Component.translatable("laborcraft.ui.station.map.max_area"),
						QuillButton.Variant.SECONDARY,
						this::useMaxArea
				);
				maxArea.tip(QuillTooltips.of(
						Component.translatable("laborcraft.ui.station.map.max_area"),
						Component.translatable("laborcraft.ui.station.map.max_area.tip"),
						null
				));
				bar.add(QuillNodes.widget(maxArea, 168, 24));
			}
		}

		return bar;
	}

	private QuillButton layerButton() {
		QuillButton layer = new QuillButton(
				0, 0, 110, 18,
				areaMap == null ? Component.empty() : areaMap.layerLabel(),
				QuillButton.Variant.GHOST,
				this::cycleLayer
		);
		layer.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.map.layer"),
				Component.translatable("laborcraft.ui.station.map.layer.tip"),
				null
		));
		return layer;
	}

	private void cycleLayer() {
		if (areaMap == null) {
			return;
		}
		mapLayer = mapLayer.next();
		areaMap.setLayer(mapLayer);
		refreshContent();
	}

	private Component selectionLabel() {
		if (areaMap == null || !areaMap.hasSelection()) {
			return Component.translatable("laborcraft.ui.station.map.no_selection");
		}
		int blocks = areaMap.selectionWidth() * areaMap.selectionDepth();
		return Component.translatable(
				"laborcraft.ui.station.map.size",
				areaMap.selectionWidth(),
				areaMap.selectionDepth(),
				blocks,
				station.pos().getY() - station.verticalRadius(),
				station.pos().getY() + station.verticalRadius()
		);
	}

	private void openMap() {
		mapOpen = true;
		refreshContent();
	}

	private void cancelMap() {
		StationPreviewManager.clear(station.pos());
		mapOpen = false;
		refreshContent();
	}

	private void confirmZone() {
		if (areaMap == null || !areaMap.selectionFitsLimit()) {
			return;
		}
		BlockPos from = areaMap.from();
		BlockPos to = areaMap.to();
		LaborCraftUiActions.run(
				LaborCraftCommands.STATION_ZONE_SET + " " + posString()
						+ " " + coordinates(from) + " " + coordinates(to)
		);
		StationPreviewManager.clear(station.pos());
	}

	private void previewZone() {
		if (areaMap == null || !areaMap.selectionFitsLimit()) {
			return;
		}
		StationPreviewManager.startPreview(
				station.pos(),
				areaMap.from(),
				areaMap.to(),
				station.verticalRadius(),
				station.areas(),
				station.otherPosts()
		);
		onClose();
	}

	private void previewSavedZone() {
		StationPreviewManager.startSavedPreview(
				station.pos(),
				station.ownAreas(),
				station.verticalRadius()
		);
		onClose();
	}

	private void clearZone() {
		LaborCraftUiActions.run(LaborCraftCommands.STATION_ZONE_CLEAR + " " + posString());
	}

	private void useMaxArea() {
		LaborCraftUiActions.run(LaborCraftCommands.STATION_ZONE_MAX + " " + posString());
	}

	private LcAreaMap createAreaMap(boolean readOnly) {
		BlockPos savedFrom = station.zone() == null ? null : station.zone().from();
		BlockPos savedTo = station.zone() == null ? null : station.zone().to();

		BlockPos selectionFrom = null;
		BlockPos selectionTo = null;
		if (!readOnly) {
			StationPreviewManager.Draft draft = StationPreviewManager.getDraft(station.pos());
			if (draft != null) {
				selectionFrom = draft.from();
				selectionTo = draft.to();
			}
			else if (savedFrom != null) {
				selectionFrom = atStationY(savedFrom);
				selectionTo = atStationY(savedTo);
			}
		}

		int mapWidth = Math.max(64, contentBounds().width() - 16);
		LcAreaMap map = new LcAreaMap(
				0,
				0,
				mapWidth,
				MAP_HEIGHT,
				station.pos(),
				station.maxRadius(),
				selectionFrom,
				selectionTo,
				savedFrom,
				savedTo,
				station.zones(),
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

	// --- Storage tab ------------------------------------------------------------------------

	private QuillFlexBox storageHeader() {
		QuillFlexBox header = QuillNodes.row(QuillSpacing.SPACE_2);
		header.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		header.add(control(label(Component.translatable(
				"laborcraft.ui.station.storage.title",
				station.storage().size()
		)).title()));
		header.add(QuillNodes.flexible(QuillNodes.spacer()));

		QuillButton detect = new QuillButton(
				0, 0, 90, 18,
				Component.translatable("laborcraft.ui.station.storage.detect"),
				QuillButton.Variant.GHOST,
				this::detectStorage
		);
		detect.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.storage.detect"),
				Component.translatable("laborcraft.ui.station.storage.detect.tip"),
				"/" + LaborCraftCommands.STATION_STORAGE_SCAN + " " + posString()
		));
		header.add(QuillNodes.widget(detect, 90, 18));
		return header;
	}

	private QuillLayoutNode storageBody() {
		if (station.storage().isEmpty()) {
			return text(Component.translatable("laborcraft.ui.station.storage.empty"), MUTED);
		}

		QuillListView<OpenStationPayload.StorageView> list = new QuillListView<>(
				0, 0, 400, 180, 40,
				this::storageRow,
				(view, index) -> {
					storageScroll = storageList == null ? 0 : storageList.scrollOffset();
					selectedStoragePos = view.pos();
					refreshContent();
				},
				theme()
		);
		list.replaceItems(station.storage());
		list.setSelected(selectedStorageView());
		list.setScrollOffset(storageScroll);
		storageList = list;
		return control(list);
	}

	private QuillLayoutNode storageRow(OpenStationPayload.StorageView view, int index) {
		QuillFlexBox row = QuillNodes.row(6);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);

		QuillFlexBox identity = QuillNodes.column(2);
		identity.add(control(label(Component.translatable(
				"laborcraft.storage.role." + view.role()
		)).color(roleColor(view.role())).title()));

		if (view.valid()) {
			identity.add(control(label(Component.translatable(view.blockKey())).muted()));
		}
		else {
			identity.add(control(label(Component.translatable(
					"laborcraft.ui.station.storage.invalid"
			)).color(DANGER)));
		}
		identity.add(control(label(Component.literal(
				view.pos().getX() + ", " + view.pos().getY() + ", " + view.pos().getZ()
						+ " \u00b7 " + view.distance() + " bl"
		)).muted()));
		row.add(QuillNodes.flexible(identity));

		QuillButton eye = new QuillButton(
				0, 0, 18, 18,
				Component.literal("E"),
				QuillButton.Variant.GHOST,
				() -> highlightStorage(view)
		);
		eye.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.storage.highlight"),
				Component.translatable("laborcraft.ui.station.storage.highlight.tip"),
				null
		));
		row.add(QuillNodes.widget(eye, 18, 18));
		return row;
	}

	private QuillFlexBox storageActions() {
		QuillFlexBox bar = QuillNodes.row(QuillSpacing.SPACE_2);
		OpenStationPayload.StorageView selected = selectedStorageView();

		QuillButton role = new QuillButton(
				0, 0, 160, 24,
				Component.translatable(
						"laborcraft.ui.station.storage.role",
						Component.translatable("laborcraft.storage.role."
								+ (selected == null ? "mixed" : selected.role()))
				),
				QuillButton.Variant.SECONDARY,
				this::cycleStorageRole
		).enabled(selected != null);
		role.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.storage.role", ""),
				Component.translatable("laborcraft.ui.station.storage.role.tip"),
				null
		));
		bar.add(QuillNodes.flexible(QuillNodes.widget(role, 160, 24), 1));

		bar.add(QuillNodes.widget(new QuillButton(
				0, 0, 120, 24,
				Component.translatable("laborcraft.ui.station.storage.unlink"),
				QuillButton.Variant.DANGER,
				this::unlinkStorage
		).enabled(selected != null), 120, 24));
		return bar;
	}

	private OpenStationPayload.StorageView selectedStorageView() {
		if (selectedStoragePos == null) {
			return null;
		}
		for (OpenStationPayload.StorageView view : station.storage()) {
			if (view.pos().equals(selectedStoragePos)) {
				return view;
			}
		}
		return null;
	}

	private void detectStorage() {
		pendingStorage = selectedStoragePos;
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

	private void highlightStorage(OpenStationPayload.StorageView view) {
		WorldHighlightManager.highlight(view.pos(), 0xFFFFC65A, new ItemStack(Items.CHEST));
		onClose();
	}

	private static String nextStorageRole(String role) {
		return switch (role == null ? "" : role) {
			case "mixed" -> "output";
			case "output" -> "input";
			case "input" -> "tools";
			default -> "mixed";
		};
	}

	private static QuillThemeData.ColorRole roleColor(String role) {
		return switch (role == null ? "" : role) {
			case "output" -> SUCCESS;
			case "input" -> INFO;
			case "tools" -> ACCENT;
			default -> TITLE;
		};
	}

	// --- Shared -----------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		if (!mapOpen || areaMap == null) {
			return;
		}
		boolean fits = areaMap.selectionFitsLimit();
		if (confirmMapButton != null) {
			confirmMapButton.enabled(fits);
		}
		if (previewMapButton != null) {
			previewMapButton.enabled(fits);
		}
		if (selectionStatus != null) {
			selectionStatus.setMessage(selectionLabel());
		}
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

	private String posString() {
		return station.pos().getX() + " " + station.pos().getY() + " " + station.pos().getZ();
	}

	private static String coordinates(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static Component stateLabel(OpenStationPayload.Summary summary) {
		return Component.translatable(
				"laborcraft.ui.worker.state." + summary.state().toLowerCase(Locale.ROOT)
		);
	}
}
