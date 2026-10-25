# 13 — Trabalho: posto, zona e atribuição do empregado

**Status:** desenho para implementação. Substitui a parte de "área por tier" do [doc 08](08-AREA-CASA-HORARIO.md)
como fonte principal do fluxo de trabalho. A base da casa (registro/claim, atribuição e vagas) está pronta;
descanso, expediente e pathing até a cama continuam pendentes.

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
  pega insumo. **Não gera área de trabalho sozinho.** O raio do posto é apenas o **limite máximo** de uma zona.
- **Zona** = retângulo 3D (canto A ↔ canto B) desenhado pelo dono. **É a área de trabalho**: o trabalhador
  arar, planta, colhe e deposita apenas dentro dela. Para o fazendeiro (e futuros cargos), a zona é
  **obrigatória**; sem zona ele pausa com um aviso.
- O player pode desenhar a zona no mapa ou usar **Usar área máxima** para definir o quadrado máximo do posto
  de uma vez.
- **Sem zona** ≠ raio livre para trabalhar: o raio é só o limite onde a zona pode ser desenhada.

## 3. Fluxo do jogador (passo a passo)

1. **Crave o posto** perto do trabalho (é o bloco `laborcraft:workstation`; visual e nome ainda mudam).
2. **Desenhe a zona** (obrigatório para o trabalhador agir) no mapa da aba Config ou com a **Estaca de
   Demarcação** (`laborcraft:survey_stake`):
   - clique direito no primeiro canto → mensagem "canto A em 120, 64, -30";
   - clique direito no canto oposto → "zona 12x4x3 (144 blocos)";
   - agachar + clique direito no posto → zona vinculada, com preview de partículas.
   - Ou **Usar área máxima** na aba Config para o quadrado máximo permitido.
3. **Vincule o trabalhador**: na tela de Empregado, botão **"Posto"** lista os postos livres; ou
   `/laborcraft station bind <trabalhador> <posto>`. Um posto aceita 1 trabalhador por padrão.
4. **Escolha o cargo** (fazendeiro) na mesma tela, se ainda não escolheu.
5. **Ligue o trabalhador** (`/laborcraft worker toggle` ou botão). Ele começa o expediente.

O trabalhador só inicia com uma zona definida; o raio do posto não substitui a zona.

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
      "storage": [
        { "x": 121, "y": 65, "z": -30, "role": "mixed" }
      ],
      "createdAt": 1730000000
    }
  ]
}
```

- O campo antigo `outputChest` é migrado automaticamente para um vínculo `mixed`.
- Baús, papéis e o protocolo de saque/depósito estão no [doc 14](14-ARMAZENAMENTO.md).

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

Fora do expediente (`schedule`): para de trabalhar, volta ao posto. Se tiver casa/cama atribuída, vai para a cama (descanso e pathing ainda pendentes).

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

## 8.1 Conflito entre postos — modelo zona-only (implementado)

Decisão atual (fazendeiro e o único emprego existente): **a área máxima é apenas um limite**; a **zona desenhada** é a área de trabalho exclusiva. Implementado em `StationAreas`, `StationRegistry` (colocação/claim/zone), `OpenStationPayload`, mapa e preview.

Regras em vigor:

- A **área máxima pode sobrepor** a área máxima de outro posto; o mapa desenha o quadrado tracejado inteiro, sem recorte.
- O **bloco do posto pode ficar dentro da área máxima** de outro, mas **não dentro da zona desenhada** de outro.
- **Zonas salvas nunca se sobrepõem**: desenho manual e **Usar área máxima** recortam as células que cruzam zonas/blocos vizinhos; um posto não pode ser colocado nem claimado dentro de uma zona salva.
- **Posto órfão** (registro no JSON, bloco quebrado) não conta para conflito nem para o mapa/recorte até o bloco ser recolocado; o registro continua para reatar o vínculo.
- **Posto sem zona não tem área de trabalho**; o fazendeiro pausa com `need_zone`. A área máxima não é usada como fallback.
- O mapa mostra **outras zonas** em azul e o limite do posto selecionado tracejado; as áreas máximas dos outros postos não são desenhadas como áreas de trabalho.
- **"Usar área máxima" e o desenho manual recortam automaticamente**: a área pedida é dividida em retângulos livres ao redor das zonas/blocos vizinhos (até 32 retângulos); se não sobrar área ou o recorte passar do cap, a ação recusa. Os retângulos são gravados na lista `zones` do mesmo posto — não são postos nem workstations diferentes.
- A **IA do fazendeiro checa pertencimento à zona** por célula; com o recorte, ela não trabalha nos vazios. O bounding box continua limitado ao quadrado máximo do posto.
- O mapa/preview desenham **todos** os retângulos salvos (preenchimento + tracejado) e a área máxima tracejada inteira; a UI recebe a lista de zonas, não só a primeira.
- Desempenho: o recorte roda só no clique (31×31 no máximo, cap 32 retângulos); a IA varre uma vez a cada 10 ticks e a checagem de pertencimento é alguns comparadores por célula, dominada pelos `getBlockState`/chunks. Continua leve no uso normal.
- A lógica antiga de área efetiva (`isEffective`/`effectiveSpans`) fica disponível para empregos de **área livre**, que terão uma análise própria e uma política por emprego (`zone_only`/`full_area`), sem criar um workstation diferente por emprego.

Pendências:
- Política para empregos de área livre (sem zona) — analisar depois; por ora o único emprego é o fazendeiro.
- O limite de 32 retângulos pode ser ajustado; acima disso o desenho manual é o fallback.

## 9. Ordem de implementação

1. `StationRegistry` + vínculo trabalhador↔posto + comandos `station bind/list/remove`.
2. Zona: estaca, seleção de dois cantos, preview e persistência (sem UI).
3. UI do Posto — aba Trabalho (lista, vagas, ações).
4. UI do Posto — aba Config com o mapa de seleção.
5. IA mínima do fazendeiro: SCAN → MOVE → WORK (colher e replantar).
6. DEPOSIT no baú de saída + insumos.
7. Casa: registro/claim, atribuição de cama e vagas (feito; ver doc 11).
8. Descanso: trabalhador vai à cama, pose/animação e retomada (pendente).
9. Expediente ligado à IA (parar/voltar), depois do descanso.

## 10. UI do Posto

Interagir com o posto abre `LaborCraftStationScreen` (mesmo tamanho das outras, 460x360), com três abas: **Trabalho**, **Config** e **Baús**.

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

- **Área de trabalho**: é a **zona desenhada**. O raio do posto é só o limite máximo e não faz o
  trabalhador agir; sem zona ele pausa (“Desenhe a área de trabalho no Posto”).
- Sem zona: o texto fica em vermelho (“Sem zona · limite do posto 25x25 (não trabalha)”) e aparece o
  botão **Usar área máxima** para definir o quadrado máximo de uma vez.
- Com zona: mostra `Zona WxD` (pode haver vários retângulos) e o botão **Limpar zona** (volta a pausar até desenhar outra).
- **Desenhar área** abre o mapa; **Confirmar** envia `station zone set` e o servidor também **recorta** a seleção contra zonas/blocos vizinhos. **Usar área máxima** faz o mesmo com o quadrado máximo (até 32 retângulos; se passar, recusa).
- O armazenamento fica na aba **Baús** (vínculos, papéis e detecção), detalhada no [doc 14](14-ARMAZENAMENTO.md).
- **Ajuda**: botão **?** no topo abre um overlay explicando área de trabalho, baús, mochila, olho e comandos.
- **Visualizar**: fecha a tela e mostra todas as zonas salvas (todos os retângulos) com partículas por 10 s; um contador fica logo acima da hotbar. Também existe um botão compacto **Visualizar área** no topo da caixa da aba Config para rever a área já confirmada, sem reabrir o desenho.

### Mapa de seleção (`LcAreaMap`)

Ao clicar em **Desenhar área**, a própria tela abre o mapa:

- Recorte do mundo visto de cima, centrado no posto (padrão ±48 blocos), 1–4 px por bloco (scroll = zoom).
- **Arrastar com o mouse** define o retângulo; a barra mostra `12x4 · 48 blocos` e o limite máximo.
- Botões: **Confirmar** (envia `station zone set`), **Cancelar**, **Visualizar** (fecha a UI e marca a área com partículas por 10 s, com contador acima da hotbar).
- Colunas fora dos chunks carregados aparecem como “?” e não podem ser selecionadas.

- Na aba Config sem abrir o desenho, o mapa aparece em modo leitura: **todas as zonas salvas** em âmbar (contorno da união); sem zona, nenhuma área de trabalho; outras zonas próximas em azul; na seleção manual, as células que serão **recortadas** ficam vermelhas, mas o **Confirmar** continua habilitado. Desenho manual e **Usar área máxima** gravam os retângulos livres.

**Como o mapa é desenhado (e por que funciona no cliente):**

- Para cada coluna, pega o bloco do topo (heightmap `MOTION_BLOCKING`, que ignora culturas/plantas e mostra o farmland) e a cor com
  `state.getMapColor(level, pos).calculateARGBColor(...)` — a mesma paleta do mapa vanilla. Barato e sem textura.
- O cliente **só tem chunks carregados**: o servidor manda o que está perto do jogador. Como ele está ao lado do
  posto, a área que interessa está carregada. (O JourneyMap mostra áreas exploradas porque lê os arquivos de
  região do mundo — isso funciona em single-player; em servidor multiplayer ele depende de dados próprios.
  Não vamos por esse caminho.)
- Textura dinâmica gerada a partir do heightmap, redesenhada a cada 5 s ou quando o zoom muda; um único blit por frame.
- **Y da zona** = Y do posto ± `area.verticalRadius` (mostrado como “de Y-2 a Y+2”). Não precisa desenhar em 3D;
  fazenda em andares pode ganhar seletor de Y depois.

**Camadas do mapa (superfície / subsolo):**

O botão **Camada** (ao lado das abas) alterna `Auto → Superfície → Subsolo`. O rótulo mostra o Y usado no subsolo.

- **Superfície**: heightmap `MOTION_BLOCKING` (farmland aparece marrom, não verde sob a cultura).
- **Auto**: superfície ao ar livre; subsolo quando o jogador está coberto. A detecção usa
  `level.getBrightness(LightLayer.SKY, pos) <= 0` e, como reforço, `playerY < superfície - 2`.
- **Subsolo**: varre a coluna a partir do Y do jogador (pivô):
  - pivô em ar → desce até achar sólido/lava e retorna a célula aberta acima;
  - pivô em sólido → sobe no máximo 10 blocos procurando abertura;
  - sem piso → coluna transparente (fundo escuro do mapa).
  O bloco do piso é colorido com `MapColor`, escurecendo conforme a profundidade (3 e 8 blocos), e a grade de
  chunk continua visível.
- O pivô acompanha o jogador e a textura é regerada quando ele muda ≥2 blocos (ou a cada 10 s no subsolo), o que
  evita recálculo por frame. Só a caverna mais próxima do pivô aparece; andares mais fundos exigem descer.
- Referência de comportamento e algoritmo: **Conflux Map** (`Conflux-Union/conflux-map`, GPL-3.0), especificamente
  `docs/reference-specs/cave-nether-layers.md`. O VoxelMap também tem cave mode, mas é All Rights Reserved — serve
  só como referência visual, não para ler código.

### Acessibilidade e fallback

- O mapa é **opcional**. A **Estaca de Demarcação** faz o mesmo: canto A, canto B, agachar + clique no posto.
- Comandos puros cobrem tudo: `station zone set <posto> <from> <to>`, `station zone clear <posto>`,
  `station zone show <posto>`.
- Regra command-first: nenhuma tela altera estado direto; ela só monta o comando.

### Validação no servidor

- Só o **dono** do posto configura.
- Zona máxima 32x32 na horizontal, dentro de `area.maxRadius` do posto.
- **Só zonas são exclusivas**: uma zona não pode cruzar outra zona nem conter o bloco de outro posto; a área máxima pode sobrepor a de outro posto.
- Colocar ou reivindicar um Posto cujo bloco está dentro da zona de outro Posto é recusado; dentro da área máxima, permitido.
- Postos **sem o bloco presente** são ignorados na validação, no recorte e no mapa (registro órfão).
- **Desenho manual e Usar área máxima** recortam contra zonas/blocos vizinhos em até 32 retângulos; sem área livre ou acima do cap, recusa. Zonas salvas nunca se sobrepõem.
- Zona inválida/recorte recusado = mensagem clara no chat, sem alterar nada.

## 11. Status da implementação

| Peça | Estado |
| --- | --- |
| `StationRegistry` (JSON em `<mundo>/data/laborcraft/stations.json`) | ✅ |
| Posto registra dono ao ser colocado; posto sem dono é reivindicado no primeiro uso | ✅ |
| Interagir com o posto abre a tela (payload `open_station`) | ✅ |
| Aba Trabalho: lista de empregados, vaga do posto, vincular/desvincular/chamar/retomar | ✅ |
| Aba Config: modo, mapa interativo, seleção por arraste, zoom, pré-visualização e confirmação da zona | ✅ |
| Aba Config: mapa read-only com todas as zonas, outras zonas próximas e destaque do que será recortado | ✅ |
| Aba Config: **Visualizar área** da zona confirmada, 10 s com contador acima da hotbar | ✅ |
| Mapa: camadas Auto/Superfície/Subsolo com floor-scan para cavernas e bases subterrâneas | ✅ |
| `StationAreas`: zonas exclusivas, checagem de pertencimento na IA, área máxima não recortada | ✅ |
| `StationRegistry.applyMaxZone` / `applyZone`: recorte do máximo e do desenho manual em até 32 retângulos livres | ✅ |
| Comandos `station bind/unbind/recall/pause/resume/open/info` | ✅ |
| Desenho da zona (mapa interativo e estaca como alternativa) | ✅ |
| Remover zona (`station zone clear`); posto fica sem área até desenhar outra | ✅ |
| Baús: vínculo, papéis (mixed/output/input/tools) e aba **Baús** | ✅ |
| Mochila do trabalhador e protocolo de saque/depósito | ⏳ ([doc 14](14-ARMAZENAMENTO.md)) |
| IA do fazendeiro (SCAN/MOVE/WORK/DEPOSIT) | ✅ MVP em `FarmerBrain` (colhe, replanta, deposita, pausa com aviso) |

O que já dá para testar em jogo: colocar o posto, abrir a tela, vincular um empregado, chamar/retomar e ver
o estado na tela. Para desenhar uma área, abra a aba Config e clique em **Desenhar área**. O mapa abre
com zoom ajustado ao limite do posto; arraste para selecionar e use o scroll para ajustar o zoom.
**Visualizar** fecha a tela e mostra a área no mundo por 10 s (contador acima da hotbar); reabra o posto para **Confirmar** ou
**Cancelar**. **Confirmar** salva a zona recortada pelo servidor. A Estaca de Demarcação também recorta e continua
como alternativa. O comando
`/laborcraft station zone clear <x> <y> <z>` remove as zonas e deixa o posto sem área de trabalho. Chunks descarregados ficam
marcados com `?`. Na aba Config, sem abrir o desenho, o mapa mostra todas as zonas em âmbar, outras zonas próximas em azul e, na seleção manual, as células que serão recortadas em vermelho; desenho manual e **Usar área máxima** aplicam o recorte.

### Status da IA (MVP)

Implementado em `FarmerBrain`: valida enxada no slot e sementes (puxa do baú vinculado), procura cultura
madura na área, anda até ela, colhe, replanta, adiciona os drops na mochila e deposita no baú quando
a mochila enche. Pausa com aviso quando falta enxada/semente, quando a enxada está quase quebrando, quando
não há baú ou quando o baú enche. A **área de trabalho é a zona desenhada** (obrigatória); o raio do posto
é apenas o limite máximo. Sem zona ele pausa com o aviso “Desenhe a área de trabalho no Posto”; dentro da
zona pode arar, plantar e colher (e usar **Usar área máxima** para o quadrado máximo). Ele também prefere um
ponto de apoio fora do farmland. Ele coleta itens dropados (sementes, produto, bone meal) dentro do raio
máximo do posto. A água é validada por **proximidade** (4 blocos/9x9, mesmo Y ou 1 acima, fluid tag/waterlogged,
chuva) via `FarmerWater`, usada em todas as rotas. Política `farmer.waterPolicy`:
- `prefer` (padrão): planta hidratado primeiro; se só houver seco, planta e avisa (1x/min), sem pausar.
- `require`: só planta/ara com água; avisa e espera (sem pausa manual), retomando sozinho quando a água aparece.
- `ignore`: desliga a validação.

Com `useBoneMeal`, aplica bone meal em culturas em crescimento; com `useComposter`, quando as sementes
passam de `seedSurplusForCompost`, leva o excedente à composteira mais próxima (raio do posto) e recolhe a
farinha (inclusive quando a composteira já está pronta, independente da contagem de sementes). Ele também
**volta ao trabalho sozinho** quando a enxada/sementes/baú são resolvidos e **equipa automaticamente** uma
enxada válida que estiver na mochila.
Ainda faltam: expediente e coleta/colocação automática de água + layout 9x9 (baixa prioridade).

## 12. Impacto no que já existe

- `WorkArea`/`AreaShape` continuam válidos como fallback de raio.
- `HousingValidator` e `HomeRegistry` cuidam do registro/claim e das vagas; descanso e pathing até a cama ainda faltam.
- O posto **não usa block entity**: o estado vive no `StationRegistry` (JSON no mundo), então quebrar o bloco
  não apaga o vínculo — recolocar na mesma posição reata.
- A tela de Empregado ainda não mostra o posto; isso entra quando o vínculo estiver estável.
