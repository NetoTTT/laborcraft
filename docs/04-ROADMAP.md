# 04 — Roadmap

## Marcos

### M0 — Esqueleto (atual, concluído)

- [x] Projeto Fabric Loom para MC 26.3, build reproduzível (`build.cmd`).
- [x] Estrutura de pacotes e entrada do mod.
- [x] SPI de moeda + registro + provedor interno (ledger JSON).
- [x] Registro de empregos + `JobType` do fazendeiro.
- [x] Modelo de contrato + período de cobrança.
- [x] Documentação (visão, arquitetura, empregos, config, compatibilidade, decisões).

### M1 — Núcleo utilizável

- [x] Comandos admin `reload` e `status` + regra command-first.
- [x] UI nativa base (tema, painel, botão, tooltip) e `/laborcraft ui`.
- [x] Prompt de valor no chat (int/texto/escolha) com botões clicáveis e comando acessível.
- [x] Edição de salário base e provedor de moeda pela UI/comandos admin.
- [ ] Provedor de moeda por scoreboard e por item.
- [ ] Gerente de contratos com persistência no mundo.
- [ ] Motor de cobrança por período + eventos de pausa/rompimento.
- [ ] Comandos: `/laborcraft reload`, `jobs`, `provider`, `balance`, `pay <jogador>`.
- [ ] Notificações ao dono (chat).
- [ ] Testes de unidade do cálculo de salário e do ciclo de cobrança.
- [ ] Abas/listas na UI, tela de contrato e feedback estruturado.

### M2 — Trabalhador existe

- [x] Entidade `laborcraft:worker` (visual de aldeão, IA básica, persistência) — E0.
- [x] Contratar, atribuir emprego e dispensar pela UI (básico, command-first).
- [ ] Atribuir área por bloco âncora e baús de entrada/saída.
- [x] Bloco `laborcraft:workstation` + item + receita (8 tábuas + baú).
- [ ] Preview de área por partículas do posto.
- [ ] Reivindicação de área e de cama (sem conflito entre trabalhadores).
- [ ] Inventário do trabalhador + depósito real.
- [ ] Validação de casa (cama + porta + enclausuramento limitado) com cache.
- [ ] Expediente (`WorkSchedule`) ligado ao tick do trabalhador.

### M3 — Fazendeiro MVP

- [ ] Colher cultura madura (loot table) dentro da área.
- [ ] Replantar com semente disponível.
- [ ] Depositar no contêiner escolhido; pausar e avisar quando cheio.
- [ ] Tier 1–4 (raio + velocidade).
- [ ] Pathfinding multiandar por escadas com quarentena de alvos inalcançáveis.
- [ ] Passar `runClient`/`runServer` em teste manual com uma fazenda real.

### M4 — Refinamento do fazendeiro

- [ ] Farinha de osso e composteira (tier 2+).
- [ ] Tags `laborcraft:harvestable` / contêineres / insumos.
- [ ] Definições data-driven de emprego (datapack) para culturas modded.
- [x] GUI simples de contrato (cliente): contratação e informações do empregado.
- [ ] Balanceamento de salário e produção (dreno econômico saudável).

### M5 — Segundo emprego e ecossistema

- [ ] Minerador (ferramentas, iluminação, descarte, blocos proibidos).
- [ ] Ponto de extensão público para mods registrarem moeda/emprego.
- [ ] API documentada para terceiros.
- [ ] Localização (pt_br, en_us no mínimo) e ícone.
- [ ] Publicação (Modrinth/CurseForge) com nome definitivo.

## Política de portabilidade (atualizar sem quebrar)

1. **Versões em um único lugar**: `gradle.properties` (`minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`).
2. **Uma branch por versão de MC** quando 26.4+ chegar: `mc/26.3` (estável), `mc/26.4` (trabalho).
3. **Atualizar na ordem**: Fabric blog → template `fabric-example-mod` → Loom/Gradle → nosso código. Nunca atualizar o Loom "no escuro".
4. **`build.cmd` no lugar de comandos crus**: evita o "loop infinito" do daemon e fixa o JDK 25.
5. **Nada de API interna do Minecraft** sem verificar no jar remapeado; se o nome mudou, o compilador aponta.
6. **Sem breaking change de dados**: `schemaVersion` na config permite migrar formato antigo;
   mudar `modid`/`group` é decisão de uma vez só, antes da publicação.
7. **Changelog obrigatório**: toda release registra versão de MC, Fabric API e mudanças de config.

## Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| API do MC 26.x muda rápido (snapshots) | travar versão em `gradle.properties`, portar por branch |
| IA de aldeão é sensível a quebras | entidade própria com IA própria; mixin só em vanilla e documentado |
| Economia desbalanceada (dinheiro infinito) | multiplicadores, limite de trabalhadores por área, dreno por período |
| Conflito entre mods de economia | SPI com fallback; nunca depender de um mod específico |
| Griefing entre jogadores | área reivindicada, baús autorizados, permissões |
| Chunk descarregado = trabalhador parado | documentar; opção explícita de "carregar apenas a área" no futuro |
