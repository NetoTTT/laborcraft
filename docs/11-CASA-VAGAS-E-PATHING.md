# 11 — Casa, vagas de trabalho e pathing

**Status:** casa opcional decidida; **validação, registro/claim, nomes e vagas implementados** (`HousingValidator`, `HomeRegistry`, `/laborcraft home ...`), com modal de registro e abas **Resumo / Emprego / Casa** no Empregado. Falta o descanso/**pathing até a cama**, as animações e o mapa visual; o expediente será ligado junto com essa etapa.

O [doc 08](08-AREA-CASA-HORARIO.md) tem o desenho técnico (flood fill, requisitos, horário). Este documento concentra as decisões de **produto**.

## 1. A casa é obrigatória? Não.

| Situação | O que acontece |
| --- | --- |
| **sem casa** | 1 trabalhador (vaga base), trabalhando com −25% de velocidade (`speedPenaltyWithoutHome`), sem bônus de conforto |
| **com casa válida** | sem penalidade, +raio, +velocidade, +conforto e **vagas extras** |

`housing.required = false` por padrão. Servidor que quiser exigir casa liga a opção (aí, sem casa validada, não contrata).

## 2. Vagas: uma cama dentro de um quarto válido

A vaga é a **cama registrada**, e a casa é o quarto que a valida.

```
vagas = 1 (base, sem casa) + camas válidas
```

A quantidade de camas que um quarto aceita vem do **tamanho interno**:

| Interior | Área | Camas (capacidade) |
| --- | ---: | ---: |
| 3x3 | 9 | inválida (mínimo 5x5) |
| 4x4 | 16 | inválida (mínimo 5x5) |
| **5x5** | 25 | **2** |
| 6x6 | 36 | 3 |
| **7x7** | 49 | **4** |
| 8x8+ | 64+ | 4 (teto `maxBedsPerHouse`) |

Regra: `capacidade = area / 12`, entre 2 e `housing.maxBedsPerHouse` (4), exigindo largura e profundidade ≥ `housing.minInteriorSize` (5).

- Casa de 7x7 **externa** com parede de 1 bloco = 5x5 interno = **2 camas**.
- Para 4 camas é preciso 9x9 externo = 7x7 interno.
- Cama colocada além da capacidade é **ignorada** (não dá vaga).
- Cada trabalhador ocupa uma cama (`oneWorkerPerBed`); dispensar libera.
- Casa com 1 cama = 1 trabalhador sem penalidade; com 2 camas = 2 trabalhadores.
- Decoração interna não muda a conta: a medida usa a caixa do piso livre.

## 3. Como a área interna é medida (implementado)

[`HousingValidator.java`](../src/main/java/com/foque/laborcraft/housing/HousingValidator.java):

1. A cama é a âncora. Semeia o flood fill no **ar acima da cama**.
2. Propaga em 6 direções, atravessando **só ar**. Parede, teto, vidro, porta fechada e trapdoor fechada são fronteira.
3. Se o fill alcançar uma célula que **vê o céu** (heightmap `MOTION_BLOCKING`) → casa aberta.
4. Limites: volume `housing.maxVolume` (512) e raio `housing.maxDistanceFromAnchor` (32).
5. O tamanho é medido **no andar da cama**, usando as duas camadas do piso (chão + altura da cabeça), para que móvel encostado na parede não encolha o interior.
6. Conta portas e camas encostadas no volume aéreo; conforto = blocos únicos da lista `comfortBlocks` (não conta a mesma face três vezes).

## 4. Sobrado: conta como casão?

**Recomendação: não somar andares — medir cada piso.** O segundo andar é outro quarto, com as próprias camas e a própria capacidade. Um sobrado 5x5 em cima de outro 5x5 dá 2 + 2 vagas, e não 4 só porque o volume total é maior.

Prós: intuitivo ("cama no andar X vale pelo andar X"), não infla com torre 1x1, sem código de grafo 3D. Efeito colateral bom: pé-direito duplo não dobra vaga.

## 5. Resultados medidos (servidor de teste, RCON)

| Construção | Interno | Volume | Portas | Camas | Vagas | Resultado |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| 7x7 externo, 1 porta, 1 cama | 5x5 (25) | 49 | 1 | 1 | **2** | válida |
| 8x8 externo | 6x6 (36) | 71 | 1 | 1 | **3** | válida |
| 9x9 externo | 7x7 (49) | 97 | 1 | 1 | **4** | válida |
| 7x7 com buraco no teto | — | — | — | — | — | inválida: vê o céu |
| 9x9 com a porta removida | 7x7 | 97 | 0 | 1 | 0 | inválida: sem porta |
| 5x5 externo | 3x3 (9) | 17 | 1 | 1 | 0 | inválida: pequena |
| 7x7 externo **com baú no canto** | 5x5 (25) | 48 | 1 | 1 | 2 | válida: decoração não encolhe |
| Sobrado (2 andares, escada no teto) | 5x5 por andar | 100 | 2 | 2 | 2 + 2 | válida por andar |

Comando usado: `/laborcraft home scan <x> <y> <z>` (gamemaster/console), apontando para a cama.

### Planta de Casa (item de medição)

`laborcraft:house_plan` — clique direito no chão: o item mede o cômodo **como se houvesse uma cama no bloco clicado** e responde no chat (válida/inválida, motivo, interno, volume, portas, camas e vagas). Não registra nada, não consome o item.

- Receita: 3 papel + 1 graveto (placeholder).
- Textura provisória: `minecraft:item/paper` (troca depois, junto com os outros itens).
- Para administração/console existe o equivalente `/laborcraft home measure <x> <y> <z>`.

## 6. Como registrar a casa (implementado)

1. Constrói o quarto (paredes, teto, porta) e coloca a cama.
2. **Agachar + clique direito na cama** com a mão vazia abre o **modal de registro**: o servidor roda o `HousingValidator`, mostra interno/portas/camas/vagas e pede um **nome** para a casa. Confirmar executa `laborcraft home set <x> <y> <z> <nome>` e registra:
   `Home{ owner, name, dimension, bedPos, bounds, capacity, beds, residents, createdAt, checkedAt, valid }`.
   - O comando `/laborcraft home set` também aceita o nome (ou usa `Casa N`).
   - A âncora é normalizada para o **pé da cama** (as duas metades não viram duas camas).
   - `beds` são as camas reais do quarto; `slots = min(beds, capacity)`.
3. O total do jogador passa a ser `worker.maxWorkersPerPlayer` (base) **+ os slots das casas válidas**.
4. **Desvincular/remover:** `/laborcraft home remove` olhando para a cama (ou com a posição). O registro sai, mas o cômodo físico continua e fica **livre para outro jogador reivindicar**.
5. **Renomear:** `/laborcraft home rename [pos] <nome>`; no modal de uma casa já sua também há o campo de nome e o botão **Renomear**.
6. Conflito: **primeiro que registra leva**; o segundo recebe "esta casa já pertence a <jogador>". Cômodos não podem se sobrepor.
7. Cama quebrada ou casa alterada: a revalidação (`housing.recheckSeconds`) suspende os slots; o dono é avisado e a casa volta quando o cômodo for consertado. O trabalhador nunca desaparece.

### 6.1 Atribuir a casa a um trabalhador (implementado)

- `/laborcraft home assign <trabalhador> [cama]` escolhe a cama livre mais próxima do trabalhador (ou a cama informada) entre as casas válidas do jogador.
- A tela **Empregado** agora tem abas **Resumo / Emprego / Casa**:
  - **Emprego**: lista de cargos (preparada para vários empregos), com o cargo atual marcado e um clique para atribuir.
  - **Casa**: lista as casas do jogador com **nome**, cama, camas livres e validade; clicar numa casa atribui uma cama livre; **Liberar casa** desfaz o vínculo.
  - **Resumo**: cargo, bônus, casa atual e tarefa.
- Uma cama por trabalhador; cada trabalhador pode morar em apenas uma casa. Uma casa com várias camas hospeda um trabalhador por cama. Atribuir de novo em outra casa **move** o trabalhador.
- Dispensar/remover o trabalhador libera a cama automaticamente.
- `/laborcraft home list` mostra casas, ocupação e limite; `/laborcraft home info` mostra o relatório da casa apontada. `/laborcraft home scan|measure` continuam como diagnóstico de gamemaster.

### 6.1.1 Gerenciar a casa (implementado)

- Agachar + clique na cama de uma casa **já sua** abre o modal com o nome atual, **Renomear**, **Desvincular casa** (dois cliques) e **Fechar**.
- Desvincular remove o registro; o cômodo continua no mundo e outra pessoa pode reivindicar (não há "casa sem dono" persistente: o registro some e a casa fica livre).
- Os trabalhadores que moravam nela perdem a cama e o limite cai; avisos/limites são recalculados.
- Comandos: `/laborcraft home rename [pos] <nome>` e `/laborcraft home remove [pos]`.

### 6.2 Mapa de casas (ideia futura)

Anotado para depois: uma visão de cima das casas registradas, com nome, cama, vagas livres e o ocupante, servindo de seletor visual na aba **Casa** (em vez de só a lista). Pode reaproveitar o `LcAreaMap`/camadas do Posto, mas é só uma ideia; não entra nesta etapa.

### 6.3 Descanso e expediente (MVP implementado)

- `WorkerRest` usa o `WorkSchedule`: dentro do horário o `FarmerBrain` trabalha; fora, o trabalhador entra em **Fora do expediente**, para a IA e vai até a cama atribuída.
- Anda até uma posição ao lado da cama, chama `startSleeping` (pose `SLEEPING` vanilla) e acorda com `stopSleeping` quando o expediente volta.
- Sem casa/cama, casa em outro chunk não carregado ou caminho inalcançável: **espera no posto** com aviso; não teleporta.
- Abre portas (`OpenDoorGoal`).
- O padrão atual é o `schedule` global `daylight`; aceita `custom`/janelas que viram o dia. Perfis por cargo ficam preparados em `WorkSchedule.isWorking(jobId, time)`.
- A pose de dormir do modelo de aldeão é a vanilla; validada em jogo, o visual fica a polir (modelo de aldeão deitado fica um pouco estranho).

## 7. Pathing — ideias

Base do doc 08: chão, portas próprias, escadas de bloco, sem ladders, quarentena de alvo e aviso quando não há caminho. A casa adiciona:

1. O trabalhador **anda até a cama** quando o expediente fecha? (recomendação: sim, sem pular a noite)
2. Cama inalcançável → pausa no posto e avisa, não fica preso tentando.
3. Caminho posto ↔ cama revalidado ao atribuir e a cada `recheckSeconds`.
4. Distância máxima posto ↔ casa: `housing.maxDistanceFromAnchor` (32).

## 8. Checklist

- [x] Casa opcional (base 1 trabalhador com penalidade)
- [x] Vaga = cama dentro de quarto validado
- [x] Capacidade por área: 5x5 = 2, 6x6 = 3, 7x7 = 4, teto 4
- [x] Sobrado: capacidade por andar
- [x] Validação de área interna (flood fill) + comando de diagnóstico
- [x] Registro persistente da casa + claim da cama
- [x] Ligar vagas ao limite de contratação
- [x] Atribuir casa/cama a um trabalhador (UI + comando)
- [x] Modal de registro com nome da casa
- [x] Renomear e desvincular a casa pelo modal/comandos
- [x] Abas no Empregado (Resumo / Emprego / Casa) e lista de casas
- [ ] Mapa visual de casas (ideia anotada)
- [x] Trabalhador anda até a cama fora do expediente (descanso MVP) e espera no posto se a casa não estiver carregada.
- [x] Pose de dormir vanilla validada (deita); [ ] polir o visual do modelo de aldeão.
- [ ] Casa compartilhada entre jogadores (fora do MVP)

## 9. Impacto no código atual

- `worker.maxWorkersPerPlayer` continua sendo a vaga **base** (sem casa); o total soma as camas válidas.
- `WorkerOwnership.limit(player)` = base + `HomeRegistry.slots(...)`; a compra, o contrato e as telas usam esse limite.
- `HomeRegistry` salvo em `<mundo>/data/laborcraft/homes.json`, com **nome**, dono, cama-âncora, bounds, capacidade, camas, moradores, validade e motivo da falha.
- `HousingReport` carrega volume, largura/profundidade, portas, camas canônicas (`bedList`), bounds e motivo da falha.
- Revalidação automática a cada `housing.recheckSeconds`; `HomeRegistry` também é limpo no stop do servidor.
