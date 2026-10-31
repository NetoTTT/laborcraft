# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento semântico a partir da primeira release pública.

## [0.2.0-alpha.3] - 2026-10-31

Núcleo de IA por emprego e correção das portas.

### Adicionado
- `WorkerBrain`, `AbstractWorkerBrain` e `JobBrains`: fluxo comum (pausa, zona, ferramenta, durabilidade, cooldown) separado da lógica de produção.
- `FarmerBrain` refatorado para o novo contrato, sem duplicar o fluxo comum.
- Validador de casa dividido em `HousingScan` (medição) e `HousingRule`/`HousingRules` (regras plugáveis). `HousingReport.rules()` expõe o resultado de cada regra.
- `EnclosureScanner` e `/laborcraft admin pen scan`: valida cercado fechado com pelo menos um portão e calcula a área interna.
- Emprego `laborcraft:herder` (Pastoreiro) registrado como protótipo, ainda sem lógica de produção.
- `/laborcraft admin set max-workers <n>` para ajustar o limite de trabalhadores por jogador.
- Configurações por emprego em `LaborCraftConfig.jobs`/`job(jobId)`; o campo `farmer` vira alias compatível.
- `EnclosureDescriptor` no Posto (seed, bounds, spans e portões) com `/laborcraft station enclosure set/clear`, separado das zonas retangulares.
- Mapa de Config do pastoreiro em modo clique: o seed vem do bloco clicado, com limite de scan configurável (`enclosure.maxScanRadius`/`maxArea`).
- `/laborcraft station job set <pos> <job>` para especializar o Posto.

### Corrigido
- O NPC agora consegue atravessar portas fechadas: o pathfinding foi habilitado com `setCanOpenDoors(true)`, então ele encontra, abre e fecha portas para entrar/sair da casa e chegar à cama.
- Fence gates: mixin no `WalkNodeEvaluator` + `OpenFenceGateGoal` para o trabalhador abrir/fechar portões no caminho.

## [0.2.0-alpha.2] - 2026-10-31

Temas escolhidos pelo jogador e balões de fala no framework.

### Adicionado
- Seletor com **seis temas** no Perfil: Labor, Vidro, Chapado, Claro, Floresta e Ametista; a escolha fica em `config/laborcraft-ui.json`.
- Tema **Vidro** com painéis translúcidos e o **Claro** com fundo de tela transparente e painel levemente translúcido.
- Balões de fala do trabalhador migrados para o `quill-ui-extras` (`QuillBubbleHud`), com projeção, suavização, fade, oclusão e sombra suave.

### Alterado
- HUDs e o grid de itens seguem o tema selecionado.
- Cores de UI declaradas com `QuillColors.rgb/argb`, sem literais hexadecimais.
- Contraste dos botões primários e de perigo corrigido nos temas escuros.

## [0.2.0-alpha.1] - 2026-10-30

Interface inteira reconstruída sobre o **Quill UI 0.3**. As três telas que ainda usavam layout
absoluto foram refeitas do zero (Posto, Mochila e Admin) e todo o código visual legado foi removido.

### Adicionado
- **Posto** novo: abas Trabalho/Config/Baús medidas, lista de empregados com Pagar e destacar,
  card da vaga, vínculo, recall/resume e chip com o saldo do jogador no header.
- **Mapa embutido** na aba Config do Posto: terreno, zonas salvas, zonas de outros postos, limite do
  posto, seleção ao vivo, camadas e união de áreas vizinhas sem linhas internas.
- **Trocar** a função do baú direto na linha, além de Detectar, Desvincular e destacar.
- **Mochila** nova: grid do jogador, hotbar, mochila 3x3 e slot de ferramenta em um board medido;
  overlay de quantidade em Quill.
- **Admin** novo com as seis abas medidas: Salário, Cobrança, Expediente, Área/Casa, Mercado e Sistema.
- **Quill UI 0.3.0-alpha.4** (`quill-ui` + `quill-ui-extras`) no cliente: `QuillChip`,
  `headerAccessory`, ícones de botão e mapa com zonas unidas.

### Alterado
- Todas as telas usam `QuillScreen`, `setContent`, `QuillNodes` e `QuillListView`.
- Painéis encolhem para caber na janela e o conteúdo rola em vez de cortar.
- O cliente precisa de `quill-ui-0.3.0-alpha.4.jar` e `quill-ui-extras-0.3.0-alpha.4.jar`; servidores
  dedicados não precisam de nenhum dos dois.

### Removido
- Telas legadas `LaborCraftAdminScreen`, `LaborCraftStationScreen` e `LaborCraftWorkerInventoryScreen`.
- Widgets antigos `LcButton`, `LcScrollList`, `LcAreaMap` e `LcJobOption`.

### Corrigido
- Abas, cards e grids não são mais esmagados ou cortados em janelas menores que o painel original.

## [0.1.0-alpha.14] - 2026-10-25

Primeiro adaptador de moeda externa: **MineLab `plims`**, com fallback para o Ledger. A base visual
também foi extraída para o **Quill UI**, um mod cliente separado.

### Adicionado
- `minelab:plims` como `CurrencyProvider` isolado, sem importar nem alterar classes do MineLab.
- Leitura/escrita do objective `plims` via scoreboard; jogadores offline resolvidos pelo cache de perfis.
- Fallback configurável para o Ledger quando o objective/jogador não está disponível.
- Configuração JSON: `compatibility.mineLabObjective`, `mineLabFormat` e `mineLabFallbackToLedger`.
- O provedor aparece no seletor de provedor do painel Admin.
- **Quill UI 0.2.0-alpha.2** (`com.quillui`): framework cliente separado com tokens, layout, botões, tabs,
  scroll list, screen e `QuillActionSink`; documentação em inglês no projeto próprio.
- Aliases `LcTheme`, `LcRender`, `LcTooltips`, `LcButton` e `LcScrollList` passam a usar Quill UI.

### Alterado
- A base visual do painel Admin, header, cards e tab strip foi consolidada em tokens/componentes.
- O cliente agora precisa de `quill-ui-0.2.0-alpha.2.jar`; servidores dedicados não precisam do jar.
- `fabric.mod.json` apenas sugere `quillui` (sem dependência dura) para preservar o servidor.

## [0.1.0-alpha.13] ÔÇö 2026-10-25

Painel admin ganhou as abas **Expediente**, **├ürea/Casa** e **Mercado**, com configura├º├úo live via comando gen├®rico. Compatibilidade MineLab documentada como prioridade alta.

### Adicionado
- Aba **Expediente**: modo, in├¡cio/fim, chuva, pausa fora do expediente e checagem de ├│cio.
- Aba **├ürea/Casa**: forma/raio vertical/raio m├íximo, b├┤nus, preview e valida├º├Áes de casa, incluindo **exigir ilumina├º├úo** (padr├úo **ligado**) com verifica├º├úo de tochas/lanternas/blocos emissores.
- Aba **Mercado**: ofertas por rota├º├úo, dias por rota├º├úo e limite base de trabalhadores.
- Comando gen├®rico `/laborcraft admin config <campo> <valor>` para os campos dessas abas.
- [Doc 17](docs/17-COMPATIBILIDADE-MINELAB.md): integra├º├úo futura com o dinheiro `plims` do MineLab via `CurrencyProvider` isolado, sem alterar o mod deles.

## [0.1.0-alpha.12] ÔÇö 2026-10-25

Bal├Áes de fala acima dos trabalhadores e chat de trabalhador com identidade visual pr├│pria.

### Adicionado
- Bal├úo de fala acima da cabe├ºa enquanto o trabalhador tem um aviso importante ativo (falta de ferramenta, sementes, zona, sal├írio atrasado etc.); o texto permanece enquanto o bloqueio existir e desaparece ao voltar ao trabalho.
- Pensamentos ocasionais para trabalhadores ativos (a cada 60ÔÇô150 s), sem poluir o chat; avisos de bloqueio continuam sempre vis├¡veis.
- Bal├úo com raio m├íximo de **10 blocos**, fade suave, flutua├º├úo e posi├º├úo interpolada/suavizada para n├úo tremer durante o movimento.
- Oculta├º├úo por paredes **ligada por padr├úo**; o admin pode liberar para todos os jogadores no painel (Sistema) ou com `admin set bubble-walls true/false`.
- O aviso ├® sincronizado pela entidade (`SynchedEntityData`) e persiste no save.
- Corrigido: o descanso limpava o aviso de bloqueio de um trabalhador `PAUSED`, fazendo o bal├úo sumir (ex.: falta de enxada).
- Chat de trabalhador no formato `[Empregado] Nome: mensagem`, com o nome na cor da raridade.
- Mensagens de cobran├ºa tamb├®m usam o nome colorido do trabalhador.

## [0.1.0-alpha.11] ÔÇö 2026-10-25

Pagamento por trabalhador tamb├®m no **Posto** e no **Perfil**, com filtro compartilhado das listas.

### Corrigido
- Posto com v├¡nculo ├│rf├úo de um trabalhador que rompeu/foi dispensado n├úo bloqueia mais a contrata├º├úo: o v├¡nculo morto pode ser substitu├¡do.
- Dispensar, remover ou romper contrato agora desvincula o trabalhador do Posto.

### Adicionado
- Bot├úo **Pagar** em cada linha de trabalhador com d├¡vida no Posto e no Perfil.
- Comandos internos `worker payprofile` e `worker paystation`; ap├│s pagar, a tela correspondente ├® reaberta j├í atualizada.
- Filtro de lista: **Todos ÔåÆ D├¡vida ÔåÆ Greve ÔåÆ Pausa**, mantendo o filtro ao pagar e recarregar a tela.
- Mensagem de lista vazia para filtros sem resultado.

## [0.1.0-alpha.10] ÔÇö 2026-10-25

Painel admin inicial: estado server-authoritative, abas **Sal├írio**, **Cobran├ºa** e **Sistema**, edi├º├úo de valores por prompt e atualiza├º├úo autom├ítica do painel ap├│s cada comando.

### Adicionado
- `OpenAdminPayload` agora carrega o snapshot da economia (base, per├¡odo, pre├ºo, gra├ºa, d├¡vida, multiplicadores e toggles).
- Aba **Sal├írio**: base di├íria, per├¡odo, pre├ºo, m├¡nimo, teto de boost, toggles da tabela e multiplicadores de rank/raridade.
- Aba **Cobran├ºa**: ativa, intervalo de checagem, gra├ºa, dispensa, humor, d├¡vida, notifica├º├Áes e expediente.
- A├º├Áes: aplicar tabela aos existentes, for├ºar cobran├ºa e atualizar mercado; abas e valores permanecem ap├│s o refresh.
- `LcValuePrompt` ganhou entrada decimal (aceita v├¡rgula e normaliza para ponto).

### Alterado
- `dismiss-after` agora conta faltas **depois da greve**, e n├úo faltas totais; o dono recebe um aviso final antes do rompimento.
- O rompimento autom├ítico devolve o Contrato de Trabalhador ao dono em vez de apagar a pessoa; se o dono estiver offline, a dispensa ├® adiada.

### Pendente
- Abas Expediente/├ürea/Casa/Mercado completas e auditoria das mudan├ºas; testar o painel em jogo.

## [0.1.0-alpha.9] ÔÇö 2026-10-25

Base de cobran├ºa/contrato: sal├írio por per├¡odo, d├¡vida, greve por atraso e configura├º├úo live por comandos administrativos. A UI do painel admin continua pendente (doc 15).

### Adicionado
- Tabela salarial configur├ível (base di├íria, multiplicadores de rank/raridade, boosts, teto e m├¡nimo) usada na gera├º├úo de ofertas; modo antigo dispon├¡vel por toggle.
- Cobran├ºa persistida por trabalhador (sal├írio/per├¡odo, pr├│ximo vencimento, d├¡vida e faltas) com auto-pagamento pelo provedor de moeda, d├¡vida/grace, greve e notifica├º├Áes.
- `/laborcraft worker pay <trabalhador>` para quitar a d├¡vida e retomar o contrato.
- Altera├º├Áes de sal├írio, rank, raridade ou pre├ºo regeneram as ofertas do mercado automaticamente; `apply-salary` atualiza os contratos carregados.
- Bot├úo **Pagar** no resumo do Empregado quando h├í d├¡vida; paga, retira da greve e atualiza a tela.
- Configura├º├úo live por `/laborcraft billing set <campo> <valor>`, `/laborcraft billing rank|rarity <id> <multiplicador>` e `/laborcraft billing apply-salary` (recalcula os trabalhadores existentes); `/laborcraft billing force` for├ºa uma cobran├ºa para testar. Campos: enabled, check-ticks, use-schedule, pause-off-duty, notify, debt, grace, dismiss-after, mood-penalty, use-table, minimum-daily, use-rank, use-rarity, use-boost, boost-cap, base-wage, price, period-days.
- Estado **Em greve** e tarefa de sal├írio atrasado; a linha de sal├írio/d├¡vida aparece no Resumo do Empregado.

### Pendente
- UI Admin de verdade (doc 15), testes de unidade do ciclo e a pol├¡tica de contratos antigos na UI.

## [0.1.0-alpha.8] ÔÇö 2026-10-25

Corre├º├úo s├│ de metadados: autor **NetoTTT** e links do GitHub (homepage/sources/issues) no
`fabric.mod.json`; descri├º├úo do mod atualizada. Nenhuma mudan├ºa de gameplay em rela├º├úo ao alpha.7.

## [0.1.0-alpha.7] ÔÇö 2026-10-25

Base da casa do trabalhador: registrar o c├┤modo com nome, atribuir uma cama e aumentar o limite de
trabalhadores. O Empregado ganhou abas e a casa agora tem modal de registro e lista de casas.
O conflito de ├íreas passou ao modelo **zona-only**: a ├írea m├íxima ├® s├│ um limite e s├│ a zona
desenhada ├® exclusiva. O trabalhador agora respeita o **expediente** e descansa na cama da casa
(pose de dormir vanilla); se a casa n├úo estiver carregada, espera no posto.

### Adicionado

- Registro persistente de casas (`HomeRegistry`, `<mundo>/data/laborcraft/homes.json`): **nome**, dono, cama-├óncora, limites internos, capacidade, camas e moradores.
- **Modal de registro**: agachar + clique direito na cama com a m├úo vazia mostra os dados do c├┤modo (interno, portas, camas, vagas) e pede um nome; confirmar registra. `/laborcraft home set [pos] [nome]` continua dispon├¡vel.
- **Gerenciar casa**: no modal de uma casa j├í sua, **Renomear** (`/laborcraft home rename [pos] <nome>`) e **Desvincular casa** (dois cliques, `/laborcraft home remove [pos]`); desvincular deixa o c├┤modo livre para outro jogador reivindicar.
- Abas **Resumo / Emprego / Casa** na tela Empregado: a aba Emprego lista os cargos (pronta para novos empregos) e a aba Casa lista as casas com nome, camas livres e validade, com **Atribuir** por casa e **Liberar casa**.
- ├éncora normalizada para o **p├® da cama**, ent├úo uma cama de casal conta como uma vaga (n├úo duas).
- Slots da casa: `limite = worker.maxWorkersPerPlayer + camas v├ílidas`, usado na compra, no contrato, na contrata├º├úo e no perfil.
- Atribui├º├úo de casa ao trabalhador por `/laborcraft home assign <trabalhador> [cama]` e pela aba Casa; `/laborcraft home unassign` remove o v├¡nculo. Uma cama por trabalhador; atribuir em outra casa move o trabalhador.
- `/laborcraft home list`, `home info`, `home remove` e `home revalidate` (gamemaster); o diagn├│stico `home scan|measure` continua.
- Revalida├º├úo autom├ítica a cada `housing.recheckSeconds`; casa quebrada suspende os slots, avisa o dono e volta quando o c├┤modo for consertado.
- Dispensar ou remover um trabalhador libera automaticamente a cama.
- **Expediente/descanso MVP**: fora do hor├írio de trabalho o fazendeiro para a IA, anda at├® a cama da casa, deita com `startSleeping` (pose vanilla, validada em jogo; visual do alde├úo a polir) e volta ao trabalho quando o expediente abre. Sem casa/cama ou com o chunk da casa descarregado, espera no posto; se a casa existe mas ├® inalcan├º├ível, tamb├®m espera no posto.
- Novo estado **Fora do expediente** com tarefas: indo para casa, dormindo, sem casa, casa longe/inalcan├º├ível. O trabalhador abre portas com `OpenDoorGoal`.
- `WorkSchedule` preparado para perfis por cargo; por enquanto o fazendeiro segue o `schedule` global (padr├úo `daylight`, aceita `custom`/janelas que viram o dia).

### Alterado

- **Conflito entre postos (zona-only)**: a ├írea m├íxima pode sobrepor a de outro posto; s├│ a **zona desenhada** ├® exclusiva. Um posto pode ser colocado/claimado dentro da ├írea m├íxima de outro, mas n├úo dentro da zona; uma zona n├úo pode cruzar outra zona nem conter outro posto.
- O mapa n├úo recorta mais a ├írea m├íxima do posto selecionado; os outros postos aparecem pelas suas **zonas** (azul), n├úo pela ├írea m├íxima, e o limite tracejado continua inteiro.
- **"Usar ├írea m├íxima" e o desenho manual recortam automaticamente** a ├írea contra zonas/blocos vizinhos, gravando v├írios ret├óngulos no mesmo posto (at├® 32). Se n├úo sobrar ├írea ou o recorte passar do cap, recusa. As c├®lulas que ser├úo cortadas aparecem em vermelho, mas o **Confirmar** fica habilitado; a Estaca de Demarca├º├úo tamb├®m recorta.
- A IA do fazendeiro checa pertencimento ├á zona e n├úo trabalha nos vazios; mapa e preview desenham a uni├úo, sem costuras internas.
- A checagem de pertencimento roda a cada 10 ticks e ├® dominada pelos `getBlockState`/chunks; o recorte s├│ roda no clique. Continua leve no uso normal.
- Zona removida deixa o posto **sem ├írea de trabalho** (o fazendeiro pausa com `need_zone`); a ├írea m├íxima n├úo ├® fallback.

### Corrigido

- **Postos ├│rf├úos** (registro no JSON com o bloco quebrado) n├úo contam mais para conflito de ├írea, recorte do m├íximo nem mapa; s├│ postos com o bloco presente bloqueiam. Isso elimina os buracos/quadradinhos vazios deixados por registros antigos.
- `HousingReport` agora exp├Áe os limites internos e a lista can├┤nica de camas; cama de casal n├úo infla mais a contagem de camas.
- O aviso **"Outro posto"** na lista de empregados do Posto n├úo fica mais escondido atr├ís do bot├úo de olho; o nome do trabalhador ├® truncado quando necess├írio.

### Pendente

- Pol├¡tica de conflito para empregos de **├írea livre** (sem zona); o ├║nico emprego atual ├® o fazendeiro. A l├│gica antiga de ├írea efetiva continua dispon├¡vel para essa an├ílise.
- **[ALTA] Painel Admin in-game (live config)**: editar economia/sal├írio, expediente, ├írea/casa e sistema sem reboot/JSON; requer o motor de cobran├ºa e a pol├¡tica para contratos existentes ÔÇö ver [doc 15](docs/15-PAINEL-ADMIN.md).

## [0.1.0-alpha.6] ÔÇö 2026-10-24

Fazendeiro automatizado de ponta a ponta: ├írea de trabalho obrigat├│ria, colheita/replantio/plantio, ├ígua,
bone meal, composteira, coleta de drops e destaques no mundo. O Posto ganhou ajuda, limite m├íximo vis├¡vel
e o bot├úo **Usar ├írea m├íxima**.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Destaque no mundo de **entidades**: o wireframe de part├¡culas acompanha o bounding box do trabalhador em movimento por 10 s, com indicador de dire├º├úo fora da tela.
- Bot├úo de olho por trabalhador na aba **Trabalho** do Posto e no **Perfil**: fecha a tela, destaca o trabalhador e avisa se ele n├úo estiver carregado por perto.
- `WorldHighlightManager` agora atende blocos e entidades e aceita o ├¡cone do alvo para o HUD.
- `WorkerStorage`: inser├º├úo s├│ em slot vazio/igual e extra├º├úo s├│ do insumo pedido, respeitando o papel do ba├║ (`mixed/output/input/tools`).
- `FarmerStorage`: mant├®m 32 sementes e at├® 16 bone meal na mochila, deposita s├│ produto/excedente e retira sementes antes do trabalho.
- Comandos `worker storage deposit|withdraw <trabalhador>` (base para a IA e teste manual).
- IA do fazendeiro MVP (`FarmerBrain`): valida enxada/sementes (puxa do ba├║), procura cultura madura na zona/raio, anda, colhe, replanta, guarda os drops e deposita no ba├║ quando enche. Pausa com aviso quando falta enxada/semente/ba├║ ou o ba├║ enche.
- IA coleta itens dropados no raio m├íximo do posto (sementes, produto e bone meal) e os usa/guarda.
- ├ügua validada por proximidade (4 blocos/9x9, mesmo Y ou 1 acima, fluid tag/waterlogged, chuva) com um ├║nico predicado. `farmer.waterPolicy`: `prefer` (padr├úo) planta mesmo em terra seca e avisa 1x/min; `require` s├│ planta/ara com ├ígua, avisa e espera (auto-retoma ao irrigar); `ignore` desliga.
- Bone meal: com `useBoneMeal`, aplica em culturas em crescimento quando h├í bone meal na mochila.
- Composteira: com `useComposter`, o excedente de sementes acima de `seedSurplusForCompost` vai para a composteira mais pr├│xima (raio do posto); ele recolhe a farinha de osso e o dep├│sito mant├®m esse estoque de sementes.
- IA agora **planta farmland vazio** com a semente dispon├¡vel e **reara o ch├úo pisoteado** (dirt/grass vira farmland e ├® replantado).
- A IA prefere um ponto de apoio **fora do farmland** para colher, reduzindo o pisoteio.
- O aviso de pausa no chat inclui o **nome do trabalhador**.

### Alterado

- O contador do destaque ficou gen├®rico (`Destacado: Ns`).

### Corrigido

- O bot├úo de olho das linhas do Posto agora fica centralizado verticalmente na linha.
- O trabalhador n├úo fica mais com farmland vazio sem semente ap├│s depositar: a mochila mant├®m 32 sementes e a IA replanta/planta os espa├ºos vazios.
- O fazendeiro agora exige **zona desenhada** como ├írea de trabalho; o raio do posto ├® apenas o limite m├íximo. Sem zona ele pausa com o aviso ÔÇ£Desenhe a ├írea de trabalho no PostoÔÇØ.
- Bot├úo **Usar ├írea m├íxima** na aba Config define a zona como o quadrado m├íximo permitido de uma vez.
- Bot├úo **?** na tela do Posto com uma ajuda r├ípida (├írea de trabalho, ba├║s, mochila, olho e comandos); r├│tulos, aviso sem zona e texto abaixo do mapa atualizados (legenda separa ├írea de trabalho em ├ómbar do limite tracejado).
- No mapa, a camada de superf├¡cie usa `MOTION_BLOCKING`: culturas/plantas s├úo ignoradas e o farmland aparece marrom, distinto do gramado.
- O tracejado do limite m├íximo do posto no mapa ficou mais vis├¡vel (azul com alpha alto).
- O fazendeiro **volta ao trabalho sozinho** quando a enxada/sementes/ba├║ s├úo resolvidos e **equipa automaticamente** uma enxada v├ílida da mochila; a pausa manual continua exigindo ÔÇ£RetomarÔÇØ.
- A composteira pronta ├® esvaziada mesmo com poucas sementes (antes ele enchia at├® 100% e parava de usar).

## [0.1.0-alpha.5] ÔÇö 2026-10-24

Mochila do trabalhador com transfer├¬ncia estilo ba├║, quantidade e valida├º├úo de ferramenta; comandos
divididos em m├│dulos. Primeira arte pixel art pr├│pria do Posto, Planta de Casa, Estaca e Contrato.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Mochila do trabalhador: 9 slots gerais + 1 de ferramenta persistidos na entidade.
- Tela **Mochila** com invent├írio do jogador ├á esquerda e mochila ├á direita; clique em um slot e depois no outro (ou arraste) para mover/trocar, com tooltip e barra de durabilidade.
- Popup de quantidade para pilhas com mais de 1: campo edit├ível com cursor piscando, **Tudo**, **Confirmar** e **Cancelar**.
- Valida├º├úo de ferramenta por cargo (`JobEquipment`): o fazendeiro aceita apenas enxada; o slot mostra o tipo aceito e recusa o resto na tela e no servidor.
- Base para layouts por cargo no futuro (o payload leva o `jobId`): seguran├ºa com armadura, minerador com picareta etc.
- Comandos `worker open` e `worker inv open|take|give|swap|deposit|withdraw <trabalhador> ...`, equivalentes ├ás a├º├Áes da tela.
- **Voltar** na mochila reabre a tela do Empregado; o X fecha.
- Texturas pixel art pr├│prias: Posto de Trabalho (topo/lateral/frente/baixo), Planta de Casa, Estaca de Demarca├º├úo e Contrato de Trabalhador.

### Alterado

- Comandos divididos em m├│dulos (`WorkerCommands`, `StationCommands`, `HomeCommands`, `AdminCommands`, `UiCommands`, `LanguageCommands`) com `CommandSupport` para helpers; `LaborCraftCommands` ficou s├│ com o registro e os IDs usados pela UI.

### Corrigido

- Campo de quantidade n├úo aceitava d├¡gitos no 26.3: as teclas s├úo scancodes SDL e os d├¡gitos n├úo formam um intervalo cont├¡nuo (0=39, 1..9=30..38); o mapeamento agora ├® expl├¡cito, incluindo o teclado num├®rico.
- Aviso de ferramenta inv├ílida/slot cheio ficava atr├ís do blur da tela; agora aparece numa faixa dentro da pr├│pria UI.
- Texto da dica da mochila era cortado; a caixa calcula a altura pelo texto quebrado e o painel cresceu.

## [0.1.0-alpha.3] ÔÇö 2026-10-24

Armazenamento do Posto (aba **Ba├║s**) e melhoria do mapa com camadas de superf├¡cie/subsolo.
Mochila do trabalhador e protocolo de saque/dep├│sito continuam para a pr├│xima etapa.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Mapa do Posto com camadas **Auto / Superf├¡cie / Subsolo** e floor-scan de cavernas (piv├┤ no Y do jogador).
- Aba **Ba├║s** no Posto: v├¡nculo de containers por dist├óncia (8 blocos, at├® 3), pap├®is `mixed/output/input/tools`, detec├º├úo autom├ítica e desvinculo.
- Comandos `/laborcraft station storage link|unlink|role|scan|list`; o antigo `outputChest` ├® migrado para um v├¡nculo `mixed`.
- Bot├úo de olho por ba├║: fecha a tela e desenha um wireframe de part├¡culas por 10 s, com contador acima da hotbar.
- Indicador de alvo fora da tela no HUD (estilo FPS), com ├¡cone, dire├º├úo e dist├óncia.
- A sele├º├úo do ba├║ e a aba s├úo preservadas quando o servidor reenvia a tela.
- Desenho do armazenamento no [doc 14](docs/14-ARMAZENAMENTO.md): ba├║ misturado, mochila do trabalhador e protocolo de saque/dep├│sito (pr├│ximas etapas).

## [0.1.0-alpha.2] ÔÇö 2026-10-23

Zona de trabalho desenhada no mapa, prioridade e recorte entre postos, e visualiza├º├úo da ├írea no mundo.
Casa e IA do fazendeiro continuam pausadas.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Mapa interativo da ├írea no Posto (`LcAreaMap`): recorte do mundo visto de cima com relevo, grade de chunk e zoom 1ÔÇô4 px/bloco; arraste define o ret├óngulo, com limite de 32x32 e status ao vivo.
- Aba Config do Posto em modo leitura: ├írea efetiva em ├ómbar, outros postos em azul, raio livre tracejado e invas├úo da sele├º├úo em vermelho apenas nas c├®lulas em conflito.
- Bot├Áes **Desenhar ├írea**, **Confirmar**, **Cancelar**, **Visualizar** e **Limpar zona**; o bot├úo compacto **Visualizar ├írea** reaproveita a zona j├í confirmada.
- Pr├®-visualiza├º├úo no mundo por 10 s com part├¡culas na borda efetiva e contador acima da hotbar.
- `StationAreas`: ├írea efetiva por c├®lula, prioridade pelo Posto mais antigo, recorte de borda e exclus├úo do bloco de postos vizinhos.
- Item **Estaca de Demarca├º├úo** (`laborcraft:survey_stake`): canto A, canto B e agachar + clique no Posto, como alternativa acess├¡vel ao mapa.
- Comandos `station zone set` e `station zone clear`; valida├º├úo no servidor (dono, 32x32, dist├óncia, sobreposi├º├úo, bloco de outro posto).
- `WorkstationBlockItem` recusa colocar o Posto dentro da ├írea efetiva de outro Posto ou onde o centro j├í pertence a ele.
- Payload `open_station` agora envia ├írea efetiva, ├íreas vizinhas e postos pr├│ximos para o mapa.

### Corrigido

- `/laborcraft station zone clear` n├úo existia: o n├│ `clear` estava aninhado em `zone set` por um par├¬ntese errado.
- O bot├úo **Visualizar** sobrepunha o **Fechar** na aba Config; a a├º├úo foi movida para o topo da caixa.

### Alterado

- O mapa ├® gerado em `DynamicTexture`/`NativeImage` (um blit por frame) e redesenhado a cada 5 s ou ao mudar o zoom.
- O mapa ├® cliente-only e usa s├│ os chunks carregados; colunas fora deles aparecem como `?`.

## [0.1.0-alpha.1] ÔÇö 2026-10-23

Primeira release alpha p├║blica: mercado de trabalho do servidor, contrato v2 com rank/raridade/boosts,
valida├º├úo de casa e Planta de Casa.

Alvo: Minecraft 26.3 / Fabric Loader 0.19.5 / Fabric API 0.164.0+26.3.

### Adicionado

- Estrutura inicial do mod Fabric (Loom 1.19.3, Gradle 9.8.2, Java 25).
- Projeto renomeado de `VillageJobs` para `LaborCraft` (modid `laborcraft`, pacote `com.foque.laborcraft`).
- Bloco `laborcraft:workstation` (Posto de Trabalho) + item + assets + receita de 8 t├íbuas + ba├║.
- Comandos `/laborcraft admin reload` e `/laborcraft admin status` (permiss├úo de gamemaster).
- UI nativa base: tema, painel, `LcButton` com variantes/foco/tooltip, `LaborCraftScreen` e tela Admin.
- Comando `/laborcraft ui` (servidor envia payload; abre a tela Admin) e receiver clientbound.
- Navega├º├úo Admin -> Config com `/laborcraft config`; tela Config com bot├úo Voltar para Admin.
- Altern├óncia de idioma `pt_br`/`en_us` pela tela Config, via `/laborcraft language <code>` + payload clientbound; o cliente salva e recarrega os recursos.
- `LcPagination<T>`: modelo reutiliz├ível de pagina├º├úo (p├ígina atual, tamanho, anterior/pr├│xima).
- `LcItemGrid<T>`: grade de itens com ├¡cone, quantidade de dom├¡nio, badge total compacto e tooltip com valor exato/quebra em pilhas.
- `/laborcraft test items` + `OpenItemTestPayload`: tela de teste em grade 10x5, com 50 itens, navega├º├úo e clique.
- `LaborCraftItemActionScreen`: base da tela de a├º├úo do item, com op├º├Áes por tipo, seletor de quantidade em `long` limitado ao estoque e retorno para a p├ígina anterior.
- A├º├Áes de item (pegar/comprar/equipar/usar) tocam o som de coleta de XP; navega├º├úo e a├º├Áes neutras mant├¬m o clique padr├úo.
- `/laborcraft players` + `OpenPlayersPayload`: tela paginada de jogadores online com rosto, nick, lat├¬ncia e destaque do jogador local.
- Bot├úo **Jogadores online** na tela Admin.
- E0 da entidade: `laborcraft:worker` gen├®rica (`PathfinderMob`), render de alde├úo, atributos, persist├¬ncia, IA b├ísica e nome vis├¡vel.
- Loja p├║blica de **Contrata├º├úo** (`/laborcraft hire`) com pre├ºo, saldo, trabalhadores ativos, contratos pendentes e limite; o bot├úo **Comprar** desabilita no limite ou sem saldo.
- Item `laborcraft:worker_contract` (Contrato de Trabalhador), com o componente `laborcraft:contract_owner`; s├│ o comprador pode usar, e ao usar spawna um trabalhador j├í vinculado e abre a tela de Empregado.
- Tela de **Empregado** com emprego atual, atribui├º├úo de emprego e dispensa com confirma├º├úo.
- `OpenHiringPayload` e `OpenWorkerPayload`; atualiza├º├úo da loja ap├│s compra e da tela ap├│s atribuir/dispensar.
- Comandos `/laborcraft hire`, `/laborcraft worker buy`, `spawn [nome]`, `assign`, `dismiss`, `info` e `remove`; `buy`/`assign`/`dismiss` validam limite/dono no servidor.
- `/laborcraft admin set balance <valor>` para definir o saldo do jogador no provedor ativo (├║til para testar a compra).
- `LedgerCurrency` carrega `config/laborcraft-ledger.json` na inicializa├º├úo.
- Trabalhadores sem v├¡nculo (spawn de admin) n├úo abrem a loja; a intera├º├úo informa que falta um contrato.
- `LcValuePrompt`: valor inteiro/texto/escolha no chat; o jogador digita apenas o valor, com cancelar, sugestoes clicaveis e validacao.
- Comandos `/laborcraft admin set wage <int>` e `/laborcraft admin set provider <id>`.
- Regra command-first: toda a├º├úo da UI envia um comando equivalente; o servidor valida e executa.
- Configura├º├úo em JSON com padr├Áes de sal├írio, per├¡odo e multiplicadores.
- SPI `CurrencyProvider` + registro + provedor interno `laborcraft:ledger`.
- Registro de empregos com `JobType` do fazendeiro.
- Modelo de `Contract` e `BillingPeriod` (di├írio/semanal/mensal).
- Modelo de trabalhador: `WorkerProfile`, maestria por emprego, `WorkerState` e regras de aquisi├º├úo/moradia/reatribui├º├úo na config.
- Modelo de ├írea (`WorkArea`, `AreaShape`), relat├│rio de casa (`HousingReport`) e expediente (`WorkSchedule`) configur├íveis.
- Documenta├º├úo de vis├úo, arquitetura, empregos, configura├º├úo, trabalhador, ├írea/casa/hor├írio, roadmap, decis├Áes e compatibilidade.
- Script `build.cmd` para build confi├ível no Windows (JDK 25 + sa├¡da redirecionada + `--no-daemon`).

- `LcScrollList`: componente reutiliz├ível de lista com viewport recortado, roda do mouse, barra e clique por linha.
- Loja de contrata├º├úo com ofertas rol├íveis (`OpenHiringPayload.Offer`), pre├ºo, manuten├º├úo e sele├º├úo antes da compra; `/laborcraft worker buy <oferta>`.
- Tela de **Perfil** (`/laborcraft profile`, `OpenProfilePayload`) com dinheiro, empregados/limite e lista rol├ível de trabalhadores com emprego, estado, tarefa, vida, fome e humor.
- Contrato guarda dados do trabalhador (`laborcraft:contract_data`: oferta, nome, emprego, boost, manuten├º├úo, fome e humor).
- Dispensar agora converte o trabalhador em Contrato de Trabalhador e remove a entidade; o contrato fica vinculado ou negoci├ível conforme `acquisition.contractTradeable`.
- `worker.maxWorkersPerPlayer` padr├úo agora ├® **1**, expans├¡vel no futuro por casas/upgrades.
- Trabalhador ganhou `hunger` (0ÔÇô20), `mood` (0ÔÇô100) e tarefa derivada do emprego/estado, persistidos e enviados ├á UI.
- Mercado de trabalho **do servidor** (`WorkerMarket`): loja global, estoque compartilhado e rota├º├úo autom├ítica; persistido em `<mundo>/data/laborcraft/market.json`.
- Ofertas determin├¡sticas a partir de `seed do mundo + rota├º├úo` (`WorkerOffers`): mesmo mundo e mesmo dia = mesmas pessoas, com nome pr├│prio sorteado.
- `WorkerRank` (Comum/J├║nior/Pleno/S├¬nior) com multiplicadores de velocidade, aprendizado, f├┤lego, manuten├º├úo e pre├ºo; rank sorteado na oferta e fixo no contrato.
- `WorkerRarity` (Comum/Incomum/Raro/├ëpico) separada do rank, controlando a quantidade e a for├ºa dos boosts.
- `WorkerBoost`/`WorkerBoostType` (velocidade, f├┤lego, aprendizado, rendimento, manuten├º├úo, aptid├úo) e `WorkerAptitudes` (1ÔÇô5 estrelas por emprego).
- Contrato v2 (`laborcraft:contract_data`) com rank, raridade, boosts, aptid├Áes, maestria por cargo, cargo atual, nome, fome e humor; contratos antigos migram para Comum/comum com cargo de fazendeiro.
- Comando `/laborcraft admin market refresh` para for├ºar nova rota├º├úo do mercado.
- Tela de Contrata├º├úo mostra pessoa, rank, raridade, aptid├úo, boosts, pre├ºo e manuten├º├úo; tela de Empregado mostra rank/raridade, aptid├úo e maestria; Perfil colore o nome pela raridade e mostra `Rank ┬À Cargo ┬À Estado`.
- `HousingValidator`: valida a ├írea interna da casa por flood fill a partir da cama (s├│ ar atravessa; porta fechada e vidro selam; interior que v├¬ o c├®u invalida; limites de volume e raio). O tamanho ├® medido no **andar da cama**, e a capacidade de camas sai de `├írea / 12` (m├¡n. 2, m├íx. `maxBedsPerHouse`), exigindo 5x5 interno.
- Comando `/laborcraft home scan <x> <y> <z>` (gamemaster/console) devolve o relat├│rio da casa: v├ílida/inv├ílida, motivo, interno, volume, portas, camas e vagas.
- Config `housing.minInteriorSize` (5) e `housing.maxBedsPerHouse` (4).
- Item `laborcraft:house_plan` (**Planta de Casa**): clique direito no ch├úo mede o c├┤modo como se houvesse uma cama ali e responde no chat com v├ílido/inv├ílido, motivo, tamanho interno, volume, portas, camas e vagas. Receita de 3 papel + 1 graveto; textura provis├│ria (papel).
- **Posto de Trabalho com estado**: `StationRegistry` salvo em `<mundo>/data/laborcraft/stations.json`, com dono, trabalhador vinculado, modo de ├írea e zona. O posto registra o dono ao ser colocado e um posto sem dono ├® reivindicado no primeiro uso.
- Interagir com o posto abre a **tela do Posto** (payload `open_station`): aba Trabalho com a lista de empregados, a vaga do posto e as a├º├Áes vincular/desvincular/chamar/retomar; aba Config com o modo de ├írea (leitura).
- Comandos `/laborcraft station bind|unbind|recall|pause|resume|open|info`.
- Comando `/laborcraft home measure <x> <y> <z>` (gamemaster/console): mesma medi├º├úo da Planta, sem exigir cama real.

### Alterado

- `housing.allowSharedHouse` agora ├® `false` por padr├úo; casa compartilhada fica para etapa futura.
- A oferta passou a vender **a pessoa**, n├úo o cargo: contrato novo entra ÔÇ£sem cargoÔÇØ e o emprego ├® escolhido na tela de Empregado.
- `worker buy` retira a pessoa do estoque do servidor; se a mochila estiver cheia, a oferta volta ao estoque e o dinheiro ├® devolvido.
- A loja deixou de ser por jogador: estoque, pre├ºo e rota├º├úo s├úo do servidor, iguais para todos.
- Trabalhador ficou **parado por enquanto**: a IA de passeio (`WaterAvoidingRandomStrollGoal`) foi removida at├® existir a IA de trabalho (├írea/casa/pathing).
- Ao abrir a tela do trabalhador, ele **para tudo** (navega├º├úo, movimento) e olha para o jogador; ao fechar, ├® liberado. O estado abre/fecha vem do cliente por `worker_screen_state`, ent├úo funciona em servidor.
- Dispensar n├úo reabre mais a loja de contrata├º├úo: apenas fecha a tela de Empregado (`close_screen`).
- O Perfil mostra os contratos ainda na mochila (`0 / 1 ┬À 2 contrato(s)`) e a loja passou a rotular ÔÇ£Trabalhadores + contratosÔÇØ, porque o limite conta os dois.
- Licen├ºa trocada de CC0-1.0 para **GPL-3.0-or-later** (copyleft): forks e trabalhos derivados precisam continuar open source.
- README principal em ingl├¬s; a vers├úo em portugu├¬s ficou em `README.pt-BR.md`.
- Novo documento de desenho do trabalho ([`docs/13-TRABALHO-E-ATRIBUICAO.md`](docs/13-TRABALHO-E-ATRIBUICAO.md)): posto como ├óncora, zona opcional desenhada com estaca e v├¡nculo trabalhadorÔåöposto; casa pausada.

### Corrigido

- Trabalhador continuava andando com a interface aberta (e o `isPauseScreen()` falso n├úo pausa o mundo em single-player): agora o congelamento ├® do servidor, n├úo do cliente.
- `/laborcraft worker buy <oferta>` rejeitava `laborcraft:offer/7/3`: o id ├® um `ResourceLocation` e o argumento era `word()` (sem `:` nem `/`). Agora usa `IdentifierArgument`, como o `<job>` do `assign`.
- No modo criativo o Contrato de Trabalhador n├úo era consumido: cada uso gerava um trabalhador e cada dispensa devolvia outro contrato, ent├úo a contagem estourava (`2/1` com 0 empregados) e a compra ficava bloqueada. Agora o contrato ├® consumido sempre.
- Dispensar com a mochila cheia sumia com o contrato: no criativo o `Inventory.add()` **destr├│i o que n├úo couber e ainda retorna `true`**. Agora a dispensa guarda uma c├│pia intacta, checa espa├ºo antes e, sem slot, deixa o contrato no ch├úo avisando no chat. A compra tamb├®m valida espa├ºo antes de cobrar.
- Porta contava como **duas** no relat├│rio da casa (a porta ocupa dois blocos): agora s├│ a metade de baixo conta.
- Clique nos itens de `LcItemGrid` n├úo abria a tela de a├º├úo no MC 26.3: o bot├úo esquerdo agora ├® tratado por `onClick`/`isValidClickButton` (no 26.3 o esquema de bot├Áes ├® 1=esquerdo, 2=meio, 3=direito).
- Build travando por causa do daemon do Gradle segurando o stdout; `build.cmd` agora usa `--no-daemon` al├®m do redirecionamento.
- Loom 1.19.3 exigindo Gradle 9.8+: wrapper atualizado para 9.8.2.
- Chaves de estado, emprego, vida e situa├º├úo do trabalhador apareciam cruas nas telas; o c├│digo e os arquivos de lang agora usam o mesmo prefixo `laborcraft.ui.worker.*`.
