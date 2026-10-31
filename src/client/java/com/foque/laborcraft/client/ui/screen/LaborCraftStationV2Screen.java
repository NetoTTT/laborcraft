package com.foque.laborcraft.client.ui.screen;

import com.quillui.theme.QuillColors;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.INFO;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;
import static com.quillui.theme.QuillThemeData.ColorRole.SUCCESS;
import static com.quillui.theme.QuillThemeData.ColorRole.TITLE;

import com.foque.laborcraft.area.EnclosureDescriptor;
import com.foque.laborcraft.client.render.StationPreviewManager;
import com.foque.laborcraft.client.render.WorldHighlightManager;
import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.WorkerListFilter;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenStationPayload;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.map.QuillMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.quillui.map.QuillMapMarker;
import com.quillui.map.QuillMapZone;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.theme.QuillThemeData;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillChip;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillListView;
import com.quillui.widget.QuillNodes;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Workstation window rebuilt on the measured Quill tree.
 *
 * <p>Work: employee list with real Pay/highlight actions, the post slot card and the bind/recall
 * bar. Config: work area summary plus the interactive map. Storage: linked containers with role
 * and unlink actions. The config tab embeds the Quill map component.</p>
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
	private static final ItemStack EYE_ICON = new ItemStack(Items.ENDER_EYE);

	private static WorkerListFilter pendingFilter;
	private static UUID pendingWorker;
	private static BlockPos pendingStorage;

	private final OpenStationPayload station;
	private Tab tab;
	private WorkerListFilter filter;
	private UUID selectedWorkerId;
	private BlockPos selectedStoragePos;
	private boolean drawing;
	private QuillMap map;
	private QuillListView<OpenStationPayload.Summary> workerList;
	private QuillListView<OpenStationPayload.StorageView> storageList;
	private QuillMap.Layer mapLayer = QuillMap.Layer.AUTO;
	private QuillButton layerButton;
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
		super(Component.translatable("laborcraft.ui.station.title"), PANEL_WIDTH, PANEL_HEIGHT, LaborCraftTheme.currentData());
		this.station = station;
		this.tab = initialTab == null ? Tab.WORK : initialTab;
		this.filter = pendingFilter == null ? WorkerListFilter.ALL : pendingFilter;
		this.selectedWorkerId = pendingWorker;
		this.selectedStoragePos = initialStorage == null ? pendingStorage : initialStorage;
		this.drawing = StationPreviewManager.hasDraft(station.pos());

		pendingFilter = null;
		pendingWorker = null;
		pendingStorage = null;
	}

	public BlockPos stationPos() {
		return station.pos();
	}

	private boolean herder() {
		return JobTypes.HERDER.id().equals(station.jobId());
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
	protected QuillLayoutNode headerAccessory() {
		return QuillNodes.widget(
				QuillChip.of(Component.literal(station.balanceText()))
						.icon(new ItemStack(Items.GOLD_INGOT))
						.accent()
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
				QuillCardBox mapCard = configMapCard();
				root.add(configHeader());
				root.add(mapCard);
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
				Component.empty(),
				QuillButton.Variant.GHOST,
				() -> highlightWorker(worker.id())
		).icon(EYE_ICON);
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
		boolean found = WorldHighlightManager.highlightEntity(id, QuillColors.rgb(72, 215, 160), EYE_ICON);
		onClose();
		if (!found && client.gui != null) {
			client.gui.hud.setOverlayMessage(
					Component.translatable("laborcraft.ui.highlight.missing"),
					false
			);
		}
	}

	// --- Config tab -------------------------------------------------------------------------

	private QuillFlexBox configHeader() {
		QuillFlexBox header = QuillNodes.row(QuillSpacing.SPACE_2);
		header.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);

		if (herder()) {
			EnclosureDescriptor enclosure = station.enclosure();
			Component mode = enclosure == null
					? Component.translatable("laborcraft.ui.station.enclosure.none")
					: Component.translatable(
							"laborcraft.ui.station.enclosure.current",
							enclosure.area(),
							enclosure.width(),
							enclosure.depth(),
							enclosure.gates().size()
					);
			header.add(control(label(mode).color(enclosure == null ? MUTED : TITLE)));
		}
		else {
			int diameter = station.maxRadius() * 2 + 1;
			Component mode = station.zone() == null
					? Component.translatable("laborcraft.ui.station.mode.free_radius", diameter, diameter)
					: Component.translatable(
							"laborcraft.ui.station.mode.zone",
							station.zone().width(),
							station.zone().depth()
					);
			header.add(control(label(mode).color(station.zone() == null ? DANGER : TITLE)));
		}
		header.add(QuillNodes.flexible(QuillNodes.spacer()));

		layerButton = new QuillButton(
				0, 0, 110, 18,
				map == null ? Component.empty() : map.layerLabel(),
				QuillButton.Variant.GHOST,
				this::cycleLayer
		);
		layerButton.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.map.layer"),
				Component.translatable("laborcraft.ui.station.map.layer.tip"),
				null
		));
		header.add(QuillNodes.widget(layerButton, 110, 18));
		return header;
	}

	private QuillCardBox configMapCard() {
		int mapHeight = Math.max(120, contentBounds().height() - 104);
		map = createMap(mapHeight);
		QuillFlexBox column = QuillNodes.column(0);
		column.add(QuillNodes.fixedHeight(mapHeight, control(map)));
		return new QuillCardBox(null, column);
	}

	private QuillFlexBox configActions() {
		QuillFlexBox bar = QuillNodes.row(QuillSpacing.SPACE_2);

		if (herder()) {
			if (station.enclosure() == null) {
				bar.add(control(label(Component.translatable(
						"laborcraft.ui.station.enclosure.click_hint"
				)).muted()));
			}
			else {
				bar.add(QuillNodes.widget(new QuillButton(
						0, 0, 150, 24,
						Component.translatable("laborcraft.ui.station.enclosure.clear"),
						QuillButton.Variant.DANGER,
						() -> LaborCraftUiActions.run(
								LaborCraftCommands.STATION_ENCLOSURE_CLEAR + " " + posString()
						)
				), 150, 24));
			}
			bar.add(QuillNodes.flexible(QuillNodes.spacer()));
			return bar;
		}

		if (drawing) {
			confirmMapButton = new QuillButton(
					0, 0, 110, 24,
					Component.translatable("laborcraft.ui.station.map.confirm"),
					QuillButton.Variant.PRIMARY,
					this::confirmZone
			).enabled(canConfirm());
			bar.add(QuillNodes.flexible(QuillNodes.widget(confirmMapButton, 110, 24), 1));

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
			).enabled(canConfirm());
			bar.add(QuillNodes.widget(previewMapButton, 100, 24));
			return bar;
		}

		QuillButton draw = new QuillButton(
				0, 0, 132, 24,
				Component.translatable("laborcraft.ui.station.map.draw"),
				QuillButton.Variant.PRIMARY,
				this::openMap
		);
		draw.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.map.draw"),
				Component.translatable("laborcraft.ui.station.config.map_hint"),
				null
		));
		bar.add(QuillNodes.flexible(QuillNodes.widget(draw, 132, 24), 1));

		if (station.zone() != null) {
			bar.add(QuillNodes.widget(new QuillButton(
					0, 0, 128, 24,
					Component.translatable("laborcraft.ui.station.map.clear"),
					QuillButton.Variant.DANGER,
					this::clearZone
			), 128, 24));

			QuillButton preview = new QuillButton(
					0, 0, 130, 24,
					Component.translatable("laborcraft.ui.station.map.preview_saved"),
					QuillButton.Variant.SECONDARY,
					this::previewSavedZone
			).enabled(!station.ownAreas().isEmpty());
			preview.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.station.map.preview_saved"),
					Component.translatable("laborcraft.ui.station.map.preview"),
					null
			));
			bar.add(QuillNodes.widget(preview, 130, 24));
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
		return bar;
	}

	private void cycleLayer() {
		if (map == null) {
			return;
		}
		mapLayer = mapLayer.next();
		map.setLayer(mapLayer);
		if (layerButton != null) {
			layerButton.setMessage(map.layerLabel());
		}
	}

	private void openMap() {
		drawing = true;
		refreshContent();
	}

	private void cancelMap() {
		StationPreviewManager.clear(station.pos());
		drawing = false;
		refreshContent();
	}

	private void confirmZone() {
		if (!canConfirm()) {
			return;
		}
		BlockPos from = map.from();
		BlockPos to = map.to();
		LaborCraftUiActions.run(
				LaborCraftCommands.STATION_ZONE_SET + " " + posString()
						+ " " + coordinates(from) + " " + coordinates(to)
		);
		StationPreviewManager.clear(station.pos());
		drawing = false;
	}

	private void previewZone() {
		if (!canConfirm()) {
			return;
		}
		StationPreviewManager.startPreview(
				station.pos(),
				map.from(),
				map.to(),
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

	private QuillMap createMap(int height) {
		int width = Math.max(64, contentBounds().width() - 16);
		int viewRadius = Math.max(
				station.maxRadius(),
				(int) Math.ceil(Math.max(width, height) / 8.0D)
		);
		if (herder() && station.enclosure() != null) {
			EnclosureDescriptor enclosure = station.enclosure();
			int span = Math.max(
					Math.max(Math.abs(enclosure.min().getX() - station.pos().getX()),
							Math.abs(enclosure.max().getX() - station.pos().getX())),
					Math.max(Math.abs(enclosure.min().getZ() - station.pos().getZ()),
							Math.abs(enclosure.max().getZ() - station.pos().getZ()))
			);
			viewRadius = Math.max(viewRadius, span + 8);
		}
		QuillMap created = new QuillMap(
				0,
				0,
				width,
				height,
				station.pos(),
				viewRadius,
				32,
				theme()
		);
		created.setLayer(mapLayer);
		created.setRadiusLimitVisible(false);
		boolean herder = herder();
		created.setReadOnly(herder || !drawing);
		created.setZones(herder ? enclosureZones() : mapZones());
		created.setMergeZones(true);
		if (herder) {
			created.setMarkers(enclosureGates());
			return created;
		}

		if (drawing) {
			BlockPos savedFrom = station.zone() == null ? null : station.zone().from();
			BlockPos savedTo = station.zone() == null ? null : station.zone().to();
			StationPreviewManager.Draft draft = StationPreviewManager.getDraft(station.pos());
			if (draft != null) {
				created.setSelection(draft.from(), draft.to());
			}
			else if (savedFrom != null) {
				created.setSelection(atStationY(savedFrom), atStationY(savedTo));
			}
		}

		return created;
	}

	private List<QuillMapZone> enclosureZones() {
		EnclosureDescriptor enclosure = station.enclosure();
		if (enclosure == null) {
			return List.of();
		}

		List<QuillMapZone> zones = new java.util.ArrayList<>(enclosure.spans().size());
		int y = enclosure.seed().getY();
		for (com.foque.laborcraft.area.EnclosureSpan span : enclosure.spans()) {
			zones.add(new QuillMapZone(
					new BlockPos(span.minX(), y, span.z()),
					new BlockPos(span.maxX(), y, span.z()),
					QuillColors.argb(40, 255, 198, 90),
					QuillColors.rgb(255, 198, 90),
					false
			));
		}
		return zones;
	}

	private List<QuillMapMarker> enclosureGates() {
		EnclosureDescriptor enclosure = station.enclosure();
		if (enclosure == null) {
			return List.of();
		}

		List<QuillMapMarker> markers = new java.util.ArrayList<>(enclosure.gates().size());
		for (BlockPos gate : enclosure.gates()) {
			markers.add(new QuillMapMarker(
					gate,
					Component.translatable("laborcraft.ui.station.enclosure.gate"),
					QuillColors.rgb(255, 198, 90)
			));
		}
		return markers;
	}

	private List<QuillMapZone> mapZones() {
		List<QuillMapZone> zones = new java.util.ArrayList<>();
		int radius = station.maxRadius();
		zones.add(new QuillMapZone(
				station.pos().offset(-radius, 0, -radius),
				station.pos().offset(radius, 0, radius),
				QuillColors.argb(0, 76, 139, 245),
				QuillColors.argb(153, 76, 139, 245),
				true
		));
		for (OpenStationPayload.AreaView area : station.ownAreas()) {
			zones.add(new QuillMapZone(
					area.from(),
					area.to(),
					QuillColors.argb(40, 255, 198, 90),
					QuillColors.rgb(255, 198, 90),
					false
			));
		}
		for (OpenStationPayload.AreaView area : station.areas()) {
			zones.add(new QuillMapZone(
					area.from(),
					area.to(),
					QuillColors.argb(40, 76, 139, 245),
					QuillColors.rgb(76, 139, 245),
					false
			));
		}
		return zones;
	}

	private boolean canConfirm() {
		return map != null && map.selectionFitsLimit() && !selectionOverlapsOther();
	}

	private boolean selectionOverlapsOther() {
		if (map == null || !map.hasSelection()) {
			return false;
		}

		int minX = Math.min(map.from().getX(), map.to().getX());
		int maxX = Math.max(map.from().getX(), map.to().getX());
		int minZ = Math.min(map.from().getZ(), map.to().getZ());
		int maxZ = Math.max(map.from().getZ(), map.to().getZ());
		for (OpenStationPayload.AreaView area : station.areas()) {
			if (area == null || !area.blocksDraft()) {
				continue;
			}

			int areaMinX = Math.min(area.from().getX(), area.to().getX());
			int areaMaxX = Math.max(area.from().getX(), area.to().getX());
			int areaMinZ = Math.min(area.from().getZ(), area.to().getZ());
			int areaMaxZ = Math.max(area.from().getZ(), area.to().getZ());
			if (areaMinX <= maxX && areaMaxX >= minX && areaMinZ <= maxZ && areaMaxZ >= minZ) {
				return true;
			}
		}
		return false;
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

		QuillButton swap = new QuillButton(
				0, 0, 56, 16,
				Component.translatable("laborcraft.ui.station.storage.swap"),
				QuillButton.Variant.GHOST,
				() -> cycleStorageRole(view)
		);
		swap.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.station.storage.swap"),
				Component.translatable("laborcraft.ui.station.storage.swap.tip"),
				null
		));
		row.add(QuillNodes.widget(swap, 56, 16));

		QuillButton eye = new QuillButton(
				0, 0, 18, 18,
				Component.empty(),
				QuillButton.Variant.GHOST,
				() -> highlightStorage(view)
		).icon(EYE_ICON);
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
		if (view != null) {
			cycleStorageRole(view);
		}
	}

	private void cycleStorageRole(OpenStationPayload.StorageView view) {
		if (view == null) {
			return;
		}
		LaborCraftUiActions.run(
				LaborCraftCommands.STATION_STORAGE_ROLE + " " + posString() + " " + coordinates(view.pos())
						+ " " + nextStorageRole(view.role())
		);
	}

	private void highlightStorage(OpenStationPayload.StorageView view) {
		WorldHighlightManager.highlight(view.pos(), QuillColors.rgb(255, 198, 90), new ItemStack(Items.CHEST));
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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (tab == Tab.CONFIG && herder() && map != null
				&& event.button() == InputConstants.MOUSE_BUTTON_LEFT
				&& map.isMouseOver(event.x(), event.y())) {
			BlockPos clicked = map.blockAt(event.x(), event.y());
			if (clicked != null) {
				clicked = new BlockPos(clicked.getX(), map.layerY(), clicked.getZ());
				LaborCraftUiActions.run(
						LaborCraftCommands.STATION_ENCLOSURE_SET + " " + posString()
								+ " " + coordinates(clicked)
				);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void tick() {
		super.tick();
		if (map != null && layerButton != null) {
			layerButton.setMessage(map.layerLabel());
		}
		if (!drawing || map == null) {
			return;
		}
		boolean fits = canConfirm();
		if (confirmMapButton != null) {
			confirmMapButton.enabled(fits);
		}
		if (previewMapButton != null) {
			previewMapButton.enabled(fits);
		}
	}

	@Override
	protected void clearWidgets() {
		closeMap();
		super.clearWidgets();
	}

	@Override
	public void removed() {
		closeMap();
		super.removed();
	}

	@Override
	public void onClose() {
		closeMap();
		super.onClose();
	}

	private void closeMap() {
		if (map != null) {
			map.close();
			map = null;
		}
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
