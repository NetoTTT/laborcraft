# 03 — Configuração

## Período e salário

A cobrança é **por trabalhador**, a cada `billingPeriodDays` (padrão 1 dia de jogo; 24000 ticks por dia). O período usa **tempo de jogo**, não o relógio do sistema.

Com `billing.useSalaryTable = true` (padrão), o valor de um período é:

```
baseDiaria = max(farmer.baseWage, billing.minimumDaily)
multiplicador = (useRank ? rankMultiplier : 1)
              * (useRarity ? rarityMultiplier : 1)
multiplicador += useBoost ? bonusDeUpkeepDosBoosts : 0
multiplicador = clamp(multiplicador, 0.1, billing.boostCap)
salarioDiario = round(baseDiaria * multiplicador)
salarioDoPeriodo = salarioDiario * farmer.billingPeriodDays
```

- `farmer.baseWage`: salário-base diário (padrão 24).
- `billing.minimumDaily`: piso aplicado à base **antes** dos multiplicadores (padrão 0).
- `billing.rankMultipliers`: `[COMUM, JUNIOR, PLENO, SENIOR]` (padrão `1.00 / 1.25 / 1.60 / 2.00`).
- `billing.rarityMultipliers`: `[COMUM, INCOMUM, RARO, EPICO]` (padrão `1.00 / 1.05 / 1.10 / 1.20`).
- `billing.boostCap`: teto do multiplicador total (padrão 2.5).
- `billing.useRankMultiplier`, `useRarityMultiplier`, `useBoostUpkeep`: ligam/desligam cada parte.
- `farmer.billingPeriodDays`: 1 = diário; 7 = semanal; 30 = mensal (aproximado em tempo de jogo).

O modo legado (`billing.useSalaryTable = false`) usa apenas o multiplicador de rank + boosts, sem raridade nem teto:

```
salarioDiario = round(baseDiaria * max(0.1, rank.upkeepMultiplier + bonusDeUpkeep))
```

As ofertas novas usam a tabela automaticamente. Trabalhadores já contratados mantêm o salário que foi gravado no contrato; use `apply-salary` para recalcular os carregados. Dívida e faltas não são apagadas por esse recálculo.

## Falta de pagamento

A cada `billing.checkTicks` (padrão 100 ticks), o mod tenta cobrar o período vencido:

| Etapa | Comportamento |
| --- | --- |
| Saldo suficiente | debita `salário + dívida`, zera dívida/faltas e retira o trabalhador da greve |
| Saldo insuficiente | `missedPayments + 1`; a dívida cresce com o salário se `billing.allowDebt = true`; aplica `missedMoodPenalty` e avisa o dono |
| `missedPayments <= billing.gracePeriods` | continua trabalhando (período de graça; padrão 1) |
| `missedPayments > billing.gracePeriods` | entra em **greve** (`Em greve`): não produz e não sai do posto |
| `billing.dismissAfterMissed > 0` e faltas **após a greve** atingem o limite | contrato é rompido; o Contrato de Trabalhador volta para o dono (ou cai no chão) e a casa é desassociada |
| `worker pay <trabalhador>` | quita a dívida, zera faltas, reinicia o próximo vencimento e retira da greve |
| `billing.enabled = false` | desliga a cobrança (não altera dívidas/estados existentes) |

- `billing.dismissAfterMissed = 0` desliga a dispensa automática (padrão). O valor conta **faltas depois que o trabalhador já está em greve**, não faltas totais; na véspera do limite o dono recebe um aviso. Se o dono estiver offline, a dispensa é adiada e o trabalhador continua em greve.
- Antes do rompimento o trabalhador permanece em greve (não produz); o contrato devolvido preserva rank, raridade, cargo, boosts, aptidões e maestria.
- `schedule.pauseBillingOffDuty = true` + `billing.useSchedule = true` adia o vencimento enquanto o trabalhador estiver fora do expediente.
- `billing.notifyOwner = false` silencia os avisos de cobrança no chat.
- A cobrança só enxerga trabalhadores **carregados** no mundo. Contratos em chunks descarregados não são processados até voltarem a carregar.
- `billing.force` força uma checagem agora para os trabalhadores carregados; útil para testar dívida/greve.

## Configuração live (admin)

Comandos de **gamemaster**; salvam `config/laborcraft.json` imediatamente:

```text
/laborcraft billing set <campo> <valor>
/laborcraft billing rank <rank> <multiplicador>
/laborcraft billing rarity <raridade> <multiplicador>
/laborcraft billing apply-salary
/laborcraft billing force
/laborcraft worker pay <trabalhador>
/laborcraft admin market refresh
```

Campos de `billing set`:

- `enabled`, `check-ticks`, `use-schedule`, `pause-off-duty`, `notify`, `debt`
- `grace`, `dismiss-after`, `mood-penalty`
- `use-table`, `minimum-daily`, `use-rank`, `use-rarity`, `use-boost`, `boost-cap`
- `base-wage` (farmer), `price` (contrato), `period-days` (farmer)

`rank` aceita `comum`, `junior`, `pleno`, `senior`; `rarity` aceita `comum`, `incomum`, `raro`, `epico`. Mudanças de salário, rank, raridade ou preço regeneram as ofertas do mercado automaticamente. `apply-salary` recalcula o salário dos trabalhadores já carregados e mantém a dívida/faltas. `admin market refresh` continua disponível para forçar uma rotação manual.

## Camadas de configuração

Precedência, da mais baixa para a mais alta:

1. `LaborCraftConfig` embutido (padrões do mod).
2. `config/laborcraft-defaults.json` — default do modpack (não sobrescrito por update do mod; planejado).
3. `config/laborcraft.json` — configuração do servidor (autoridade final).
4. Override do contrato pelo dono — previsto, ainda não implementado para cobrança.

Campos atuais de `config/laborcraft.json`:

```json
{
  "schemaVersion": 1,
  "currencyProvider": "laborcraft:ledger",
  "allowOwnerOverrides": true,
  "worker": {
    "masteryLevelThresholds": [0, 100, 300, 700, 1500],
    "masteryXpPerHarvest": 1,
    "allowReassignment": true,
    "reassignmentFee": 0,
    "reassignmentCooldownDays": 0,
    "allowPickup": true,
    "pickupRequiresEmptyInventory": true,
    "maxWorkersPerPlayer": 1
  },
  "offers": {
    "offerCount": 5,
    "rotationDays": 1
  },
  "housing": {
    "required": false,
    "requireBed": true,
    "requireRoof": true,
    "requireDoor": true,
    "requireEnclosure": true,
    "requireLight": true,
    "allowSharedHouse": false,
    "oneWorkerPerBed": true,
    "maxDistanceFromAnchor": 32,
    "maxVolume": 512,
    "minInteriorSize": 5,
    "maxBedsPerHouse": 4,
    "recheckSeconds": 60,
    "bonusRadius": 2,
    "bonusSpeed": 0.10,
    "bonusMaxWorkers": 1,
    "speedPenaltyWithoutHome": 0.25,
    "comfortBlocks": ["minecraft:chest", "minecraft:crafting_table", "minecraft:furnace", "minecraft:bookshelf", "minecraft:torch", "minecraft:flower_pot"],
    "comfortBonusPerBlock": 0.02,
    "comfortBonusMax": 0.10
  },
  "acquisition": {
    "mode": "purchase",
    "price": 64,
    "revenueAccount": "server",
    "contractTradeable": false,
    "keepProfileBackup": true
  },
  "area": {
    "shape": "square",
    "radiusByTier": [4, 6, 8, 9],
    "verticalRadius": 4,
    "maxRadius": 12,
    "useHouseBonus": true,
    "previewParticles": true
  },
  "storage": {
    "maxDistance": 8,
    "maxContainers": 3,
    "equipFromChest": false,
    "workerSlots": 9
  },
  "schedule": {
    "mode": "daylight",
    "startTick": 0,
    "endTick": 12000,
    "workInRain": true,
    "pauseBillingOffDuty": false,
    "idleCheckTicks": 100
  },
  "pathing": {
    "maxPathLength": 64,
    "maxPathFailures": 5,
    "allowLadders": false,
    "allowDoors": true
  },
  "speech": {
    "bubbleThroughWalls": false
  },
  "compatibility": {
    "mineLabObjective": "plims",
    "mineLabFormat": "%,d plims",
    "mineLabFallbackToLedger": true
  },
  "billing": {
    "enabled": true,
    "checkTicks": 100,
    "useSchedule": true,
    "notifyOwner": true,
    "allowDebt": true,
    "gracePeriods": 1,
    "dismissAfterMissed": 0,
    "missedMoodPenalty": 10,
    "useSalaryTable": true,
    "minimumDaily": 0,
    "useRankMultiplier": true,
    "useRarityMultiplier": true,
    "useBoostUpkeep": true,
    "boostCap": 2.5,
    "rankMultipliers": [1.00, 1.25, 1.60, 2.00],
    "rarityMultipliers": [1.00, 1.05, 1.10, 1.20]
  },
  "farmer": {
    "enabled": true,
    "baseWage": 24,
    "maxTier": 4,
    "workRadius": 4,
    "billingPeriodDays": 1,
    "useBoneMeal": true,
    "useComposter": true,
    "seedSurplusForCompost": 64,
    "waterPolicy": "prefer"
  }
}
```

- `farmer.waterPolicy`: `prefer` (padrão) planta mesmo em terra seca e avisa uma vez por minuto; `require` só planta/ara com água, avisa e espera (retoma sozinho ao irrigar); `ignore` desliga a validação.
- `farmer.seedSurplusForCompost`: com `useComposter`, quantas sementes ele guarda; o excedente vai para a composteira (e define o estoque mantido no depósito).
- `billing.rankMultipliers` é indexado por `WorkerRank`: COMUM, JUNIOR, PLENO, SENIOR.
- `billing.rarityMultipliers` é indexado por `WorkerRarity`: COMUM, INCOMUM, RARO, EPICO.

`config/laborcraft-ledger.json` guarda os saldos do provedor interno (`UUID` → valor). Não edite com o servidor rodando.

### Trabalhador, maestria, moradia e aquisição

| Chave | Efeito |
| --- | --- |
| `worker.masteryLevelThresholds` | XP necessário para cada nível de maestria (por emprego) |
| `worker.masteryXpPerHarvest` | XP ganho por colheita |
| `worker.allowReassignment` | permite trocar o emprego ativo |
| `worker.reassignmentFee` / `reassignmentCooldownDays` | custo e espera para trocar de emprego |
| `worker.allowPickup` | permite guardar o trabalhador de volta no Contrato ("ovo") |
| `worker.pickupRequiresEmptyInventory` | só guarda com inventário vazio |
| `worker.maxWorkersPerPlayer` | limite **base** de trabalhadores ativos + contratos por jogador; padrão 1. O limite efetivo soma as camas válidas das casas registradas. 0 = ilimitado. |
| `offers.offerCount` | quantas pessoas o mercado do servidor oferece por rotação (padrão 5, máx. 20) |
| `offers.rotationDays` | de quantos em quantos dias do mundo o mercado troca o estoque (padrão 1) |
| `housing.required` | exige moradia para trabalhar |
| `housing.requireLight` | exige pelo menos um bloco emissor de luz (tocha, lanterna, glowstone etc.) no interior; padrão `true` |
| `housing.requireBed` / `requireRoof` / `requireDoor` / `requireEnclosure` | validação mínima da casa |
| `housing.allowSharedHouse` / `oneWorkerPerBed` | cama compartilhada (adiada; default desliga a casa compartilhada) |
| `housing.maxDistanceFromAnchor` / `maxVolume` / `recheckSeconds` | limite, custo e frequência da validação |
| `housing.minInteriorSize` / `maxBedsPerHouse` | tamanho mínimo do interior e teto de camas por casa |
| `housing.bonusRadius` / `bonusSpeed` / `bonusMaxWorkers` | bônus de casa válida |
| `housing.comfortBlocks` / `comfortBonus*` | itens opcionais que dão bônus de desempenho |
| `housing.speedPenaltyWithoutHome` | penalidade quando moradia não é obrigatória |
| `area.shape` | `square` ou `circle` |
| `area.radiusByTier` | raio por tier de maestria |
| `area.verticalRadius` | alcance vertical (andares) |
| `area.maxRadius` / `useHouseBonus` | teto e bônus de casa na área |
| `schedule.mode` | `daylight`, `night`, `custom` ou `always` |
| `schedule.startTick` / `endTick` | janela do modo `custom` (tempo de jogo) |
| `schedule.workInRain` | trabalha na chuva |
| `schedule.pauseBillingOffDuty` | pausa a cobrança fora do expediente |
| `schedule.idleCheckTicks` | frequência da checagem de ócio do trabalhador |
| `storage.maxDistance` / `maxContainers` | distância e quantidade de baús do posto |
| `storage.equipFromChest` | retira ferramenta/insumo do baú automaticamente |
| `storage.workerSlots` | slots do inventário do trabalhador (padrão 9) |
| `pathing.maxPathLength` / `maxPathFailures` | limite e quarentena de caminhos |
| `pathing.allowLadders` / `allowDoors` | escadas de mão (desligado: só escadas de blocos) e portas |
| `acquisition.mode` | `purchase` ou `free` (craft/admin planejados) |
| `acquisition.price` | preço do Contrato na loja de contratação (`/laborcraft hire`) |
| `acquisition.revenueAccount` | quem recebe: `server`, `government` ou UUID |
| `acquisition.contractTradeable` | permite negociar o Contrato entre jogadores (planejado) |
| `acquisition.keepProfileBackup` | mantém perfil no mundo para recuperação por admin |
| `billing` | salário, dívida, graça, greve e notificações (ver tabela acima) |
| `speech.bubbleThroughWalls` | mostra balões de fala através de paredes; padrão `false` (oculto), controlável pelo painel Admin ou `/laborcraft admin set bubble-walls <true/false>` |
| `compatibility.mineLabObjective` / `mineLabFormat` / `mineLabFallbackToLedger` | objective, formato e fallback do provedor `minelab:plims`; o provedor é escolhido em `currencyProvider` |

Detalhes e justificativas em [`07-TRABALHADOR.md`](07-TRABALHADOR.md).

## Alçadas (quem pode o quê)

| Ação | Quem |
| --- | --- |
| Trocar moeda, preços, multiplicadores, travar overrides | operador do servidor (nível 2+) / modpack |
| Contratar, demitir, definir área e baús | dono do contrato (jogador) |
| Configurar salário, rank, raridade e cobrança ao vivo | gamemaster (`/laborcraft billing ...`) |
| Pagar a dívida do próprio trabalhador | dono (`/laborcraft worker pay`) |
| Recarregar config com o servidor rodando | comandos `billing` salvam na hora; reload geral via comando futuro `/laborcraft reload` |

O override de salário pelo dono (`allowOwnerOverrides`) ainda não está implementado para a cobrança; por enquanto a tabela é global e controlada pelo gamemaster.

## Como o modpack define tudo (receita)

1. Escolher/criar a moeda: configurar `currencyProvider` (ex.: `laborcraft:item` com esmeralda ou scoreboard).
2. Definir `farmer.baseWage`, `billing.rankMultipliers`, `billing.rarityMultipliers` e `boostCap` conforme a escala de dinheiro do pack.
3. Travar `allowOwnerOverrides = false` para preços previsíveis (quando o override for implementado).
4. (Futuro) embutir `laborcraft-defaults.json` no pack para que updates do mod não apaguem a configuração do autor do pack.

## Receita do Posto de Trabalho

O bloco `laborcraft:workstation` tem receita shaped padrão: 8 tábuas (tag `minecraft:planks`) + 1 baú. O arquivo fica em `data/laborcraft/recipe/workstation.json` e pode ser substituído ou removido por datapack, como qualquer receita vanilla. Ainda não há toggle em `laborcraft.json`; para desabilitar, remova ou substitua a receita no pack.

O posto também tem loot table própria (`data/laborcraft/loot_table/blocks/workstation.json`) e entra na tag `minecraft:mineable/axe`.

O item não está numa aba do criativo; obtenha pela receita ou por `/give laborcraft:workstation`.

## Datapack (futuro)

Definições data-driven previstas em `data/<namespace>/laborcraft/jobs/<job>.json`:

```json
{
  "job": "laborcraft:farmer",
  "harvestable_tag": "laborcraft:harvestable",
  "replant": true,
  "inputs": ["minecraft:bone_meal"],
  "radius_by_tier": [4, 6, 8, 9]
}
```

Assim, um modpack adiciona cultura modded ao fazendeiro **sem escrever Java**: basta a tag e a definição.
