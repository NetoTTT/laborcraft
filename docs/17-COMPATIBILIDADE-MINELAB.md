# 17 — Compatibilidade MineLab (prioridade alta)

**Status:** prioridade alta; primeiro adaptador implementado em `alpha.14`. Ainda falta validar em servidor com o MineLab real e evoluir para outros provedores.
**Fonte:** `compatibilidade/MineLab-mods-codigo/` (código de outro servidor, de Kaue; **não modificar/redistribuir**).
**Objetivo:** permitir que o LaborCraft use o dinheiro do MineLab sem criar jar novo deles e sem depender das classes internas deles.

## O que encontramos

- O conjunto é composto por `minelab-ponte` (servidor), `minelab-bancos`, `minelab-hud`, `minelab-assombro`, `minelab-protecao` e `minelab-spawners`.
- O dinheiro do MineLab é um **scoreboard objective chamado `plims`** (valor `long`). É lido/escrito por `minelab-ponte` em `Ec.java` e `Mercado.java` e por outros mods do conjunto.
- `minelab-ponte` também sobe uma **API HTTP local** (`127.0.0.1`) com token Bearer em `config/minelab-ponte.json`:
  - endpoints como `/saude`, `/jogadores`, `/comando`, `/loja`, `/mercado/...`;
  - `/jogadores` **não devolve saldo**; não há endpoint direto de saldo/depósito;
  - `/comando` executa comandos como o bot, mas depende do bot online e do token.
- Portanto, o **contrato estável e mais simples é o objetivo `plims`**, não a API HTTP nem as classes Java deles.
- `minelab-bancos` usa os mesmos `plims`; a compatibilidade com o objetivo cobre bancos e a economia do servidor.
- Licença do MineLab: **All-Rights-Reserved**; não copiar código, não modificar o jar e não redistribuir.

## Modelo de conexão (adaptador isolado)

1. **Camada única:** criar `MineLabCurrencyProvider implements CurrencyProvider` em `com.foque.laborcraft.economy.compat`.
2. **Contrato:** ler e escrever apenas o scoreboard objective configurado (`plims` por padrão), usando a API do Minecraft (`Scoreboard`/`ScoreHolder`), sem importar `dev.kauedev.minelab.*`.
3. **Configuração:**
   - `compat.mineLab.enabled` (default `false`);
   - `compat.mineLab.objective` (default `plims`);
   - `compat.mineLab.format` (default `%,d plims`, locale `pt_BR`);
   - `compat.mineLab.fallbackToLedger` (default `true`).
4. **Detecção/segurança:** se o mod não estiver presente, o objective não existir ou o valor não puder ser lido, usar o `LedgerCurrency` e **nunca derrubar o servidor**.
5. **Escrita:** tentar a API de scoreboard; se a versão do Minecraft mudar, só este adaptador é reescrito. O núcleo de cobrança (`BillingManager`) continua falando apenas com `CurrencyProvider`.
6. **API HTTP (opcional, futura):** se o objetivo `plims` deixar de ser o contrato, criar `MineLabHttpBridge` atrás da mesma interface, lendo porta/token do JSON e com timeout/fallback. Não usar o bot para operações de saldo se o scoreboard resolve.
7. **Testes mínimos:**
   - sem MineLab: provedor não registra/usa fallback;
   - com MineLab: leitura, débito, crédito e saldo insuficiente;
   - objective renomeado: mudar só a config;
   - MineLab atualizado: recompilar sem erros porque não há dependência das classes internas.

## Implementado

- `minelab:plims` registrado como `CurrencyProvider` no registro de moedas.
- Lê/escreve o objective `plims` via API vanilla de scoreboard; não importa classes `dev.kauedev.minelab.*`.
- Resolve jogadores online e offline pelo cache de perfis do servidor.
- Fallback para o Ledger quando o objective/jogador não está disponível (configurável).
- Objective, formato e fallback ficam no JSON (`compatibility.mineLab*`).
- O provedor aparece no seletor de provedor do painel Admin; a seleção ainda é por `currencyProvider` (JSON/painel).

## Risco: scoreboard é leitura, não necessariamente a verdade

No código de `minelab-ponte` não há escrita no objective `plims`: `Ec.java` e `Mercado.java` só leem o score; `Loja.java` formata um saldo que veio do bot. As compras/pagamentos chamam `MineLabPonte.pedido(...)`, que apenas enfileira um evento JSON para o bot/API; o bot/API pode então executar `/comando` (inclusive `scoreboard players ...`) ou mexer no banco. O banco e o bot não estão neste repositório, então não dá para provar qual é o centro da verdade só por ele.

Consequência prática: uma escrita direta no `plims` feita por outro mod pode ser sobrescrita na próxima reconciliação/transação do bot. O scoreboard serve bem para leitura ao vivo, mas não é um contrato transacional seguro. O adaptador `minelab:plims` atual escreve direto; isso deve ser validado com o bot real.

Teste decisivo: (1) coloque um valor conhecido no `plims`; (2) faça uma compra/pagamento pelo bot; (3) veja se o valor externo foi mantido ou reescrito. Se for reescrito, precisamos de um endpoint de transação do bot (get/add/remove/set) e o adaptador deve passar a usá-lo, mantendo o scoreboard apenas como leitura.

## O que ainda falta

- ~~Confirmar a API de escrita de score~~ (feito com `Scoreboard#getOrCreatePlayerScore`).
- Validar saldo/débito/crédito num servidor com o MineLab real.
- Persistir/migrar saldos do fallback Ledger se o objective `plims` aparecer depois.
- Adicionar o adaptador a outros provedores (Vault/outros) atrás do mesmo SPI.
- **Não** alterar o jar, o código ou a licença do MineLab; toda adaptação fica no nosso lado.

## Decisão de arquitetura

O `CurrencyProvider` já é a fronteira correta. O MineLab entra como **mais um provider**, não como dependência do core. Assim, se o MineLab mudar a implementação de dinheiro, mudamos somente o adaptador (ou a config), sem tocar em cobrança, salário, contratos ou UI.
