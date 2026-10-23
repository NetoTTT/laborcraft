# 11 — Casa, vagas de trabalho e pathing

**Status:** casa opcional decidida; **validação da área interna implementada e testada** (`HousingValidator` + `/laborcraft home scan`). Falta o registro/claim da casa e a ligação com o limite de trabalhadores.

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

## 6. Como registrar a casa (ainda não implementado)

1. Constrói o quarto (paredes, teto, porta) e coloca a cama.
2. **Agachar + clique direito na cama** com a mão vazia → `laborcraft home set` → o servidor roda o `HousingValidator` e registra:
   `Home{ owner, dimension, bedPos, bounds, capacity, createdAt }`.
3. Cada cama registrada vira 1 vaga; o total do jogador soma as casas.
4. Remover: comando olhando para a cama (item "Chave" depois, se fizer sentido).
5. Conflito: **primeiro que registra leva**; o segundo recebe "esta cama já pertence a <jogador>".
6. Cama quebrada ou casa alterada: a revalidação (`housing.recheckSeconds`) derruba a vaga; o trabalhador **pausa e avisa**, nunca desaparece.

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
- [ ] Registro persistente da casa + claim da cama
- [ ] Ligar vagas ao limite de contratação
- [ ] Trabalhador anda até a cama fora do expediente?
- [ ] Casa compartilhada entre jogadores (fora do MVP)

## 9. Impacto no código atual

- `worker.maxWorkersPerPlayer` continua sendo a vaga **base** (sem casa); o total passa a somar as camas válidas.
- `WorkerOwnership.total()` continua contando ativos + contratos; a checagem de limite ganha o termo "camas válidas".
- Falta um `HomeRegistry` (salvo no mundo, como o `WorkerMarket`) com dono, cama, bounds e capacidade.
- `HousingReport` já carrega volume, largura/profundidade, portas, camas, capacidade, conforto e motivo da falha.
