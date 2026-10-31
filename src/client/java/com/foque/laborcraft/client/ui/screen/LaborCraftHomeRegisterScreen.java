package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.foque.laborcraft.network.OpenHomeRegisterPayload;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.theme.QuillThemeData;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillNodes;
import com.quillui.widget.QuillTextField;
import net.minecraft.network.chat.Component;

/** Register, rename or release a house after sneak-clicking a bed. */
public final class LaborCraftHomeRegisterScreen extends QuillScreen {
	private final OpenHomeRegisterPayload home;
	private QuillTextField nameBox;
	private QuillButton unclaimButton;
	private boolean confirmUnclaim;

	public LaborCraftHomeRegisterScreen(OpenHomeRegisterPayload home) {
		super(Component.translatable("laborcraft.ui.home.register.title"), 440, 260, LaborCraftTheme.currentData());
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
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);
		root.add(new QuillCardBox(null, info()));

		if (home.canRegister() || home.alreadyOwner()) {
			root.add(text(Component.translatable("laborcraft.ui.home.register.name")));
			nameBox = QuillTextField.of(
					font,
					Component.translatable("laborcraft.ui.home.register.name.hint")
			);
			nameBox.setMaxLength(32);
			nameBox.setValue(home.suggestedName());
			nameBox.setHint(Component.translatable("laborcraft.ui.home.register.name.hint"));
			root.add(control(nameBox, 20));
			root.add(text(Component.translatable(
					home.alreadyOwner()
							? "laborcraft.ui.home.register.hint.manage"
							: "laborcraft.ui.home.register.hint"
			), MUTED));
		}

		root.add(actions());
		setContent(root);

		if (nameBox != null) {
			setInitialFocus(nameBox);
		}
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillFlexBox info() {
		QuillFlexBox column = QuillNodes.column(2);
		column.add(text(Component.translatable(
				"laborcraft.ui.home.register.bed",
				home.x(),
				home.y(),
				home.z()
		), MUTED));
		column.add(text(Component.translatable(
				"laborcraft.ui.home.register.size",
				home.width(),
				home.depth(),
				home.doors(),
				home.beds(),
				home.slots(),
				home.capacity()
		)));

		Component status;
		QuillThemeData.ColorRole role;
		if (!home.valid()) {
			status = Component.translatable(
					"laborcraft.ui.home.register.reason",
					Component.translatable("laborcraft.housing.reason."
							+ (home.failure().isBlank() ? "invalid" : home.failure()))
			);
			role = DANGER;
		}
		else if (home.canRegister()) {
			status = home.claimed()
					? Component.translatable("laborcraft.ui.home.register.repair")
					: Component.translatable("laborcraft.ui.home.register.ready");
			role = ACCENT;
		}
		else if (home.alreadyOwner()) {
			status = Component.translatable("laborcraft.ui.home.register.already", home.existingName());
			role = MUTED;
		}
		else {
			status = Component.translatable(
					"laborcraft.ui.home.register.claimed",
					home.ownerName(),
					home.existingName()
			);
			role = DANGER;
		}
		column.add(text(status, role));
		return column;
	}

	private QuillFlexBox actions() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);

		if (home.canRegister()) {
			row.add(QuillNodes.widget(primary(
					"laborcraft.ui.home.register.confirm",
					this::confirm
			), 170, 24));
			row.add(QuillNodes.widget(ghost(
					"laborcraft.ui.home.register.cancel",
					this::onClose
			), 110, 24));
			return row;
		}

		if (home.alreadyOwner()) {
			QuillButton rename = primary("laborcraft.ui.home.register.rename", this::rename);
			rename.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.home.register.rename"),
					Component.translatable("laborcraft.ui.home.register.rename.tip"),
					"/" + LaborCraftCommands.HOME_RENAME
			));
			row.add(QuillNodes.widget(rename, 100, 24));

			unclaimButton = new QuillButton(
					0, 0, 160, 24,
					Component.translatable("laborcraft.ui.home.register.unclaim"),
					QuillButton.Variant.DANGER,
					this::unclaim
			);
			unclaimButton.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.home.register.unclaim"),
					Component.translatable("laborcraft.ui.home.register.unclaim.tip"),
					"/" + LaborCraftCommands.HOME_REMOVE
			));
			row.add(QuillNodes.widget(unclaimButton, 160, 24));

			row.add(QuillNodes.widget(ghost(
					"laborcraft.ui.worker.close",
					this::onClose
			), 80, 24));
			return row;
		}

		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		row.add(QuillNodes.widget(ghost("laborcraft.ui.worker.close", this::onClose), 100, 24));
		return row;
	}

	private QuillButton primary(String key, Runnable action) {
		return new QuillButton(
				0, 0, 120, 24,
				Component.translatable(key),
				QuillButton.Variant.PRIMARY,
				action
		);
	}

	private QuillButton ghost(String key, Runnable action) {
		return new QuillButton(
				0, 0, 120, 24,
				Component.translatable(key),
				QuillButton.Variant.GHOST,
				action
		);
	}

	private void confirm() {
		String name = cleanName();
		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_SET + " "
						+ home.x() + " " + home.y() + " " + home.z() + " " + name
		);
		onClose();
	}

	private void rename() {
		String name = cleanName();
		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_RENAME + " "
						+ home.x() + " " + home.y() + " " + home.z() + " " + name
		);
		onClose();
	}

	private void unclaim() {
		if (!confirmUnclaim) {
			confirmUnclaim = true;
			unclaimButton.setMessage(Component.translatable(
					"laborcraft.ui.home.register.unclaim.confirm"
			));
			unclaimButton.tip(QuillTooltips.of(
					Component.translatable("laborcraft.ui.home.register.unclaim.confirm"),
					Component.translatable("laborcraft.ui.home.register.unclaim.confirm.tip"),
					null
			));
			return;
		}

		LaborCraftUiActions.run(
				LaborCraftCommands.HOME_REMOVE + " "
						+ home.x() + " " + home.y() + " " + home.z()
		);
		onClose();
	}

	private String cleanName() {
		String name = nameBox == null ? "" : nameBox.getValue().replace('"', '\'').strip();
		return name.isBlank() ? home.suggestedName() : name;
	}
}
