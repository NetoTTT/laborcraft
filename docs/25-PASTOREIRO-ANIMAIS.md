# Pastoreiro — animais, produtos e população

Documento de design. A ordem de implementação recomendada é **galinha → ovelha → vaca → porco**,
porque cada passo adiciona um tipo de interação sem matar o animal.

## 1. Animais e produtos

| Animal | Produto | Como | Mata? | Estado |
| --- | --- | --- | --- | --- |
| Galinha | Ovo | ovo no chão dentro do cercado | não | implementado (coleta + depósito) |
| Ovelha | Lã | tesoura em adulta com lã | não | próximo |
| Vaca | Leite | balde vazio em adulta | não | depois |
| Porco | Carne | abate | **sim** | último |

Galinha também é a mais simples: não exige ferramenta, o produto é um `ItemEntity` e não há
cooldown por animal.

## 2. Regras por animal

### Galinha (fase 1)

- procura qualquer item de pastoreiro no chão dentro dos spans: ovo, sementes/comida, lã, leite, tesoura, balde;
- anda até o ovo, recolhe para o inventário (slots 0–8, nunca o slot de ferramenta);
- quando não há mais ovos no cercado e há ovo no inventário, vai até o baú de saída e deposita;
- nada de matar, nada de ferramenta, nada de cooldown por galinha.

### Ovelha (fase 2)

- exige tesoura no slot de ferramenta;
- só tesoura ovelha adulta com lã (`Sheep.hasWool`/`Sheep.getSheared`);
- consome durabilidade da tesoura;
- deposita lã (qualquer cor) no baú de saída;
- se a tesoura quebrar, pausa com `need_tool` e o fluxo comum do `AbstractWorkerBrain` resolve.

### Vaca (fase 3)

- exige balde vazio; o balde vira `MILK_BUCKET` e é depositado;
- o trabalhador precisa de baldes vazios no baú/entrada ou no inventário;
- cooldown simples por vaca para não ordenhar em loop.

### Porco (fase 4 — só depois)

- exige espada;
- abate apenas se a população estiver acima do **mínimo** e abaixo do **máximo**;
- drops vão para o baú de saída (carne);
- precisa de cooldown por animal e de checagem de população antes de cada abate.

## 3. População

Chaves em `config/laborcraft.json` → `herder`:

```json
"herder": {
  "collectDrops": true,
  "shearSheep": true,
  "milkCows": false,
  "breeding": false,
  "maxAnimalsPerPen": 12,
  "minAnimalsPerPen": 4,
  "eggScanRadius": 16
}
```

- `maxAnimalsPerPen`: teto por cercado; reprodução para de alimentar ao atingir.
- `minAnimalsPerPen`: piso; **nunca** culling/abate abaixo disso.
- Contagem é feita por tipo e por cercado (spans), não pelo raio do posto.

### Reprodução (fase 2.5)

- exige `breeding=true`;
- só adultos (`AgeableMob.isBaby() == false`);
- exige dois animais da mesma espécie dentro do cercado;
- item de amor: sementes (galinha), trigo (ovelha/vaca), cenoura/batata (porco);
- respeita o teto: não alimenta se `population >= maxAnimalsPerPen`;
- respeita o cooldown do vanilla (`setInLove`) e não repete todo tick.

## 4. Ferramentas e baús

- ferramenta no slot de ferramenta (`TOOL_SLOT`), nunca no inventário de produto;
- tesoura/balde vêm do baú de **entrada** ou de **ferramentas** quando faltarem;
- produto só vai para baú de **saída** ou **misto**;
- se não houver baú de saída, o trabalhador mantém o produto e avisa uma vez
  (`laborcraft.ui.worker.task.no_chest`).

## 5. Ciclo do brain (fase 1)

Dentro do cercado (`GRAZE`):

1. se estiver depositando, continua até o baú;
2. senão procura ovo no cercado e vai até ele;
3. se não há ovo e há produto no inventário, vai depositar;
4. senão passeia por um ponto interno aleatório.

O ciclo entrar → pastar → sair continua igual, com portão abrindo/fechando pelo
`OpenFenceGateGoal`.

## 6. Decisões

- Nada de abate enquanto os produtos sem morte não estiverem estáveis.
- O porco é um caso separado porque exige morte e controle de população mais rígido.
- Galinha primeiro porque valida coleta de drop, inventário e depósito sem ferramenta.
- Ovelha e vaca adicionam ferramenta/consumível e cooldown por animal.
- A contagem de população é sempre por cercado; dois cercados não compartilham teto.

## Portões múltiplos

- O pastoreiro fecha qualquer portão do cercado que esteja aberto e **sem trabalhador cujo próximo nó do path seja aquele portão**.`r`n- Fora do expediente, `WorkerRest` também fecha portões ociosos do cercado.
- Isso roda a cada tick do brain, então portão esquecido aberto por retry, troca de fase ou recarregamento de chunk é fechado.
- O portão em uso (travessia) é preservado.
- `OpenFenceGateGoal` também fecha ao parar e quando o trabalhador é removido.
## Ferramentas automáticas

- `herder.autoTools` liga a troca sozinha.
- O brain decide a ferramenta pela fauna do cercado: hoje **tesoura** se houver ovelha.
- Ordem de busca:
  1. slot de ferramenta;
  2. mochila (slots 0–8);
  3. baú de **ferramentas** (role TOOLS).
- Ao trocar, a ferramenta antiga vai para um slot vazio da mochila; se não houver, vai para o baú de ferramentas.
- Ferramenta danificada conta como válida (mesmo item, dano ignorado).
- Ferramenta quebrada é substituída na próxima checagem.
## Tab Rebanho (por posto)

- Aparece só em posto de pastoreiro.
- Contagem viva: galinhas, ovelhas, vacas.
- Toggles: coletar itens, tosquiar, ordenhar, reproduzir, auto-ferramenta.
- Steppers: máximo/mínimo por espécie e threshold de entrega.
- Cada controle envia `/laborcraft station herd set <pos> <chave> <valor>` e reabre o payload.
- Dados ficam em `stations.json` por posto, com defaults copiados do global na criação.