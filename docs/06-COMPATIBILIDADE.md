# 06 — Compatibilidade

## Regra número um

O mod **não declara dependência** de nenhum mod de economia, de aldeões ou de agricultura. Tudo é opcional, detectado em runtime, e a ausência de uma integração desliga a feature em vez de quebrar o jogo.

## Moeda

### Provedor interno (`laborcraft:ledger`)

- Saldo em `config/laborcraft-ledger.json`.
- Bom para single-player, testes e servidores sem economia.
- Não é seguro editar com o servidor rodando; não é sincronizado entre servidores.

### Scoreboard (`laborcraft:scoreboard` — planejado)

- Usa um objetivo configurável (`laborcraft.currency` por padrão).
- Funciona com economia vanilla por `/scoreboard` e com plugins que escrevem no scoreboard.
- Requer que o objetivo exista; se não existir, o mod avisa e cai no fallback.

### Item (`laborcraft:item` — planejado)

- Item configurável (default `minecraft:emerald`), retirado/depositado de um contêiner-banco autorizado.
- Risco: perda de itens se o baú for quebrado. Precisa de regra clara e talvez "conta digital" separada.

### Mods/plugins de economia (planejado)

- Adaptadores registrados por mods de terceiros via entrypoint/ponto de extensão.
- Nenhum adaptador é embutido no jar principal se exigir dependência pesada; a ideia é ter um jar de integração separado por mod (ou um único jar "compat" opcional).
- O id do provedor é a única coisa que a config conhece.

## Plantações e blocos

- Tag `laborcraft:harvestable`: blocos que o fazendeiro pode colher.
- Tag `laborcraft:deposit`: contêineres aceitos para saída.
- Tag `laborcraft:supply`: contêineres de insumos (sementes, farinha de osso).
- Mods de agricultura adicionam suas culturas à tag via datapack; não é necessário Java.
- Se um bloco não está na tag, o trabalhador **não toca nele** (seguro por padrão).

## Aldões e entidades

- Não alteramos aldeões vanilla (ver ADR-003).
- Se o caminho "contratar aldeão existente" for aprovado no futuro, será opt-in e limitado a aldeões marcados pelo jogador, para não quebrar farms, trades e zumbificação.
- Trabalhadores não são atraídos por sino, não geram ferro, não participam de raids.

## Mundos e saves

- Estado do mod fica em dados do mundo + `config/`.
- Atualização de versão deve migrar via `schemaVersion`.
- Remover o mod do mundo deve deixar os trabalhadores como entidades inertes/removíveis, sem corromper o save (testar antes de release).

## Servidor dedicado

- O mod é primariamente server-side; o cliente sem o mod deve conseguir entrar (environment `*`, mas sem classes de cliente obrigatórias no caminho do servidor).
- UI futura será client-side opcional; comandos funcionam sem ela.
- Testar sempre `runServer` além de `runClient`.

## Performance

- Trabalhador não roda IA todo tick: intervalos configuráveis.
- Limite de trabalhadores por chunk/área (a definir).
- Sem chunk loading por padrão.
- Scan de área com cache e invalidação por evento de bloco, não varredura completa a cada tick.

## O que evitar

- Mixin em classes de outros mods.
- `@Redirect`/`@Overwrite` em vanilla sem necessidade documentada.
- Assumir que todo baú é do dono: sempre validar autorização.
- Chamar API de economia de outro mod por reflexão sem fallback.
