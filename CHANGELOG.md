# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento semântico a partir da primeira release pública.

## [Não lançado]

Base da casa do trabalhador: registrar o cômodo com nome, atribuir uma cama e aumentar o limite de
trabalhadores. O Empregado ganhou abas e a casa agora tem modal de registro e lista de casas.
O conflito de áreas passou ao modelo **zona-only**: a área máxima é só um limite e só a zona
desenhada é exclusiva. O trabalhador agora respeita o **expediente** e descansa na cama da casa
(pose de dormir vanilla); se a casa não estiver carregada, espera no posto.

### Adicionado

- Registro persistente de casas (`HomeRegistry`, `<mundo>/data/laborcraft/homes.json`): **nome**, dono, cama-âncora, limites internos, capacidade, camas e moradores.
- **Modal de registro**: agachar + clique direito na cama com a mão vazia mostra os dados do cômodo (interno, portas, camas, vagas) e pede um nome; confirmar registra. `/laborcraft home set [pos] [nome]` continua disponível.
- **Gerenciar casa**: no modal de uma casa já sua, **Renomear** (`/laborcraft home rename [pos] <nome>`) e **Desvincular casa** (dois cliques, `/laborcraft home remove [pos]`); desvincular deixa o cômodo livre para outro jogador reivindicar.
- Abas **Resumo / Emprego / Casa** na tela Empregado: a aba Emprego lista os cargos (pronta para novos empregos) e a aba Casa lista as casas com nome, camas livres e validade, com **Atribuir** por casa e **Liberar casa**.
- Âncora normalizada para o **pé da cama**, então uma cama de casal conta como uma vaga (não duas).
- Slots da casa: `limite = worker.maxWorkersPerPlayer + camas válidas`, usado na compra, no contrato, na contratação e no perfil.
- Atribuição de casa ao trabalhador por `/laborcraft home assign <trabalhador> [cama]` e pela aba Casa; `/laborcraft home unassign` remove o vínculo. Uma cama por trabalhador; atribuir em outra casa move o trabalhador.
- `/laborcraft home list`, `home info`, `home remove` e `home revalidate` (gamemaster); o diagnóstico `home scan|measure` continua.
- Revalidação automática a cada `housing.recheckSeconds`; casa quebrada suspende os slots, avisa o dono e volta quando o cômodo for consertado.
- Dispensar ou remover um trabalhador libera automaticamente a cama.
- **Expediente/descanso MVP**: fora do horário de trabalho o fazendeiro para a IA, anda até a cama da casa, deita com `startSleeping` (pose vanilla, validada em jogo; visual do aldeão a polir) e volta ao trabalho quando o expediente abre. Sem casa/cama ou com o chunk da casa descarregado, espera no posto; se a casa existe mas é inalcançável, também espera no posto.
- Novo estado **Fora do expediente** com tarefas: indo para casa, dormindo, sem casa, casa longe/inalcançável. O trabalhador abre portas com `OpenDoorGoal`.
- `WorkSchedule` preparado para perfis por cargo; por enquanto o fazendeiro segue o `schedule` global (padrão `daylight`, aceita `custom`/janelas que viram o dia).

### Alterado

- **Conflito entre postos (zona-only)**: a área máxima pode sobrepor a de outro posto; só a **zona desenhada** é exclusiva. Um posto pode ser colocado/claimado dentro da área máxima de outro, mas não dentro da zona; uma zona não pode cruzar outra zona nem conter outro posto.
- O mapa não recorta mais a área máxima do posto selecionado; os outros postos aparecem pelas suas **zonas** (azul), não pela área máxima, e o limite tracejado continua inteiro.
- **"Usar área máxima" e o desenho manual recortam automaticamente** a área contra zonas/blocos vizinhos, gravando vários retângulos no mesmo posto (até 32). Se não sobrar área ou o recorte passar do cap, recusa. As células que serão cortadas aparecem em vermelho, mas o **Confirmar** fica habilitado; a Estaca de Demarcação também recorta.
- A IA do fazendeiro checa pertencimento à zona e não trabalha nos vazios; mapa e preview desenham a união, sem costuras internas.
- A checagem de pertencimento roda a cada 10 ticks e é dominada pelos `getBlockState`/chunks; o recorte só roda no clique. Continua leve no uso normal.
- Zona removida deixa o posto **sem área de trabalho** (o fazendeiro pausa com `need_zone`); a área máxima não é fallback.

### Corrigido

- **Postos órfãos** (registro no JSON com o bloco quebrado) não contam mais para conflito de área, recorte do máximo nem mapa; só postos com o bloco presente bloqueiam. Isso elimina os buracos/quadradinhos vazios deixados por registros antigos.
- `HousingReport` agora expõe os limites internos e a lista canônica de camas; cama de casal não infla mais a contagem de camas.
- O aviso **"Outro posto"** na lista de empregados do Posto não fica mais escondido atrás do botão de olho; o nome do trabalhador é truncado quando necessário.

### Pendente

- Política de conflito para empregos de **área livre** (sem zona); o único emprego atual é o fazendeiro. A lógica antiga de área efetiva continua disponível para essa análise.

## [0.1.0-alpha.6] — 2026-10-24

Fazendeiro automatizado de ponta a ponta: área de trabalho obrigatória, colheita/replantio/plantio, água,
bone meal, composteira, coleta de drops e destaques no mundo. O Posto ganhou ajuda, limite máximo visível
e o botão **Usar área máxima**.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Destaque no mundo de **entidades**: o wireframe de partículas acompanha o bounding box do trabalhador em movimento por 10 s, com indicador de direção fora da tela.
- Botão de olho por trabalhador na aba **Trabalho** do Posto e no **Perfil**: fecha a tela, destaca o trabalhador e avisa se ele não estiver carregado por perto.
- `WorldHighlightManager` agora atende blocos e entidades e aceita o ícone do alvo para o HUD.
- `WorkerStorage`: inserção só em slot vazio/igual e extração só do insumo pedido, respeitando o papel do baú (`mixed/output/input/tools`).
- `FarmerStorage`: mantém 32 sementes e até 16 bone meal na mochila, deposita só produto/excedente e retira sementes antes do trabalho.
- Comandos `worker storage deposit|withdraw <trabalhador>` (base para a IA e teste manual).
- IA do fazendeiro MVP (`FarmerBrain`): valida enxada/sementes (puxa do baú), procura cultura madura na zona/raio, anda, colhe, replanta, guarda os drops e deposita no baú quando enche. Pausa com aviso quando falta enxada/semente/baú ou o baú enche.
- IA coleta itens dropados no raio máximo do posto (sementes, produto e bone meal) e os usa/guarda.
- Água validada por proximidade (4 blocos/9x9, mesmo Y ou 1 acima, fluid tag/waterlogged, chuva) com um único predicado. `farmer.waterPolicy`: `prefer` (padrão) planta mesmo em terra seca e avisa 1x/min; `require` só planta/ara com água, avisa e espera (auto-retoma ao irrigar); `ignore` desliga.
- Bone meal: com `useBoneMeal`, aplica em culturas em crescimento quando há bone meal na mochila.
- Composteira: com `useComposter`, o excedente de sementes acima de `seedSurplusForCompost` vai para a composteira mais próxima (raio do posto); ele recolhe a farinha de osso e o depósito mantém esse estoque de sementes.
- IA agora **planta farmland vazio** com a semente disponível e **reara o chão pisoteado** (dirt/grass vira farmland e é replantado).
- A IA prefere um ponto de apoio **fora do farmland** para colher, reduzindo o pisoteio.
- O aviso de pausa no chat inclui o **nome do trabalhador**.

### Alterado

- O contador do destaque ficou genérico (`Destacado: Ns`).

### Corrigido

- O botão de olho das linhas do Posto agora fica centralizado verticalmente na linha.
- O trabalhador não fica mais com farmland vazio sem semente após depositar: a mochila mantém 32 sementes e a IA replanta/planta os espaços vazios.
- O fazendeiro agora exige **zona desenhada** como área de trabalho; o raio do posto é apenas o limite máximo. Sem zona ele pausa com o aviso “Desenhe a área de trabalho no Posto”.
- Botão **Usar área máxima** na aba Config define a zona como o quadrado máximo permitido de uma vez.
- Botão **?** na tela do Posto com uma ajuda rápida (área de trabalho, baús, mochila, olho e comandos); rótulos, aviso sem zona e texto abaixo do mapa atualizados (legenda separa área de trabalho em âmbar do limite tracejado).
- No mapa, a camada de superfície usa `MOTION_BLOCKING`: culturas/plantas são ignoradas e o farmland aparece marrom, distinto do gramado.
- O tracejado do limite máximo do posto no mapa ficou mais visível (azul com alpha alto).
- O fazendeiro **volta ao trabalho sozinho** quando a enxada/sementes/baú são resolvidos e **equipa automaticamente** uma enxada válida da mochila; a pausa manual continua exigindo “Retomar”.
- A composteira pronta é esvaziada mesmo com poucas sementes (antes ele enchia até 100% e parava de usar).

## [0.1.0-alpha.5] — 2026-10-24

Mochila do trabalhador com transferência estilo baú, quantidade e validação de ferramenta; comandos
divididos em módulos. Primeira arte pixel art própria do Posto, Planta de Casa, Estaca e Contrato.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Mochila do trabalhador: 9 slots gerais + 1 de ferramenta persistidos na entidade.
- Tela **Mochila** com inventário do jogador à esquerda e mochila à direita; clique em um slot e depois no outro (ou arraste) para mover/trocar, com tooltip e barra de durabilidade.
- Popup de quantidade para pilhas com mais de 1: campo editável com cursor piscando, **Tudo**, **Confirmar** e **Cancelar**.
- Validação de ferramenta por cargo (`JobEquipment`): o fazendeiro aceita apenas enxada; o slot mostra o tipo aceito e recusa o resto na tela e no servidor.
- Base para layouts por cargo no futuro (o payload leva o `jobId`): segurança com armadura, minerador com picareta etc.
- Comandos `worker open` e `worker inv open|take|give|swap|deposit|withdraw <trabalhador> ...`, equivalentes às ações da tela.
- **Voltar** na mochila reabre a tela do Empregado; o X fecha.
- Texturas pixel art próprias: Posto de Trabalho (topo/lateral/frente/baixo), Planta de Casa, Estaca de Demarcação e Contrato de Trabalhador.

### Alterado

- Comandos divididos em módulos (`WorkerCommands`, `StationCommands`, `HomeCommands`, `AdminCommands`, `UiCommands`, `LanguageCommands`) com `CommandSupport` para helpers; `LaborCraftCommands` ficou só com o registro e os IDs usados pela UI.

### Corrigido

- Campo de quantidade não aceitava dígitos no 26.3: as teclas são scancodes SDL e os dígitos não formam um intervalo contínuo (0=39, 1..9=30..38); o mapeamento agora é explícito, incluindo o teclado numérico.
- Aviso de ferramenta inválida/slot cheio ficava atrás do blur da tela; agora aparece numa faixa dentro da própria UI.
- Texto da dica da mochila era cortado; a caixa calcula a altura pelo texto quebrado e o painel cresceu.

## [0.1.0-alpha.3] — 2026-10-24

Armazenamento do Posto (aba **Baús**) e melhoria do mapa com camadas de superfície/subsolo.
Mochila do trabalhador e protocolo de saque/depósito continuam para a próxima etapa.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Mapa do Posto com camadas **Auto / Superfície / Subsolo** e floor-scan de cavernas (pivô no Y do jogador).
- Aba **Baús** no Posto: vínculo de containers por distância (8 blocos, até 3), papéis `mixed/output/input/tools`, detecção automática e desvinculo.
- Comandos `/laborcraft station storage link|unlink|role|scan|list`; o antigo `outputChest` é migrado para um vínculo `mixed`.
- Botão de olho por baú: fecha a tela e desenha um wireframe de partículas por 10 s, com contador acima da hotbar.
- Indicador de alvo fora da tela no HUD (estilo FPS), com ícone, direção e distância.
- A seleção do baú e a aba são preservadas quando o servidor reenvia a tela.
- Desenho do armazenamento no [doc 14](docs/14-ARMAZENAMENTO.md): baú misturado, mochila do trabalhador e protocolo de saque/depósito (próximas etapas).

## [0.1.0-alpha.2] — 2026-10-23

Zona de trabalho desenhada no mapa, prioridade e recorte entre postos, e visualização da área no mundo.
Casa e IA do fazendeiro continuam pausadas.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Mapa interativo da área no Posto (`LcAreaMap`): recorte do mundo visto de cima com relevo, grade de chunk e zoom 1–4 px/bloco; arraste define o retângulo, com limite de 32x32 e status ao vivo.
- Aba Config do Posto em modo leitura: área efetiva em âmbar, outros postos em azul, raio livre tracejado e invasão da seleção em vermelho apenas nas células em conflito.
- Botões **Desenhar área**, **Confirmar**, **Cancelar**, **Visualizar** e **Limpar zona**; o botão compacto **Visualizar área** reaproveita a zona já confirmada.
- Pré-visualização no mundo por 10 s com partículas na borda efetiva e contador acima da hotbar.
- `StationAreas`: área efetiva por célula, prioridade pelo Posto mais antigo, recorte de borda e exclusão do bloco de postos vizinhos.
- Item **Estaca de Demarcação** (`laborcraft:survey_stake`): canto A, canto B e agachar + clique no Posto, como alternativa acessível ao mapa.
- Comandos `station zone set` e `station zone clear`; validação no servidor (dono, 32x32, distância, sobreposição, bloco de outro posto).
- `WorkstationBlockItem` recusa colocar o Posto dentro da área efetiva de outro Posto ou onde o centro já pertence a ele.
- Payload `open_station` agora envia área efetiva, áreas vizinhas e postos próximos para o mapa.

### Corrigido

- `/laborcraft station zone clear` não existia: o nó `clear` estava aninhado em `zone set` por um parêntese errado.
- O botão **Visualizar** sobrepunha o **Fechar** na aba Config; a ação foi movida para o topo da caixa.

### Alterado

- O mapa é gerado em `DynamicTexture`/`NativeImage` (um blit por frame) e redesenhado a cada 5 s ou ao mudar o zoom.
- O mapa é cliente-only e usa só os chunks carregados; colunas fora deles aparecem como `?`.

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
- **Posto de Trabalho com estado**: `StationRegistry` salvo em `<mundo>/data/laborcraft/stations.json`, com dono, trabalhador vinculado, modo de área e zona. O posto registra o dono ao ser colocado e um posto sem dono é reivindicado no primeiro uso.
- Interagir com o posto abre a **tela do Posto** (payload `open_station`): aba Trabalho com a lista de empregados, a vaga do posto e as ações vincular/desvincular/chamar/retomar; aba Config com o modo de área (leitura).
- Comandos `/laborcraft station bind|unbind|recall|pause|resume|open|info`.
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
