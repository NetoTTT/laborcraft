package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.network.OpenHomeRegisterPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Modal shown after sneak-clicking a bed; registers, renames or releases the house. */
public final class LaborCraftHomeRegisterScreen extends LaborCraftScreen {
	private final OpenHomeRegisterPayload home;
	private EditBox nameBox;
	private LcButton unclaimButton;
	private boolean confirmUnclaim;

	public LaborCraftHomeRegisterScreen(OpenHomeRegisterPayload home) {
		super(Component.translatable("laborcraft.ui.home.register.title"), 440, 240);
		this.home = home;
	}

	@Override
	protected Component subtitle() {
		if (!home.valid()) {
			return Component.translatable("laborcraft.ui.home.register.subtitle.invalid");
		}
		if (home.canRegister()) {
			return Component.translatable("laborcraft.ui.home.register.subtitle.confirm");
		}
		if (home.alreadyOwner()) {
			return Component.translatable("laborcraft.ui.home.register.subtitle.mine");
		}
		if (home.claimed()) {
			return Component.translatable("laborcraft.ui.home.register.subtitle.claimed");
		}
		return Component.translatable("laborcraft.ui.home.register.subtitle.invalid");
	}

	@Override
	protected void buildContent() {
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int buttonsY = panelY + 158;

		if (home.canRegister()) {
			addNameBox(x, width);
			LcButton confirm = new LcButton(
					x,
					buttonsY,
					170,
					24,
					Component.translatable("laborcraft.ui.home.register.confirm"),
					LcButton.Variant.PRIMARY,
					this::confirm
			);
			addRenderableWidget(confirm);

			LcButton cancel = new LcButton(
					x + 178,
					buttonsY,
					110,
					24,
					Component.translatable("laborcraft.ui.home.register.cancel"),
					LcButton.Variant.GHOST,
					this::onClose
			);
			addRenderableWidget(cancel);
		} else if (home.alreadyOwner()) {
			addNameBox(x, width);
			LcButton rename = new LcButton(
					x,
					buttonsY,
					100,
					24,
					Component.translatable("laborcraft.ui.home.register.rename"),
					LcButton.Variant.PRIMARY,
					this::rename
			);
			rename.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.home.register.rename"),
					Component.translatable("laborcraft.ui.home.register.rename.tip"),
					"/" + LaborCraftCommands.HOME_RENAME
			));
			addRenderableWidget(rename);

			unclaimButton = new LcButton(
					x + 108,
					buttonsY,
					160,
					24,
					Component.translatable("laborcraft.ui.home.register.unclaim"),
					LcButton.Variant.DANGER,
					this::unclaim
			);
			unclaimButton.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.home.register.unclaim"),
					Component.translatable("laborcraft.ui.home.register.unclaim.tip"),
					"/" + LaborCraftCommands.HOME_REMOVE
			));
			addRenderableWidget(unclaimButton);

			LcButton close = new LcButton(
					x + 276,
					buttonsY,
					80,
					24,
					Component.translatable("laborcraft.ui.worker.close"),
					LcButton.Variant.GHOST,
					this::onClose
			);
			addRenderableWidget(close);
		} else {
			LcButton close = new LcButton(
					x + width - 100,
					buttonsY,
					100,
					24,
					Component.translatable("laborcraft.ui.worker.close"),
					LcButton.Variant.GHOST,
					this::onClose
			);
			addRenderableWidget(close);
		}
	}

	private void addNameBox(int x, int width) {
		nameBox = new EditBox(
				font,
				x,
				panelY + 130,
				width,
				20,
				Component.translatable("laborcraft.ui.home.register.name")
		);
		nameBox.setMaxLength(32);
		nameBox.setValue(home.suggestedName());
		nameBox.setHint(Component.translatable("laborcraft.ui.home.register.name.hint"));
		addRenderableWidget(nameBox);
		setInitialFocus(nameBox);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		int x = panelX + 22;
		graphics.text(
				font,
				Component.translatable("laborcraft.ui.home.register.bed", home.x(), home.y(), home.z()),
				x,
				panelY + 64,
				LcTheme.MUTED
		);
		graphics.text(
				font,
				Component.translatable(
						"laborcraft.ui.home.register.size",
						home.width(),
						home.depth(),
						home.doors(),
						home.beds(),
						home.slots(),
						home.capacity()
				),
				x,
				panelY + 78,
				LcTheme.TEXT
		);

		Component status;
		int statusColor;
		if (!home.valid()) {
			status = Component.translatable(
					"laborcraft.ui.home.register.reason",
					Component.translatable("laborcraft.housing.reason." + (home.failure().isBlank() ? "invalid" : home.failure()))
			);
			statusColor = LcTheme.DANGER;
		} else if (home.canRegister()) {
			status = home.claimed()
					? Component.translatable("laborcraft.ui.home.register.repair")
					: Component.translatable("laborcraft.ui.home.register.ready");
			statusColor = LcTheme.ACCENT;
		} else if (home.alreadyOwner()) {
			status = Component.translatable("laborcraft.ui.home.register.already", home.existingName());
			statusColor = LcTheme.MUTED;
		} else {
			status = Component.translatable(
					"laborcraft.ui.home.register.claimed",
					home.ownerName(),
					home.existingName()
			);
			statusColor = LcTheme.DANGER;
		}
		graphics.text(font, status, x, panelY + 94, statusColor);

		if (home.canRegister() || home.alreadyOwner()) {
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.home.register.name"),
					x,
					panelY + 116,
					LcTheme.TEXT
			);
			graphics.text(
					font,
					Component.translatable(
							home.alreadyOwner()
									? "laborcraft.ui.home.register.hint.manage"
									: "laborcraft.ui.home.register.hint"
					),
					x,
					panelY + 188,
					LcTheme.MUTED
			);
		}
	}

	private void confirm() {
		String name = cleanName();
		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_SET + " " + home.x() + " " + home.y() + " " + home.z() + " " + name
		);
		onClose();
	}

	private void rename() {
		String name = cleanName();
		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_RENAME + " " + home.x() + " " + home.y() + " " + home.z() + " " + name
		);
		onClose();
	}

	private void unclaim() {
		if (!confirmUnclaim) {
			confirmUnclaim = true;
			unclaimButton.setMessage(Component.translatable("laborcraft.ui.home.register.unclaim.confirm"));
			unclaimButton.tip(LcTooltips.of(
					Component.translatable("laborcraft.ui.home.register.unclaim.confirm"),
					Component.translatable("laborcraft.ui.home.register.unclaim.confirm.tip"),
					null
			));
			return;
		}

		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_REMOVE + " " + home.x() + " " + home.y() + " " + home.z()
		);
		onClose();
	}

	private String cleanName() {
		String name = nameBox == null ? "" : nameBox.getValue().replace('"', '\'').strip();
		return name.isBlank() ? home.suggestedName() : name;
	}
}
