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

## Shell-only screens

These extend `QuillScreen` but keep absolute internals. They opt out of panel clamping with
`constrainPanel() == false` until the internal layout is measured:

- Admin (`LaborCraftAdminScreen`)
- Station (`LaborCraftStationScreen`)
- Worker Inventory (`LaborCraftWorkerInventoryScreen`)

## Next pass

1. **Admin**
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
3. **Station**
   - Work tab with `QuillListView` (validated in the harness);
   - Config with `LcAreaMap` as a measured widget;
   - Storage with `QuillListView`;
   - remove `constrainPanel` override.

## Release gate for LC 2

- No screen overrides `constrainPanel()`.
- No new screen code imports `LcTheme`, `LcButton`, `LcScrollList` or `LcRender`.
- Every screen opens, resizes and closes without layout overlap.
- `./gradlew build` and the in-game smoke test pass.
