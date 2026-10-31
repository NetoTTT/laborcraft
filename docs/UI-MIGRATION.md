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

The legacy `LaborCraftStationScreen` still exists as a fallback, but the client opens the V2
screen. It can be deleted once the in-game smoke test passes.

## Shell-only screens

These extend `QuillScreen` but keep absolute internals. They opt out of panel clamping with
`constrainPanel() == false` until the internal layout is measured:

- Admin (`LaborCraftAdminScreen`) — System and Billing tabs already measured, the rest not
- Worker Inventory (`LaborCraftWorkerInventoryScreen`)

## Next pass

1. **Admin**
   - rebuild from zero on the measured tree instead of patching the mixed screen;
   - tab row with `QuillNodes.tabs`;
   - each tab in a column/card;
   - salary/billing/schedule forms with measured rows;
   - system tab as a stack of measured buttons;
   - remove `constrainPanel` override.
2. **Worker Inventory**
   - slot grid as a measured widget;
   - labels/back button in the content tree;
   - quantity modal over the measured layout;
   - remove `constrainPanel` override.
3. **Cleanup**
   - delete the legacy station screen;
   - update `AGENTS.md` and this checklist.

## Release gate for LC 2

- No screen overrides `constrainPanel()`.
- No new screen code imports `LcTheme`, `LcButton`, `LcScrollList` or `LcRender`.
- Every screen opens, resizes and closes without layout overlap.
- `./gradlew build` and the in-game smoke test pass.
