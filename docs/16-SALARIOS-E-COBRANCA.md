# 16 — Salários e cobrança (pesquisa + tabela inicial)

**Status:** base implementada em `alpha.9` (dev): tabela salarial configurável, cobrança persistida,
dívida/grace/greve, `/laborcraft worker pay` e comandos live `/laborcraft billing ...`. A UI do
painel admin e os testes de unidade continuam pendentes.

## 1. Referências pesquisadas

| Jogo | Como paga | O que aproveitar |
| --- | --- | --- |
| **Schedule I** | Salário **diário** (US$100/200/300 por tipo); taxa de contratação; cama obrigatória; desconta do dinheiro da casa; sem pagamento o funcionário fica ocioso e reclama, mas **não pede demissão** | Separação taxa de contratação × salário; casa/cama; diário; dívida/pausa sem sumir |
| **MineColonies** | Salário **semanal** (evento a cada 7 dias); `salário = nível × 100`; se o tesouro não tem dinheiro, avisa e o cidadão fica `isPaid=false` (recusa trabalhar) | Salário semanal, escala por nível, pausa por falta de pagamento, notificação |
| **Mount & Blade** | Semanal no Warband, **diário** no Bannerlord; base por nível; mercenário +50%, montado +66%, companheiro +100%; guarnição/descanso −50%; liderança −5%/nível; dívida e perda de moral | Multiplicadores por tipo/nível, período, dívida, moral |
| **Battle Brothers** | **Diário**; base por background (3–35) × nível (+10%/nível até 11, +3% depois) × traits/eventos; ±10% individual; sem pagar → moral cai e pode desertar | Base + nível + trait + variação individual; consequência progressiva |
| **Jagged Alliance** | Contratos **diários/semanais/quinzenais** com salários muito diferentes por mercenário | Faixa ampla de preços; contrato com duração |
| **Tycoon / game economy (Soamee)** | Salários por nível em multiplicadores **1.0 / 1.3 / 1.6 / 2.0 / 2.5**; custo não linear para evitar snowball; moral decai e pressiona o jogador | Escala não linear; evitar “contratar infinito” |
| **Kenshi** | Recrutas **não têm salário**; mercenários são contrato diário | Contraste: salário recorrente é uma escolha de design, não obrigação |

Links:
- https://teemo.dev/game-design/schedule-i/systems/employees-and-automation/
- https://github.com/ldtteam/minecolonies/wiki/Treasury-(Implementation-details)
- https://mountandblade.fandom.com/wiki/Wages
- https://battlebrothers.fandom.com/wiki/Game_Guide
- https://battlebrothersgame.com/dev-blog-110-more-things/
- https://rllamp.nl/games/downloads/Hirelings.html
- https://soamee.com/en/blog/en-game-economy-design
- https://kenshi.zone/en/recruitment-squad

## 2. Princípios para o LaborCraft

1. **Taxa de contratação ≠ salário.** O preço da oferta é pago uma vez; o salário é recorrente.
2. **Base diária; período só decide quando cobra.** Diário é o padrão do fazendeiro; semanal = ×7;
   mensal = ×30. Sem desconto por período no MVP (avaliar 2–5% depois).
3. **Rank é o nível; raridade é o “talent”/boost.** Rank escala o salário; raridade dá um prêmio
   pequeno (os boosts já encarecem via `UPKEEP`).
4. **Não linear, mas não absurdo.** O rank atual (1.0/1.25/1.6/2.0) já é não linear; manter como
   ponto de partida e deixar configurável.
5. **Falha de pagamento progressiva.** Aviso + dívida/grace → pausa → (futuro) risco de sair. Nunca
   sumir no primeiro atraso.
6. **Salário é um dreno.** O valor diário deve caber numa fração do valor produzido; o servidor
   define a moeda, então a tabela é relativa.

## 3. Tabela inicial proposta

### 3.1 Base diária por emprego (o “mínimo” é o COMUM)

| Emprego | Base diária (COMUM) | Observação |
| --- | ---: | --- |
| Fazendeiro | 24 | valor atual de `farmer.baseWage` |
| Minerador (futuro) | 30 | risco/ferramenta |
| Construtor (futuro) | 28 | consome materiais |
| Guarda (futuro) | 26 | turno noturno |
| Outros | por cargo | base própria por `JobSettings` |

O **salário mínimo do emprego** é a base do rank COMUM. Rank e raridade só aumentam a partir dela.

### 3.2 Multiplicadores

| Rank | Multiplicador de salário | Preço de contratação (atual) |
| --- | ---: | ---: |
| COMUM | 1.00 | 1.00 |
| JUNIOR | 1.25 | 1.10 |
| PLENO | 1.60 | 1.25 |
| SENIOR | 2.00 | 1.50 |

| Raridade | Prêmio de salário (novo) | Preço (atual) |
| --- | ---: | ---: |
| COMUM | 1.00 | 1.00 |
| INCOMUM | 1.05 | 1.15 |
| RARO | 1.10 | 1.40 |
| ÉPICO | 1.20 | 1.80 |

- `diária = round(base × rank × raridade + boostUpkeep × base)`.
- `boostUpkeep` é o total de `WorkerBoostType.UPKEEP` (aditivo ao multiplicador), com teto de
  segurança de **2.5×** a base.
- `semanal = diária × 7`; `mensal = diária × 30`.
- A taxa de contratação continua `price = acquisition.price × rank.price × rarity.price`
  (atualmente base 64). Avaliar subir a base para 96–120 (3–5 dias de salário COMUM).

### 3.3 Exemplos com base 24

| Rank | COMUM/dia | INCOMUM/dia | RARO/dia | ÉPICO/dia |
| --- | ---: | ---: | ---: | ---: |
| COMUM | 24 | 25 | 26 | 29 |
| JUNIOR | 30 | 32 | 33 | 36 |
| PLENO | 38 | 40 | 42 | 46 |
| SENIOR | 48 | 50 | 53 | 58 |

Semanal/mensal: multiplique por 7/30. Ex.: SENIOR ÉPICO = 58/dia, 406/semana, 1.740/mês.

### 3.4 Contratação (taxa única) — referência atual

`price = 64 × rank.priceMultiplier × rarity.priceMultiplier`:

| Rank × raridade | COMUM | INCOMUM | RARO | ÉPICO |
| --- | ---: | ---: | ---: | ---: |
| COMUM | 64 | 74 | 90 | 115 |
| JUNIOR | 70 | 81 | 99 | 127 |
| PLENO | 80 | 92 | 112 | 144 |
| SENIOR | 96 | 110 | 134 | 173 |

Comparação: Schedule I cobra ~5 dias de salário na contratação; aqui o SENIOR ÉPICO fica em ~3 dias.
Se quiser seguir essa referência, subir `acquisition.price` para ~96–120.

## 4. Cobrança (regras propostas)

- **Vencimento:** a cada período, a partir da contratação (ou do próximo ciclo), cobra
  `wagePerPeriod` do `CurrencyProvider` do dono.
- **Sem saldo:** dívida acumulada (`debt`), aviso no chat. **1º atraso = grace** (continua
  trabalhando); **2º = pausa** (não trabalha até pagar); **não auto-demite no MVP**.
- **Dívida:** o pagamento posterior quita a dívida primeiro e retoma o contrato.
- **`pauseBillingOffDuty`:** padrão `false` (cobra mesmo fora do expediente, como custo fixo).
  Se `true`, o vencimento durante folga é adiado, não perdido.
- **Dono offline:** cobra se houver saldo; senão dívida. Sempre server-side.
- **Contratos existentes:** o `upkeep` atual é snapshot. O MVP mantém o snapshot; a UI Admin
  terá “aplicar nova tabela aos existentes” com confirmação. Novas ofertas usam a tabela.
- **Notificações:** chat ao dono; UI do Empregado/Perfil mostra salário/período/próximo
  vencimento/dívida/faltas.

## 5. Balanceamento

- **Regra de bolso:** salário diário ≤ **30%** do valor médio produzido por dia; taxa de
  contratação 3–5 dias de salário. Se o servidor não tiver preço de produção, ajustar a base
  manualmente e usar o painel admin.
- Evitar snowball: rank não linear + limite de trabalhadores por casas + dreno diário.
- Moral: atraso reduz humor; no futuro, humor baixo pode aumentar risco de saída (não no MVP).

## 6. Decisões pendentes

- [ ] Manter os multiplicadores de rank atuais (1.0/1.25/1.6/2.0) ou suavizar (1.0/1.15/1.35/1.6)?
- [ ] Raridade entra no salário (proposta 1.0/1.05/1.10/1.20) ou só no preço/boosts?
- [ ] Taxa de contratação base 64 (atual) ou 96–120 (3–5 dias)?
- [ ] Salário por dia ou por período? (proposta: diário; período só cobra)
- [ ] Desconto semanal/mensal (0% no MVP)?
- [ ] Contratos existentes: snapshot ou recálculo automático? (proposta: snapshot + botão)
- [ ] Após 2º atraso: pausa; 3º: só dívida no MVP, sem auto-demissão?
- [ ] `pauseBillingOffDuty` padrão false (proposta) ou true?
- [ ] Incluir variação individual de ±10% no salário da oferta (Battle Brothers) agora ou depois?
- [ ] Perfil por cargo já no MVP ou só o farmer (proposta: só farmer; API pronta)?

## 7. Configuração live (implementado)

`/laborcraft billing set <campo> <valor>` (gamemaster):

- `enabled`, `check-ticks`, `use-schedule`, `pause-off-duty`, `notify`, `debt`, `grace`, `dismiss-after`, `mood-penalty`
- `use-table`, `minimum-daily`, `use-rank`, `use-rarity`, `use-boost`, `boost-cap`
- `base-wage`, `price`, `period-days`

`/laborcraft billing rank <rank> <multiplicador>` e `billing rarity <raridade> <multiplicador>` ajustam as tabelas; `billing apply-salary` recalcula os trabalhadores carregados com a tabela atual (não apaga dívida/faltas). Mudanças de salário, rank, raridade ou preço regeneram as ofertas automaticamente; `billing force` força uma cobrança para testar e `/laborcraft admin market refresh` continua disponível para rotação manual.

## 8. Próximos passos

1. Testar em jogo dívida/grace/greve/`worker pay`/`billing force` e o recálculo de existentes.
2. UI do Empregado/Perfil já mostra salário/dívida; falta a aba Economia do painel admin (doc 15).
3. Testes de unidade do cálculo salarial e do ciclo de cobrança.
4. Persistir contratos descarregados para cobrar sem o chunk carregado.
