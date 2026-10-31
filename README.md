# LaborCraft

[![GitHub release](https://img.shields.io/github/v/release/NetoTTT/laborcraft?include_prereleases&label=release)](https://github.com/NetoTTT/laborcraft/releases)
[![License: GPL-3.0-or-later](https://img.shields.io/badge/license-GPL--3.0--or--later-blue)](LICENSE)

[Português (Brasil)](README.pt-BR.md) · English

> Working title. The final name, `modid` and package may still change before the first public release.

A Fabric mod for Minecraft **26.3** about **automated labor**: you hire workers (villager-like NPCs) and pay them a wage to run repetitive jobs — harvest and replant crops, use composters and bone meal, store the output in chests — while the server decides the rules.

The core idea: a worker is not a dumb machine, it is an **employee**. There is a contract, a wage, a house, a rank, and consequences for not paying.

## Premises

1. **Work, not magic.** The NPC works inside a service area defined by its owner, following harvest/replant/deposit rules like a player would.
2. **Contract and wage.** Every worker has a contract with a billing period (daily/weekly/monthly), price, escalation and consequences for missed payments.
3. **Currency-agnostic.** The mod does not know what "money" is: it talks to a *currency provider*. It works with its own ledger, scoreboard, items (emeralds) or any economy mod in the pack.
4. **Job-agnostic.** Jobs are registered by type. Farmer ships first; miner and others come later without rewriting the core.
5. **Compatibility first.** No hard dependency on other mods, no mixins into third-party code when avoidable, tags for crops/chests, and as much as possible data-driven through datapacks.
6. **The server rules.** Prices, periods and limits are decided by the server owner or modpack author; the player decides only what the server allows.

## Status

Functional skeleton (M0): it builds, ships a `.jar`, and the backbone is in place.

**Working today**

- JSON config (`config/laborcraft.json`) and a pluggable currency SPI with a built-in ledger provider (`config/laborcraft-ledger.json`).
- `laborcraft:workstation` block (Work Post) + item + recipe, plus the `laborcraft:worker` entity and the `laborcraft:worker_contract` item.
- **Server-wide labor market** (`/laborcraft hire`): one shop per world, shared stock, daily rotation, persisted at `<world>/data/laborcraft/market.json`. Offers are deterministic from the world seed, so they can be replayed.
- **Workers are people, not jobs**: rank (`Common`, `Junior`, `Adept`, `Senior`), rarity (`Common`, `Uncommon`, `Rare`, `Epic`) with 0–3 innate boosts, per-job aptitude (1–5 stars) and per-job mastery. The job is chosen *after* hiring, on the Employee screen.
- **Contract v2** carrying the whole profile (rank, rarity, boosts, aptitudes, mastery, current job, hunger, mood). Old contracts migrate to Common with a farmer job.
- Native UI: Admin, Config, Hiring (scrollable shop), Employee, Profile, Online players, Item test; shared components (`LcScrollList`, `LcItemGrid`, `LcPagination`, `LcButton`, chat value prompt).
- **House validation** by flood fill (`HousingValidator`): sealed interior, roof, door, minimum 5×5, and a computed bed capacity (5×5 = 2, 6×6 = 3, 7×7 = 4).
- **House Plan** item (`laborcraft:house_plan`): right-click the ground to measure a room as if a bed were there and get a chat report.
- pt-BR and en-US translations, command-first UI (every action runs a server command).

**Not implemented yet**

- Farmer work AI (scan/harvest/replant/deposit), and therefore the real production effect of rank/boosts.
- Automatic wage billing and in-game mastery gain.
- House claiming/registry, bed-based worker slots, and pathfinding to the house.
- The weighted comfort system (designed in [`docs/12-CONFORTO.md`](docs/12-CONFORTO.md)).

## Download

Grab the latest jar from [**Releases**](https://github.com/NetoTTT/laborcraft/releases).

Alpha builds target **Minecraft 26.3** with **Fabric Loader 0.19.5+** and **Fabric API 0.164.0+26.3**
(Java 25). Drop the jar into `mods/` next to Fabric API. On the **client**, also install
[**Quill UI**](https://github.com/NetoTTT/quill-ui) `0.3.0-alpha.9` (`quill-ui-0.3.0-alpha.9.jar`)
and **Quill UI Extras** (`quill-ui-extras-0.3.0-alpha.9.jar`); dedicated servers do not need either
of them.

## Requirements

- **JDK 25** (Minecraft 26.3 requires Java 25; the build runs on JVM 25).
- No local Gradle needed: the wrapper handles it (Gradle 9.8.2, Loom 1.19.3).

## Building

On Windows, use the script (it finds JDK 25, runs the wrapper, and avoids the "hung console"):

```bat
build.cmd build
build.cmd runClient
build.cmd runServer
```

The script redirects output to `build-out.log`, uses `--no-daemon`, and prints the result at the end. This is needed because the **Gradle daemon inherits stdout**, so a plain pipe (`gradlew.bat build | ...`) never sees EOF and looks stuck forever.

If `JAVA_HOME` is not JDK 25+, pass it explicitly:

```bat
set VJ_JAVA_HOME=C:\Program Files\Java\jdk-25
build.cmd build
```

Or use the wrapper directly and redirect yourself:

```bat
gradlew.bat build --console=plain --no-daemon > build-out.log 2>&1
```

The `.jar` is written to `build/libs/laborcraft-<version>.jar`.

## Target versions

| Item | Version |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.164.0+26.3 |
| Loom | 1.19.3 |
| Gradle | 9.8.2 |
| Java | 25 |

All of that lives in `gradle.properties`. The update policy is in [`docs/04-ROADMAP.md`](docs/04-ROADMAP.md).

## Structure

```
src/main/java/com/foque/laborcraft/
  LaborCraft.java            mod entrypoint
  block/                      work post (block + item)
  config/                     server/modpack config
  economy/                    currency SPI + registry + built-in ledger
  job/                        job types and registry
  contract/                   contract and billing period
  worker/                     worker profile, market, ranks, boosts, mastery, states
  area/                       work post area and shapes
  housing/                    house validation (flood fill) and report
  schedule/                   shift (day/night/custom)
  command/                    server commands (admin, contract, house)
src/client/java/com/foque/laborcraft/client/
  ui/                         theme, components and native screens
docs/                         design, decisions and roadmap (mostly pt-BR for now)
```

## Documentation

The design docs are currently in Portuguese and will be translated over time.

| Document | Content |
| --- | --- |
| [docs/00-VISAO.md](docs/00-VISAO.md) | premise, what the mod is and is not, game loop |
| [docs/01-ARQUITETURA.md](docs/01-ARQUITETURA.md) | layers, currency SPI, jobs, contracts, data |
| [docs/02-EMPREGOS.md](docs/02-EMPREGOS.md) | farmer in detail, miner later, tiers |
| [docs/03-CONFIGURACAO.md](docs/03-CONFIGURACAO.md) | wage, period, escalation, permissions |
| [docs/07-TRABALHADOR.md](docs/07-TRABALHADOR.md) | entity, contract/egg, mastery, housing, pathfinding |
| [docs/08-AREA-CASA-HORARIO.md](docs/08-AREA-CASA-HORARIO.md) | post, area by tier, house validation, floors, shift |
| [docs/09-UI.md](docs/09-UI.md) | 26.3 GUI API, components and command-first rule |
| [docs/10-CONTRATACAO-E-PERFIL.md](docs/10-CONTRATACAO-E-PERFIL.md) | offers, limits, dismissal and profile |
| [docs/11-CASA-VAGAS-E-PATHING.md](docs/11-CASA-VAGAS-E-PATHING.md) | house, bed slots and pathing (decision draft) |
| [docs/12-CONFORTO.md](docs/12-CONFORTO.md) | comfort blocks list (mood/speed) |
| [docs/13-TRABALHO-E-ATRIBUICAO.md](docs/13-TRABALHO-E-ATRIBUICAO.md) | work post, drawn work zone and worker assignment |
| [docs/04-ROADMAP.md](docs/04-ROADMAP.md) | milestones M0–M5 and portability policy |
| [docs/05-DECISOES.md](docs/05-DECISOES.md) | open decisions and ADRs |
| [docs/06-COMPATIBILIDADE.md](docs/06-COMPATIBILIDADE.md) | currencies, tags, datapacks, third-party mods |
| [docs/RELEASING.md](docs/RELEASING.md) | standard English release-note structure |

## License

**GNU General Public License v3.0 or later** (`GPL-3.0-or-later`), see [LICENSE](LICENSE).

You may use, study, modify and redistribute the mod, including forks, as long as derivative works stay
open source under the same license and keep the copyright notices. That is deliberate: the project should
stay open for everyone, including server packs and forks.

The license applies to the source code. Minecraft itself is not distributed here and remains subject to
Mojang's terms.
