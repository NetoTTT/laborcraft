# 11 — Casa, vagas de trabalho e pathing (rascunho)

**Status:** rascunho de discussão, nada implementado.
O [doc 08](08-AREA-CASA-HORARIO.md) já tem o desenho técnico (flood fill, requisitos, horário). Este documento concentra as decisões de **produto**: para que serve a casa, como ela vira vaga e como o jogador registra.

## 1. A casa é obrigatória para ter empregado?

| Opção | Como fica | Prós | Contras |
| --- | --- | --- | --- |
| **A. Base sem casa (recomendada)** | 1 trabalhador sem casa; casa dá mais vagas e bônus | jogador testa o mod na hora; casa vira meta de progressão | começo menos "realista" |
| B. Casa obrigatória | sem casa validada, não contrata | força o loop de construção | trava servidor novo; muita gente desiste antes do primeiro trabalhador |
| C. Configurável | `housing.required` (já existe, default `false`) | servidores escolhem | precisa de UI/avisos claros quando faltar casa |

**Recomendação: A com o toggle C.** `housing.required = false` por padrão.

Sem casa o trabalhador **trabalha normalmente**, mas:
- perde `housing.speedPenaltyWithoutHome` (default 25%) de velocidade;
- não ganha os bônus de conforto;
- humor (futuro) cai mais rápido.

Com casa: sem penalidade, +raio, +velocidade e as vagas extras.

## 2. Quantos trabalhadores posso ter?

Modelo proposto: **a vaga é a cama**, e a casa é o invólucro que valida a cama.

```
vagas = worker.maxWorkersPerPlayer (base, default 1) + camas registradas
```

- `worker.maxWorkersPerPlayer` passa a ser a **base sem casa** (hoje é o teto absoluto = 1).
- `housing.slotsPerBed = 1` (configurável; servidor "duro" pode deixar 0 e usar só a base).
- `housing.maxBedsPerHouse = 4` (teto por casa, evita galpão com 40 camas).
- `oneWorkerPerBed = true` (já existe): cada trabalhador tem a própria cama; dispensar libera a cama.

| Situação | Vagas |
| --- | --- |
| nenhuma casa | 1 |
| 1 casa com 1 cama | 2 |
| 1 casa com 2 camas | 3 |
| 2 casas, 3 camas no total | 4 |
| casa com 6 camas | 3 (base 1 + 4 do teto) |

Se a casa ficar inválida (parede quebrada, cama removida), as vagas caem. **Ninguém some**: os trabalhadores acima do limite pausam e avisam o dono, como já está previsto no doc 08.

Alternativa mais simples (não recomendada agora): casa = +1 vaga fixa, cama só decoração. Menos escolhas, menos abuso, mas também menos motivo para construir casas maiores.

## 3. Como o jogador registra a casa?

Regra command-first: a UI e o mundo só disparam comandos; o servidor valida.

Fluxo proposto:

1. O jogador constrói o quarto (paredes, teto, porta) e coloca **uma cama por trabalhador**.
2. **Agachar + clique direito na cama** com a mão vazia (ou `/laborcraft home set` olhando para a cama) →
   comando `laborcraft home set` → o servidor:
   - confere distância do jogador (≤ 4 blocos) e se ele é dono do terreno/região (futuro: claims);
   - roda a validação do doc 08: cama + não vê o céu + enclausuramento (flood fill) + porta;
   - registra `Home{ owner, dimension, bedPos, volume, bounds, createdAt }`;
   - soma 1 vaga.
3. Repetir para cada cama da casa (até `maxBedsPerHouse`). A mesma casa pode ser validada uma vez e as camas registradas em sequência.
4. **Remover:** agachar + clique direito na cama com o item "Chave de Casa"? Ou `/laborcraft home remove` olhando para a cama. Enquanto não existe item, comando é suficiente.

Detalhes que precisam de decisão:

- **Conflito de cama entre jogadores:** quem registra primeiro leva? Ou ninguém pode registrar uma cama cujo quarto já pertence a outro jogador (checagem de sobreposição de bounds)? Recomendação: **primeiro que registra**, com aviso "esta cama já pertence a <jogador>".
- **Quebrar a cama/casa:** a vaga cai na revalidação (`housing.recheckSeconds`); o trabalhador pausa e avisa. Não apaga o trabalhador.
- **Casa compartilhada:** fora do MVP (`allowSharedHouse = false`), ou seja, duas pessoas não dividem a mesma casa.
- **Cama em casa de outro jogador:** bloqueado pelo mesmo conflito acima.

## 4. Atribuição da cama ao trabalhador

- Ao contratar, o trabalhador pega **automaticamente** a primeira cama livre do dono (se houver) e o jogador vê isso na tela de Empregado: "Dormitório: cama em 120, 64, -34".
- Se não houver cama, ele trabalha sem dormitório (com penalidade).
- Trocar de cama: na tela de Empregado (lista de camas livres) ou por comando.
- Dispensar libera a cama.
- Se a cama for ocupada por outro trabalhador, o mod escolhe a próxima livre e avisa.

## 5. Pathing — ideias

Base do doc 08: chão, portas próprias, escadas de bloco, sem ladders, quarentena de alvo e aviso quando não há caminho.

Perguntas que a casa adiciona:

1. O trabalhador precisa **ir até a casa** para dormir/descansar à noite, ou o expediente só pausa no lugar?
   - Recomendação: ir até a cama quando o expediente fecha (dá propósito ao pathing), mas **sem** pular a noite.
2. Se a cama for inalcançável (casa lacrada, porta trancada), ele pausa no posto e avisa — não fica preso tentando.
3. O caminho do posto até a cama é revalidado quando? Ao atribuir e a cada `recheckSeconds`.
4. Distância máxima posto ↔ casa (`housing.maxDistanceFromAnchor = 32`) evita "casa do outro lado do mapa".

## 6. O que precisa de decisão (checklist)

- [ ] Casa obrigatória? (A base sem casa / B obrigatória / C configurável — recomendação: C com default `false`)
- [ ] Vaga por **cama** ou por **casa**? (recomendação: cama)
- [ ] Teto de camas por casa (recomendação: 4)
- [ ] `slotsPerBed` configurável? (recomendação: sim, default 1)
- [ ] Cama automática ao contratar ou escolhida na tela? (recomendação: automática, trocável)
- [ ] Trabalhador anda até a cama fora do expediente? (recomendação: sim, sem dormir de verdade)
- [ ] Conflito de cama: primeiro que registra? (recomendação: sim)
- [ ] Como remover uma casa: comando agora, item "Chave" depois?

## 7. Impacto no código atual

- `worker.maxWorkersPerPlayer` deixa de ser teto absoluto e passa a ser base; `WorkerOwnership.total()` continua contando ativos + contratos.
- Precisa de um registro de casas por jogador (novo `HomeRegistry`, salvo no mundo como o mercado) com `owner`, cama, bounds e dimensão.
- A validação já existe em `HousingReport` (modelo) — falta o flood fill real e o registro.
- A tela de Empregado ganha a linha "Dormitório" e a ação "Trocar cama".
- Nada disso muda a IA de trabalho em si; o pathing continua no doc 08 §4.
