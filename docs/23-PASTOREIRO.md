# Pastoreiro e cercados (protótipo)

## Comandos de teste

```text
/laborcraft admin set max-workers 10
/laborcraft admin pen scan
/laborcraft worker assign <worker> laborcraft:herder
```

`pen scan` usa a posição do jogador como ponto interno. Fique dentro do cercado, em espaço livre.

Definir o emprego do Posto:

```text
/laborcraft station job set <posição-do-posto> laborcraft:herder
```

Aplicar/remover num Posto:

```text
/laborcraft station enclosure set <posição-do-posto> <posição-dentro-do-cercado>
/laborcraft station enclosure clear <posição-do-posto>
```

## O que já existe

- `EnclosureScanner` / `EnclosureScan`: flood fill horizontal de 2D, delimitado por cerca, parede e
  fence gate. Conta área, largura, profundidade, portões e blocos de borda. Exige pelo menos um
  portão.
- `JobType` `laborcraft:herder` registrado, com nome, descrição e tarefa ("Pastoreando").
- Nenhum `WorkerBrain` para herder ainda: atribuir o emprego deixa o trabalhador sem produção.

## Decisões em aberto

### Quem define a profissão?

Recomendação: **o trabalhador tem a profissão**; o Posto registra a especialização quando o primeiro
trabalhador é vinculado.

- Posto vazio aceita qualquer trabalhador.
- Ao vincular, o Posto passa a exigir aquele `jobId`.
- Trabalhador de outro emprego é recusado com mensagem clara.
- Trocar a especialização exige desvincular todos.
- O Posto adapta a UI e a ferramenta de área pelo `jobId`, mas não muda a profissão do trabalhador
  silenciosamente.

### Vários trabalhadores no mesmo Posto

Todos devem ser do **mesmo emprego**. Funções diferentes no mesmo Posto exigiriam áreas, baús,
expedientes e UIs separadas; não vale a complexidade agora.

### Área do pastoreiro

Não é um retângulo desenhado. O jogador clica dentro do cercado e o scan calcula o interior.

Opções de armazenamento:

1. **Row runs**: decompor o interior em retângulos de uma linha e guardar na lista `zones`
   existente (limite atual de 32 retângulos).
2. **Enclosure descriptor**: guardar o ponto-semente + bounds e revalidar o cercado
   periodicamente; a área é recalculada.

Decisão: usar **descriptor próprio** (`EnclosureDescriptor`), com seed, bounds, área, spans
horizontais e posições dos portões. O retângulo continua existindo para empregos que preferem área
retangular (ex.: fazendeiro).

### UI por emprego

O Posto deve ter um `PostKind` derivado do `jobId`:

- ferramenta de área (retângulo, cercado, curral, mina…);
- abas/indicadores específicos;
- validações e notices específicos.

## Limitações conhecidas do protótipo

- Salário/cobrança ainda usam `config.farmer` para todos os empregos.
- O scan de cercado ainda não é aplicado a um Posto.
- Herder não tem produção, então não pausa por falta de cerca/portão; apenas o comando valida.

## Config por emprego

`LaborCraftConfig` agora tem `jobs` (`Map<String, JobSettings>`) e `job(jobId)`. O campo antigo
`farmer` continua existindo como alias do emprego farmer para JSONs antigos.

- `config.job(worker.jobId())` é usado por cobrança, contrato, farmer e storage.
- `config.job("laborcraft:herder")` tem valores próprios (`JobSettings.herder()`).
- `config.defaultJob()` mantém as ofertas genéricas no farmer até o mercado escolher emprego.
- O painel Admin ainda edita o farmer; um seletor de emprego na aba Salário fica para a próxima UI.

## Persistência do Posto

`Station` ganhou `jobId` e `enclosure`. `stations.json` salva os spans e portões do descriptor.
Stations antigas carregam com `jobId=""` e `enclosure=null`.

`station enclosure set` grava o descriptor e mantém `mode=ZONES`; `hasEnclosure()` é separado de
`hasZone()` para o fazendeiro continuar usando retângulos.
## Mapa do pastoreiro

Quando o Posto está em `laborcraft:herder`, a aba Config muda:

- o mapa é somente leitura;
- **clicar** num bloco dentro do cercado envia o seed para o servidor;
- o servidor roda `EnclosureScanner` com limite (`enclosure.maxScanRadius`, `enclosure.maxArea`);
- o descriptor volta no `OpenStationPayload` e é desenhado como spans âmbar;
- portões aparecem como marcadores;
- o botão **Limpar cercado** remove o descriptor.

O retângulo continua sendo o modo dos outros empregos. Se o clique não encontrar um cercado dentro do limite, o servidor responde com o motivo (`no_gate`, `no_fence`, `too_large`, etc.).