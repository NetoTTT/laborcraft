package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcRender;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.component.LcPagination;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.command.LaborCraftCommands;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class LaborCraftPlayersScreen extends LaborCraftScreen {
	private static final int PAGE_SIZE = 6;

	private final LcPagination<PlayerInfo> pagination;
	private LcButton previousButton;
	private LcButton nextButton;

	public LaborCraftPlayersScreen() {
		super(Component.translatable("laborcraft.ui.players.title"), 420, 300);
		this.pagination = new LcPagination<>(players(), PAGE_SIZE);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.ui.players.subtitle");
	}

	@Override
	protected void buildContent() {
		previousButton = new LcButton(
				panelX + panelWidth() - 52,
				panelY + panelHeight() - 46,
				20,
				24,
				Component.literal("<"),
				LcButton.Variant.SECONDARY,
				this::previousPage
		);
		previousButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.players.previous"),
				Component.translatable("laborcraft.ui.players.previous.tip"),
				null
		));
		addRenderableWidget(previousButton);

		nextButton = new LcButton(
				panelX + panelWidth() - 26,
				panelY + panelHeight() - 46,
				20,
				24,
				Component.literal(">"),
				LcButton.Variant.SECONDARY,
				this::nextPage
		);
		nextButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.players.next"),
				Component.translatable("laborcraft.ui.players.next.tip"),
				null
		));
		addRenderableWidget(nextButton);

		LcButton back = new LcButton(
				panelX + 22,
				panelY + panelHeight() - 46,
				90,
				24,
				Component.translatable("laborcraft.ui.players.back"),
				LcButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.UI)
		);
		back.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.players.back"),
				Component.translatable("laborcraft.ui.players.back.tip"),
				"/" + LaborCraftCommands.UI
		));
		addRenderableWidget(back);

		LcButton close = new LcButton(
				panelX + panelWidth() - 30,
				panelY + 8,
				20,
				20,
				Component.literal("X"),
				LcButton.Variant.GHOST,
				this::onClose
		);
		close.tip(LcTooltips.of(
				Component.translatable("laborcraft.ui.players.close"),
				Component.translatable("laborcraft.ui.players.close.tip"),
				null
		));
		addRenderableWidget(close);

		refreshPage();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		Minecraft client = Minecraft.getInstance();
		UUID localId = client.player == null ? null : client.player.getUUID();
		int x = panelX + 22;
		int width = panelWidth() - 44;
		int rowY = panelY + 66;

		graphics.text(
				font,
				Component.translatable("laborcraft.ui.players.count", pagination.total()),
				x,
				panelY + 54,
				LcTheme.MUTED
		);

		List<PlayerInfo> visible = pagination.pageItems();
		if (visible.isEmpty()) {
			graphics.text(
					font,
					Component.translatable("laborcraft.ui.players.none"),
					x,
					rowY + 10,
					LcTheme.MUTED
			);
		}

		for (int index = 0; index < visible.size(); index++) {
			PlayerInfo info = visible.get(index);
			boolean local = Objects.equals(info.getProfile().id(), localId);
			int y = rowY + index * 28;

			graphics.fill(x, y, x + width, y + 26, 0x401B232D);
			LcRender.border(graphics, x, y, width, 26, rowBorder(local));
			PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), x + 6, y + 5, 16);
			graphics.text(font, name(info), x + 30, y + 4, local ? LcTheme.TITLE : LcTheme.TEXT);
			graphics.text(
					font,
					local
							? Component.translatable("laborcraft.ui.players.self", info.getLatency())
							: Component.translatable("laborcraft.ui.players.ping", info.getLatency()),
					x + 30,
					y + 14,
					local ? LcTheme.ACCENT : LcTheme.MUTED
			);
		}

		graphics.centeredText(
				font,
				Component.translatable(
						"laborcraft.ui.players.page",
						pagination.pageIndex() + 1,
						pagination.pageCount()
				),
				panelX + panelWidth() / 2,
				panelY + 240,
				LcTheme.MUTED
		);
	}

	private static int rowBorder(boolean local) {
		return local ? LcTheme.ACCENT : LcTheme.DIVIDER;
	}

	private void previousPage() {
		if (pagination.previous()) {
			refreshPage();
		}
	}

	private void nextPage() {
		if (pagination.next()) {
			refreshPage();
		}
	}

	private void refreshPage() {
		previousButton.active = pagination.hasPrevious();
		nextButton.active = pagination.hasNext();
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

		UUID localId = client.player == null ? null : client.player.getUUID();
		List<PlayerInfo> players = new ArrayList<>(client.getConnection().getOnlinePlayers());
		players.sort(
				Comparator
						.comparing((PlayerInfo info) -> !Objects.equals(info.getProfile().id(), localId))
						.thenComparing(info -> name(info).getString(), String.CASE_INSENSITIVE_ORDER)
		);
		return players;
	}
}
