# 15 — Painel Admin (pendente)

**Status:** prioridade alta. A base de cobrança e a configuração live por comandos já existem
(`/laborcraft billing ...`), mas a UI do painel ainda não. Hoje o Admin é um menu de atalhos
(`LaborCraftAdminScreen`) e o `config` só troca idioma. O painel será a camada validada em cima do
`LaborCraftConfig` + comandos atuais.

## Objetivo

Um painel in-game, server-authoritative, para reconfigurar o servidor **sem reboot e sem editar JSON**:
salário-base, multiplicadores por rank/raridade, período, graça/faltas, expediente, provedor de moeda,
área/casa, limites e mercado. O JSON continua sendo a persistência.

## O que já está pronto

- `LaborCraftConfig` é um singleton mutável; `save()` persiste e os sistemas leem cada tick.
- Cobrança por trabalhador com salário/período, dívida, graça, greve, notificações e `billing.force`.
- Tabela salarial configurável: base, rank, raridade, boosts, teto e mínimo.
- Comandos live gamemaster:
  - `/laborcraft billing set <campo> <valor>`
  - `/laborcraft billing rank|rarity <id> <multiplicador>`
  - `/laborcraft billing apply-salary`
  - `/laborcraft billing force`
  - `/laborcraft worker pay <trabalhador>`
- Mudanças de salário/rank/raridade/preço regeneram as ofertas; `admin market refresh` força rotação.
- Snapshot: trabalhadores existentes só mudam com `apply-salary`; a UI deve deixar isso explícito.

## O que a UI precisa resolver

1. **Salário é snapshot.** Oferta e contrato guardam o `upkeep` do momento. A UI deve oferecer
   “aplicar aos novos” (padrão) e “aplicar aos existentes” com confirmação.
2. **Período.** O valor é diário multiplicado por `billingPeriodDays` (1/7/30). Mudar o período de um
   contrato existente só vale após `apply-salary`; decidir se a UI também reinicia o próximo vencimento.
3. **Cache/estado.** Ofertas do mercado, `upkeep`, provedor, raio e zonas são snapshots/caches. A UI
   precisa indicar o que é imediato e o que exige “atualizar mercado”/“aplicar”.
4. **Provedor de moeda.** Trocar pode “perder” saldos do provedor antigo; avisar/migrar antes.
5. **Segurança/auditoria.** Permissão de gamemaster/op, validação de faixa, log de quem mudou o quê.
   O cliente nunca é autoridade; o servidor valida e salva.
6. **Cobrança offline.** Hoje só trabalhadores carregados são cobrados; a UI deve mostrar esse limite
   até existir um registro persistido de contratos.

## Abas/escopo sugeridos

- **Economia:** base diária, rank, raridade, boosts, mínimo, teto, período, graça, faltas, dívida,
  notificação, “aplicar aos existentes” e “forçar cobrança”.
- **Expediente:** modo (`always/daylight/night/custom`), start/end, `workInRain`, `pauseBillingOffDuty`;
  depois perfil por cargo.
- **Área/Casa:** `area.maxRadius`, vertical, limites de casa/cama, recheck; revalidar áreas/casas.
- **Trabalhadores/Mercado:** limite base, rotação/ofertas, refresh do mercado, status econômico.
- **Sistema:** reload, status, versão, logs, “restaurar padrões”.

## Tabela salarial implementada (padrões alpha.9)

| Campo | Padrão |
| --- | ---: |
| base diária (`farmer.baseWage`) | 24 |
| mínimo diário (`billing.minimumDaily`) | 0 |
| rank COMUM / JUNIOR / PLENO / SENIOR | 1.00 / 1.25 / 1.60 / 2.00 |
| raridade COMUM / INCOMUM / RARO / ÉPICO | 1.00 / 1.05 / 1.10 / 1.20 |
| bônus de upkeep dos boosts | soma ao multiplicador |
| teto do multiplicador (`boostCap`) | 2.5 |
| diária | `round(max(base, mínimo) × (rank × raridade + boosts))` |
| período | diária × `billingPeriodDays` (1/7/30) |
| graça | 1 falta |
| dispensa automática | desligada (`dismissAfterMissed = 0`) |

Tudo acima é configurável por comando; a UI deve expor os mesmos campos com validação de faixa.
Decisões ainda em aberto: reiniciar o próximo vencimento ao aplicar tabela/período, migração de
provedor e cobrança de contratos em chunks descarregados.

## Ordem de implementação

1. **UI Economia** com os campos atuais + “aplicar aos existentes” + “forçar cobrança”.
2. Payload de estado/aplicação e validação server-side (permissão, faixas, save).
3. Abas Expediente/Área/Casa/Mercado/Sistema.
4. Auditoria/log e testes de unidade do cálculo e do ciclo.
5. Persistência de contratos para cobrança offline.

## Checklist

- [x] Motor de cobrança + contrato persistido no trabalhador
- [x] Tabela salarial configurável (mínimo/rank/raridade/boosts/período)
- [x] Comandos live e `apply-salary`/`force`
- [ ] UI Admin live (Economia/Expediente/Área/Casa/Sistema)
- [x] Aplicar aos contratos existentes por comando (falta UI com confirmação)
- [x] Notificações de cobrança
- [ ] Auditoria e testes de unidade do ciclo
