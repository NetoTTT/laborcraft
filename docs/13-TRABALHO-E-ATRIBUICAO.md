# 13 — Trabalho: posto, zona e atribuição do empregado

**Status:** desenho para implementação. Substitui a parte de "área por tier" do [doc 08](08-AREA-CASA-HORARIO.md)
como fonte principal do fluxo de trabalho. Casa fica pausada.

## 1. O problema

O jogador tem duas fazendas 12x4 separadas e quer **um trabalhador em cada**, sem que um invada o outro.
Hoje o mod só sabe *onde fica o posto*, não *o que cada trabalhador pode tocar*.

Precisa de três coisas:

1. **Um ponto de trabalho** (o posto) que o trabalhador reconhece como base.
2. **Uma área de trabalho** que pode ser o raio do posto ou um retângulo desenhado.
3. **Um vínculo** entre trabalhador e posto, feito pelo jogador.

## 2. Modelo proposto

```
Posto (bloco)  ──  guarda   ──►  Zonas (0..N retângulos)  +  baú de saída
     ▲
     │ vínculo 1:1 (configurável)
Trabalhador (entidade)
```

- **Posto** = ponto de referência e "mesa" do trabalhador: onde ele fica ocioso, onde deposita e de onde
  pega insumo. **Não gera área sozinho.**
- **Zona** = retângulo 3D (canto A ↔ canto B) desenhado pelo dono. O trabalhador só age dentro da zona.
- **Sem zona** = modo livre: ele trabalha dentro do raio do posto (`area.radiusByTier`, hoje 4–9).
- **Zona tem prioridade** sobre o raio. Se a zona existir, o raio é ignorado.

Isso cobre os dois casos com o mesmo código: quem quer precisão desenha 12x4; quem quer "se vira" não desenha nada.

## 3. Fluxo do jogador (passo a passo)

1. **Crave o posto** perto do trabalho (é o bloco `laborcraft:workstation`; visual e nome ainda mudam).
2. **Desenhe a zona** (opcional) com a **Estaca de Demarcação** (`laborcraft:survey_stake`, ideia de nome):
   - clique direito no primeiro canto → mensagem "canto A em 120, 64, -30";
   - clique direito no canto oposto → "zona 12x4x3 (144 blocos)";
   - agachar + clique direito no posto → zona vinculada, com preview de partículas.
3. **Vincule o trabalhador**: na tela de Empregado, botão **"Posto"** lista os postos livres; ou
   `/laborcraft station bind <trabalhador> <posto>`. Um posto aceita 1 trabalhador por padrão.
4. **Escolha o cargo** (fazendeiro) na mesma tela, se ainda não escolheu.
5. **Ligue o trabalhador** (`/laborcraft worker toggle` ou botão). Ele começa o expediente.

Sem zona, o passo 2 não existe — o posto já vale pelo raio.

## 4. Dados

`StationRegistry` (salvo no mundo, como o `WorkerMarket`):

```json
{
  "stations": [
    {
      "id": "laborcraft:station/12",
      "dimension": "minecraft:overworld",
      "pos": [120, 64, -30],
      "owner": "uuid",
      "worker": "uuid|null",
      "mode": "zones",
      "zones": [
        { "from": [118, 64, -32], "to": [129, 66, -28] }
      ],
      "outputChest": [121, 65, -30],
      "createdAt": 1730000000
    }
  ]
}
```

- Posto quebrado → estação fica órfã, trabalhador pausa e avisa; recolocar o bloco na mesma posição reata.
- Zona sobreposta de outro jogador → recusa com aviso (primeiro que registra leva, como a cama).

## 5. IA do trabalhador (primeira versão)

Máquina de estados simples, rodando a cada N ticks (não por tick):

```
IDLE ──expediente──► SCAN ──achou──► MOVE ──chegou──► WORK ──cheio──► DEPOSIT ──► SCAN
  ▲                    │                                             │
  └───fora do horário──┘                                             └──► volta ao posto
```

- **SCAN**: varre a zona (ou o raio) em lotes, procurando alvos válidos do cargo (cultura madura, terra vazia
  com água, etc.). Cache invalidado por mudança de bloco ou a cada `recheckSeconds`.
- **MOVE**: `PathNavigation` até um ponto alcançável perto do alvo; falhou N vezes → quarentena do alvo.
- **WORK**: colhe, replanta, usa insumo — em alcance (~4,5 blocos), sem quebrar blocos.
- **DEPOSIT**: leva o inventário ao baú de saída do posto; se estiver cheio, pausa e avisa.
- **Sem alvo**: fica ocioso no posto; se a zona não tem nada plantado, avisa "sem plantação na zona".

Fora do expediente (`schedule`): para de trabalhar, volta ao posto. Se tiver casa (fase futura), vai para a cama.

## 6. Por que posto + zona, e não só raio

| Abordagem | Prós | Contras |
| --- | --- | --- |
| Só raio (tier) | zero configuração | duas fazendas próximas = trabalhadores se misturam |
| Só zona | controle total | obriga desenhar antes de contratar; chato para horta pequena |
| **Posto + zona opcional** | precisa quando importa, simples quando não | um item e um registro a mais |

É o padrão que o **MineColonies** usa: o `Forester's Hut` corta árvores num raio de ~150 blocos por padrão e
oferece uma **zoning tool** opcional ("clique um canto, clique o oposto") para restringir a área. O
`Farmer's Hut` segue a mesma ideia. Copiar o conceito (não o código) evita reinventar UX.

## 7. Referências

- [MineColonies — Forester's Hut](https://minecolonies.com/wiki/buildings/lumberjack/): trabalhador com raio
  padrão + zona opcional desenhada com ferramenta; também "Recall Workers" e "Tasks".
- [MineColonies — Farmer's Hut](https://minecolonies.com/wiki/buildings/farmer/): mesmas regras para plantação.
- **Tektopia** ([resumo](https://sourceforge.net/app/tektopia-minecraft-mod/)): blocos de profissão como âncora do
  emprego, colonos com inventário e necessidades.
- **RimWorld** (zonas de plantio e "work priorities") e **Dwarf Fortress** (activity zones): a ideia de
  "zona de trabalho" + "quem pode trabalhar nela" é a mesma, fora do Minecraft.
- **Ancient Warfare 2**: contratar NPC e dar uma ordem de trabalho com área.

## 8. Decisões que faltam

- [ ] Nome do bloco: manter `workstation`/“Posto de Trabalho” ou trocar (ex.: “Estação de Trabalho”)?
- [ ] Nome do item de zona: “Estaca de Demarcação”, “Trena”, “Marcador de Área”?
- [ ] Zona: 1 por posto ou várias (2–3)?
- [ ] Posto aceita 1 trabalhador (recomendado) ou N?
- [ ] Baú de saída: automático (baú encostado no posto) ou escolhido com clique?
- [ ] Modo livre (raio) existe desde o começo ou só depois da zona?
- [ ] Trabalhador precisa de ferramenta no inventário (enxada) ou é automático?
- [ ] Mapa: ±48 blocos do posto é suficiente? Zoom 1–4 px/bloco?
- [ ] Zona usa Y do posto ± `verticalRadius` (recomendado) ou Y desenhado?
- [ ] Vagas: 1 por posto agora (recomendado) e upgrade no futuro?

## 9. Ordem de implementação

1. `StationRegistry` + vínculo trabalhador↔posto + comandos `station bind/list/remove`.
2. Zona: estaca, seleção de dois cantos, preview e persistência (sem UI).
3. UI do Posto — aba Trabalho (lista, vagas, ações).
4. UI do Posto — aba Config com o mapa de seleção.
5. IA mínima do fazendeiro: SCAN → MOVE → WORK (colher e replantar).
6. DEPOSIT no baú de saída + insumos.
7. Expediente ligado à IA (parar/voltar).
8. Casa: cama e rotina noturna (retomar o doc 11).

## 10. UI do Posto

Interagir com o posto abre `LaborCraftStationScreen` (mesmo tamanho das outras, 460x360), com duas abas.

### Aba Trabalho (padrão)

```
┌ Posto de Trabalho ──────────────────────────────── X ┐
│  Seus empregados (12)        │  Vagas do posto (1/1) │
│  ┌────────────────────────┐  │  ┌─────────────────┐  │
│  │ Bento Almeida   Pleno  │  │  │ Bento Almeida   │  │
│  │ Fazendeiro · ativo     │  │  │ Fazendeiro      │  │
│  │              [vincular]│  │  │ trabalhando     │  │
│  ├────────────────────────┤  │  │ [chamar] [pausa]│  │
│  │ Clara Souza     Comum  │  │  └─────────────────┘  │
│  │ sem cargo · ocioso     │  │  ┌─────────────────┐  │
│  └────────────────────────┘  │  │ vaga livre      │  │
│                              │  │ [vincular...]   │  │
│                              │  └─────────────────┘  │
└──────────────────────────────────────────────────────┘
```

- **Esquerda**: todos os empregados do jogador (com scroll). Mostra nome, rank, cargo, estado e se já estão presos a outro posto.
- **Direita**: as vagas do posto. Hoje 1 vaga; o modelo já nasce com lista para permitir upgrades futuros.
- **Ações** (todas command-first):

| Botão | Comando | Efeito |
| --- | --- | --- |
| Vincular | `laborcraft station bind <posto> <trabalhador>` | ocupa a vaga |
| Desvincular | `laborcraft station unbind <posto> <trabalhador>` | libera a vaga |
| Chamar (recall) | `laborcraft station recall <posto>` | para o que está fazendo e volta ao posto |
| Pausar / Deixar ocioso | `laborcraft worker pause <id>` | fica no posto sem trabalhar |
| Retomar | `laborcraft worker resume <id>` | volta ao trabalho |

“Mandar para casa” fica para quando a casa existir (fase 2); por ora, pausar já deixa ocioso.

### Aba Config

- **Modo da área**: `Livre (raio do posto)` ou `Zona desenhada` (escolha única).
- No modo livre: mostra o raio efetivo (tier/maestria) e o que ele cobre. Sem botão de “aumentar” por enquanto.
- No modo zona: lista as zonas (hoje 1) com tamanho e botão **Desenhar área**.
- **Baú de saída**: mostra o baú detectado ao redor do posto (ou “nenhum”) e um botão **Detectar**.
- **Preview**: liga/desliga as partículas da área.

### Mapa de seleção (`LcAreaMap`)

Ao clicar em **Desenhar área**, a própria tela abre o mapa:

- Recorte do mundo visto de cima, centrado no posto (padrão ±48 blocos), 1–4 px por bloco (scroll = zoom).
- **Arrastar com o mouse** define o retângulo; a barra mostra `12x4 · 48 blocos` e o limite máximo.
- Botões: **Confirmar** (envia `station zone set`), **Cancelar**, **Visualizar** (fecha a UI e marca a área com partículas por ~10 s).
- Colunas fora dos chunks carregados aparecem como “?” e não podem ser selecionadas.

**Como o mapa é desenhado (e por que funciona no cliente):**

- Para cada coluna, pega o bloco do topo (heightmap `WORLD_SURFACE`) e a cor com
  `state.getMapColor(level, pos).calculateARGBColor(...)` — a mesma paleta do mapa vanilla. Barato e sem textura.
- O cliente **só tem chunks carregados**: o servidor manda o que está perto do jogador. Como ele está ao lado do
  posto, a área que interessa está carregada. (O JourneyMap mostra áreas exploradas porque lê os arquivos de
  região do mundo — isso funciona em single-player; em servidor multiplayer ele depende de dados próprios.
  Não vamos por esse caminho.)
- Cache por chunk; redesenha só o recorte visível e só quando algo muda (bloco alterado ou a cada N segundos).
- **Y da zona** = Y do posto ± `area.verticalRadius` (mostrado como “de Y-2 a Y+2”). Não precisa desenhar em 3D;
  fazenda em andares pode ganhar seletor de Y depois.

### Acessibilidade e fallback

- O mapa é **opcional**. A **Estaca de Demarcação** faz o mesmo: canto A, canto B, agachar + clique no posto.
- Comandos puros cobrem tudo: `station zone set <posto> <from> <to>`, `station zone clear <posto>`,
  `station zone show <posto>`.
- Regra command-first: nenhuma tela altera estado direto; ela só monta o comando.

### Validação no servidor

- Só o **dono** do posto configura.
- Zona máxima 32x32 na horizontal, dentro de `area.maxRadius` do posto, volume limitado.
- Não pode sobrepor zona/posto de outro jogador (primeiro que registra leva).
- Zona recusada = mensagem clara no chat, sem alterar nada.

## 11. Status da implementação

| Peça | Estado |
| --- | --- |
| `StationRegistry` (JSON em `<mundo>/data/laborcraft/stations.json`) | ✅ |
| Posto registra dono ao ser colocado; posto sem dono é reivindicado no primeiro uso | ✅ |
| Interagir com o posto abre a tela (payload `open_station`) | ✅ |
| Aba Trabalho: lista de empregados, vaga do posto, vincular/desvincular/chamar/retomar | ✅ |
| Aba Config: modo de área (livre/zona) e tamanho da zona | ✅ (leitura) |
| Comandos `station bind/unbind/recall/pause/resume/open/info` | ✅ |
| Desenho da zona (estaca e mapa) | ⏳ próxima etapa |
| Baú de saída e insumos | ⏳ |
| IA do fazendeiro (SCAN/MOVE/WORK/DEPOSIT) | ⏳ |

O que já dá para testar em jogo: colocar o posto, abrir a tela, vincular um empregado, chamar/retomar e ver
o estado na tela. O vínculo já aparece no `/laborcraft station info`.

## 12. Impacto no que já existe

- `WorkArea`/`AreaShape` continuam válidos como fallback de raio.
- `HousingValidator` não muda; casa fica pausada.
- O posto **não usa block entity**: o estado vive no `StationRegistry` (JSON no mundo), então quebrar o bloco
  não apaga o vínculo — recolocar na mesma posição reata.
- A tela de Empregado ainda não mostra o posto; isso entra quando o vínculo estiver estável.
