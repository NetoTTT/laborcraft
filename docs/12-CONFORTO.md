# 12 — Conforto da casa: o que decora e melhora o empregado

**Status:** proposta de design, ainda não implementado.

A lista de blocos foi extraída do **próprio jogo 26.3** (1.288 blocos em `assets/minecraft/blockstates/` do
client jar), conferida com as receitas do jar e com os dados públicos citados em §6. Nomes em pt-BR são
tradução livre (o jar 26.3 só traz `en_us`).

A pergunta que este documento responde: **quais blocos, quando colocados dentro de uma casa validada, devem
melhorar humor/produtividade do trabalhador — e quanto cada um vale, considerando o custo de conseguir?**

## 1. Como o conforto funciona

Cada bloco de conforto dá **pontos**. O total vira dois efeitos, com teto:

| Efeito | Fórmula | Teto |
| --- | --- | ---: |
| Humor | `+1 por ponto` (regenera até 100) | +20 |
| Velocidade de trabalho | `+0,5% por ponto` | +10% |

20 pontos = teto dos dois. Passar disso não adianta: a casa fica bonita, mas não vira fábrica.

## 2. Regras

1. **Só conta bloco dentro do volume aéreo da casa validada** (o mesmo flood fill do `HousingValidator`).
2. **Cada tipo conta uma vez.** Não adianta 20 baús: baú vale 2 pontos, uma vez.
3. **Famílias de cor contam uma vez.** Cama, tapete, estandarte, vela e lã de cores diferentes = 1 tipo.
   Sem isso, 16 tapetes coloridos dariam 16 pontos por quase nada.
4. **Teto por categoria: 6 pontos.** Força variedade em vez de uma sala só de lanternas.
5. **A cama do trabalhador conta** (2 pontos) — ela já está lá.
6. Conforto **não substitui** casa válida: sem casa, o trabalhador continua com a penalidade.

Total proposto: **70 tipos** em 5 categorias, somando **163 pontos** — bem acima do
teto de 20, de propósito: o jogador escolhe o caminho (barato ou caro) para chegar lá.

## 3. Lista de blocos

### Luz

Tochas, lanternas e velas. Barato e sempre útil: é o primeiro conforto que qualquer casa ganha.

| Bloco | Nome (pt-BR) | Como obter | Dificuldade | Pontos |
| --- | --- | --- | :---: | :---: |
| `minecraft:torch` | Tocha | carvão + graveto | 1 | **1** |
| `minecraft:soul_torch` | Tocha das almas | carvão + graveto + areia das almas | 2 | **2** |
| `minecraft:copper_torch` | Tocha de cobre | graveto + pepita de cobre + carvão | 2 | **2** |
| `minecraft:lantern` | Lanterna | pepita de ferro + tocha | 2 | **2** |
| `minecraft:soul_lantern` | Lanterna das almas | pepita de ferro + tocha das almas | 3 | **2** |
| `minecraft:copper_lantern` | Lanterna de cobre | tocha de cobre + pepita de cobre | 3 | **2** |
| `minecraft:candle` | Vela | favo de mel + linha | 2 | **1** |
| `minecraft:campfire` | Fogueira | tronco + graveto + carvão | 2 | **2** |
| `minecraft:soul_campfire` | Fogueira das almas | tronco + graveto + areia das almas | 3 | **2** |
| `minecraft:jack_o_lantern` | Abóbora iluminada | abóbora + tocha | 2 | **2** |
| `minecraft:glowstone` | Pedra luminosa | pó de glowstone (Nether) | 4 | **3** |
| `minecraft:sea_lantern` | Lanterna do mar | prismarine (monumento) | 4 | **3** |
| `minecraft:shroomlight` | Luz de cogumelo | floresta de fungos (Nether) | 4 | **3** |
| `minecraft:redstone_lamp` | Lâmpada de redstone | redstone + pedra luminosa | 3 | **2** |
| `minecraft:end_rod` | Bastão do End | fruta do chorus + blaze rod | 3 | **3** |
| `minecraft:amethyst_cluster` | Aglomerado de ametista | geodo de ametista (raro) | 4 | **4** |
| `minecraft:ochre_froglight` | Luz de sapo ocre | sapo no Nether | 5 | **3** |

### Macio

Cama, tapete, estante, vaso. É o que faz a casa parecer moradia e não galpão.

| Bloco | Nome (pt-BR) | Como obter | Dificuldade | Pontos |
| --- | --- | --- | :---: | :---: |
| `minecraft:white_bed` | Cama | lã + tábuas | 1 | **2** |
| `minecraft:white_carpet` | Tapete | lã (2 por bloco) | 1 | **1** |
| `minecraft:white_banner` | Estandarte | lã + graveto | 2 | **1** |
| `minecraft:item_frame` | Moldura | graveto + couro | 1 | **1** |
| `minecraft:glow_item_frame` | Moldura brilhante | moldura + saco de tinta brilhante | 3 | **2** |
| `minecraft:bookshelf` | Estante de livros | tábuas + livros | 2 | **2** |
| `minecraft:chiseled_bookshelf` | Estante esculpida | tábuas + lajes | 2 | **2** |
| `minecraft:oak_shelf` | Prateleira | tronco descascado | 1 | **1** |
| `minecraft:flower_pot` | Vaso de flor | tijolo | 1 | **1** |
| `minecraft:decorated_pot` | Vaso decorado | fragmentos de cerâmica | 2 | **1** |
| `minecraft:potted_poppy` | Flor no vaso | vaso + flor | 1 | **1** |

### Mobília

Estação de trabalho e armazenamento. Custa mais, mas dá pontos altos porque também é útil.

| Bloco | Nome (pt-BR) | Como obter | Dificuldade | Pontos |
| --- | --- | --- | :---: | :---: |
| `minecraft:crafting_table` | Mesa de trabalho | tábuas | 1 | **1** |
| `minecraft:cartography_table` | Mesa de cartografia | tábuas + papel | 2 | **2** |
| `minecraft:fletching_table` | Mesa de flecheiro | tábuas + sílex | 2 | **2** |
| `minecraft:smithing_table` | Mesa de ferreiro | tábuas + ferro | 2 | **2** |
| `minecraft:loom` | Tear | tábuas + linha | 2 | **2** |
| `minecraft:stonecutter` | Cortador de pedra | pedra + ferro | 2 | **2** |
| `minecraft:composter` | Composteira | lajes de madeira | 1 | **1** |
| `minecraft:barrel` | Barril | tábuas + lajes | 1 | **2** |
| `minecraft:chest` | Baú | tábuas | 1 | **2** |
| `minecraft:copper_chest` | Baú de cobre | cobre + baú | 3 | **3** |
| `minecraft:furnace` | Fornalha | pedra | 1 | **2** |
| `minecraft:smoker` | Defumador | tronco + fornalha | 2 | **2** |
| `minecraft:blast_furnace` | Alto-forno | pedra lisa + ferro + fornalha | 2 | **2** |
| `minecraft:cauldron` | Caldeirão | ferro | 2 | **2** |
| `minecraft:anvil` | Bigorna | bloco de ferro + ferro | 3 | **3** |
| `minecraft:grindstone` | Rebolo | pedra + tábuas | 2 | **2** |
| `minecraft:lectern` | Atril | lajes + estante | 2 | **2** |
| `minecraft:bell` | Sino | vila (loot) | 2 | **3** |
| `minecraft:note_block` | Bloco de nota | tábuas + redstone | 2 | **3** |
| `minecraft:jukebox` | Toca-discos | tábuas + diamante (precisa de disco) | 3 | **4** |
| `minecraft:brewing_stand` | Suporte de poções | pedra + blaze rod | 3 | **3** |
| `minecraft:ender_chest` | Baú do End | obsidiana + olho do ender | 4 | **4** |
| `minecraft:enchanting_table` | Mesa de encantamento | obsidiana + diamante + livros | 5 | **5** |

### Natureza

Planta, musgo, bambu. Barato e bonito; bom para enchimento sem inflar a pontuação.

| Bloco | Nome (pt-BR) | Como obter | Dificuldade | Pontos |
| --- | --- | --- | :---: | :---: |
| `minecraft:azalea` | Azaleia | caverna exuberante | 1 | **1** |
| `minecraft:flowering_azalea` | Azaleia florida | caverna exuberante | 2 | **2** |
| `minecraft:moss_block` | Bloco de musgo | caverna exuberante | 1 | **1** |
| `minecraft:moss_carpet` | Tapete de musgo | musgo | 1 | **1** |
| `minecraft:glow_lichen` | Líquen brilhante | cavernas | 2 | **2** |
| `minecraft:vine` | Trepadeira | selva | 1 | **1** |
| `minecraft:big_dripleaf` | Planta-gota grande | caverna exuberante | 2 | **2** |
| `minecraft:hanging_roots` | Raízes suspensas | caverna exuberante | 2 | **2** |
| `minecraft:bamboo` | Bambu | selva / naufrágio | 1 | **1** |
| `minecraft:cactus_flower` | Flor de cacto | deserto | 2 | **2** |
| `minecraft:chorus_flower` | Flor do coro | End | 4 | **3** |

### Prestígio

Itens de fim de jogo. Dão o teto de conforto para quem já venceu.

| Bloco | Nome (pt-BR) | Como obter | Dificuldade | Pontos |
| --- | --- | --- | :---: | :---: |
| `minecraft:beacon` | Sinalizador | nether star + obsidiana + vidro | 5 | **5** |
| `minecraft:conduit` | Conduíte | coração do mar + conchas | 5 | **5** |
| `minecraft:respawn_anchor` | Âncora de renascimento | pedra do choro (Nether) | 4 | **4** |
| `minecraft:lodestone` | Magnetita | pedra + lingote de netherite | 3 | **3** |
| `minecraft:amethyst_block` | Bloco de ametista | 9 fragmentos de ametista | 4 | **4** |
| `minecraft:dragon_head` | Cabeça de dragão | matar o dragão | 5 | **5** |
| `minecraft:skeleton_skull` | Crânio de esqueleto | esqueleto (raro) | 3 | **4** |
| `minecraft:wither_skeleton_skull` | Crânio de esqueleto wither | esqueleto wither (raro) | 4 | **5** |

### Dificuldade

| Nível | Significado |
| :---: | --- |
| 1 | tábuas/pedra/terra: qualquer jogador faz no primeiro dia |
| 2 | ferro, vidro, lã, itens de vila |
| 3 | diamante, obsidiana, blaze, oceano |
| 4 | Nether profundo, geodo, estruturas raras |
| 5 | fim de jogo (nether star, dragão, coração do mar) |

## 4. Configuração proposta

Substitui `housing.comfortBlocks` + `comfortBonusPerBlock` por um mapa com peso:

```json
{
  "comfort": {
    "enabled": true,
    "maxPoints": 20,
    "maxPointsPerCategory": 6,
    "moodPerPoint": 1.0,
    "speedPerPoint": 0.005,
    "maxSpeedBonus": 0.10,
    "countUniqueTypes": true,
    "mergeColorFamilies": true,
    "blocks": {
      "minecraft:torch": 1,
      "minecraft:soul_torch": 2,
      "minecraft:copper_torch": 2,
      "minecraft:lantern": 2,
      "minecraft:soul_lantern": 2,
      "minecraft:copper_lantern": 2,
      "minecraft:candle": 1,
      "minecraft:campfire": 2,
      "minecraft:soul_campfire": 2,
      "minecraft:jack_o_lantern": 2,
      "minecraft:glowstone": 3,
      "minecraft:sea_lantern": 3,
      "minecraft:shroomlight": 3,
      "minecraft:redstone_lamp": 2,
      "minecraft:end_rod": 3,
      "minecraft:amethyst_cluster": 4,
      "minecraft:ochre_froglight": 3,
      "minecraft:white_bed": 2,
      "minecraft:white_carpet": 1,
      "minecraft:white_banner": 1,
      "minecraft:item_frame": 1,
      "minecraft:glow_item_frame": 2,
      "minecraft:bookshelf": 2,
      "minecraft:chiseled_bookshelf": 2,
      "minecraft:oak_shelf": 1,
      "minecraft:flower_pot": 1,
      "minecraft:decorated_pot": 1,
      "minecraft:potted_poppy": 1,
      "minecraft:crafting_table": 1,
      "minecraft:cartography_table": 2,
      "minecraft:fletching_table": 2,
      "minecraft:smithing_table": 2,
      "minecraft:loom": 2,
      "minecraft:stonecutter": 2,
      "minecraft:composter": 1,
      "minecraft:barrel": 2,
      "minecraft:chest": 2,
      "minecraft:copper_chest": 3,
      "minecraft:furnace": 2,
      "minecraft:smoker": 2,
      "minecraft:blast_furnace": 2,
      "minecraft:cauldron": 2,
      "minecraft:anvil": 3,
      "minecraft:grindstone": 2,
      "minecraft:lectern": 2,
      "minecraft:bell": 3,
      "minecraft:note_block": 3,
      "minecraft:jukebox": 4,
      "minecraft:brewing_stand": 3,
      "minecraft:ender_chest": 4,
      "minecraft:enchanting_table": 5,
      "minecraft:azalea": 1,
      "minecraft:flowering_azalea": 2,
      "minecraft:moss_block": 1,
      "minecraft:moss_carpet": 1,
      "minecraft:glow_lichen": 2,
      "minecraft:vine": 1,
      "minecraft:big_dripleaf": 2,
      "minecraft:hanging_roots": 2,
      "minecraft:bamboo": 1,
      "minecraft:cactus_flower": 2,
      "minecraft:chorus_flower": 3,
      "minecraft:beacon": 5,
      "minecraft:conduit": 5,
      "minecraft:respawn_anchor": 4,
      "minecraft:lodestone": 3,
      "minecraft:amethyst_block": 4,
      "minecraft:dragon_head": 5,
      "minecraft:skeleton_skull": 4,
      "minecraft:wither_skeleton_skull": 5
    }
  }
}
```

Compatibilidade: se `comfort.blocks` estiver vazio, o mod mantém o comportamento antigo
(`housing.comfortBlocks` + `comfortBonusPerBlock`, 2% por bloco até 10%).

## 5. Como detectar (técnico)

O `HousingValidator` já percorre o volume aéreo e coleta os vizinhos sólidos. Basta trocar a checagem atual
(`comfortBlocks.contains(id)`) por um mapa `id -> pontos`, guardando um `Set<String>` de ids únicos e
aplicando família e teto. Custo: O(vizinhos) durante a validação, que roda raro (não por tick).

O `HousingReport` passaria a carregar `comfortPoints` e o total por categoria, para a UI mostrar
“Conforto: 12/20 (Luz 4, Macio 4, Mobília 4)”.

## 6. Fontes

- **Client jar 26.3** (fonte da verdade para ids): `assets/minecraft/blockstates/*.json`; receitas em
  `data/minecraft/recipe/*.json`.
- [misode/mcmeta](https://github.com/misode/mcmeta) — dados gerados por versão (branch `registries`);
  conferido via `gh api`.
- [PrismarineJS/minecraft-data](https://github.com/PrismarineJS/minecraft-data) — dataset/API de blocos em JSON
  (`data/pc/<versão>/blocks.json`).
- [Minecraft Wiki — Block](https://minecraft.wiki/w/Block) — categorias, obtenção e raridade.

## 7. Ideias e decisões

| Ideia | Status | Observação |
| --- | --- | --- |
| Quadros (`minecraft:painting`) | **aceita** | é entidade, não bloco: contar exige varrer entidades dentro do volume |
| Exigir pelo menos 1 ponto de Luz para o humor subir à noite | **aceita** | vira regra de validação, não só de pontuação |
| Categoria favorita por emprego | **aceita** | fazendeiro → Natureza, minerador → Luz, etc. |
| Conforto visível (ícone/partículas no posto) | **aceita, para depois** | primeiro o número no relatório/UI |
| Conforto negativo (teia, magma) | **rejeitada por ora** | só volta se o bloco “ruim” também trouxer um benefício claro que justifique colocar na casa |
| Qualidade por variante (mesa de encantamento + estantes) | ideia | depende de checagem extra por bloco |
