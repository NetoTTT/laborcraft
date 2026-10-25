# 17 — Compatibilidade MineLab (prioridade alta)

**Status:** prioridade alta; pesquisa inicial feita, implementação pendente.
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

## O que ainda falta

- Confirmar na versão 26.3 a API exata de escrita de score (`Scoreboard#getOrCreatePlayerScore`/equivalente).
- Adicionar `compat.mineLab` ao `LaborCraftConfig` e ao painel Admin (Sistema).
- Criar o provider/adaptador e os testes de integração.
- Validar com o mod real num mundo de teste e documentar o fluxo de seleção do provedor.
- **Não** alterar o jar, o código ou a licença do MineLab; toda adaptação fica no nosso lado.

## Decisão de arquitetura

O `CurrencyProvider` já é a fronteira correta. O MineLab entra como **mais um provider**, não como dependência do core. Assim, se o MineLab mudar a implementação de dinheiro, mudamos somente o adaptador (ou a config), sem tocar em cobrança, salário, contratos ou UI.
