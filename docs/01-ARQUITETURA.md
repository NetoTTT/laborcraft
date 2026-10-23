# 01 — Arquitetura

## Visão de camadas

```
                +---------------------------------------------------+
                |                     Jogo                          |
                |  blocos, entidades, comandos, eventos (Fabric API) |
                +---------------------------------------------------+
                                     |
   +---------------------------------+---------------------------------+
   |                                                                   |
[ Camada de apresentacao ]                            [ Camada de simulacao ]
 - comandos                                            - Trabalhador (entidade/IA)
 - GUI / HUD (cliente, futuro)                         - Tarefas de trabalho (WorkTask)
 - notificacoes ao dono                                - Area de servico / reivindicacao
   |                                                                   |
   +---------------------------------+---------------------------------+
                                     |
                        [ Nucleo modular ]
                        - Registro de empregos (JobType)
                        - Contratos e cobranca (Contract, BillingPeriod)
                        - Motor de pagamento (usa CurrencyProvider)
                        - Configuracao em camadas
                                     |
                        [ SPI de integracao ]
                        - CurrencyProvider (moeda)
                        - JobType (emprego)
                        - Definicoes data-driven (datapack)
```

Regra de ouro: **nada acima depende de um mod específico de economia**, e **nada no núcleo depende de "fazenda"**. Fazenda e moeda são plugins internos que usam as mesmas interfaces que um mod de terceiros usaria.

## 1. SPI de moeda

O mod nunca manipula "dinheiro". Ele pede a um provedor:

```java
public interface CurrencyProvider {
    String id();                                  // "laborcraft:ledger", "server:scoreboard", ...
    long balance(UUID account);
    boolean withdraw(UUID account, long amount);
    void deposit(UUID account, long amount);
    String format(long amount);
}
```

Implementações previstas:

| Provedor | Como funciona | Serve para |
| --- | --- | --- |
| `laborcraft:ledger` | saldo em JSON próprio (`config/laborcraft-ledger.json`) | single-player, testes, server sem economia |
| `laborcraft:scoreboard` | objetivo de scoreboard (`/scoreboard players get`) | servidores vanilla com economia por scoreboard |
| `laborcraft:item` | itens configuráveis como moeda (ex.: esmeralda) no inventário/baú-banco | servers sem plugin, modpacks temáticos |
| adaptador de mod/plugin | registrado por mod de terceiros via entrada de integração | mods de economia conhecidos |

Regras:

- `CurrencyProviders.active()` lê o id configurado. Se o id não existir, cai no `ledger` e **loga erro** — nunca crash, nunca cobrança fantasma.
- `withdraw` é a única operação que pode falhar; `false` significa "sem saldo".
- Contas são `UUID` (jogador, dono do contrato). Nada de nome de exibição como chave.
- Operações de escrita são serializadas (sincronizadas) para evitar saldo duplicado.
- Mods de terceiros registram seu provedor no evento de inicialização; o núcleo não conhece o mod.

## 2. Registro de empregos

`JobType` é a identidade do emprego (id + chave de tradução). O **comportamento** vem em duas partes:

- `WorkTask`: o que fazer num passo (encontrar alvo, colher, replantar, depositar, usar composteira).
- `JobDefinition` (data-driven): quais blocos/plantas/insumos valem, raio, tier máximo, salário base.

```java
public record JobType(String id, String translationKey) { ... }
```

Empregos não sabem de salário nem de pagamento. Salário é do contrato; contrato aponta para um `JobType`.

Novos empregos entram por:
1. registro em código (`JobTypes.register`) para quem cria o comportamento, ou
2. datapack (`data/<ns>/laborcraft/jobs/*.json`) para variar parâmetros sem código.

## 3. Contratos e cobrança

```java
public record Contract(
    UUID id, UUID owner, UUID worker, String jobId,
    long wagePerPeriod, BillingPeriod period,
    boolean autoRenew, int missedPayments
) { ... }
```

- `BillingPeriod`: `DAILY` (1 dia), `WEEKLY` (7), `MONTHLY` (30). Dia = 24000 ticks. Período em tempo **de jogo**, não real.
- Cobrança a cada fim de período: `withdraw(owner, wage)`. Sucesso → `missedPayments = 0`. Falha → contador sobe.
- Ao passar do limite de graça (`missedPaymentGracePeriods`): o trabalhador **pausa**. Passando de novo: o contrato é rompido e o trabalhador vai embora.
- Opção `requirePaymentToWork = false` permite "trabalho devendo" (útil para teste/administração).

Estado (contratos, saldos devidos, pertences do trabalhador) é persistido no mundo, não em memória.

## 4. Trabalhador e área de serviço

Decisão ainda aberta (ver `05-DECISOES.md`). Restrição dura: **não quebrar aldeões vanilla**. Caminho preferido:

- Entidade própria (`laborcraft:worker`), visual de aldeão, IA própria, persistência própria.
- Ela guarda: emprego, tier, contrato, "estação de trabalho" (bloco âncora), área, baús de entrada/saída, inventário.
- Sem colisão de IA com o cérebro de aldeão vanilla, sem `Attachment` em entidade de terceiros, sobrevive a `/kill` de aldeões e a mudanças de profissão vanilla.

Área de serviço: retângulo/caixa em volta de um bloco âncora, raio definido pelo tier (ex.: tier 1 = 4, tier 4 = 9). Reivindicação evita conflito entre dois trabalhadores no mesmo bloco de plantação.

## 5. Motor de trabalho (tick)

Ordem de prioridade típica de um passo de IA:

1. Pagamento em dia? Não → pausar/não trabalhar.
2. Tem trabalho imediato? (inventário cheio, planta madura na mão para depositar) → depositar.
3. Sem semente/insumo? → buscar no baú de insumos; se não há, pausar e avisar.
4. Plantas maduras na área? → colher (drops pelo loot table do bloco).
5. Espaço vazio com semente disponível? → replantar.
6. Insumo aplicável e habilitado (farinha de osso)? → aplicar conforme tier.
7. Compostagem habilitada e composteira disponível? → alimentar composteira e/ou pegar farinha de osso pronta.
8. Nada a fazer → esperar um intervalo configurado (economia de tick).

Metas de desempenho: o trabalhador deve ser **melhor que colheita manual por unidade de tempo** (colhe e replanta sem deslocamento desnecessário), mas não mais barato que um jogador — o custo é o salário.

## 6. Configuração em camadas

Precedência (a última vence):

1. Padrões embutidos no mod.
2. Padrão do modpack (`config/laborcraft-defaults.json`, embutível no pack).
3. Config do servidor (`config/laborcraft.json`) — autoridade final no servidor.
4. Override do contrato (jogador), **só** se `allowOwnerOverrides = true`.

Um modpack pode travar valores (por exemplo, fixar a moeda em esmeralda e impedir overrides). Detalhes em `03-CONFIGURACAO.md`.

## 7. Compatibilidade e limites

- **Tags** para plantações colhíveis (`laborcraft:harvestable`), contêineres de saída/entrada e insumos. Modded crops entram por tag, sem código.
- **Sem mixin em mod alheio.** Mixin apenas em vanilla, quando inevitável, e documentado.
- **Nada de chunk loading** por padrão.
- **Nada de alterar aldeão vanilla.**
- Integrações são detectadas por id de mod na inicialização; ausência = feature desligada silenciosamente (com log em nível `debug`).

## 8. Estado atual do código

| Módulo | Estado |
| --- | --- |
| `config/` | funcional (load/save JSON) |
| `economy/` | SPI + registro + ledger funcional; scoreboard/item pendentes |
| `job/` | registro + `JobType` do fazendeiro; sem comportamento |
| `contract/` | modelo de contrato e período; sem gerente/persistência |
| `entity/` | `laborcraft:worker` genérico: atributos, persistência, dono, emprego ativo, interação e IA básica de deambulação |
| trabalhador / IA de trabalho | E0/E1: entidade, contratação e atribuição de emprego; `WorkTask` do fazendeiro ainda não iniciado |
| comandos / UI | admin, config, idioma, jogadores, teste de itens, Contratação/Empregado e comandos de worker |
