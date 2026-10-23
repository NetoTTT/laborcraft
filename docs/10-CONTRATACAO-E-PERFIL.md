# 10 — Contratação, dispensa e perfil do jogador

Este documento registra as regras pedidas para a próxima etapa. A implementação pode chegar em partes, mas estas decisões são a referência.

## 1. Loja de contratação

- A contratação **não é infinita**: a loja mostra uma lista de ofertas disponíveis **naquele momento**.
- A lista fica no rodapé da tela e usa **scroll** (roda do mouse + barra), no estilo da lista de receitas/ingredientes do Minecraft.
- Cada oferta representa um trabalhador/emprego disponível. O padrão são os empregos comuns; no futuro podem existir:
  - trabalhadores com boost aleatório;
  - trabalhadores definidos por admin;
  - ofertas melhores, com **compra inicial mais cara** e **manutenção maior**.
- Cada oferta deve mostrar, no mínimo: emprego, nome/variação, boost (quando houver), preço inicial e custo de manutenção.
- O jogador compra uma oferta e recebe um **Contrato de Trabalhador**. O contrato é o item que spawna o trabalhador.
- A loja não deve vender “um contrato genérico” sem contexto de emprego/oferta; a compra é sempre de uma oferta da lista.

## 2. Limite de trabalhadores

- O limite padrão deve ser **1 trabalhador ativo por jogador**.
- O limite continua configurável em `worker.maxWorkersPerPlayer`.
- Formas futuras de aumentar o limite: casas, upgrades, reputação, etc. A regra de limite não deve ser fixa no código.
- A loja conta trabalhadores ativos **e** contratos ainda não usados; ao atingir o limite, a compra fica desabilitada.

## 3. Dispensa

- Dispensar **não apaga** o trabalhador nem seus dados. No futuro, maestria, histórico e boosts devem ser preservados.
- Dispensar transforma o trabalhador de volta em um **Contrato de Trabalhador** com os dados do trabalhador.
- A entidade não deve ficar largada no mundo “sem rumo pedindo outro contrato”.
- O contrato devolvido:
  - preserva nome, emprego, boost e (futuramente) maestria/histórico;
  - se `acquisition.contractTradeable = false`, fica vinculado a quem dispensou;
  - se `acquisition.contractTradeable = true`, pode ser negociado com outro jogador.
- Usar o contrato devolvido spawna o mesmo trabalhador, com os mesmos dados, para o novo dono.

## 4. Perfil do jogador

- Nova tela pública de perfil, aberta por comando (ex.: `/laborcraft profile`).
- Mostra:
  - dinheiro/saldo atual no provedor ativo;
  - quantos empregados o jogador tem e o limite atual;
  - lista rolável de trabalhadores com nome, emprego, tarefa atual, vida, fome e humor;
  - informações gerais resumidas (quantos estão ativos, pausados, sem emprego etc.).
- A lista de trabalhadores usa o mesmo componente de scroll da loja.
- A tela é somente leitura; ações continuam em comandos/telas específicas.

## 5. Componente de scroll

- Criar um componente reutilizável (`LcScrollList`/`LcScrollPanel`) com:
  - viewport recortado;
  - conteúdo maior que a área visível;
  - roda do mouse;
  - barra/indicador de posição;
  - clique e hover nos itens;
  - navegação por teclado quando possível.
- O componente não deve depender de APIs internas da Creative Inventory; a ideia é reproduzir o comportamento com widgets próprios.
- Reutilizar em: loja de contratação, perfil do jogador e futuras listas (contratos, trabalhadores, ofertas).

## 6. Dados do trabalhador

- Adicionar ao trabalhador, com persistência e envio ao cliente:
  - `hunger` (fome; 0–20, padrão 20);
  - `mood` (humor; 0–100, padrão 100);
  - `task`/descrição da tarefa atual (texto curto; “Sem tarefa” quando não houver IA ativa).
- A simulação de fome/humor pode ficar para depois; nesta etapa eles existem como estado visível e persistente.
- O perfil do jogador e a tela de Empregado devem exibir esses valores.

## 7. Ordem sugerida

1. Componente de scroll.
2. Modelo de ofertas + loja rolável.
3. Contrato com dados do trabalhador + dispensa devolvendo contrato.
4. Limite padrão 1.
5. Perfil do jogador.
6. Fome/humor/tarefa no trabalhador e nos payloads.

## 8. Status da implementação

- Implementado: `LcScrollList`, loja com ofertas, compra por oferta, contrato com dados, dispensa devolvendo contrato, limite padrão 1, perfil por `/laborcraft profile`, fome/humor/tarefa no trabalhador e nas telas.
- Ainda não implementado: simulação de fome/humor, efeito real dos boosts, maestria/histórico no contrato e mais ofertas além do fazendeiro padrão.
