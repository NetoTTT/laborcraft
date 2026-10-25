# 15 — Painel Admin (a fazer)

**Status:** anotado como **prioridade alta**; não entra no alpha.7. Hoje o Admin é um menu de atalhos
(`LaborCraftAdminScreen`) e o `config` só troca idioma. O `admin set wage`/`provider` já alteram o
`LaborCraftConfig`, salvam o JSON e valem na hora — a base para o painel existe.

## Objetivo

Um painel in-game, server-authoritative, para reconfigurar o servidor **sem reboot e sem editar JSON**:
salário mínimo, período de cobrança, multiplicadores por rank/raridade, expediente, provedor de moeda,
área/casa, limites e mercado. O JSON continua sendo a persistência; a UI é uma camada validada em cima.

## O que é fácil

- Config é um singleton mutável (`LaborCraftConfig.get()`); `save()` persiste; sistemas leem cada tick.
- Um payload de estado (valores/limites/opções) + um payload/comando de aplicação; o servidor valida
  permissão/faixa, muta, salva e devolve o estado. Sem restart.
- Tela com abas/formulários/dropdowns e o `LcButton`/`EditBox` existentes.

## O que dá trabalho

1. **Salário é snapshot.** `WorkerOffer.upkeep` é gerado na oferta (`farmer.baseWage × rank.upkeepMultiplier
   + boosts`) e copiado para o trabalhador/contrato. Mudar a base **não muda** contratos/ofertas existentes
   sem uma ação explícita “aplicar aos existentes” (com aviso). Decidir: manter snapshot ou recalcular.
2. **Cobrança ainda não existe.** O `upkeep` aparece na UI, mas não há motor de vencimento, graça, pausa e
   rompimento. Sem ele, mudar diário ↔ semanal ↔ mensal não tem efeito real.
3. **Rank × raridade.** Rank (`COMUM/JUNIOR/PLENO/SENIOR`) mexe no upkeep hoje; raridade
   (`COMUM/INCOMUM/RARO/ÉPICO`) mexe em preço/boosts, **não no salário**. Se a raridade pagar mais, criar a
   tabela explicitamente.
4. **Período.** Definir se o valor é por dia (semanal = ×7, mensal = ×30) ou por período. Recomendado:
   mínimo **por dia**; o período só decide quando cobra.
5. **Cache/estado existente.** Ofertas do mercado, `upkeep` dos contratos, provedor de moeda, raio máximo e
   zonas são snapshots/caches. Alguns valores mudam na hora, outros só valem para novos; a UI precisa de
   “atualizar mercado” e “aplicar aos existentes” quando fizer sentido.
6. **Troca de provedor de moeda** pode “perder” saldos do provedor antigo; avisar/migrar.
7. **Segurança/auditoria:** permissão de gamemaster/op no servidor, validação de faixa, log de quem mudou o
   quê. O cliente nunca é autoridade.

## Abas/escopo sugeridos

- **Economia:** provedor, salário mínimo diário, multiplicadores por rank e raridade, período
  (`diário/semanal/mensal`), graça/faltas, “aplicar aos novos” × “aplicar aos existentes”.
- **Expediente:** modo (`always/daylight/night/custom`), start/end, `workInRain`, `pauseBillingOffDuty`;
  depois perfil por cargo.
- **Área/Casa:** `area.maxRadius`, vertical, limites de casa/cama, recheck; revalidar áreas/casas.
- **Trabalhadores/Mercado:** limite base, rotação/ofertas, refresh do mercado, status econômico.
- **Sistema:** reload, status, versão, logs, “restaurar padrões”.

## Tabela salarial (proposta inicial)

| Campo | Exemplo |
| --- | ---: |
| mínimo diário | 24 |
| rank COMUM / JUNIOR / PLENO / SENIOR | 1.00 / 1.10 / 1.25 / 1.50 |
| raridade COMUM / INCOMUM / RARO / ÉPICO | 1.00 / 1.05 / 1.10 / 1.20 |
| diária | `round(min × rank × raridade)` |
| semanal / mensal | diária × 7 / × 30 |

Decisões pendentes: se raridade entra no salário; se boosts de upkeep somam; se contratos antigos são
recalculados automaticamente ou por botão; o que acontece ao trocar diário → semanal no meio do ciclo.

## Ordem de implementação

1. Fechar a tabela salarial e a política de contratos existentes.
2. Motor de contrato/cobrança (vencimento, graça, pausa, rompimento, notificações, testes).
3. Payload de estado/aplicação + tela Admin com abas, validação e save.
4. Ações explícitas: “aplicar aos existentes”, “atualizar mercado”, “revalidar casas/áreas”.
5. Auditoria/permissões e testes de unidade do ciclo.

## Checklist

- [ ] Motor de cobrança + contrato persistido
- [ ] Tabela salarial configurável (mínimo/rank/raridade/período)
- [ ] UI Admin live (Economia/Expediente/Área/Casa/Sistema)
- [ ] Aplicar aos contratos existentes com confirmação
- [ ] Notificações e auditoria
- [ ] Testes do ciclo de cobrança e do cálculo
