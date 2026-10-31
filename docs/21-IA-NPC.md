# Núcleo de IA dos NPCs

## Camadas

1. **Shell compartilhado** — `LaborCraftWorker`
   - estado (`ACTIVE`, `PAUSED`, `OFF_DUTY`, `STRIKE`, `STORED`, `QUIT`);
   - dono, contrato, salário, dívida, humor, fome, rank/raridade/boosts;
   - inventário e ferramenta;
   - descanso/expediente (`WorkerRest`);
   - chat, pensamentos e avisos;
   - pathfinding base e portas (`getNavigation().setCanOpenDoors(true)` + `OpenDoorGoal`).

2. **Fluxo comum dos cérebros** — `AbstractWorkerBrain`
   - cooldown por cérebro;
   - ignora cliente e jogador atendendo a tela;
   - exige Posto e zona;
   - pausa/retoma por `need_zone`, `need_tool` e `tool_breaking`;
   - equipa ferramenta válida da mochila;
   - valida ferramenta e durabilidade;
   - chama `tickJob(level, station)` só quando tudo está ok.

3. **Cérebro do emprego** — `FarmerBrain extends AbstractWorkerBrain`
   - `tickJob`: sementes, composter, depósito, coleta, alvo, colher/plantar/till/fertilizar;
   - `canResumeJob`: `need_seeds`, `no_chest`, `chest_full`;
   - `onPaused`: limpa alvo e depósito.

4. **Registry** — `JobBrains`
   - `register(jobId, factory)`;
   - `create(jobId, worker)` devolve um cérebro no-op para emprego vazio/desconhecido;
   - `bootstrap()` registra o farmer.

## Regra

`LaborCraftWorker` não contém lógica de produção. Tudo que é específico de emprego vive no cérebro
daquele emprego. Correções no fluxo comum (pausa, zona, ferramenta, portas, pathfinding, descanso)
valem para todos os empregos automaticamente.

## Adicionar um emprego

1. Registre o `JobType` em `JobTypes`.
2. Crie `MeuEmpregoBrain extends AbstractWorkerBrain`.
3. Implemente `tickJob` e, se precisar, `canResumeJob`/`onPaused`/`prepareTool`.
4. Registre em `JobBrains.bootstrap()`:

```java
register(JobTypes.MINER.id(), MinerBrain::new);
```

## Portas e portões

O bug de porta era o pathfinding: `OpenDoorGoal` já existia, mas a navegação não podia atravessar
portas fechadas. O shell agora chama:

```java
getNavigation().setCanOpenDoors(true);
```

Isso vale para trabalho, descanso e qualquer cérebro futuro. O `OpenDoorGoal` abre/fecha a porta
quando o caminho passa por ela.

Fence gates são um caso separado do vanilla: `WalkNodeEvaluator` trata portão fechado como
`PathType.FENCE` e `OpenDoorGoal` só conhece `DoorBlock`. O LC adiciona:

- `WalkNodeEvaluatorMixin`: portão fechado vira `DOOR_WOOD_CLOSED` para `LaborCraftWorker`;
- `OpenFenceGateGoal`: abre o portão no caminho e fecha quando o trabalhador se afasta.
