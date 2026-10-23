# 02 — Empregos

## Empregos planejados

| Emprego | Estado | Custo relativo | Observação |
| --- | --- | --- | --- |
| Fazendeiro | primeiro a ser implementado | baixo | colhe, replanta, usa composteira/farinha de osso, deposita |
| Minerador | depois | alto | exige ferramenta, área, torres de acesso; mais risco |
| Lenhador | ideia | médio | replantio de mudas, poda de árvores |
| Pastor | ideia | baixo | recolher itens de animais, alimentar |
| Ferreiro/Artesão | ideia | médio | consumir de baú, produzir em fornalha |
| Guarda | ideia | médio | patrulhar área, atacar hostis |

O núcleo (contrato, salário, área, tiers, pagamento) é o mesmo para todos.

## Fazendeiro (MVP detalhado)

### Objetivo

Produzir colheitas contínuas na área contratada, com eficiência maior que colheita manual, depositando tudo no contêiner escolhido, sem nunca sair da área nem tocar em propriedade não autorizada.

### Área de serviço

- Um **bloco âncora** define o centro (ex.: bloco de "mesa de trabalho" ou um baú marcado).
- Raio por tier: 4 / 6 / 8 / 9 (tier 1..4). A forma pode ser quadrado ou círculo (config).
- Só trabalha em blocos **dentro da área** e **reivindicados** por este trabalhador.
- Dois trabalhadores não podem reivindicar o mesmo bloco de plantação; o segundo recebe erro claro ao contratar.

### Ciclo de trabalho

1. **Checar pagamento** — contrato em dia.
2. **Descarregar** — se inventário cheio, caminha até o baú de saída e deposita.
3. **Pegar insumos** — sementes/farinha de osso no baú de insumos, se necessário e habilitado.
4. **Colher** — bloco maduro (`laborcraft:harvestable` + estado "pronto") → drops do loot table, respeitando ferramenta da tier.
5. **Replantar** — se possível, planta a mesma cultura (a semente vem do inventário ou do baú de insumos).
6. **Fertilizar** — se `useBoneMeal` e tier permitir: aplica farinha de osso com limite por período (evita consumo infinito).
7. **Compostar** — se `useComposter`: coloca excedente/insumos na composteira e recolhe farinha de osso pronta.
8. **Esperar** — sem alvo, aguarda N ticks configuráveis.

### Regras e limites

- Nunca colhe cultura imatura.
- Nunca colhe "de graça" modded sem regra: se o bloco não está em tag, precisa de definição no datapack.
- Não quebra blocos que não sejam cultura (não "limpa" a fazenda sem instrução).
- Não planta em bloco já ocupado; não planta fora da área.
- Não pega item de baú fora da lista de contêineres autorizados.
- Se não há espaço no baú de saída: pausa e avisa o dono (não joga item no chão por padrão).
- Se não há semente/insumo: pausa e avisa (config pode permitir "colher sem replantar").
- Se o chunk não está carregado: não trabalha.
- Se o inventário do trabalhador ficar cheio e não houver baú: pausa e avisa.

### Eficiência por tier

| Tier | Raio | Velocidade | Farinha de osso | Comporteira | Custo (multiplicador) |
| --- | --- | --- | --- | --- | --- |
| 1 | 4 | base | não | não | 1.0x |
| 2 | 6 | +60% | sim (limitada) | sim | 1.5x |
| 3 | 8 | +120% | sim | sim | 2.25x |
| 4 | 9 | +200% | sim (sem limite de lote) | sim | 3.4x |

Os multiplicadores vêm de `wageMultiplierPerTier` (default 1.5) elevado ao tier-1. Tudo configurável.

### Anúncio e transparência

Toda cobrança, pausa e rompimento gera mensagem ao dono (chat e/ou item de recibo, config). Sem surpresa: o jogador sempre sabe quanto pagou e por quê o trabalhador parou.

## Minerador (esboço)

Diferenças que o núcleo precisa suportar desde já:

- Trabalhador **consome durabilidade de ferramenta** (picareta no baú; troca quando quebra).
- Trabalha em volume (2D/3D) e precisa de **tochas/iluminação** e **descarte** (lava, lixo, entulho).
- **Não cava por estruturas** protegidas/reivindicadas; respeita regiões.
- **Blocos proibidos** (bedrock, obsidiana sem ferramenta adequada, spawners) por tag/config.
- Salário e custo de ferramentas tornam o emprego caro → bom para economias grandes.
- Risco de lava/queda: IA precisa preferir caminhos seguros e pausar se não houver rota.

Nada disso deve exigir mudar o contrato, a moeda ou o ciclo de pagamento — só adicionar `WorkTask` e definição.
