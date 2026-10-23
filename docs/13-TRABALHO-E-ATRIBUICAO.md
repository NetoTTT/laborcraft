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

## 9. Ordem de implementação

1. `StationRegistry` + vínculo trabalhador↔posto + comando `station bind/list/remove`.
2. Zona: estaca, seleção de dois cantos, preview e persistência.
3. IA mínima do fazendeiro: SCAN → MOVE → WORK (colher e replantar).
4. DEPOSIT no baú de saída + insumos.
5. Expediente ligado à IA (parar/voltar).
6. Casa: cama e rotina noturna (retomar o doc 11).

## 10. Impacto no que já existe

- `WorkArea`/`AreaShape` continuam válidos como fallback de raio.
- `HousingValidator` não muda; casa fica pausada.
- A tela de Empregado ganha a linha **Posto** e o botão de vínculo.
- O `laborcraft:workstation` ganha um **block entity** (inventário, dono, vínculo) — hoje é só um bloco decorativo.
