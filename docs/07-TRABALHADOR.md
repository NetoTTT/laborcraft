# 07 — Trabalhador (decisões de base)

Escopo: definir a base do trabalhador **agora**, com o fazendeiro como único emprego implementado. Tudo abaixo é configurável e o núcleo fica aberto para minerador, lenhador etc.

## 1. Nome

**Recomendação:** manter `LaborCraft` e `modid = laborcraft`.

- O mod não é sobre "aldeões": é sobre **empregos**. LaborCraft é temático (o trabalhador parece aldeão) mas não amarra o mod a uma entidade.
- "Jobs" sozinho é genérico demais e colide fácil com outros mods/plugins.
- Renomear depois quebra mundo, configs e datapacks. Se for trocar, troque **antes do M1**.

Alternativa segura: nome de exibição `LaborCraft` e nome interno neutro `laborcraft`, como está hoje.

## 2. Como o jogador obtém o trabalhador

O "ovo" funciona, mas não como spawn egg de criatura: é um **Contrato de Trabalhador** (`laborcraft:worker_contract`). Nesta etapa o item guarda o UUID do comprador no componente `laborcraft:contract_owner`; ao ser usado, spawna um trabalhador já vinculado a ele. Guardar um perfil completo dentro do item (nome, maestria, histórico) é uma etapa futura.

Modos configuráveis (`acquisition.mode`):

| Modo | Como funciona | Indicado para |
| --- | --- | --- |
| `purchase` (default) | compra na loja pública `/laborcraft hire` pagando com o `CurrencyProvider`; o dinheiro vai para `revenueAccount` (`server`, `government` ou UUID) | servidor com governo/banco, como o seu |
| `free` | a loja entrega o contrato sem cobrar | single-player e testes |
| `craft` | receita define o contrato; sem custo em dinheiro (planejado) | single-player e modpacks |
| `admin` | só operador entrega via comando (planejado) | servidores controlados |

Por que "Agência" e não só crafting: no seu caso o dinheiro precisa **entrar** em algum lugar. Uma compra que paga a conta do governo/servidor vira um dreno econômico coerente, e o servidor pode controlar oferta e preço.

O item NÃO deve ser um spawn egg vanilla:
- spawn egg vanilla não guarda perfil (nome, maestria, histórico);
- spawn egg cria entidade sem dono/contrato;
- qualquer spawn egg pode ser duplicado com comandos/farms.

## 3. Entidade genérica ou especializada?

**Recomendação: uma entidade genérica** `laborcraft:worker`, com o emprego atribuído por dados.

- Uma classe de entidade, um cérebro, e "módulos de trabalho" (`WorkTask`) por emprego.
- Trocar de emprego não troca de entidade.
- Adicionar minerador não exige nova entidade; exige novo `WorkTask` + definição.
- Especializar por emprego (`FarmerWorker`, `MinerWorker`) só se algum emprego precisar de locomoção radicalmente diferente (voo, natação constante). Não é o caso do fazendeiro.

Aparência: modelo/render de aldeão, com roupa/ícone por emprego. O perfil guarda aparência (profissão visual) separada do emprego ativo.

## 4. Reatribuir emprego? Maestria múltipla?

**Recomendação: sim, reatribuível, com maestria por emprego.**

- O trabalhador tem `Map<emprego, XP>` — maestria **separada por emprego**.
- Só **um emprego ativo** por vez; a produção é do emprego ativo.
- Trocar de emprego preserva a maestria antiga. O operário "fazendeiro nível 4 + minerador nível 2" fica valioso — exatamente a ideia de mercado de trabalhadores treinados.
- `worker.allowReassignment` (default `true`), `worker.reassignmentFee`, `worker.reassignmentCooldownDays`.
- A maestria do emprego ativo define tier, velocidade, raio e insumos liberados.
- Opcional futuro: `masteryDecay` para desvalorizar trabalhador parado (default desligado).

Regra de balanceamento: reatribuir é livre, mas **não é de graça em tempo** — cooldown/fee configuráveis; e um trabalhador só rende no emprego ativo, então "treinar tudo" custa tempo de uso.

Níveis de maestria (`worker.masteryLevelThresholds`, default `0, 100, 300, 700, 1500`):

| Nível | XP | Efeito no fazendeiro |
| --- | --- | --- |
| 1 | 0 | base, raio 4 |
| 2 | 100 | +velocidade, raio 6 |
| 3 | 300 | farinha de osso liberada |
| 4 | 700 | raio 8, composteira |
| 5 | 1500 | raio 9, produção máxima |

XP ganho por colheita: `worker.masteryXpPerHarvest` (default 1). Sem XP por ficar parado.

## 5. Voltar para "ovo" (guardar o trabalhador)

**Recomendação: sim, com um item Contrato/Kit que guarda o perfil completo.**

- Ação "Guardar trabalhador" (agachar + clique com contrato, ou item próprio) remove a entidade e devolve o item com o perfil.
- O item guarda: UUID do trabalhador, dono, nome, emprego ativo, **mapa de maestria**, aparência, schema.
- Só o dono pode guardar; contrato com salário atrasado não pode ser guardado (config futuro).
- Exige inventário vazio do trabalhador (default `worker.pickupRequiresEmptyInventory = true`), senão ele primeiro deposita no baú de saída.
- `worker.allowPickup` (default `true`).

### Persistência e anti-duplicação

O perfil vive em **duas camadas**:

1. **No item** (data component / `CUSTOM_DATA` com JSON versionado): portátil, sobrevive a baú, troca, backup do mapa.
2. **No mundo** (`SavedData`/registro de trabalhadores por UUID): impede duplicar.

Regras:
- Ao usar o item, o servidor verifica se aquele UUID já está ativo. Se estiver, **recusa** (não gasta o item) com mensagem clara.
- Guardar é atômico: remove entidade → grava perfil no item → devolve item. Nunca as duas coisas ao mesmo tempo.
- `acquisition.keepProfileBackup = true`: o UUID/perfil também fica no registro do mundo, permitindo recuperação por admin se o item for destruído. Sem isso, item perdido = trabalhador perdido (comportamento "hardcore", configurável).
- Schema versionado (`WorkerProfile.CURRENT_SCHEMA`) para migrar saves.
- Item não empilhável e, por padrão, **não negociável** (`acquisition.contractTradeable = false`) para evitar mercado de duplicatas; ligue se quiser mercado de trabalhadores.

## 6. Moradia

**Recomendação: opcional, desligada por padrão; validação simples, sem "detectar casa".**

Validar casa por estrutura (paredes, teto, porta) é frágil, caro e quebra com mods. O que dá para validar de forma barata e previsível:

- **Cama reivindicada** pelo trabalhador (`requireBed`, default `true` se moradia exigida).
- **Teto**: `level.canSeeSky(posiçãoDaCama)` falso (`requireRoof`). Heurística barata e mod-agnóstica.
- Uma cama por trabalhador e um trabalhador por cama (`oneWorkerPerBed`).

Config (`housing`):
- `required` (default `false`): sem casa, o trabalhador não é contratado/não trabalha.
- `speedPenaltyWithoutHome` (default `0.25`): se `required=false`, trabalhar sem casa deixa 25% mais lento. É o meio-termo que incentiva moradia sem travar.
- Casa não substitui salário: são custos diferentes (capital vs. recorrente).
- `home` fica registrado no perfil (dimensão + posição da cama); se a cama sumir, o trabalhador pausa e avisa.

Sem "sistema de pathing pronto" para casa: o trabalhador usa pathfinding vanilla até a cama; se não chegar, dorme no lugar? Não — pausa e avisa (previsível).

## 7. Pathfinding: o que o Minecraft já dá

Sim, existe e é o **pathfinding vanilla** (`PathfinderMob` + `GroundPathNavigation`). Fabric não adiciona outro; ele expõe o vanilla.

Para o fazendeiro:
- Não navegar por cada planta: escanear área com cache, escolher o alvo **alcançável mais próximo**, navegar uma vez, trabalhar um pequeno grupo.
- Alcance de coleta ~4,5 blocos; colher/replantar sem pathfinding quando já está no alcance.
- Se o caminho falhar N vezes (`pathing.maxPathFailures`, default 5), marca o alvo como indisponível por um tempo e tenta outro.
- Não atravessar água/lava por padrão; não quebrar blocos para abrir caminho.
- Nunca abre portas de terceiros: só portas dentro da área autorizada.
- Área pequena (raio 4–9) torna o pathfinding barato.

## 8. Regras base (config) — resumo

```json
{
  "worker": {
    "masteryLevelThresholds": [0, 100, 300, 700, 1500],
    "masteryXpPerHarvest": 1,
    "allowReassignment": true,
    "reassignmentFee": 0,
    "reassignmentCooldownDays": 0,
    "allowPickup": true,
    "pickupRequiresEmptyInventory": true,
    "maxWorkersPerPlayer": 1
  },
  "housing": {
    "required": false,
    "requireBed": true,
    "requireRoof": true,
    "oneWorkerPerBed": true,
    "speedPenaltyWithoutHome": 0.25
  },
  "acquisition": {
    "mode": "purchase",
    "price": 64,
    "revenueAccount": "server",
    "contractTradeable": false,
    "keepProfileBackup": true
  }
}
```

## 9. O que fica de fora do MVP (e por quê)

- Mercado/leilão de trabalhadores entre jogadores: depende de GUI e de confiança entre contas; depois.
- Moradia com validação estrutural: frágil; só cama + teto.
- Simulação de moral/felicidade: o estado (humor 0–100) já existe e é exibido, mas decaimento, eventos e efeitos de produtividade ficam para depois.
- Trabalhador com múltiplos empregos simultâneos: quebra a economia e a área de serviço; um por vez.
- Herança de maestria ao "morrer": por padrão o perfil é preservado no item/registro, não se perde.

## 10. Próximo passo de implementação

1. **E0 concluído:** entidade `laborcraft:worker` genérica, render de aldeão, atributos, persistência, vínculo e IA básica (float, deambular, olhar).
2. **E1 básico concluído:** loja de contratação, item Contrato de Trabalhador, tela de informações do empregado e atribuição de emprego pela UI.
3. Próximo: `WorkTask`/`JobBehavior` do fazendeiro (escaneamento, colheita, replantio e depósito).
4. Componente de perfil no contrato (com schema) e registro por UUID no mundo.
5. Reivindicação de área/cama e baús.
6. Cobrança de salário usando o `CurrencyProvider` ativo.
7. Comandos de admin para inspecionar maestria e recuperar contratos.

## 11. Fluxo de contratação e comandos

O fluxo público é uma **loja de contratação**. O jogador abre `/laborcraft hire`, vê o preço, o saldo e o limite, e compra um **Contrato de Trabalhador**. O contrato vai para a mochila; ao ser usado, gera um trabalhador **já vinculado ao comprador** e abre a tela de Empregado. O limite `worker.maxWorkersPerPlayer` conta trabalhadores ativos e contratos ainda não usados, então a loja desabilita a compra ao atingir o teto.

Na etapa atual a loja lista ofertas roláveis (padrão: fazendeiro; futuramente variantes com boost, preço inicial e manutenção maiores). Dispensar não deixa o trabalhador solto: ele volta a ser um Contrato de Trabalhador com nome, emprego, fome e humor preservados. O contrato devolvido fica vinculado a quem dispensou quando `acquisition.contractTradeable = false`; com `true`, pode ser negociado. O perfil do jogador abre por `/laborcraft profile` e mostra dinheiro, empregados e a lista rolável de trabalhadores com tarefa, vida, fome e humor.

O comando de admin `/laborcraft worker spawn [nome]` continua criando um trabalhador sem vínculo para testes; interagir com ele apenas informa que não há contrato.

```
/laborcraft hire
/laborcraft worker buy [oferta]
/laborcraft worker assign <uuid> <job>
/laborcraft worker dismiss <uuid>
/laborcraft worker info
/laborcraft profile
/laborcraft worker spawn [nome]
/laborcraft worker remove
/laborcraft admin set balance <valor>
```

- `spawn` e `remove` exigem `COMMANDS_GAMEMASTER`; `buy`, `assign` e `dismiss` são validados no servidor.
- A UI compõe esses mesmos comandos; nenhuma tela altera a entidade ou o inventário diretamente.
- `buy` valida limite, saldo e espaço na mochila antes de entregar o contrato; o servidor cobra o preço configurado em `acquisition.price`.
- `assign` e `dismiss` falham se o jogador não for o dono.
- `info` procura o trabalhador do jogador mais próximo; `remove` aceita qualquer trabalhador num raio de 16 blocos.
- `admin set balance` define o saldo do jogador no provedor ativo e serve para testar a compra.
- A entidade é persistente e não despawna; `remove` usa descarte definitivo, não morte.
