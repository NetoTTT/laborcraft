package com.foque.laborcraft.client.ui.screen;

import com.foque.laborcraft.client.ui.theme.LaborCraftTheme;

import com.foque.laborcraft.client.ui.LaborCraftUiActions;
import com.foque.laborcraft.client.ui.component.LcPagination;
import com.foque.laborcraft.client.ui.widget.LcItemGrid;
import com.foque.laborcraft.command.LaborCraftCommands;
import com.quillui.layout.QuillFlexBox;
import com.quillui.render.QuillTooltips;
import com.quillui.screen.QuillScreen;
import com.quillui.theme.QuillSpacing;
import com.quillui.widget.QuillButton;
import com.quillui.widget.QuillNodes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LaborCraftItemTestScreen extends QuillScreen {
	private static final int PAGE_SIZE = 50;
	private static final int COLUMNS = 10;
	private static final int ROWS = 5;
	private static final int CELL = 26;
	private static final int GAP = 4;

	private final LcPagination<ItemEntry> pagination;

	public LaborCraftItemTestScreen() {
		this(0);
	}

	public LaborCraftItemTestScreen(int page) {
		super(Component.translatable("laborcraft.test.title"), 460, 300, LaborCraftTheme.currentData());
		this.pagination = new LcPagination<>(items(), PAGE_SIZE);
		this.pagination.setPage(page);
	}

	@Override
	protected Component subtitle() {
		return Component.translatable("laborcraft.test.subtitle");
	}

	@Override
	protected void buildContent() {
		LcItemGrid<ItemEntry> grid = new LcItemGrid<>(
				0,
				0,
				COLUMNS,
				ROWS,
				CELL,
				GAP,
				ItemEntry::stack,
				ItemEntry::amount,
				this::openItem
		);
		grid.setEntries(pagination.pageItems());

		QuillFlexBox centered = QuillNodes.row(0);
		centered.add(QuillNodes.flexible(QuillNodes.spacer()));
		centered.add(QuillNodes.sized(
				COLUMNS * CELL + (COLUMNS - 1) * GAP,
				ROWS * CELL + (ROWS - 1) * GAP,
				control(grid)
		));
		centered.add(QuillNodes.flexible(QuillNodes.spacer()));

		QuillFlexBox pageRow = QuillNodes.row(0);
		pageRow.add(QuillNodes.flexible(QuillNodes.spacer()));
		pageRow.add(text(Component.translatable(
				"laborcraft.test.page",
				pagination.pageIndex() + 1,
				pagination.pageCount()
		)));
		pageRow.add(QuillNodes.flexible(QuillNodes.spacer()));

		QuillFlexBox root = QuillNodes.column(QuillSpacing.SPACE_2);
		root.add(centered);
		root.add(pageRow);
		root.add(actions());
		setContent(root);
		addRenderableWidget(closeButton(this::onClose));
	}

	private QuillFlexBox actions() {
		QuillFlexBox row = QuillNodes.row(QuillSpacing.SPACE_2);

		QuillButton previous = new QuillButton(
				0, 0, 80, 24,
				Component.literal("<"),
				QuillButton.Variant.SECONDARY,
				() -> {
					if (pagination.previous()) {
						refreshContent();
					}
				}
		).enabled(pagination.hasPrevious());
		previous.tip(QuillTooltips.of(
				Component.translatable("laborcraft.test.previous"),
				Component.translatable("laborcraft.test.previous.tip"),
				null
		));
		row.add(QuillNodes.widget(previous, 80, 24));

		QuillButton admin = new QuillButton(
				0, 0, 64, 24,
				Component.translatable("laborcraft.test.admin"),
				QuillButton.Variant.GHOST,
				() -> LaborCraftUiActions.run(LaborCraftCommands.UI)
		);
		admin.tip(QuillTooltips.of(
				Component.translatable("laborcraft.test.admin"),
				Component.translatable("laborcraft.test.admin.tip"),
				"/" + LaborCraftCommands.UI
		));
		row.add(QuillNodes.flexible(QuillNodes.spacer()));
		row.add(QuillNodes.widget(admin, 64, 24));

		QuillButton next = new QuillButton(
				0, 0, 80, 24,
				Component.literal(">"),
				QuillButton.Variant.SECONDARY,
				() -> {
					if (pagination.next()) {
						refreshContent();
					}
				}
		).enabled(pagination.hasNext());
		next.tip(QuillTooltips.of(
				Component.translatable("laborcraft.test.next"),
				Component.translatable("laborcraft.test.next.tip"),
				null
		));
		row.add(QuillNodes.widget(next, 80, 24));
		return row;
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
