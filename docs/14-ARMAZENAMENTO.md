# 14 — Armazenamento: baús, mochila e ferramentas

**Status:** desenho aprovado; implementação em etapas. Depende do [doc 13](13-TRABALHO-E-ATRIBUICAO.md)
(posto, zona e vínculo) e prepara o motor para os próximos empregos.

## 1. Princípios

1. O **posto continua sendo âncora**, não baú. Sem `BlockEntity`: o estado vive no `StationRegistry`.
2. **Nada de posição fixa de baú.** O jogador escolhe onde decorar; o vínculo é por distância e caminho.
3. **Um baú misturado é o caso normal.** O trabalhador distingue insumo, produto e ferramenta; o jogador
   não precisa organizar nada.
4. **Tudo data-driven pelo cargo** (`JobDefinition.storage`), para minerador/pedreiro reutilizarem o mesmo
   motor sem código novo.
5. **Command-first:** toda ação da UI existe como comando.

## 2. Modelo

```
Posto ── storage[] ──► Baú/Barril (role: MIXED | OUTPUT | INPUT | TOOLS)
  ▲
  │ vínculo 1:1
Trabalhador ── mochila (9) + ferramenta (1)
```

- **Baús**: 0..N por posto (padrão 3), cada um com papel. `MIXED` serve para tudo; os outros restringem.
- **Mochila do trabalhador**: 9 slots gerais + 1 slot de ferramenta, persistidos na entidade.
- O jogador gerencia a mochila pela UI (não há auto-equipar por padrão).

## 3. Vínculo do baú

- Alcance `storage.maxDistance` (padrão 8, máximo 16) e `storage.maxContainers` (padrão 3).
- O baú precisa ser um `Container` de verdade (baú, barril, baú de cobre, baú de mod) e não pode ser o
  próprio posto.
- Formas:
  - **Detectar** (`station storage scan`): procura containers no raio e vincula o mais próximo ainda livre.
  - **Clique** (`station storage link <posto> <baú> [papel]`): usado pelo botão "Vincular" e pela UI.
  - **Auto**: se o posto não tem baú, a tela sugere detectar; não vincula sozinho.
- **Olho (destacar)**: cada linha da aba **Baús** tem um botão de olho que fecha a tela e desenha um
  wireframe de partículas no baú por 10 s, com contador acima da hotbar. O `WorldHighlightManager` é
  reutilizável para outros destaques no mundo (alvos do pedreiro, veios do minerador etc.).
  Se o baú estiver **fora da câmera**, um HUD no estilo FPS (`TargetIndicatorHud`) mostra na borda da tela
  um badge com o ícone do baú, uma seta apontando na direção e a distância; quando ele entra na tela o HUD
  some e ficam só as partículas.
- A linha selecionada continua selecionada quando o servidor reenvia a tela (troca de papel, detecção),
  porque o cliente preserva aba + baú selecionado entre refreshes.
- No passo da IA, o vínculo também valida **caminho alcançável** (pathfinding com cache). O passo 1 valida
  só distância + container.
- Baú quebrado/removido: o vínculo continua no registro (posição), aparece **inválido** na UI e a IA pausa
  com aviso. Recolocar o bloco na mesma posição reata.

### Papéis

| Papel | O trabalhador... |
| --- | --- |
| `MIXED` | deposita produto e retira insumo/ferramenta do mesmo baú |
| `OUTPUT` | só deposita |
| `INPUT` | só retira insumo |
| `TOOLS` | só retira/guarda ferramenta |

## 4. Protocolo do baú misturado

O cargo declara o que conhece:

```json
"storage": {
  "inputs":  ["#minecraft:seeds", "minecraft:bone_meal"],
  "outputs": ["#minecraft:crops"],
  "keep":    { "minecraft:wheat_seeds": 32 },
  "target":  { "minecraft:bone_meal": 16 },
  "tools":   ["#laborcraft:hoes"]
}
```

### Saque

1. Só retira itens de `inputs` ou `tools`, de baús com papel `MIXED`/`INPUT`/`TOOLS`.
2. Só até o alvo: `keep`/`target` limitam quanto ele carrega; nunca retira produto.
3. Se faltar insumo essencial (semente) ou ferramenta exigida → pausa e avisa.

### Depósito

1. Só insere em slot **vazio** ou que já tenha o **mesmo item**; nunca em slot com item de outro tipo.
   Itens do jogador no baú ficam intocados.
2. Ordem: baús `MIXED`/`OUTPUT` na ordem do vínculo → mochila → pausa com aviso ("sem espaço").
3. Semente é insumo **e** produto: `keep` resolve. Ele mantém o estoque na mochila e deposita o excedente;
   sem `keep`, pode acabar pegando o que acabou de guardar.

### Estados de erro

| Situação | Comportamento |
| --- | --- |
| Nenhum baú válido | pausa + mensagem |
| Todos os baús cheios | tenta mochila; se cheia, pausa + mensagem |
| Insumo/ferramenta ausente | pausa + mensagem |
| Baú removido | vínculo inválido na UI; pausa |

## 5. Mochila e ferramentas

- 9 slots + 1 ferramenta; persistidos com `ItemStack.CODEC` na entidade.
- Tela do Empregado ganha a seção **Mochila** com grade, transferência por clique e tooltip.
- Comandos equivalentes: `worker inv give|take <slot> [quantidade]`.
- Ferramenta: só a que o jogador colocar. `storage.equipFromChest` (desligado por padrão) permite puxar do
  baú de ferramentas.
- Durabilidade: perto de quebrar, ele devolve ao baú `TOOLS` e avisa; sem ferramenta exigida pelo cargo,
  pausa.
- Drops de ferramenta quebrada não são consumidos silenciosamente.

## 6. Posto quebrável e upgrades

- O `StationRegistry` continua sendo a fonte da verdade; quebrar não apaga o registro.
- O upgrade fica no **item do posto** (data component `laborcraft:station`), não no bloco, e o hover mostra:
  nome do posto, `Armazenamento II`, `Velocidade I` etc.
- No MVP, quebrar com trabalhador vinculado **desvincula** (aviso no chat). O item leva nome + upgrades.
- Futuro: "relocate" — o item leva o vínculo e o trabalhador passa a usar o novo posto.

## 7. Outros empregos (reuso)

- **Minerador**: `inputs` = picareta/tochas, `outputs` = minérios; sem `keep`.
- **Pedreiro/schematic**: `inputs` = materiais do schematic, `outputs` = sobras; consome e devolve.
- **Fazendeiro**: insumos + `keep` de sementes + bone meal opcional.
- A IA só troca o `WorkTask`; o armazenamento é o mesmo motor.

## 8. Comandos

| Comando | Efeito |
| --- | --- |
| `station storage link <posto> <baú> [papel]` | vincula (valida dono, distância, container, limite) |
| `station storage unlink <posto> <baú>` | desvincula |
| `station storage role <posto> <baú> <papel>` | muda `mixed/output/input/tools` |
| `station storage scan <posto>` | procura no raio e vincula o mais próximo livre |
| `station storage list <posto>` | lista os vínculos no chat |

## 9. Ordem de implementação

1. **Vínculos + UI** (esta etapa): `StorageLink` no registro, comandos, aba **Baús** no Posto, migração do
   antigo `outputChest`.
2. **Mochila**: inventário de 9 + ferramenta, persistência e UI da tela do Empregado.
3. **Protocolo do fazendeiro**: saque/depósito com `keep`/`target`, pausa/avisos, durabilidade.
4. **Relocate + upgrades no item** e hover.
5. Minerador/pedreiro reutilizando o mesmo spec.

## 10. Status

| Peça | Estado |
| --- | --- |
| `StorageLink`/`StorageRole` no `StationRegistry` (com migração do `outputChest`) | ✅ |
| Comandos `station storage link/unlink/role/scan/list` | ✅ |
| Aba **Baús** no Posto (lista, detectar, desvincular, papel) | ✅ |
| Destacar baú no mundo (partículas 10 s) e seleção persistente entre refreshes | ✅ |
| Indicador fora da tela (HUD estilo FPS) com direção e distância | ✅ |
| Mochila do trabalhador + UI | ⏳ |
| Protocolo de saque/depósito do fazendeiro | ⏳ |
| Upgrades no item + hover / relocate | ⏳ |
