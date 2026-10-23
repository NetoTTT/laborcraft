# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento semântico a partir da primeira release pública.

## [0.1.0-alpha.1] — 2026-10-23

Primeira release alpha pública: mercado de trabalho do servidor, contrato v2 com rank/raridade/boosts,
validação de casa e Planta de Casa.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Estrutura inicial do mod Fabric (Loom 1.19.3, Gradle 9.8.2, Java 25).
- Projeto renomeado de `VillageJobs` para `LaborCraft` (modid `laborcraft`, pacote `com.foque.laborcraft`).
- Bloco `laborcraft:workstation` (Posto de Trabalho) + item + assets + receita de 8 tábuas + baú.
- Comandos `/laborcraft admin reload` e `/laborcraft admin status` (permissão de gamemaster).
- UI nativa base: tema, painel, `LcButton` com variantes/foco/tooltip, `LaborCraftScreen` e tela Admin.
- Comando `/laborcraft ui` (servidor envia payload; abre a tela Admin) e receiver clientbound.
- Navegação Admin -> Config com `/laborcraft config`; tela Config com botão Voltar para Admin.
- Alternância de idioma `pt_br`/`en_us` pela tela Config, via `/laborcraft language <code>` + payload clientbound; o cliente salva e recarrega os recursos.
- `LcPagination<T>`: modelo reutilizável de paginação (página atual, tamanho, anterior/próxima).
- `LcItemGrid<T>`: grade de itens com ícone, quantidade de domínio, badge total compacto e tooltip com valor exato/quebra em pilhas.
- `/laborcraft test items` + `OpenItemTestPayload`: tela de teste em grade 10x5, com 50 itens, navegação e clique.
- `LaborCraftItemActionScreen`: base da tela de ação do item, com opções por tipo, seletor de quantidade em `long` limitado ao estoque e retorno para a página anterior.
- Ações de item (pegar/comprar/equipar/usar) tocam o som de coleta de XP; navegação e ações neutras mantêm o clique padrão.
- `/laborcraft players` + `OpenPlayersPayload`: tela paginada de jogadores online com rosto, nick, latência e destaque do jogador local.
- Botão **Jogadores online** na tela Admin.
- E0 da entidade: `laborcraft:worker` genérica (`PathfinderMob`), render de aldeão, atributos, persistência, IA básica e nome visível.
- Loja pública de **Contratação** (`/laborcraft hire`) com preço, saldo, trabalhadores ativos, contratos pendentes e limite; o botão **Comprar** desabilita no limite ou sem saldo.
- Item `laborcraft:worker_contract` (Contrato de Trabalhador), com o componente `laborcraft:contract_owner`; só o comprador pode usar, e ao usar spawna um trabalhador já vinculado e abre a tela de Empregado.
- Tela de **Empregado** com emprego atual, atribuição de emprego e dispensa com confirmação.
- `OpenHiringPayload` e `OpenWorkerPayload`; atualização da loja após compra e da tela após atribuir/dispensar.
- Comandos `/laborcraft hire`, `/laborcraft worker buy`, `spawn [nome]`, `assign`, `dismiss`, `info` e `remove`; `buy`/`assign`/`dismiss` validam limite/dono no servidor.
- `/laborcraft admin set balance <valor>` para definir o saldo do jogador no provedor ativo (útil para testar a compra).
- `LedgerCurrency` carrega `config/laborcraft-ledger.json` na inicialização.
- Trabalhadores sem vínculo (spawn de admin) não abrem a loja; a interação informa que falta um contrato.
- `LcValuePrompt`: valor inteiro/texto/escolha no chat; o jogador digita apenas o valor, com cancelar, sugestoes clicaveis e validacao.
- Comandos `/laborcraft admin set wage <int>` e `/laborcraft admin set provider <id>`.
- Regra command-first: toda ação da UI envia um comando equivalente; o servidor valida e executa.
- Configuração em JSON com padrões de salário, período e multiplicadores.
- SPI `CurrencyProvider` + registro + provedor interno `laborcraft:ledger`.
- Registro de empregos com `JobType` do fazendeiro.
- Modelo de `Contract` e `BillingPeriod` (diário/semanal/mensal).
- Modelo de trabalhador: `WorkerProfile`, maestria por emprego, `WorkerState` e regras de aquisição/moradia/reatribuição na config.
- Modelo de área (`WorkArea`, `AreaShape`), relatório de casa (`HousingReport`) e expediente (`WorkSchedule`) configuráveis.
- Documentação de visão, arquitetura, empregos, configuração, trabalhador, área/casa/horário, roadmap, decisões e compatibilidade.
- Script `build.cmd` para build confiável no Windows (JDK 25 + saída redirecionada + `--no-daemon`).

- `LcScrollList`: componente reutilizável de lista com viewport recortado, roda do mouse, barra e clique por linha.
- Loja de contratação com ofertas roláveis (`OpenHiringPayload.Offer`), preço, manutenção e seleção antes da compra; `/laborcraft worker buy <oferta>`.
- Tela de **Perfil** (`/laborcraft profile`, `OpenProfilePayload`) com dinheiro, empregados/limite e lista rolável de trabalhadores com emprego, estado, tarefa, vida, fome e humor.
- Contrato guarda dados do trabalhador (`laborcraft:contract_data`: oferta, nome, emprego, boost, manutenção, fome e humor).
- Dispensar agora converte o trabalhador em Contrato de Trabalhador e remove a entidade; o contrato fica vinculado ou negociável conforme `acquisition.contractTradeable`.
- `worker.maxWorkersPerPlayer` padrão agora é **1**, expansível no futuro por casas/upgrades.
- Trabalhador ganhou `hunger` (0–20), `mood` (0–100) e tarefa derivada do emprego/estado, persistidos e enviados à UI.
- Mercado de trabalho **do servidor** (`WorkerMarket`): loja global, estoque compartilhado e rotação automática; persistido em `<mundo>/data/laborcraft/market.json`.
- Ofertas determinísticas a partir de `seed do mundo + rotação` (`WorkerOffers`): mesmo mundo e mesmo dia = mesmas pessoas, com nome próprio sorteado.
- `WorkerRank` (Comum/Júnior/Pleno/Sênior) com multiplicadores de velocidade, aprendizado, fôlego, manutenção e preço; rank sorteado na oferta e fixo no contrato.
- `WorkerRarity` (Comum/Incomum/Raro/Épico) separada do rank, controlando a quantidade e a força dos boosts.
- `WorkerBoost`/`WorkerBoostType` (velocidade, fôlego, aprendizado, rendimento, manutenção, aptidão) e `WorkerAptitudes` (1–5 estrelas por emprego).
- Contrato v2 (`laborcraft:contract_data`) com rank, raridade, boosts, aptidões, maestria por cargo, cargo atual, nome, fome e humor; contratos antigos migram para Comum/comum com cargo de fazendeiro.
- Comando `/laborcraft admin market refresh` para forçar nova rotação do mercado.
- Tela de Contratação mostra pessoa, rank, raridade, aptidão, boosts, preço e manutenção; tela de Empregado mostra rank/raridade, aptidão e maestria; Perfil colore o nome pela raridade e mostra `Rank · Cargo · Estado`.
- `HousingValidator`: valida a área interna da casa por flood fill a partir da cama (só ar atravessa; porta fechada e vidro selam; interior que vê o céu invalida; limites de volume e raio). O tamanho é medido no **andar da cama**, e a capacidade de camas sai de `área / 12` (mín. 2, máx. `maxBedsPerHouse`), exigindo 5x5 interno.
- Comando `/laborcraft home scan <x> <y> <z>` (gamemaster/console) devolve o relatório da casa: válida/inválida, motivo, interno, volume, portas, camas e vagas.
- Config `housing.minInteriorSize` (5) e `housing.maxBedsPerHouse` (4).
- Item `laborcraft:house_plan` (**Planta de Casa**): clique direito no chão mede o cômodo como se houvesse uma cama ali e responde no chat com válido/inválido, motivo, tamanho interno, volume, portas, camas e vagas. Receita de 3 papel + 1 graveto; textura provisória (papel).
- Comando `/laborcraft home measure <x> <y> <z>` (gamemaster/console): mesma medição da Planta, sem exigir cama real.

### Alterado

- `housing.allowSharedHouse` agora é `false` por padrão; casa compartilhada fica para etapa futura.
- A oferta passou a vender **a pessoa**, não o cargo: contrato novo entra “sem cargo” e o emprego é escolhido na tela de Empregado.
- `worker buy` retira a pessoa do estoque do servidor; se a mochila estiver cheia, a oferta volta ao estoque e o dinheiro é devolvido.
- A loja deixou de ser por jogador: estoque, preço e rotação são do servidor, iguais para todos.
- Trabalhador ficou **parado por enquanto**: a IA de passeio (`WaterAvoidingRandomStrollGoal`) foi removida até existir a IA de trabalho (área/casa/pathing).
- Ao abrir a tela do trabalhador, ele **para tudo** (navegação, movimento) e olha para o jogador; ao fechar, é liberado. O estado abre/fecha vem do cliente por `worker_screen_state`, então funciona em servidor.
- Dispensar não reabre mais a loja de contratação: apenas fecha a tela de Empregado (`close_screen`).
- O Perfil mostra os contratos ainda na mochila (`0 / 1 · 2 contrato(s)`) e a loja passou a rotular “Trabalhadores + contratos”, porque o limite conta os dois.
- Licença trocada de CC0-1.0 para **GPL-3.0-or-later** (copyleft): forks e trabalhos derivados precisam continuar open source.
- README principal em inglês; a versão em português ficou em `README.pt-BR.md`.
- Novo documento de desenho do trabalho ([`docs/13-TRABALHO-E-ATRIBUICAO.md`](docs/13-TRABALHO-E-ATRIBUICAO.md)): posto como âncora, zona opcional desenhada com estaca e vínculo trabalhador↔posto; casa pausada.

### Corrigido

- Trabalhador continuava andando com a interface aberta (e o `isPauseScreen()` falso não pausa o mundo em single-player): agora o congelamento é do servidor, não do cliente.
- `/laborcraft worker buy <oferta>` rejeitava `laborcraft:offer/7/3`: o id é um `ResourceLocation` e o argumento era `word()` (sem `:` nem `/`). Agora usa `IdentifierArgument`, como o `<job>` do `assign`.
- No modo criativo o Contrato de Trabalhador não era consumido: cada uso gerava um trabalhador e cada dispensa devolvia outro contrato, então a contagem estourava (`2/1` com 0 empregados) e a compra ficava bloqueada. Agora o contrato é consumido sempre.
- Dispensar com a mochila cheia sumia com o contrato: no criativo o `Inventory.add()` **destrói o que não couber e ainda retorna `true`**. Agora a dispensa guarda uma cópia intacta, checa espaço antes e, sem slot, deixa o contrato no chão avisando no chat. A compra também valida espaço antes de cobrar.
- Porta contava como **duas** no relatório da casa (a porta ocupa dois blocos): agora só a metade de baixo conta.
- Clique nos itens de `LcItemGrid` não abria a tela de ação no MC 26.3: o botão esquerdo agora é tratado por `onClick`/`isValidClickButton` (no 26.3 o esquema de botões é 1=esquerdo, 2=meio, 3=direito).
- Build travando por causa do daemon do Gradle segurando o stdout; `build.cmd` agora usa `--no-daemon` além do redirecionamento.
- Loom 1.19.3 exigindo Gradle 9.8+: wrapper atualizado para 9.8.2.
- Chaves de estado, emprego, vida e situação do trabalhador apareciam cruas nas telas; o código e os arquivos de lang agora usam o mesmo prefixo `laborcraft.ui.worker.*`.
