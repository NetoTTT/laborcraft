package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;
import static com.quillui.theme.QuillThemeData.ColorRole.TITLE;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.widget.WorkerInventoryBoard;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobEquipment;
import com.foque.laborcraft.network.OpenWorkerInventoryPayload;
import com.foque.laborcraft.network.WorkerScreenStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.overlay.QuillOverlay;
import com.quillui.render.QuillRender;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillColors;
import com.quillui.theme.QuillSpacing;
import com.quillui.theme.QuillThemeData;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillNodes;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Chest-like transfer screen rebuilt on the measured Quill tree.
 *
 * <p>The board is a measured widget that paints the player grid, the worker backpack and the tool
 * slot. Clicks and drags are mapped back to slots here; every transfer stays a command, so the
 * server remains authoritative. Stacks bigger than one open a quantity overlay.</p>
 */
public final class LaborCraftWorkerInventoryV2Screen extends QuillScreen {
	private static final int PANEL_WIDTH = 540;
	private static final int PANEL_HEIGHT = 340;

	private record PendingTransfer(WorkerInventoryBoard.SlotHit from, WorkerInventoryBoard.SlotHit to, int max) {
	}

	private OpenWorkerInventoryPayload worker;
	private WorkerInventoryBoard board;
	private WorkerInventoryBoard.SlotHit selected;
	private WorkerInventoryBoard.SlotHit dragSource;
	private boolean dragging;
	private PendingTransfer pending;

	public LaborCraftWorkerInventoryV2Screen(OpenWorkerInventoryPayload worker) {
		super(Component.translatable("laborcraft.ui.worker.inv.title"), PANEL_WIDTH, PANEL_HEIGHT, LaborCraftTheme.currentData());
		this.worker = worker;
	}

	public UUID workerId() {
		return worker.workerId();
	}

	public void update(OpenWorkerInventoryPayload payload) {
		this.worker = payload;
	}

	@Override
	public void added() {
		super.added();
		sendScreenState(true);
	}

	@Override
	public void removed() {
		sendScreenState(false);
		super.removed();
	}

	private void sendScreenState(boolean open) {
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null || !ClientPlayNetworking.canSend(WorkerScreenStatePayload.TYPE)) {
			return;
		}

		ClientPlayNetworking.send(new WorkerScreenStatePayload(worker.workerId(), open));
	}

	@Override
	protected Component subtitle() {
		return Component.literal(worker.name());
	}

	@Override
	protected void buildContent() {
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);

		board = new WorkerInventoryBoard(
				this::stackAt,
				Component.translatable("laborcraft.ui.worker.inv.player"),
				Component.translatable("laborcraft.ui.worker.inv.storage"),
				Component.translatable("laborcraft.ui.worker.inv.tool"),
				JobEquipment.toolName(worker.jobId()),
				Component.translatable("laborcraft.ui.worker.inv.hint"),
				theme()
		);
		board.select(selected);
		root.add(QuillNodes.flexible(control(board), 1));
		root.add(actions());

		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillFlexBox actions() {
		QuillFlexBox bar = QuillNodes.row(QuillSpacing.SPACE_2);
		QuillButton back = new QuillButton(
				0, 0, 110, 24,
				Component.translatable("laborcraft.ui.worker.inv.back"),
				QuillButton.Variant.GHOST,
				this::backToEmployee
		);
		back.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.worker.inv.back"),
				Component.translatable("laborcraft.ui.worker.inv.back.tip"),
				"/" + LaborCraftCommands.WORKER_OPEN + " " + worker.workerId()
		));
		bar.add(QuillNodes.widget(back, 110, 24));
		bar.add(QuillNodes.flexible(QuillNodes.spacer()));
		return bar;
	}

	private void backToEmployee() {
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_OPEN + " " + worker.workerId());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (pending != null || board == null) {
			return;
		}
		WorkerInventoryBoard.SlotHit hovered = board.slotAt(mouseX, mouseY);
		if (hovered == null) {
			return;
		}

		ItemStack stack = stackAt(hovered);
		if (stack.isEmpty()) {
			List<Component> lines = hovered.side() == WorkerInventoryBoard.Side.WORKER
					&& hovered.index() == 9
					? List.of(
							Component.translatable(
									"laborcraft.ui.worker.inv.tool.with",
									JobEquipment.toolName(worker.jobId())
							),
							Component.translatable("laborcraft.ui.worker.inv.empty")
					)
					: List.of(Component.translatable("laborcraft.ui.worker.inv.empty"));
			graphics.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY);
		}
		else {
			graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (pending != null) {
			return true;
		}
		if (board == null) {
			return super.mouseClicked(event, doubleClick);
		}

		WorkerInventoryBoard.SlotHit hit = board.slotAt(event.x(), event.y());
		if (hit == null) {
			selected = null;
			board.select(null);
			return super.mouseClicked(event, doubleClick);
		}

		if (selected != null && selected.side() != hit.side()) {
			requestTransfer(selected, hit);
			selected = null;
			board.select(null);
			dragSource = null;
			return true;
		}

		selected = selected != null && selected.equals(hit) ? null : hit;
		board.select(selected);
		dragSource = selected;
		dragging = false;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (pending != null) {
			return true;
		}
		if (dragSource != null) {
			dragging = true;
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (pending != null) {
			return true;
		}
		if (dragSource != null) {
			WorkerInventoryBoard.SlotHit hit = board == null ? null : board.slotAt(event.x(), event.y());
			if (dragging && hit != null && hit.side() != dragSource.side()) {
				requestTransfer(dragSource, hit);
				selected = null;
				if (board != null) {
					board.select(null);
				}
			}
			dragSource = null;
			dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	private void requestTransfer(WorkerInventoryBoard.SlotHit from, WorkerInventoryBoard.SlotHit to) {
		ItemStack source = stackAt(from);
		if (source.isEmpty()) {
			return;
		}

		ItemStack target = stackAt(to);
		boolean sameKind = target.isEmpty() || ItemStack.isSameItemSameComponents(target, source);
		if (!sameKind) {
			sendSwap(from, to);
			return;
		}

		if (to.side() == WorkerInventoryBoard.Side.WORKER && to.index() == 9
				&& !JobEquipment.validTool(worker.jobId(), source)) {
			showToast(Component.translatable(
					"laborcraft.command.worker.inv.tool.invalid",
					JobEquipment.toolName(worker.jobId())
			), 2.5F);
			return;
		}

		int room = target.isEmpty()
				? source.getMaxStackSize()
				: target.getMaxStackSize() - target.getCount();
		int max = Math.min(source.getCount(), room);
		if (max <= 0) {
			showToast(Component.translatable("laborcraft.command.worker.inv.slot_full"), 2.5F);
			return;
		}
		if (source.getCount() <= 1 || max <= 1) {
			sendAmount(from, to, Math.min(source.getCount(), max));
			return;
		}

		pending = new PendingTransfer(from, to, max);
		openOverlay(new QuantityOverlay(
				source.getHoverName(),
				max,
				amount -> {
					PendingTransfer transfer = pending;
					pending = null;
					if (transfer != null) {
						sendAmount(transfer.from(), transfer.to(), amount);
					}
				},
				() -> pending = null,
				theme()
		));
	}

	private void sendSwap(WorkerInventoryBoard.SlotHit from, WorkerInventoryBoard.SlotHit to) {
		int workerSlot = from.side() == WorkerInventoryBoard.Side.WORKER ? from.index() : to.index();
		int playerSlot = from.side() == WorkerInventoryBoard.Side.PLAYER ? from.index() : to.index();
		LaborCraftUiActions.run(
				LaborCraftCommands.WORKER_INV_SWAP + " " + worker.workerId() + " " + workerSlot + " " + playerSlot
		);
	}

	private void sendAmount(WorkerInventoryBoard.SlotHit from, WorkerInventoryBoard.SlotHit to, int amount) {
		int workerSlot = from.side() == WorkerInventoryBoard.Side.WORKER ? from.index() : to.index();
		int playerSlot = from.side() == WorkerInventoryBoard.Side.PLAYER ? from.index() : to.index();
		String command = from.side() == WorkerInventoryBoard.Side.PLAYER
				? LaborCraftCommands.WORKER_INV_DEPOSIT
				: LaborCraftCommands.WORKER_INV_WITHDRAW;
		LaborCraftUiActions.run(
				command + " " + worker.workerId() + " " + workerSlot + " " + playerSlot + " " + amount
		);
	}

	private ItemStack stackAt(WorkerInventoryBoard.Side side, int index) {
		if (side == WorkerInventoryBoard.Side.WORKER) {
			if (index == 9) {
				return worker.tool();
			}
			return index >= 0 && index < worker.slots().size() ? worker.slots().get(index) : ItemStack.EMPTY;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || index < 0 || index >= 36) {
			return ItemStack.EMPTY;
		}
		return client.player.getInventory().getItem(index);
	}

	private ItemStack stackAt(WorkerInventoryBoard.SlotHit hit) {
		return stackAt(hit.side(), hit.index());
	}

	/** Numeric prompt for stack transfers: All, Confirm and Cancel, with digits typed in-place. */
	private static final class QuantityOverlay implements QuillOverlay {
		private static final int WIDTH = 244;
		private static final int HEIGHT = 118;
		private static final int BUTTON_HEIGHT = 20;

		private final Component itemName;
		private final int max;
		private final IntConsumer onConfirm;
		private final Runnable onCancel;
		private final QuillThemeData theme;
		private String quantity = "";
		private boolean open = true;
		private boolean notified;
		private int panelX;
		private int panelY;

		private QuantityOverlay(
				Component itemName,
				int max,
				IntConsumer onConfirm,
				Runnable onCancel,
				QuillThemeData theme
		) {
			this.itemName = itemName;
			this.max = Math.max(1, max);
			this.onConfirm = onConfirm;
			this.onCancel = onCancel;
			this.theme = theme == null ? QuillThemeData.DEFAULT : theme;
		}

		@Override
		public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			if (!open) {
				return;
			}
			panelX = (graphics.guiWidth() - WIDTH) / 2;
			panelY = (graphics.guiHeight() - HEIGHT) / 2;
			Font font = Minecraft.getInstance().font;

			graphics.fill(
					0,
					0,
					graphics.guiWidth(),
					graphics.guiHeight(),
					QuillColors.withAlpha(
							theme.color(QuillThemeData.ColorRole.PANEL_BG),
							0xAA
					)
			);
			QuillRender.panel(graphics, panelX, panelY, WIDTH, HEIGHT, theme);
			QuillRender.text(
					graphics,
					font,
					Component.translatable("laborcraft.ui.worker.inv.quantity"),
					panelX + 12,
					panelY + 10,
					theme.color(TITLE)
			);
			QuillRender.text(
					graphics,
					font,
					Component.translatable("laborcraft.ui.worker.inv.quantity.range", itemName, max),
					panelX + 12,
					panelY + 26,
					theme.color(MUTED)
			);

			int fieldX = panelX + 12;
			int fieldY = panelY + 42;
			int fieldWidth = WIDTH - 24;
			QuillRender.box(
					graphics,
					fieldX,
					fieldY,
					fieldWidth,
					20,
					theme.color(QuillThemeData.ColorRole.BUTTON_BG),
					theme.color(QuillThemeData.ColorRole.BUTTON_BORDER)
			);
			QuillRender.text(
					graphics,
					font,
					Component.literal(quantity),
					fieldX + 6,
					fieldY + 6,
					theme.color(QuillThemeData.ColorRole.TEXT)
			);
			if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
				QuillRender.text(
						graphics,
						font,
						Component.literal("_"),
						fieldX + 6 + font.width(quantity),
						fieldY + 6,
						theme.color(QuillThemeData.ColorRole.ACCENT)
				);
			}

			int buttonY = panelY + HEIGHT - 32;
			drawButton(graphics, font, Component.translatable("laborcraft.ui.worker.inv.all"),
					panelX + 12, buttonY, 58, mouseX, mouseY, false);
			drawButton(graphics, font, Component.translatable("laborcraft.ui.worker.inv.confirm"),
					panelX + 78, buttonY, 92, mouseX, mouseY, true);
			drawButton(graphics, font, Component.translatable("laborcraft.ui.worker.inv.cancel"),
					panelX + WIDTH - 12 - 58, buttonY, 58, mouseX, mouseY, false);
		}

		private void drawButton(
				GuiGraphicsExtractor graphics,
				Font font,
				Component label,
				int x,
				int y,
				int width,
				int mouseX,
				int mouseY,
				boolean primary
		) {
			boolean hovered = inside(mouseX, mouseY, x, y, width, BUTTON_HEIGHT);
			int background = primary
					? theme.color(hovered
							? QuillThemeData.ColorRole.PRIMARY_BG_HOVER
							: QuillThemeData.ColorRole.PRIMARY_BG)
					: theme.color(hovered
							? QuillThemeData.ColorRole.BUTTON_HOVER
							: QuillThemeData.ColorRole.BUTTON_BG);
			int border = primary
					? theme.color(QuillThemeData.ColorRole.ACCENT)
					: theme.color(hovered
							? QuillThemeData.ColorRole.ACCENT
							: QuillThemeData.ColorRole.BUTTON_BORDER);
			int text = primary
					? theme.color(QuillThemeData.ColorRole.ON_ACCENT)
					: theme.color(QuillThemeData.ColorRole.TEXT);
			QuillRender.box(graphics, x, y, width, BUTTON_HEIGHT, background, border);
			QuillRender.centeredText(graphics, font, label, x + width / 2, y + 6, text);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (!open) {
				return false;
			}
			int buttonY = panelY + HEIGHT - 32;
			if (inside(event.x(), event.y(), panelX + 12, buttonY, 58, BUTTON_HEIGHT)) {
				confirm(max);
			}
			else if (inside(event.x(), event.y(), panelX + 78, buttonY, 92, BUTTON_HEIGHT)) {
				confirm(parseQuantity());
			}
			else if (inside(
					event.x(),
					event.y(),
					panelX + WIDTH - 12 - 58,
					buttonY,
					58,
					BUTTON_HEIGHT
			)) {
				cancel();
			}
			return true;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (!open) {
				return false;
			}
			int key = event.key();
			if (key == InputConstants.KEY_ESCAPE) {
				cancel();
				return true;
			}
			if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
				confirm(parseQuantity());
				return true;
			}
			if (key == InputConstants.KEY_BACKSPACE) {
				if (!quantity.isEmpty()) {
					quantity = quantity.substring(0, quantity.length() - 1);
				}
				return true;
			}

			int digit = digitFor(key);
			if (digit >= 0) {
				String next = quantity + digit;
				if (next.length() > 2) {
					next = next.substring(1);
				}
				quantity = next;
			}
			return true;
		}

		private static int digitFor(int key) {
			if (key == InputConstants.KEY_0) {
				return 0;
			}
			if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
				return key - InputConstants.KEY_1 + 1;
			}
			if (key == InputConstants.KEY_NUMPAD0) {
				return 0;
			}
			if (key >= InputConstants.KEY_NUMPAD1 && key <= InputConstants.KEY_NUMPAD9) {
				return key - InputConstants.KEY_NUMPAD1 + 1;
			}
			return -1;
		}

		private int parseQuantity() {
			if (quantity.isEmpty()) {
				return 1;
			}
			try {
				return Mth.clamp(Integer.parseInt(quantity), 1, max);
			}
			catch (NumberFormatException exception) {
				return 1;
			}
		}

		private void confirm(int amount) {
			if (!open) {
				return;
			}
			open = false;
			notified = true;
			if (onConfirm != null) {
				onConfirm.accept(Mth.clamp(amount, 1, max));
			}
		}

		private void cancel() {
			if (!open) {
				return;
			}
			open = false;
			if (!notified) {
				notified = true;
				if (onCancel != null) {
					onCancel.run();
				}
			}
		}

		@Override
		public boolean isOpen() {
			return open;
		}

		@Override
		public void close() {
			cancel();
		}

		@Override
		public boolean dismissOnOutsideClick() {
			return false;
		}

		@Override
		public boolean blocksInput() {
			return true;
		}

		private static boolean inside(
				double mouseX, double mouseY, int x, int y, int width, int height
		) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}
}
