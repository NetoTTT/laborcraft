# 05 — Decisões e questões abertas

Formato curto de ADR. Cada item: contexto → decisão (ou opções) → consequência.

## ADR-001 — Moeda via SPI, nunca tipo fixo

**Status:** aceita.

**Contexto:** o mod precisa cobrar salário em servidores que usam scoreboard, itens, plugins ou mods de economia.

**Decisão:** `CurrencyProvider` é a única porta de entrada/saída de valor. O mod não conhece nenhum mod de economia.

**Consequência:** funciona em qualquer cenário; exige fallback interno e tratamento de provedor ausente. Resolve o requisito de "usuário pode trocar a moeda".

## ADR-002 — Emprego como extensão registrada + dados

**Status:** aceita.

**Decisão:** `JobType` + `WorkTask` + definição data-driven. Fazendeiro é só o primeiro registro.

**Consequência:** minerador e culturas modded não reescrevem o núcleo; exige disciplina de API.

## ADR-003 — Trabalhador é entidade própria e genérica

**Status:** aceita em 2026-10-22.

**Decisão:** uma única entidade `laborcraft:worker`, com emprego atribuído por dados. Não usamos aldeão vanilla e não criamos uma entidade por emprego.

**Opções:**

1. **Entidade própria** `laborcraft:worker` estendendo `PathfinderMob`, renderizando como aldeão.
   - Prós: zero conflito com o cérebro vanilla, profissão, trades, `VillagerProfession`; persistência e IA controladas; não quebra farms de aldeão.
   - Contras: precisa de modelo/render próprio; não é "o aldeão da vila" literal.
2. **Contrato anexado a um aldeão vanilla** (Attachment/atributo + mixin para suprimir comportamento).
   - Prós: visual e som 100% vanilla, "contratar o aldeão que já existe".
   - Contras: briga com IA/cérebro vanilla, trades, poção, zumbificação; alto risco de incompatibilidade e de quebrar farms.

**Recomendação:** opção 1 no MVP; reavaliar "contratar aldeão existente" como cosmético (o trabalhador pode nascer com nome/aparencia do aldeão contratado).

**Consequência:** o trabalhador tem IA e persistência próprias; não quebra farms, trades, raids nem zumbificação de aldeões. Custo: modelo/render próprios.

## ADR-008 — Aquisição por Contrato ("ovo"), não spawn egg

**Status:** aceita em 2026-10-22.

**Decisão:** o jogador obtém um **item Contrato de Trabalho** que carrega o perfil. Modos: `purchase` (Agência de Empregos, paga via `CurrencyProvider` para `revenueAccount`), `craft`, `admin`, `any`.

**Consequência:** o dinheiro entra na economia do servidor de forma explícita (governo/banco); o perfil sobrevive ao item; spawn egg vanilla fica fora do fluxo.

## ADR-009 — Maestria por emprego, um emprego ativo

**Status:** aceita em 2026-10-22.

**Decisão:** `WorkerProfile` guarda `Map<emprego, XP>`. Só o emprego ativo produz. Reatribuir preserva maestria, com fee/cooldown configuráveis.

**Consequência:** trabalhador versátil fica valioso; "treinar tudo" custa tempo de uso; tier e salário vêm da maestria do emprego ativo.

## ADR-010 — Guardar/restaurar com perfil em duas camadas

**Status:** aceita em 2026-10-22.

**Decisão:** perfil completo no item (componente versionado) + registro por UUID no mundo. Guardar é atômico; usar item com UUID já ativo é recusado.

**Consequência:** portabilidade e anti-duplicação. Com `keepProfileBackup`, admin recupera perfil de item perdido.

## ADR-011 — Moradia opcional com validação mínima

**Status:** aceita em 2026-10-22.

**Decisão:** `housing.required = false` por padrão. Quando exigida: cama reivindicada + teto (`canSeeSky` falso). Sem validação de paredes/porta. Sem casa (quando não exigida): penalidade de velocidade configurável.

**Consequência:** regra simples, mod-agnóstica e previsível; evita "detector de casa" frágil.

## ADR-012 — Pathfinding vanilla

**Status:** aceita em 2026-10-22.

**Decisão:** `PathfinderMob` + `GroundPathNavigation`. Escanear área com cache, escolher alvo alcançável próximo, navegar uma vez, trabalhar no alcance. Falhas repetidas colocam o alvo em quarentena.

**Consequência:** nenhuma dependência de biblioteca de pathfinding; desempenho controlado pelo raio pequeno e intervalos.

## ADR-004 — Período em tempo de jogo

**Status:** aceita.

**Decisão:** dia = 24000 ticks. `DAILY`/`WEEKLY`/`MONTHLY` = 1/7/30 dias.

**Consequência:** previsível em servidor, mas "mês" é longo (~10h reais). Se o público preferir tempo real, vira opção (`realTimeBilling`) — decisão futura.

## ADR-005 — Preço escalona com tier e com quantidade

**Status:** aceita (valores ainda provisórios).

**Decisão:** `base * 1.5^(tier-1) * 1.1^(workers-1)`.

**Consequência:** evita automação barata demais e spam de trabalhadores; precisa de ajuste com dados reais de produção.

## ADR-006 — Config em camadas com autoridade do servidor

**Status:** aceita.

**Decisão:** padrões do mod < padrão do modpack < config do servidor < override do dono (se permitido).

**Consequência:** modpack e admin mantêm controle; jogador tem agência limitada.

## ADR-007 — Build não deve depender de JAVA_HOME do usuário

**Status:** aceita parcialmente.

**Decisão:** `build.cmd` procura JDK 25 e redireciona saída. `gradle.properties` continua sendo a fonte das versões.

**Consequência:** build confiável no Windows; Linux/macOS usam `./gradlew` com `JAVA_HOME` 25. Não usar daemon com pipe direto.

## ADR-013 — Área por posto + raio por tier, com bônus de casa

**Status:** aceita em 2026-10-22.

**Decisão:** bloco próprio `laborcraft:workstation` é o centro. Área quadrada/circular com `radiusByTier` e `verticalRadius` (andares). Casa válida soma raio/velocidade/limite, dentro de `maxRadius`.

**Consequência:** área previsível e fácil de mostrar; casa vira investimento com retorno claro. `verticalRadius` cobre fazendas em andares.

## ADR-014 — Casa validada por enclausuramento limitado + cama/porta

**Status:** aceita em 2026-10-22.

**Decisão:** `canSeeSky` + flood fill limitado (`maxVolume`, distância) para provar que é fechada; exige cama e porta; itens de conforto são bônus opcional. Revalidação periódica (60 s) e em eventos, nunca por tick.

**Consequência:** aceita construções razoáveis, rejeita buracos; custo controlado. Sem validação de estética/estrutura.

## ADR-015 — Expediente em tempo de jogo

**Status:** aceita em 2026-10-22.

**Decisão:** `schedule.mode` (`daylight`, `night`, `custom`, `always`) em ticks de jogo; fora do expediente não produz. Salário continua fixo por período (opção `pauseBillingOffDuty`).

**Consequência:** independe de dormir; servidores com noite longa podem usar turno noturno ou 24h. Custo fixo mantém contrato simples de auditar.

## ADR-016 — Pathfinding vanilla com suporte a andares via escadas

**Status:** aceita em 2026-10-22.

**Decisão:** `PathfinderMob` + `GroundPathNavigation`; escadas/lajes/portas funcionam; ladders/andaimes ficam desligados por padrão; sem quebrar/colocar blocos; quarentena de alvos inalcançáveis.

**Consequência:** multiandar funciona com escada de blocos; jogador precisa construir caminho válido. Sem dependências externas.

## ADR-017 — Posto de trabalho craftável (8 tábuas + baú)

**Status:** aceita em 2026-10-22.

**Decisão:** `laborcraft:workstation` é bloco + item registrados; receita shaped padrão de 8 tábuas (tag `minecraft:planks`) + 1 baú em `data/laborcraft/recipe/workstation.json`. O arquivo é a fonte da receita, então modpacks podem substituí-la ou removê-la sem Java.

**Consequência:** o jogador consegue o posto sem comando; a Agência de Empregos continua opcional (venda pronta é questão futura). Ainda sem aba do criativo nem GUI.

## ADR-018 — Casa compartilhada adiada

**Status:** aceita em 2026-10-22.

**Decisão:** `housing.allowSharedHouse = false` por padrão. O primeiro build valida apenas casa separada por trabalhador; a regra multi-cama fica anotada no código como etapa futura.

**Consequência:** validação de moradia mais simples e previsível; servidores que quiserem várias camas podem ligar a opção quando a implementação existir.

## ADR-019 — UI nativa com componentes próprios

**Status:** aceita em 2026-10-22.

**Decisão:** construir a UI com `Screen`/`AbstractWidget` do 26.3 e componentes reutilizáveis (`LcTheme`, `LcRender`, `LcTooltips`, `LcButton`, `LaborCraftScreen`). Sem lib externa de GUI (Cloth Config, YACL, owo-lib etc.). Bibliotecas open-source (ModMenu, YACL) servem só como referência de API.

**Consequência:** controle total do visual e nenhuma dependência extra; custo de manter os componentes. A UI acompanha a API de render state do 26.3.

## ADR-020 — Command-first: toda ação da UI tem comando

**Status:** aceita em 2026-10-22.

**Decisão:** toda ação que altera estado é registrada em `LaborCraftCommands` e a UI apenas envia o comando equivalente (`LaborCraftUiActions.run`). A UI não tem lógica de negócio nem valida permissão; o servidor é a fonte de verdade.

**Consequência:** uma implementação só, auditável e testável por comando; UI e automação usam o mesmo caminho. O feedback ainda é chat; payload estruturado fica para depois, sem mudar a regra.

## ADR-021 — Mercado de trabalho é do servidor, não do jogador

**Status:** aceita em 2026-10-22.

**Decisão:** a loja de contratação é um **mercado global do servidor**: uma lista só, igual para todos, com estoque compartilhado (quem compra primeiro leva) e rotação automática a cada `offers.rotationDays` dias do mundo. O estoque é reproduzível a partir de `seed do mundo + rotação` e fica persistido em `<mundo>/data/laborcraft/market.json` apenas com o que já foi vendido.

**Consequência:** a economia é a mesma para todos e o servidor controla oferta, preço e giro; não existe "loja por jogador". Como o estoque é compartilhado, dois jogadores não podem comprar a mesma pessoa: a segunda compra falha com mensagem clara. Forçar nova rotação é possível por `/laborcraft admin market refresh`.

## ADR-022 — Rank sorteado, raridade separada e cargo depois da contratação

**Status:** aceita em 2026-10-22.

**Decisão:** a oferta vende **a pessoa**, não o cargo. A pessoa tem rank (`Comum`, `Júnior`, `Pleno`, `Sênior`) sorteado na loja e **fixo** no contrato; raridade (`Comum`, `Incomum`, `Raro`, `Épico`) separada do rank e responsável por 0–3 boosts inatos; aptidão de 1–5 estrelas por emprego; e maestria por cargo, ganha trabalhando. O cargo é escolhido **depois** da contratação, na tela de Empregado, e pode ser trocado sem perder a maestria antiga. Rank não é promoção.

**Consequência:** a loja tem variedade real ("Sênior comum" vs "Comum épico") sem power creep, porque os multiplicadores de rank são modestos e os boosts entram de forma aditiva. O contrato v2 precisa carregar o perfil completo para sobreviver a baú, troca e dispensa. Contratos antigos (`farmer_standard`) migram para Comum/comum com cargo de fazendeiro e maestria 0.

## Questões abertas

1. **Nome definitivo / modid.** `LaborCraft` fica até o M1; renomear depois quebra saves/configs. Decidir antes da publicação.
2. **Moeda padrão:** ledger interno ou scoreboard? Ledger é mais simples; scoreboard integra melhor com servidores.
3. **Posto de trabalho:** craft resolvido (ADR-017); falta definir o visual definitivo e se também será vendido pronto na Agência.
4. **Chunk loading:** permitir que o trabalhador mantenha a área carregada? Custo de performance e "cheat" de automação offline; default hoje é **não**.
5. **Trabalho offline:** o trabalhador produz com o dono offline? Comportamento deve ser igual ao de chunk carregado; decidir se pausa sem dono online.
6. **Griefing:** quem pode contratar dentro do terreno de outro? Integração com mods de claim ou lista de confiança própria?
7. **Salário em itens:** se a moeda for item, o pagamento sai do inventário do dono ou de um baú-banco? (afeta UX e risco de perda)
8. **UI:** comando + livro de contrato ou GUI própria? GUI é mais amigável, mas exige cliente.
9. **Durabilidade/ferramentas:** fazendeiro usa enxada? (vanilla não exige, mas dá profundidade e custo)
10. **Limite de trabalhadores:** por jogador (default 1 (expansível por casas/upgrades)), por área ou por chunk? Precisa de limite para performance.
11. **Licença final:** CC0 (template) ou MIT? Definir antes da release.
12. **Casa compartilhada:** adiada; default atual é casa separada (`allowSharedHouse = false`, ADR-018).
13. **Cobrança fora do expediente:** salário fixo (default) ou pausar quando fora do horário?
14. **Ladders/andaimes:** fora do MVP (`allowLadders = false`); reavaliar se houver demanda por navegação customizada.
15. **Forma da área:** quadrado (default, mais simples) ou círculo (mais natural com casa redonda)?

## Registro de mudanças neste documento

Toda decisão que sair de "proposta" para "aceita" deve ser movida para uma seção "aceita" com a data e o motivo, e refletida no código/docs correspondentes.
