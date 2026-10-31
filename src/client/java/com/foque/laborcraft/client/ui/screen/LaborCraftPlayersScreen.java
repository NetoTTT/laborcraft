package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.component.LcPagination;
import com.foque.laborcraft.client.ui.widget.PlayerFaceWidget;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.quillui.layout.QuillCrossAxisAlignment;
import com.quillui.layout.QuillFlexBox;
import com.quillui.layout.QuillLayoutNode;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillListView;
import com.quillui.widget.QuillNodes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Online players with pagination, rebuilt on the measured content tree. */
public final class LaborCraftPlayersScreen extends QuillScreen {
	private static final int PAGE_SIZE = 6;

	private final LcPagination<PlayerInfo> pagination;
	private QuillListView<PlayerInfo> list;

	public LaborCraftPlayersScreen() {
		super(Component.translatable("laborcraft.ui.players.title"), 430, 320, LaborCraftTheme.currentData());
		this.pagination = new LcPagination<>(players(), PAGE_SIZE);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.players.subtitle");
	}

	@Override
	protected void buildContent() {
		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.crossAxisAlignment(QuillCrossAxisAlignment.STRETCH);

		root.add(text(Component.translatable(
				"laborcraft.ui.players.count",
				pagination.total()
		), MUTED));

		if (pagination.pageItems().isEmpty()) {
			root.add(text(Component.translatable("laborcraft.ui.players.none"), MUTED));
		}
		else {
			list = new QuillListView<>(
					0, 0, 300, 156, 26,
					this::playerRow,
					null
			);
			list.replaceItems(pagination.pageItems());
			root.add(QuillNodes.flexible(control(list), 1));
		}

		root.add(paginationRow());
		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillLayoutNode playerRow(PlayerInfo info, int index) {
		boolean local = Objects.equals(info.getProfile().id(), localId());
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.crossAxisAlignment(QuillCrossAxisAlignment.CENTER);
		row.add(QuillNodes.widget(new PlayerFaceWidget(info.getSkin(), 16), 16, 16));

		QuillFlexBox identity = QuillNodes.column(2);
		identity.add(control(local ? label(name(info)).title() : label(name(info))));
		Component detail = Component.translatable(
				local ? "laborcraft.ui.players.self" : "laborcraft.ui.players.ping",
				info.getLatency()
		);
		identity.add(control(local ? label(detail).accent() : label(detail).muted()));
		row.add(QuillNodes.flexible(identity));
		return row;
	}

	private QuillFlexBox paginationRow() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);
		row.add(QuillNodes.widget(backButton(), 90, 24));
		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		row.add(text(Component.translatable(
				"laborcraft.ui.players.page",
				pagination.pageIndex() + 1,
				pagination.pageCount()
		), MUTED));
		row.add(QuillNodes.widget(previousButton(), 20, 24));
		row.add(QuillNodes.widget(nextButton(), 20, 24));
		return row;
	}

	private QuillButton previousButton() {
		QuillButton button = new QuillButton(
				0, 0, 20, 24,
				Component.literal("<"),
				QuillButton.Variant.SECONDARY,
				() -> {
					if (pagination.previous()) {
						refreshContent();
					}
				}
		).enabled(pagination.hasPrevious());
		button.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.players.previous"),
				Component.translatable("laborcraft.ui.players.previous.tip"),
				null
		));
		return button;
	}

	private QuillButton nextButton() {
		QuillButton button = new QuillButton(
				0, 0, 20, 24,
				Component.literal(">"),
				QuillButton.Variant.SECONDARY,
				() -> {
					if (pagination.next()) {
						refreshContent();
					}
				}
		).enabled(pagination.hasNext());
		button.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.players.next"),
				Component.translatable("laborcraft.ui.players.next.tip"),
				null
		));
		return button;
	}

	private QuillButton backButton() {
		QuillButton button = new QuillButton(
				0, 0, 90, 24,
				Component.translatable("laborcraft.ui.players.back"),
				QuillButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.UI)
		);
		button.tip(QuillTooltips.of(
				Component.translatable("laborcraft.ui.players.back"),
				Component.translatable("laborcraft.ui.players.back.tip"),
				"/" + LaborCraftCommands.UI
		));
		return button;
	}

	private static UUID localId() {
		Minecraft client = Minecraft.getInstance();
		return client.player == null ? null : client.player.getUUID();
	}

	private static Component name(PlayerInfo info) {
		Component display = info.getTabListDisplayName();
		if (display != null) {
			return display.copy();
		}
		String name = info.getProfile().name();
		return Component.literal(name == null ? "?" : name);
	}

	private static List<PlayerInfo> players() {
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null) {
			return List.of();
		}

		UUID localId = localId();
		List<PlayerInfo> players = new ArrayList<>(client.getConnection().getOnlinePlayers());
		players.sort(
				Comparator
						.comparing((PlayerInfo info) ->
								!Objects.equals(info.getProfile().id(), localId))
						.thenComparing(info -> name(info).getString(), String.CASE_INSENSITIVE_ORDER)
		);
		return players;
	}
}
