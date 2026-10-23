# 08 — Área, casa e horário de trabalho

Escopo: como o trabalhador sabe **onde** trabalhar, **onde mora** e **quando** trabalha. Tudo configurável; o fazendeiro é o caso concreto, mas o modelo serve para qualquer emprego.

## 1. Bloco âncora

**Proposta:** um bloco próprio, `laborcraft:workstation` ("Posto de Trabalho"), colocado pelo dono.

- Um posto por contrato. O emprego vem do contrato/trabalhador, não do bloco — o mesmo posto serve para fazendeiro, minerador etc.
- O posto define o centro da área, guarda a referência do trabalhador e abre o preview/GUI ao interagir.
- Quebrar o posto **não perde** o trabalhador: ele pausa, o item do posto volta e o contrato fica "sem base" até recolocar.
- Não usar bloco vanilla (baú, lectern, crafting table) como âncora: conflito de clique, ambiguidade e risco de outro mod usar o mesmo bloco.

Receita padrão: 8 tábuas + 1 baú, em `data/laborcraft/recipe/workstation.json` (substituível por datapack). Registrado como bloco + item; ainda sem GUI, preview ou integração de aba do criativo.

Interação proposta:

| Ação | Resultado |
| --- | --- |
| clique direito | mostra a área (partículas) e o status do contrato |
| agachar + clique direito | atribui/reatribui o trabalhador selecionado ao posto |
| clique com Contrato na mão | vincula o trabalhador daquele contrato ao posto |
| GUI (futuro) | área, casa, horário, baús, salário |

## 2. Área de serviço

Forma e tamanho vêm do tier/maestria:

```json
"area": {
  "shape": "square",
  "radiusByTier": [4, 6, 8, 9],
  "verticalRadius": 4,
  "maxRadius": 12,
  "useHouseBonus": true,
  "previewParticles": true
}
```

- `shape = square` usa distância de Chebyshev (quadrado); `circle` usa distância euclidiana. Quadrado é mais fácil de explicar, visualizar e balancear.
- `radiusByTier[i]` = raio do tier `i+1`. `maxRadius` limita qualquer bônus.
- `verticalRadius` = quantos blocos para cima/baixo a partir do Y do posto. É o que permite **fazenda em andares**.
- O posto é o centro; a área **não se move** quando a casa é validada — a casa só **soma raio**.

### Bônus da casa

Se `useHouseBonus = true` e a casa é válida (e está a até `housing.maxDistanceFromAnchor` blocos do posto):

| Bônus | Default | Config |
| --- | --- | --- |
| raio horizontal | +2 | `housing.bonusRadius` |
| velocidade | +10% | `housing.bonusSpeed` |
| limite de trabalhadores | +1 por jogador | `housing.bonusMaxWorkers` |

O raio final é `min(radiusByTier[tier] + bonus, area.maxRadius)`. Assim a casa "expande" a área sem virar área infinita.

### Varredura da área

- Cache por trabalhador: lista de blocos-alvo por tipo (maduro, vazio, insumo), invalidada por evento de bloco dentro da área ou a cada N segundos.
- Nunca varrer a caixa inteira a cada tick. Varredura em lotes (ex.: 200 blocos por tick) para não travar o servidor.
- O trabalhador só age em blocos **dentro da área** e **reivindicados** por ele. Dois trabalhadores não reivindicam o mesmo bloco de plantação.

## 3. Casa: validação

Objetivo: aceitar construções razoáveis sem virar um "detector de casa" frágil. Regra mínima: **fechada, com porta e cama**.

### Pré-checagem barata

1. Existe uma cama não reivindicada (tag `minecraft:beds`) a até `maxDistanceFromAnchor` do posto.
2. A cama **não vê o céu** (`!level.canSeeSky(bedPos)`), se `requireRoof = true`.
3. O jogador marcou a cama como "cama do trabalhador" (interagiu com o posto/contrato).

### Enclausuramento (flood fill limitado)

Só `canSeeSky` não detecta parede furada. Para validar:

1. Semeia o flood fill no ar acima da cama.
2. Propaga em 6 direções por blocos **não sólidos** (ar, água?, plantas — configurável; ideal: só ar e blocos substituíveis).
3. Limite de volume `housing.maxVolume` (default 512) e de distância (ex.: 16 blocos da cama).
4. Se o fill **vazar** para fora / bater no limite / alcançar um bloco que vê o céu → casa inválida (aberta).
5. A fronteira deve ser sólida ou uma porta (tag `minecraft:doors`). Vidro conta como sólido; trapdoor e porta contam como passagem permitida.
6. Conta portas e camas encontradas na fronteira/interior.

Custo: O(volume) e roda **raro** — ao contratar, ao trocar de casa, ao revalidar (`/laborcraft revalidate`), ou a cada `housing.recheckSeconds` (default 60). Nunca por tick.

### Requisitos

| Regra | Default | Config |
| --- | --- | --- |
| cama | obrigatória | `requireBed` |
| teto (não ver céu) | obrigatório | `requireRoof` |
| porta | obrigatória | `requireDoor` |
| enclausuramento | obrigatório | `requireEnclosure` |
| uma cama por trabalhador | sim | `oneWorkerPerBed` |
| casa compartilhada (várias camas) | desligada (adiada) | `allowSharedHouse` |
| distância máxima do posto | 32 | `maxDistanceFromAnchor` |
| volume máximo validado | 512 | `maxVolume` |

### Itens de conforto (opcional, bônus)

Lista configurável; cada bloco encontrado dentro da casa dá um pequeno bônus de velocidade, com teto:

```json
"comfortBlocks": [
  "minecraft:chest",
  "minecraft:crafting_table",
  "minecraft:furnace",
  "minecraft:bookshelf",
  "minecraft:torch",
  "minecraft:flower_pot"
],
"comfortBonusPerBlock": 0.02,
"comfortBonusMax": 0.10
```

São **opcionais**: não invalidam a casa, só melhoram o desempenho. É o "bem-estar do empregado" sem sistema de moral.

### Se a casa ficar inválida

- `housing.required = true`: trabalhador pausa e avisa o dono; não perde maestria.
- `housing.required = false`: perde o bônus e sofre `speedPenaltyWithoutHome` (default 25%) até regularizar.
- A validação é reexecutada periodicamente, então quebrar uma parede é detectado sem custo por tick.

## 4. Pathfinding e andares

O Minecraft já tem pathfinding: `PathfinderMob` + `GroundPathNavigation`. Não precisamos de biblioteca externa.

### O que funciona

- **Escadas e lajes**: o ground navigation sobe degraus de 0,5–1 bloco. Fazenda em andares funciona com escada de blocos.
- **Portas**: configurável (`pathing.allowDoors`); o trabalhador abre/fecha portas dentro da área autorizada.
- **Corredores de 1 bloco**: ok, desde que haja 2 blocos de altura livre.

### O que não funciona por padrão

- **Escadas de mão (ladders), andaimes e trepadeiras**: o ground navigation não roteia por eles de forma confiável. `pathing.allowLadders = false` por padrão. Suporte real exigiria navegação customizada (fase futura) ou "elevador" com água/alma.
- **Pular vãos de 2+ blocos**, nadar longas distâncias, quebrar/colocar blocos: o trabalhador **não faz**. Se não há caminho, ele pausa e avisa.
- **Lava/perigo**: evita; não atravessa por padrão.

### Estratégia

1. Escanear todos os andares dentro de `verticalRadius` (cache).
2. Filtrar alvos com caminho viável (checagem barata: mesmo andar/alcance, ou escada próxima).
3. Navegar até o alvo; trabalhar em alcance de ~4,5 blocos.
4. `pathing.maxPathLength` (default 64) evita perambular; `pathing.maxPathFailures` (default 5) coloca o alvo em quarentena e tenta outro.
5. Se todos falharem: pausa e mensagem "sem caminho até a plantação".

Requisitos de construção para multiandar (documentar para o jogador): escada de blocos de 1 bloco de largura, 2 de altura livre, sem buraco no meio; cada andar dentro de `verticalRadius` do posto.

## 5. Horário de trabalho

Servidor não dorme — e ninguém é obrigado a dormir. O trabalhador segue um **expediente configurável**, independente do jogador.

```json
"schedule": {
  "mode": "daylight",
  "startTick": 0,
  "endTick": 12000,
  "workInRain": true,
  "pauseBillingOffDuty": false,
  "idleCheckTicks": 100
}
```

| Modo | Janela | Efeito |
| --- | --- | --- |
| `daylight` | 0–12000 | trabalha de dia, descansa à noite (default) |
| `night` | 12000–24000 | turno da noite |
| `custom` | `startTick`–`endTick` | janela livre, com suporte a virada (ex.: 12000→2000) |
| `always` | 24h | turno contínuo |

Referência de tempo de jogo (ticks ÷ 24000):

| Tick | Hora |
| --- | --- |
| 0 | 06:00 |
| 6000 | 12:00 |
| 12000 | 18:00 (pôr do sol) |
| 18000 | 00:00 |
| 23000 | 05:00 |

Regras:

- Fora do expediente o trabalhador **não produz**: vai para a cama/casa (se tiver) ou fica ocioso no posto. Não dorme de verdade (não pula a noite).
- `workInRain`: chuva não interrompe por padrão.
- **Salário continua sendo cobrado** por período, independentemente do expediente (`pauseBillingOffDuty = false`). É o custo fixo do contrato; quem quer justiça pode ligar `pauseBillingOffDuty = true` (cobrança só em horas trabalhadas) — mais complexo de auditar.
- `idleCheckTicks`: frequência da checagem de expediente (default 100 ticks = 5 s), para não checar por tick.
- O expediente é global (config do servidor) por padrão; no futuro, override por contrato se `allowOwnerOverrides`.
- Se o servidor tem tempo fixo (`doDaylightCycle=false`), o modo efetivo vira "sempre dia" ou "sempre noite" — documentar no comando `/laborcraft status`.

### Por que não usar tempo real

Tempo real cria diferença entre servidores e fuso horário, e permite "trabalhar enquanto o dono dorme" de forma imprevisível. Tempo de jogo é justo e previsível. (Fase futura: opção `realTime`, se houver demanda.)

## 6. Regras base adicionadas na config

```json
"area": {
  "shape": "square",
  "radiusByTier": [4, 6, 8, 9],
  "verticalRadius": 4,
  "maxRadius": 12,
  "useHouseBonus": true,
  "previewParticles": true
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
}
```

## 7. Pontos ainda em aberto

1. **Nome/visual do posto**: `laborcraft:workstation` está registrado, com receita de 8 tábuas + baú. Falta decidir o visual definitivo e se também será vendido pronto na Agência.
2. **Casa compartilhada**: várias camas valem para vários trabalhadores do mesmo dono? Adiado; default é casa separada (`allowSharedHouse = false`).
3. **Crescimento por casa**: +1 trabalhador por casa ou só bônus de raio/velocidade? (default os três)
4. **Cobrança fora do expediente**: manter fixa (default) ou pausar? Decide o balanceamento do servidor.
5. **Ladders**: fora do MVP (`allowLadders = false`); navegação customizada só se houver demanda.
6. **Área circular vs. quadrada**: quadrado é default, mas fazenda circular com casa pode pedir círculo.

## 8. Ordem de implementação

1. Bloco posto + preview de área (partículas) + persistência simples.
2. Cálculo de área por tier (já modelado em `WorkArea`/`AreaShape`).
3. Validação de casa (flood fill limitado) + cache + recheck.
4. Expansão por casa + bônus de conforto.
5. Expediente (`WorkSchedule`) ligado ao tick do trabalhador.
6. Pathfinding multiandar + quarentena de alvos.
