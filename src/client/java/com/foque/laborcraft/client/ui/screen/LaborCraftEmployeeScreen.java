package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.LcWorkerText;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.ui.widget.LcJobOption;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.job.JobType;
import com.foque.laborcraft.job.JobTypes;
import com.foque.laborcraft.network.OpenWorkerPayload;
import com.foque.laborcraft.network.WorkerScreenStatePayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LaborCraftEmployeeScreen extends LaborCraftWorkerScreen {
	private boolean confirmDismiss;
	private LcButton dismissButton;

	public LaborCraftEmployeeScreen(OpenWorkerPayload worker) {
		super(worker, Component.translatable("laborcraft.ui.employee.title"), 460, 360);
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
		return Component.translatable("laborcraft.ui.employee.subtitle");
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int buttonsY = panelY + panelHeight() - 50;
		int closeWidth = 90;
		int gap = 8;

		dismissButton = new LcButton(
				x,
				buttonsY,
				130,
				24,
				Component.translatable("laborcraft.ui.employee.dismiss"),
				LcButton.Variant.DANGER,
				this::dismiss
		);
		dismissButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.employee.dismiss"),
				Component.translatable("laborcraft.ui.employee.dismiss.tip"),
				null
		));
		addRenderableWidget(dismissButton);

		LcButton backpack = new LcButton(
				x + 140,
				buttonsY,
				120,
				24,
				Component.translatable("laborcraft.ui.employee.backpack"),
				LcButton.Variant.SECONDARY,
				this::openBackpack
		);
		backpack.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.employee.backpack"),
				Component.translatable("laborcraft.ui.employee.backpack.tip"),
				"/" + LaborCraftCommands.WORKER_INV_OPEN + " " + workerId()
		));
		addRenderableWidget(backpack);

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

		JobType job = JobTypes.FARMER;
		String command = LaborCraftCommands.WORKER_ASSIGN + " " + workerId() + " " + job.id();
		LcJobOption option = new LcJobOption(
				x,
				panelY + 252,
				width,
				48,
				Component.translatable(job.translationKey()),
				Component.translatable(job.translationKey() + ".desc"),
				jobIcon(job),
				job.id().equals(worker.jobId()),
				"/" + command,
				() -> {
					if (!job.id().equals(worker.jobId())) {
						LaborCraftUiActions.run(command);
					}
				}
		);
		addRenderableWidget(option);

		addRenderableWidget(closeButton(this::onClose));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		int width = panelWidth() - 44;
		Component status = worker.jobId() == null || worker.jobId().isBlank()
				? Component.translatable("laborcraft.ui.worker.status.no_job")
				: stateLabel();
		int statusColor = worker.jobId() == null || worker.jobId().isBlank() ? LcTheme.MUTED : LcTheme.ACCENT;
		drawIdentity(graphics, x, panelY + 64, width, 90, status, statusColor);

		int jobY = panelY + 164;
		graphics.fill(x, jobY, x + width, jobY + 56, 0x401B232D);
		LcRender.border(graphics, x, jobY, width, 56, LcTheme.DIVIDER);
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.employee.current"),
				x + 12,
				jobY + 9,
				LcTheme.MUTED
		);
		graphics.text(font, jobLabel(), x + 12, jobY + 25, LcTheme.TITLE);
		Component state = Component.translatable("laborcraft.ui.employee.state", stateLabel());
		graphics.text(font, state, x + width - 12 - font.width(state), jobY + 25, LcTheme.TEXT);

		Component detail = worker.jobId() == null || worker.jobId().isBlank()
				? Component.translatable("laborcraft.ui.employee.no_cargo")
				: LcWorkerText.join(List.of(aptitudeLabel(), masteryLabel()), LcWorkerText.SEPARATOR);
		graphics.text(font, detail, x + 12, jobY + 41, LcTheme.MUTED);

		List<Component> boostLabels = LcWorkerText.boosts(worker.boosts());
		if (!boostLabels.isEmpty()) {
			graphics.text(
					font,
					LcWorkerText.join(boostLabels, LcWorkerText.SEPARATOR),
					x,
					panelY + 226,
					LcTheme.ACCENT
			);
		}

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.employee.assign"),
				x,
				panelY + 240,
				LcTheme.TEXT
		);
	}

	private void dismiss() {
		if (!confirmDismiss) {
			confirmDismiss = true;
			dismissButton.setMessage(Component.translatable("laborcraft.ui.employee.dismiss.confirm"));
			dismissButton.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.employee.dismiss.confirm"),
					Component.translatable("laborcraft.ui.employee.dismiss.confirm.tip"),
					null
			));
			return;
		}

		LaborCraftUiActions.run(LaborCraftCommands.WORKER_DISMISS + " " + workerId());
	}

	private void openBackpack() {
		LaborCraftUiActions.run(LaborCraftCommands.WORKER_INV_OPEN + " " + workerId());
	}

	private static ItemStack jobIcon(JobType job) {
		if (JobTypes.FARMER.id().equals(job.id())) {
			return new ItemStack(Items.WHEAT);
		}

		return new ItemStack(Items.EMERALD);
	}
}
