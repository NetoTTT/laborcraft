# 03 — Configuração

## Período de cobrança

| Valor | Duração (tempo de jogo) | Duração real aprox. |
| --- | --- | --- |
| `DAILY` | 24000 ticks | ~20 min |
| `WEEKLY` | 168000 ticks (7 dias) | ~2h20 |
| `MONTHLY` | 720000 ticks (30 dias) | ~10h |

Período é **tempo de jogo** (não depende do horário do relógio do sistema) e configura-se por emprego e/ou por contrato.

## Salário

Fórmula padrão do valor cobrado por período:

```
valor = baseWage
      * wageMultiplierPerTier ^ (tier - 1)
      * wageMultiplierPerWorker ^ (trabalhadoresAtivos - 1)
```

- `baseWage`: por emprego (fazendeiro default 24).
- `wageMultiplierPerTier` (default 1.5): cada tier custa mais — mais produção, mais salário.
- `wageMultiplierPerWorker` (default 1.1): evita "spam de trabalhadores baratos"; o enésimo trabalhador custa mais.
- Período maior pode ter desconto configurável (`periodDiscount`, futuro): pagar mês à vista sai mais barato que dia a dia.

Justificativa de balanceamento: o custo tem que crescer mais rápido que a produção, senão automação vira dinheiro infinito.

## Falta de pagamento

| Etapa | Comportamento |
| --- | --- |
| Cobrança falha | `missedPayments + 1`, aviso ao dono |
| `missedPayments <= missedPaymentGracePeriods` | continua trabalhando (período de graça) |
| passou da graça, ainda sem pagar | **pausa** (não produz, guarda pertences) |
| nova falha após a pausa | contrato **rompido**, trabalhador vai embora com o inventário dele |
| `requirePaymentToWork = false` | ignora falta de pagamento (modo admin/teste) |

Config extra prevista: `autoRenew` (renova sozinho ou exige confirmação), `noticeCharges` (chat a cada cobrança).

## Camadas de configuração

Precedência, da mais baixa para a mais alta:

1. `LaborCraftConfig` embutido (padrões do mod).
2. `config/laborcraft-defaults.json` — default do modpack (não sobrescrito por update do mod).
3. `config/laborcraft.json` — configuração do servidor (autoridade final).
4. Override do contrato pelo dono — permitido só se `allowOwnerOverrides = true`.

Campos atuais de `config/laborcraft.json`:

```json
{
  "schemaVersion": 1,
  "currencyProvider": "laborcraft:ledger",
  "allowOwnerOverrides": true,
  "requirePaymentToWork": true,
  "missedPaymentGracePeriods": 1,
  "wageMultiplierPerTier": 1.5,
  "wageMultiplierPerWorker": 1.1,
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
    "allowSharedHouse": false,
    "oneWorkerPerBed": true,
    "maxDistanceFromAnchor": 32,
    "maxVolume": 512,
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
| `housing.requireBed` / `requireRoof` / `requireDoor` / `requireEnclosure` | validação mínima da casa |
| `housing.allowSharedHouse` / `oneWorkerPerBed` | cama compartilhada (adiada; default desliga a casa compartilhada) |
| `housing.maxDistanceFromAnchor` / `maxVolume` / `recheckSeconds` | limite, custo e frequência da validação |
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
| `pathing.maxPathLength` / `maxPathFailures` | limite e quarentena de caminhos |
| `pathing.allowLadders` / `allowDoors` | escadas de mão (desligado: só escadas de blocos) e portas |
| `acquisition.mode` | `purchase` ou `free` (craft/admin planejados) |
| `acquisition.price` | preço do Contrato na loja de contratação (`/laborcraft hire`) |
| `acquisition.revenueAccount` | quem recebe: `server`, `government` ou UUID |
| `acquisition.contractTradeable` | permite negociar o Contrato entre jogadores (planejado) |
| `acquisition.keepProfileBackup` | mantém perfil no mundo para recuperação por admin |

Detalhes e justificativas em [`07-TRABALHADOR.md`](07-TRABALHADOR.md).

## Alçadas (quem pode o quê)

| Ação | Quem |
| --- | --- |
| Trocar moeda, preços, multiplicadores, travar overrides | operador do servidor (nível 2+) / modpack |
| Contratar, demitir, definir área e baús | dono do contrato (jogador) |
| Configurar salário/período além do padrão | dono, **se** `allowOwnerOverrides` |
| Pagar salário de outro jogador | operador (modo admin) |
| Recarregar config com o servidor rodando | operador, via comando futuro `/laborcraft reload` |

## Como o modpack define tudo (receita)

1. Escolher/criar a moeda: configurar `currencyProvider` (ex.: `laborcraft:item` com esmeralda ou scoreboard).
2. Definir `baseWage` e multiplicadores conforme a escala de dinheiro do pack.
3. Travar `allowOwnerOverrides = false` para preços previsíveis.
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
