package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.LcTheme;
import com.foque.laborcraft.client.ui.LcTooltips;
import com.foque.laborcraft.client.ui.component.LcPagination;
import com.foque.laborcraft.client.ui.widget.LcButton;
import com.foque.laborcraft.client.ui.widget.LcItemGrid;
import com.foque.laborcraft.command.LaborCraftCommands;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LaborCraftItemTestScreen extends LaborCraftScreen {
	private static final int PAGE_SIZE = 50;
	private static final int COLUMNS = 10;
	private static final int ROWS = 5;
	private static final int CELL = 26;
	private static final int GAP = 4;

	private final LcPagination<ItemEntry> pagination;
	private LcItemGrid<ItemEntry> grid;
	private LcButton previousButton;
	private LcButton nextButton;

	public LaborCraftItemTestScreen() {
		this(0);
	}

	public LaborCraftItemTestScreen(int page) {
		super(Component.translatable("laborcraft.test.title"), 460, 300);
		this.pagination = new LcPagination<>(items(), PAGE_SIZE);
		this.pagination.setPage(page);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.test.subtitle");
	}

	@Override
	protected void buildContent() {
		int gridWidth = COLUMNS * CELL + (COLUMNS - 1) * GAP;
		int gridX = panelX + (panelWidth() - gridWidth) / 2;
		int gridY = panelY + 68;

		grid = new LcItemGrid<>(
				gridX,
				gridY,
				COLUMNS,
				ROWS,
				CELL,
				GAP,
				ItemEntry::stack,
				ItemEntry::amount,
				this::openItem
		);
		addRenderableWidget(grid);

		previousButton = new LcButton(
				panelX + 22,
				panelY + 232,
				80,
				24,
				Component.literal("<"),
				LcButton.Variant.SECONDARY,
				this::previousPage
		);
		previousButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.previous"),
				Component.translatable("laborcraft.test.previous.tip"),
				null
		));
		addRenderableWidget(previousButton);

		nextButton = new LcButton(
				panelX + panelWidth() - 102,
				panelY + 232,
				80,
				24,
				Component.literal(">"),
				LcButton.Variant.SECONDARY,
				this::nextPage
		);
		nextButton.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.next"),
				Component.translatable("laborcraft.test.next.tip"),
				null
		));
		addRenderableWidget(nextButton);

		LcButton admin = new LcButton(
				panelX + panelWidth() - 100,
				panelY + 8,
				64,
				20,
				Component.translatable("laborcraft.test.admin"),
				LcButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.UI)
		);
		admin.tip(LcTooltips.of(
				Component.translatable("laborcraft.test.admin"),
				Component.translatable("laborcraft.test.admin.tip"),
				"/" + LaborCraftCommands.UI
		));
		addRenderableWidget(admin);

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
				Component.translatable("laborcraft.test.close"),
				Component.translatable("laborcraft.test.close.tip"),
				null
		));
		addRenderableWidget(close);

		refreshPage();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		graphics.centeredText(
				font,
				Component.translatable(
						"laborcraft.test.page",
						pagination.pageIndex() + 1,
						pagination.pageCount()
				),
				panelX + panelWidth() / 2,
				panelY + 220,
				LcTheme.MUTED
		);
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
		grid.setEntries(pagination.pageItems());
		previousButton.active = pagination.hasPrevious();
		nextButton.active = pagination.hasNext();
	}

	private void openItem(ItemEntry entry) {
		Minecraft client = Minecraft.getInstance();
		int page = pagination.pageIndex();
		client.setScreenAndShow(new LaborCraftItemActionScreen(
				entry.stack(),
				entry.kind().label(),
				optionsFor(entry),
				() -> client.setScreenAndShow(new LaborCraftItemTestScreen(page)),
				entry.amount()
		));
	}

	private List<LaborCraftItemActionScreen.Option> optionsFor(ItemEntry entry) {
		Component name = entry.stack().getHoverName();
		Kind kind = entry.kind();
		List<LaborCraftItemActionScreen.Option> options = new ArrayList<>();

		options.add(new LaborCraftItemActionScreen.Option(
				kind.actionLabel(),
				Component.translatable("laborcraft.test.action.desc"),
				null,
				false,
				screen -> {
					screen.notify(Component.translatable(
							"laborcraft.test.message.action",
							kind.actionLabel(),
							name
					));
					screen.back();
				}
		));

		options.add(new LaborCraftItemActionScreen.Option(
				Component.translatable("laborcraft.test.item.take"),
				Component.translatable("laborcraft.test.item.take.desc"),
				null,
				true,
				screen -> {
					screen.notify(Component.translatable(
							"laborcraft.test.message.took",
							screen.quantity(),
							name
					));
					screen.back();
				}
		));

		options.add(new LaborCraftItemActionScreen.Option(
				Component.translatable("laborcraft.test.item.leave"),
				Component.translatable("laborcraft.test.item.leave.desc"),
				null,
				false,
				screen -> {
					screen.notify(Component.translatable(
							"laborcraft.test.message.left",
							name
					));
					screen.back();
				},
				null
		));

		return options;
	}

	private static List<ItemEntry> items() {
		return List.of(
				entry(Items.APPLE, 12, Kind.FOOD),
				entry(Items.BREAD, 5, Kind.FOOD),
				entry(Items.GOLDEN_APPLE, 2, Kind.FOOD),
				entry(Items.COOKED_BEEF, 9, Kind.FOOD),
				entry(Items.CARROT, 16, Kind.FOOD),
				entry(Items.POTATO, 7, Kind.FOOD),
				entry(Items.COOKED_CHICKEN, 8, Kind.FOOD),
				entry(Items.GOLDEN_CARROT, 4, Kind.FOOD),
				entry(Items.MELON_SLICE, 13, Kind.FOOD),
				entry(Items.SWEET_BERRIES, 21, Kind.FOOD),
				entry(Items.PUMPKIN_PIE, 6, Kind.FOOD),
				entry(Items.COOKIE, 18, Kind.FOOD),
				entry(Items.IRON_PICKAXE, 1, Kind.TOOL),
				entry(Items.DIAMOND_SWORD, 1, Kind.TOOL),
				entry(Items.WOODEN_SHOVEL, 1, Kind.TOOL),
				entry(Items.BOW, 1, Kind.TOOL),
				entry(Items.FISHING_ROD, 1, Kind.TOOL),
				entry(Items.SHEARS, 1, Kind.TOOL),
				entry(Items.IRON_AXE, 1, Kind.TOOL),
				entry(Items.IRON_SHOVEL, 1, Kind.TOOL),
				entry(Items.IRON_SWORD, 1, Kind.TOOL),
				entry(Items.DIAMOND_PICKAXE, 1, Kind.TOOL),
				entry(Items.CROSSBOW, 1, Kind.TOOL),
				entry(Items.TRIDENT, 1, Kind.TOOL),
				entry(Items.OAK_LOG, 32, Kind.BLOCK),
				entry(Items.STONE, 160, Kind.BLOCK),
				entry(Items.GLASS, 16, Kind.BLOCK),
				entry(Items.TORCH, 24, Kind.BLOCK),
				entry(Items.CRAFTING_TABLE, 1, Kind.BLOCK),
				entry(Items.FURNACE, 1, Kind.BLOCK),
				entry(Items.OAK_PLANKS, 64, Kind.BLOCK),
				entry(Items.COBBLESTONE, 128, Kind.BLOCK),
				entry(Items.SAND, 96, Kind.BLOCK),
				entry(Items.DIRT, 64, Kind.BLOCK),
				entry(Items.BRICKS, 32, Kind.BLOCK),
				entry(Items.LANTERN, 12, Kind.BLOCK),
				entry(Items.IRON_BLOCK, 5, Kind.BLOCK),
				entry(Items.GOLD_BLOCK, 4, Kind.BLOCK),
				entry(Items.DIAMOND, 6, Kind.MATERIAL),
				entry(Items.EMERALD, 3, Kind.MATERIAL),
				entry(Items.IRON_INGOT, 20, Kind.MATERIAL),
				entry(Items.GOLD_INGOT, 8, Kind.MATERIAL),
				entry(Items.COPPER_INGOT, 36, Kind.MATERIAL),
				entry(Items.REDSTONE, 2400, Kind.MATERIAL),
				entry(Items.COAL, 24, Kind.MATERIAL),
				entry(Items.STICK, 48, Kind.MATERIAL),
				entry(Items.WHEAT, 30, Kind.MATERIAL),
				entry(Items.STRING, 12, Kind.MATERIAL),
				entry(Items.LEATHER, 9, Kind.MATERIAL),
				entry(Items.CLAY_BALL, 14, Kind.MATERIAL)
		);
	}

	private static ItemEntry entry(Item item, long amount, Kind kind) {
		return new ItemEntry(new ItemStack(item), amount, kind);
	}

	private record ItemEntry(ItemStack stack, long amount, Kind kind) {
	}

	private enum Kind {
		FOOD("laborcraft.test.kind.food", "laborcraft.test.action.eat"),
		TOOL("laborcraft.test.kind.tool", "laborcraft.test.action.equip"),
		BLOCK("laborcraft.test.kind.block", "laborcraft.test.action.place"),
		MATERIAL("laborcraft.test.kind.material", "laborcraft.test.action.inspect");

		private final String labelKey;
		private final String actionKey;

		Kind(String labelKey, String actionKey) {
			this.labelKey = labelKey;
			this.actionKey = actionKey;
		}

		Component label() {
			return Component.translatable(labelKey);
		}

		Component actionLabel() {
			return Component.translatable(actionKey);
		}
	}
}
