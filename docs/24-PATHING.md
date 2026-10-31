# Pathfinding — estudo e decisões

Documento de referência para o pathing dos trabalhadores. O objetivo é parar de ajustar
parâmetros no escuro: primeiro entender como os projetos maduros resolvem, depois escolher o que
cabe no LaborCraft.

## 1. Vanilla (`net.minecraft.world.level.pathfinder`)

- `WalkNodeEvaluator` transforma blocos em `PathType` (WALKABLE, FENCE, DOOR_*, etc.).
- Portão de cerca **fechado** é classificado como cerca (`BlockTags.FENCES`), mesmo com
  `Mob#setCanOpenDoors(true)`. O `OpenDoorGoal` só cobre `DoorBlock`, não `FenceGateBlock`.
- Resultado: um `Path` pode terminar antes do portão, ou nem incluir o portão.
- `MoveControl` é o nível mais baixo de movimento: recebe uma posição desejada, gira o mob e anda.
  É o que `PathNavigation` usa por baixo.

Conclusão: pedir para a navegação vanilla atravessar um portão fechado é frágil. Ou o caminho
existe porque o portão está aberto, ou não existe.

## 2. MineColonies (referência principal)

Repositório: <https://github.com/ldtteam/minecolonies>

### Pathfinding próprio

- `com.minecolonies.core.entity.pathfinding` tem um A* próprio (`MNode`, `AbstractPathJob`,
  `PathJobMoveToLocation`), assíncrono por chunks, com timeout.
- `PathingOptions` separa explicitamente:
  - `enterGates` (padrão `true`) — portões são atravessáveis;
  - `enterDoors` (padrão `false`);
  - `canOpenDoors`.
- `AbstractPathJob.isPassable` aceita:
  `(canEnterGates && block instanceof FenceGateBlock)`.
  Ou seja: eles **ensinam o avaliador de nós** que portão é passável, em vez de depender do vanilla.

### Movimento

- `MinecoloniesAdvancedPathNavigate` não usa o `travel` do vanilla; ele chama
  `mob.getMoveControl().setWantedPosition(...)` nó a nó.
- `getSmartGroundY(...)`: para `FenceGateBlock`, `DoorBlock` e `TrapDoorBlock` retorna o Y original
  em vez do topo da collision shape. Isso evita o mob tentar “subir” no portão (a collision do
  portão chega a 1,5 bloco, o que elevaria o alvo e travaria a passagem).

### Stuck

- `PathingStuckHandler` escala em vez de desistir:
  1. pula um nó do path;
  2. recalcula o path;
  3. anda para longe em direção aleatória;
  4. teleporta alguns nós à frente (opcional);
  5. ações completas (quebrar bloco, ladders, teleporte ao destino).
- Detecção usa **índice do próximo nó**, não só posição: se o índice não muda, está preso.
- Timeout global proporcional à distância (`timePerBlockDistance`).

## 3. Baritone (referência de A*)

Repositório: <https://github.com/cabaletta/baritone>

- A* genérico com custos por movimento (`MovementTraverse`, `MovementAscend`, etc.).
- Excelente referência de custo/heurística e de como tratar portas/portões como movimentos, mas é
  grande demais para importar. Serve para copiar ideias de custo e de “movement” atômico.

## 4. O que o LaborCraft faz hoje

- Navegação vanilla para trechos abertos (fora do cercado e dentro dele).
- Travessia do portão feita manualmente:
  - abre o portão;
  - para a navegação;
  - dirige 2 blocos pelo eixo do portão com `MoveControl`;
  - fecha o portão ao terminar.
- Recuperação: timeout de travessia, retry com backoff e aviso no chat a cada 30 s.
- Diagnóstico: em falha, o chat mostra `pos`, `inside`, `spans`, `bbox`, `gate`, `inward`, `path`,
  e o mesmo vai para `latest.log`.

## 5. Próximos passos recomendados

1. **Avaliador de nós próprio** (modelo MineColonies): marcar `FenceGateBlock` como passável no
   `WalkNodeEvaluator` de forma completa, não só um `PathType`. Isso eliminaria a travessia manual.
2. **Ground Y inteligente**: ao mirar o portão, usar o Y da base (como `getSmartGroundY`), nunca o
   topo da collision.
3. **Stuck por índice de nó**: trocar o detector de posição pelo índice do path, com escalada
   (pular nó → recalcular → afastar → desistir).
4. **Path assíncrono** só quando houver muitos trabalhadores; hoje o custo é baixo.
5. **Nunca** teleportar silenciosamente em produção; no máximo teleporte de recuperação com log.

## 6. Links

- MineColonies pathfinding: <https://github.com/ldtteam/minecolonies/tree/version/main/src/main/java/com/minecolonies/core/entity/pathfinding>
- `PathingOptions`: <https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/core/entity/pathfinding/PathingOptions.java>
- `PathingStuckHandler`: <https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/core/entity/pathfinding/navigation/PathingStuckHandler.java>
- `MinecoloniesAdvancedPathNavigate`: <https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/core/entity/pathfinding/navigation/MinecoloniesAdvancedPathNavigate.java>
- `AbstractPathJob` (regra de portão): <https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/core/entity/pathfinding/pathjobs/AbstractPathJob.java>
- Baritone: <https://github.com/cabaletta/baritone>
