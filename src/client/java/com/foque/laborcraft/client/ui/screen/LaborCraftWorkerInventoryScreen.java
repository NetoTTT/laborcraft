package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobEquipment;
import com.foque.laborcraft.network.OpenWorkerInventoryPayload;
import com.foque.laborcraft.network.WorkerScreenStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Chest-like transfer screen: player inventory on the left, worker backpack (nine slots plus the
 * tool slot) on the right. Click a slot and then the other side to move/swap, or drag between the
 * two panels. Stacks with more than one item open a quantity prompt. Every transfer is a command,
 * so the server stays authoritative.
 */
public final class LaborCraftWorkerInventoryScreen extends com.quillui.screen.QuillScreen {
	private static final int SLOT = 32;
	private static final int STEP = 34;
	private static final int TOOL_SLOT = 9;
	private static final int MODAL_WIDTH = 230;
	private static final int MODAL_HEIGHT = 100;

	private enum Side {
		PLAYER,
		WORKER
	}

	private record SlotHit(Side side, int index) {
	}

	private record PendingTransfer(SlotHit from, SlotHit to, int max, boolean deposit) {
	}

	private OpenWorkerInventoryPayload worker;
	private int playerX;
	private int playerY;
	private int workerX;
	private int workerY;
	private SlotHit selected;
	private SlotHit dragSource;
	private boolean dragging;
	private PendingTransfer pending;
	private String quantity = "";
	private Component errorMessage;
	private long errorUntil;

	public LaborCraftWorkerInventoryScreen(OpenWorkerInventoryPayload worker) {
		super(Component.translatable("laborcraft.ui.worker.inv.title"), 540, 320);
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
		playerX = panelX + 22;
		playerY = panelY + 76;
		workerX = playerX + 9 * STEP + 20;
		workerY = playerY;

		int buttonsY = panelY + panelHeight() - 44;
		LcButton back = new LcButton(
				playerX,
				buttonsY,
				110,
				24,
				Component.translatable("laborcraft.ui.worker.inv.back"),
				LcButton.Variant.GHOST,
				this::backToEmployee
		);
		back.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.worker.inv.back"),
				Component.translatable("laborcraft.ui.worker.inv.back.tip"),
				"/" + LaborCraftCommands.WORKER_OPEN + " " + worker.workerId()
		));
		addRenderableWidget(back);
		addRenderableWidget(closeButton(this::onClose));
	}

	private void backToEmployee() {
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_OPEN + " " + worker.workerId());
	}

	private void showError(Component message) {
		this.errorMessage = message;
		this.errorUntil = System.currentTimeMillis() + 3000L;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.worker.inv.player"),
				playerX,
				panelY + 62,
				LcTheme.MUTED
		);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.worker.inv.storage"),
				workerX,
				panelY + 62,
				LcTheme.MUTED
		);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.worker.inv.tool"),
				workerX + 3 * STEP + 8,
				panelY + 62,
				LcTheme.MUTED
		);
		graphics.text(
				font,
				JobEquipment.toolName(worker.jobId()),
				workerX + 3 * STEP + 8,
				workerY + SLOT + 3,
				LcTheme.ACCENT
		);

		SlotHit hovered = pending == null ? slotAt(mouseX, mouseY) : null;
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int index = 9 + row * 9 + column;
				drawSlot(
						graphics,
						Side.PLAYER,
						index,
						playerX + column * STEP,
						playerY + row * STEP,
						hovered
				);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(
					graphics,
					Side.PLAYER,
					column,
					playerX + column * STEP,
					playerY + 3 * STEP + 10,
					hovered
			);
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				int index = row * 3 + column;
				drawSlot(
						graphics,
						Side.WORKER,
						index,
						workerX + column * STEP,
						workerY + row * STEP,
						hovered
				);
			}
		}
		drawSlot(graphics, Side.WORKER, TOOL_SLOT, workerX + 3 * STEP + 8, workerY, hovered);

		int hintX = workerX;
		int hintY = workerY + 3 * STEP + 10;
		int hintWidth = playerX + (panelWidth() - 44) - hintX;
		List<net.minecraft.util.FormattedCharSequence> lines = font.split(
				Component.translatable("laborcraft.ui.worker.inv.hint"),
				hintWidth - 16
		);
		int hintHeight = 12 + lines.size() * 11;
		graphics.fill(hintX, hintY, hintX + hintWidth, hintY + hintHeight, 0x401B232D);
		LcRender.border(graphics, hintX, hintY, hintWidth, hintHeight, LcTheme.DIVIDER);
		for (int index = 0; index < lines.size(); index++) {
			graphics.text(font, lines.get(index), hintX + 8, hintY + 6 + index * 11, LcTheme.MUTED);
		}

		if (errorMessage != null && System.currentTimeMillis() < errorUntil) {
			int errorY = hintY + hintHeight + 4;
			graphics.fill(
					panelX + 22,
					errorY,
					panelX + panelWidth() - 22,
					errorY + 12,
					0xCC101820
			);
			graphics.centeredText(
					font,
					errorMessage,
					panelX + panelWidth() / 2,
					errorY + 2,
					0xFFFF5555
			);
		} else {
			errorMessage = null;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (pending != null) {
			drawQuantityModal(graphics, mouseX, mouseY);
			return;
		}

		SlotHit hovered = slotAt(mouseX, mouseY);
		if (hovered == null) {
			return;
		}

		ItemStack stack = stackAt(hovered);
		if (stack.isEmpty()) {
			List<Component> lines = hovered.side() == Side.WORKER && hovered.index() == TOOL_SLOT
					? List.of(
							Component.translatable(
									"laborcraft.ui.worker.inv.tool.with",
									JobEquipment.toolName(worker.jobId())
							),
							Component.translatable("laborcraft.ui.worker.inv.empty")
					)
					: List.of(Component.translatable("laborcraft.ui.worker.inv.empty"));
			graphics.setTooltipForNextFrame(
					font,
					lines,
					java.util.Optional.empty(),
					mouseX,
					mouseY
			);
		} else {
			graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}

	private void drawSlot(
			GuiGraphicsExtractor graphics,
			Side side,
			int index,
			int x,
			int y,
			SlotHit hovered
	) {
		boolean isSelected = selected != null && selected.side() == side && selected.index() == index;
		boolean isHovered = hovered != null && hovered.side() == side && hovered.index() == index;

		graphics.fill(x, y, x + SLOT, y + SLOT, LcTheme.BUTTON_BG);
		int border = isSelected
				? LcTheme.ACCENT
				: (isHovered ? LcTheme.TITLE : LcTheme.BUTTON_BORDER);
		LcRender.border(graphics, x, y, SLOT, SLOT, border);
		if (isSelected) {
			graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x2248D7A0);
		}

		ItemStack stack = stackAt(side, index);
		if (stack.isEmpty()) {
			return;
		}

		graphics.item(stack, x + 8, y + 8);
		if (stack.getCount() > 1) {
			String count = String.valueOf(stack.getCount());
			int textWidth = font.width(count);
			graphics.text(font, count, x + SLOT - textWidth - 2, y + SLOT - 10, 0xFFFFFFFF);
		}
		if (stack.isDamageableItem() && stack.isDamaged()) {
			int max = stack.getMaxDamage();
			int remaining = max - stack.getDamageValue();
			int barWidth = Math.max(1, (int) ((SLOT - 4) * (remaining / (float) max)));
			int color = remaining / (float) max > 0.5F
					? 0xFF48D7A0
					: (remaining / (float) max > 0.2F ? 0xFFFFC65A : 0xFFFF5555);
			graphics.fill(x + 2, y + SLOT - 4, x + SLOT - 2, y + SLOT - 3, 0xFF101820);
			graphics.fill(x + 2, y + SLOT - 4, x + 2 + barWidth, y + SLOT - 3, color);
		}
	}

	private void drawQuantityModal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = modalX();
		int top = modalY();
		graphics.fill(0, 0, width, height, 0x99000000);
		graphics.fill(left, top, left + MODAL_WIDTH, top + MODAL_HEIGHT, 0xF0101820);
		LcRender.border(graphics, left, top, MODAL_WIDTH, MODAL_HEIGHT, LcTheme.ACCENT);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.worker.inv.quantity"),
				left + 10,
				top + 8,
				LcTheme.TITLE
		);

		ItemStack stack = stackAt(pending.from());
		graphics.text(
				font,
				Component.translatable(
						"laborcraft.ui.worker.inv.quantity.range",
						stack.getHoverName(),
						pending.max()
				),
				left + 10,
				top + 20,
				LcTheme.MUTED
		);

		graphics.fill(left + 10, top + 34, left + MODAL_WIDTH - 10, top + 54, 0xFF1B232D);
		LcRender.border(graphics, left + 10, top + 34, MODAL_WIDTH - 20, 20, LcTheme.BUTTON_BORDER);
		graphics.text(font, quantity, left + 15, top + 40, LcTheme.TEXT);
		if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
			graphics.text(font, "_", left + 15 + font.width(quantity), top + 40, LcTheme.ACCENT);
		}

		drawModalButton(graphics, left + 10, top + 64, 60, Component.translatable("laborcraft.ui.worker.inv.all"), mouseX, mouseY);
		drawModalButton(graphics, left + 76, top + 64, 80, Component.translatable("laborcraft.ui.worker.inv.confirm"), mouseX, mouseY);
		drawModalButton(graphics, left + 162, top + 64, 58, Component.translatable("laborcraft.ui.worker.inv.cancel"), mouseX, mouseY);
	}

	private void drawModalButton(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int width,
			Component label,
			int mouseX,
			int mouseY
	) {
		boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 22;
		graphics.fill(x, y, x + width, y + 22, hovered ? LcTheme.BUTTON_HOVER : LcTheme.BUTTON_BG);
		LcRender.border(graphics, x, y, width, 22, hovered ? LcTheme.ACCENT : LcTheme.BUTTON_BORDER);
		graphics.centeredText(font, label, x + width / 2, y + 7, hovered ? LcTheme.TITLE : LcTheme.TEXT);
	}

	private int modalX() {
		return (width - MODAL_WIDTH) / 2;
	}

	private int modalY() {
		return (height - MODAL_HEIGHT) / 2;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (pending != null) {
			int left = modalX();
			int top = modalY();
			if (inside(event.x(), event.y(), left + 10, top + 64, 60, 22)) {
				confirm(pending.max());
			} else if (inside(event.x(), event.y(), left + 76, top + 64, 80, 22)) {
				confirm(parseQuantity());
			} else if (inside(event.x(), event.y(), left + 162, top + 64, 58, 22)) {
				cancelQuantity();
			}
			return true;
		}

		SlotHit hit = slotAt(event.x(), event.y());
		if (hit == null) {
			selected = null;
			return super.mouseClicked(event, doubleClick);
		}

		if (selected != null && selected.side() != hit.side()) {
			requestTransfer(selected, hit);
			selected = null;
			dragSource = null;
			return true;
		}

		selected = selected != null && selected.equals(hit) ? null : hit;
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
			SlotHit hit = slotAt(event.x(), event.y());
			if (dragging && hit != null && hit.side() != dragSource.side()) {
				requestTransfer(dragSource, hit);
				selected = null;
			}
			dragSource = null;
			dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (pending == null) {
			return super.keyPressed(event);
		}

		int key = event.key();
		if (key == InputConstants.KEY_ESCAPE) {
			cancelQuantity();
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
		// 26.3 uses SDL scancodes: 1..9 = 30..38 and 0 = 39, so the digit constants are not a
		// contiguous range in numeric order.
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
			return Mth.clamp(Integer.parseInt(quantity), 1, pending.max());
		} catch (NumberFormatException exception) {
			return 1;
		}
	}

	private void confirm(int amount) {
		if (pending == null) {
			return;
		}
		sendAmount(pending.from(), pending.to(), Mth.clamp(amount, 1, pending.max()));
		pending = null;
		quantity = "";
		selected = null;
	}

	private void cancelQuantity() {
		pending = null;
		quantity = "";
	}

	private void requestTransfer(SlotHit from, SlotHit to) {
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

		if (to.side() == Side.WORKER && to.index() == TOOL_SLOT && !JobEquipment.validTool(worker.jobId(), source)) {
			showError(Component.translatable(
					"laborcraft.command.worker.inv.tool.invalid",
					JobEquipment.toolName(worker.jobId())
			));
			return;
		}

		int room = target.isEmpty()
				? source.getMaxStackSize()
				: target.getMaxStackSize() - target.getCount();
		int max = Math.min(source.getCount(), room);
		if (max <= 0) {
			showError(Component.translatable("laborcraft.command.worker.inv.slot_full"));
			return;
		}
		if (source.getCount() <= 1 || max <= 1) {
			sendAmount(from, to, Math.min(source.getCount(), max));
			return;
		}

		pending = new PendingTransfer(from, to, max, from.side() == Side.PLAYER);
		quantity = "";
	}

	private void sendSwap(SlotHit from, SlotHit to) {
		int workerSlot = from.side() == Side.WORKER ? from.index() : to.index();
		int playerSlot = from.side() == Side.PLAYER ? from.index() : to.index();
		LaborCraftUiActions.run(
				LaborCraftCommands.WORKER_INV_SWAP + " " + worker.workerId() + " " + workerSlot + " " + playerSlot
		);
	}

	private void sendAmount(SlotHit from, SlotHit to, int amount) {
		int workerSlot = from.side() == Side.WORKER ? from.index() : to.index();
		int playerSlot = from.side() == Side.PLAYER ? from.index() : to.index();
		String command = from.side() == Side.PLAYER
				? LaborCraftCommands.WORKER_INV_DEPOSIT
				: LaborCraftCommands.WORKER_INV_WITHDRAW;
		LaborCraftUiActions.run(
				command + " " + worker.workerId() + " " + workerSlot + " " + playerSlot + " " + amount
		);
	}

	private ItemStack stackAt(SlotHit hit) {
		return stackAt(hit.side(), hit.index());
	}

	private ItemStack stackAt(Side side, int index) {
		if (side == Side.WORKER) {
			if (index == TOOL_SLOT) {
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

	private SlotHit slotAt(double mouseX, double mouseY) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int index = 9 + row * 9 + column;
				if (inside(mouseX, mouseY, playerX + column * STEP, playerY + row * STEP, SLOT, SLOT)) {
					return new SlotHit(Side.PLAYER, index);
				}
			}
		}
		for (int column = 0; column < 9; column++) {
			if (inside(mouseX, mouseY, playerX + column * STEP, playerY + 3 * STEP + 10, SLOT, SLOT)) {
				return new SlotHit(Side.PLAYER, column);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				if (inside(mouseX, mouseY, workerX + column * STEP, workerY + row * STEP, SLOT, SLOT)) {
					return new SlotHit(Side.WORKER, row * 3 + column);
				}
			}
		}
		if (inside(mouseX, mouseY, workerX + 3 * STEP + 8, workerY, SLOT, SLOT)) {
			return new SlotHit(Side.WORKER, TOOL_SLOT);
		}
		return null;
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
