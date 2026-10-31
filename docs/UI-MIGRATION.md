# UI migration to Quill UI 0.3

Status of the screen-by-screen rewrite on `feat/quill-0.3-ui`.

## Rules

- New and migrated screens extend `com.quillui.screen.QuillScreen`.
- Content is built with `setContent` and `QuillNodes`; lists use `QuillListView`.
- New code does not use `LcTheme`, `LcButton`, `LcScrollList` or `LcRender`.
- `LcTheme` remains only as a deprecated bridge for screens not yet rewritten.
- Screen sizes should come from the panel/content bounds, not from fixed `x`/`y` math.

## Measured screens

These use `setContent` and respond to the available content area:

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

## Station rewrite

`LaborCraftStationV2Screen` replaces the absolute station internals:

- `QuillNodes.tabs` for Work / Config / Storage;
- Work: `QuillListView<Summary>` with Pay and highlight row actions, slot card and the
  bind/recall bar;
- Config: summary card or the `LcAreaMap` widget with live selection, layer cycling and
  confirm/cancel/preview;
- Storage: `QuillListView<StorageView>` with role cycling, unlink, detect and highlight;
- the panel clamps and the content scrolls when the window is smaller than the panel;
- reopen keeps tab, filter, selected worker and selected container.

## Worker Inventory rewrite

`LaborCraftWorkerInventoryV2Screen` uses a measured board widget (`WorkerInventoryBoard`) for the
player grid, the worker backpack and the tool slot. Transfers keep the same command flow, the
quantity picker is a Quill overlay, and the screen keeps the worker id for live payload updates.

## Admin rewrite

`LaborCraftAdminV2Screen` replaces the mixed absolute screen with six measured tabs:

- Salary: contract controls in a card plus rank/rarity multipliers in a second card;
- Billing: nine policy controls in a measured column;
- Schedule: mode, ticks, rain and idle checks in a card, with the note beside it;
- Area: work-area limits, housing requirements and housing limits across three cards;
- Market: offers, rotation, worker cap and refresh;
- System: reload, status, items, players, provider, market, bubble and config;
- Apply/Force/Market actions stay at the bottom of Salary and Billing.

## Legacy screens

The client now opens the V2 station, inventory and admin screens. The legacy
`LaborCraftStationScreen`, `LaborCraftWorkerInventoryScreen` and `LaborCraftAdminScreen` are kept
only as a fallback for the in-game smoke test. They still override `constrainPanel()`, so they must
be deleted before the LC 2 release.

## Next pass

1. In-game smoke test of Admin, Station and Worker Inventory at normal and small window sizes.
2. Delete the three legacy screens.
3. Re-run `./gradlew build` and prepare the LC 2 changelogs.

## Release gate for LC 2

- No screen overrides `constrainPanel()`.
- No new screen code imports `LcTheme`, `LcButton`, `LcScrollList` or `LcRender`.
- Every screen opens, resizes and closes without layout overlap.
- `./gradlew build` and the in-game smoke test pass.
