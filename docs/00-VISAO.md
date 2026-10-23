# 00 — Visão

## O que é

Um mod de **automação de mão de obra**. O jogador não constrói uma máquina: ele **contrata um trabalhador**. O trabalhador executa um emprego dentro de uma área delimitada, produz recursos de forma mais eficiente que uma colheita manual, e **custa dinheiro**.

O ciclo de jogo pretendido:

1. O jogador prepara a área (fazenda, baús de saída, baú de insumos, composteira).
2. Contrata um trabalhador: escolhe emprego, área, período e salário.
3. O trabalhador planta/colhe/deposita enquanto o contrato estiver ativo e pago.
4. O salário é cobrado a cada período. Sem dinheiro, o trabalhador para; continuando sem pagamento, ele vai embora.
5. Trabalhadores de tier maior trabalham melhor (mais rápido, área maior, usa insumos) e custam mais.

Isso transforma a automação em uma **decisão econômica**: vale mais a pena contratar mais um fazendeiro, ou subir o tier do que já existe?

## O que o mod é

- Um sistema de **empregos contratáveis** com salário recorrente.
- Um **automatizador de tarefas repetitivas** com comportamento conservador e previsível (colher o que está maduro, replantar, guardar no lugar combinado).
- Um **núcleo modular** onde moeda e tipo de emprego são extensões, não código fixo.
- Um mod **server-side por natureza**: o cliente é opcional (só HUD/GUI).

## O que o mod não é (por enquanto)

- Não substitui aldeões vanilla: o comportamento de aldeão comum continua intacto.
- Não é um mod de economia: ele consome a economia que já existe no servidor.
- Não é um mod de terreno/máquinas: nada de "bloco mágico que colhe 9x9 sozinho". O trabalhador se move e faz o serviço.
- Não carrega chunks por padrão. Se a área não está carregada, o trabalhador não trabalha (config opcional, com aviso claro).

## Princípios de design

| Princípio | Consequência prática |
| --- | --- |
| Nada de dependência obrigatória | Nenhum mod de economia é `depends`; integração é opcional e detectada |
| Moeda é um contrato, não um tipo | `CurrencyProvider` com id, saldo, saque, depósito, formatação |
| Emprego é dado, não `if` | Registro de `JobType` + definições data-driven por datapack |
| Salário é configurável em camadas | Padrão do mod → padrão do modpack → config do servidor → override do contrato (se permitido) |
| Servidor é autoridade | Cobrança, contrato e inventário do trabalhador vivem no servidor |
| Compatibilidade é requisito | Tags para plantações/baús, nada de mixin em mod alheio, comportamento opt-in |
| Falhar com aviso, nunca travar | Provedor ausente ou config inválida → fallback + log de erro, nunca crash |

## Público

- **Servidores com economia** (scoreboard, plugins, mods de moeda): o mod vira um serviço contratável e um dreno de dinheiro saudável.
- **Modpacks single-player/co-op**: o modpack define moeda e preços, e o jogador automatiza sem depender de outro mod.
- **Modders**: ponto de extensão claro para empregos e moedas.

## Nome

Provisório. Candidatos: LaborCraft, VilaTrabalho, MãoDeObra, Contratos, VillageWorks. A decisão precisa acontecer **antes** de publicar (mudar `modid` depois quebra mundos e datapacks salvos).
