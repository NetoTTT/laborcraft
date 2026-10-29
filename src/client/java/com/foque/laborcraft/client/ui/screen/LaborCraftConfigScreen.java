package com.foque.laborcraft.client.ui.screen;

import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.quillui.layout.QuillFlexBox;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillCardBox;
import com.quillui.widget.QuillNodes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Language and client preferences. Rebuilt with the measured Quill API. */
public final class LaborCraftConfigScreen extends QuillScreen {
	public LaborCraftConfigScreen() {
		super(Component.translatable("laborcraft.ui.config.title"), 420, 260);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.config.subtitle");
	}

	@Override
	protected void buildContent() {
		String current = Minecraft.getInstance().getLanguageManager().getSelected();
		boolean portuguese = current.startsWith("pt");
		boolean english = current.startsWith("en");

		QuillFlexBox languages = QuillNodes.row(QuillSpacing.SPACE_2);
		languages.add(QuillNodes.flexible(control(languageButton(
				Component.translatable("laborcraft.ui.config.language.pt"),
				portuguese,
				LaborCraftCommands.LANGUAGE_PT_BR
		))));
		languages.add(QuillNodes.flexible(control(languageButton(
				Component.translatable("laborcraft.ui.config.language.en"),
				english,
				LaborCraftCommands.LANGUAGE_EN_US
		))));

		QuillFlexBox body = QuillNodes.column(QuillSpacing.SPACE_2);
		body.add(text(
				Component.translatable("laborcraft.ui.config.language.desc"),
				MUTED
		));
		body.add(languages);

		QuillFlexBox actions = QuillNodes.row(QuillSpacing.SPACE_2);
		actions.add(QuillNodes.widget(backButton(), 90, 24));
		actions.add(QuillNodes.flexible(QuillNodes.spacer()));

		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_3);
		root.add(new QuillCardBox(
				Component.translatable("laborcraft.ui.config.language"),
				body
		));
		root.add(actions);

		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillButton languageButton(Component label, boolean selected, String command) {
		QuillButton button = new QuillButton(
				0,
				0,
				120,
				26,
				label,
				QuillButton.Variant.SECONDARY,
				() -> LaborCraftUiActions.run(command)
		);
		button.selected(selected);
		button.tip(QuillTooltips.of(
				label,
				Component.translatable("laborcraft.ui.config.language.desc"),
				"/" + command
		));
		return button;
	}

	private QuillButton backButton() {
		QuillButton back = new QuillButton(
				0,
				0,
				90,
				24,
				Component.translatable("laborcraft.ui.config.back"),
				QuillButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.UI)
		);
		back.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.config.back"),
				Component.translatable("laborcraft.ui.config.back.tip"),
				"/" + LaborCraftCommands.UI
		));
		return back;
	}
}
