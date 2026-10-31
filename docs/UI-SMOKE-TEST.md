# UI smoke test checklist

Run in game on `feat/quill-0.3-ui` with Quill UI `0.3.0-alpha.2`. Test at the normal window size
and again with a window smaller than the panel.

## Admin (`/laborcraft admin`)

- [ ] Tabs Salário, Cobrança, Expediente, Área/Casa, Mercado and Sistema fit and switch.
- [ ] Salário: the nine contract controls and the eight rank/rarity multipliers open prompts.
- [ ] Cobrança: the nine toggles/values open prompts; Apply, Force and Market send commands.
- [ ] Expediente: mode choice and start/end/idle values.
- [ ] Área/Casa: area limits, housing toggles and housing limits.
- [ ] Mercado: offers, rotation, worker cap and refresh.
- [ ] Sistema: reload, status, items, players, provider, market, bubble and config.
- [ ] After a command the panel reopens on the same tab.

## Station (right-click the work post)

- [ ] Work: list scrolls, filter cycles, Pay and E work, Bind/Unbind and Recall/Resume work.
- [ ] Work: slot card shows the bound worker, or the free-slot hint.
- [ ] Config: Draw area opens the map, dragging selects, size text updates, layer cycles.
- [ ] Config: Confirm, Cancel, Preview and Clear/Max area send commands.
- [ ] Adjacent same-style zones render as one area, without internal border lines.
- [ ] Storage: Detect, select, row Swap, role cycle, unlink and E highlight.
- [ ] The header shows the gold balance chip and the X closes the screen.

## Worker Inventory (employee screen → backpack)

- [ ] Player grid, hotbar, worker 3x3 and tool slot render aligned.
- [ ] Click one slot then the other side to transfer; drag also transfers.
- [ ] Stacks over one open the quantity overlay: All, digits, Enter, Escape and Cancel.
- [ ] Tool slot rejects invalid tools with a toast.
- [ ] Back returns to the employee screen.

## Existing measured screens

- [ ] Config
- [ ] Jogadores online
- [ ] Home/Register
- [ ] Hiring
- [ ] Profile
- [ ] Item Action (via Item Test)
- [ ] Item Test
- [ ] Employee

## After the test

- Delete `LaborCraftAdminScreen`, `LaborCraftStationScreen` and
  `LaborCraftWorkerInventoryScreen` (the legacy fallbacks).
- Confirm no screen overrides `constrainPanel()`.
- Prepare the LC 2 changelogs.
