# UI migration to Quill UI 0.3

Status of the screen-by-screen rewrite on `feat/quill-0.3-ui`.

## Rules

- New and migrated screens extend `com.quillui.screen.QuillScreen`.
- Content is built with `setContent` and `QuillNodes`; lists use `QuillListView`.
- No legacy UI class remains; screens, HUDs and widgets read `LaborCraftTheme`/`QuillThemeData`.
- Screen sizes come from the panel/content bounds, not from fixed `x`/`y` math.
- UI colours use `QuillColors.rgb(...)`/`QuillColors.argb(...)`, never hexadecimal literals.

## Measured screens

All screens use `setContent` and respond to the available content area:

- Config
- Jogadores online
- Home/Register
- Hiring
- Profile
- Item Action
- Item Test
- Employee
- Station (`LaborCraftStationV2Screen`, all three tabs)
- Worker Inventory (`LaborCraftWorkerInventoryV2Screen`, measured slot board)
- Admin (`LaborCraftAdminV2Screen`, all six tabs)

## Station

`LaborCraftStationV2Screen`:

- `QuillNodes.tabs` for Work / Config / Storage;
- Work: `QuillListView<Summary>` with Pay and highlight row actions, slot card and the
  bind/recall bar;
- Config: the `QuillMap` widget from `quill-ui-extras` is always embedded (read-only summary or
  draw mode) with saved/other zones, live selection, layer cycling and confirm/cancel/preview;
- Storage: `QuillListView<StorageView>` with a per-row role swap, unlink, detect and highlight;
- header balance through `QuillScreen.headerAccessory` and `QuillChip`;
- `QuillMap.setMergeZones(true)` merges touching same-style zones;
- the panel clamps and the content scrolls when the window is smaller than the panel.

## Worker Inventory

`LaborCraftWorkerInventoryV2Screen` uses a measured board widget (`WorkerInventoryBoard`) for the
player grid, the worker backpack and the tool slot. Transfers keep the same command flow, the
quantity picker is a Quill overlay, and the screen keeps the worker id for live payload updates.

## Admin

`LaborCraftAdminV2Screen` has six measured tabs:

- Salary: contract controls in a card plus rank/rarity multipliers in a second card;
- Billing: nine policy controls in a measured column;
- Schedule: mode, ticks, rain and idle checks in a card, with the note beside it;
- Area: work-area limits, housing requirements and housing limits across three cards;
- Market: offers, rotation, worker cap and refresh;
- System: reload, status, items, players, provider, market, bubble and config;
- Apply/Force/Market actions stay at the bottom of Salary and Billing.

## Removed legacy UI

The legacy station, worker inventory and admin screens were deleted after the V2 screens passed
the in-game smoke test, together with the unused widgets:

- `LaborCraftAdminScreen`, `LaborCraftStationScreen`, `LaborCraftWorkerInventoryScreen`
- `LcButton`, `LcScrollList`, `LcAreaMap`, `LcJobOption`, `LcTheme`, `LcRender`

No screen overrides `constrainPanel()` and the legacy UI aliases no longer exist. Screens, HUDs and
`LcItemGrid` read the player-selected `LaborCraftTheme` (six palettes, see `docs/20-TEMAS.md`).

## Next pass

1. In-game smoke test of Admin, Station and Worker Inventory at normal and small window sizes.
2. Prepare the LC 2 changelog.
3. Release LC 2 with `quill-ui` and `quill-ui-extras` as required dependencies.

## Release gate for LC 2

- No screen overrides `constrainPanel()`.
- No screen or widget imports a legacy UI alias; all UI reads `QuillThemeData`.
- Every screen opens, resizes and closes without layout overlap.
- `./gradlew build` and the in-game smoke test pass.
