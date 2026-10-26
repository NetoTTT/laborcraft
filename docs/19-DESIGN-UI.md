# 19 — Design base da UI LaborCraft

**Status:** primeira base implementada no alpha.14.
O painel Admin é a tela de referência; as outras telas serão migradas em etapas.

**Quill UI:** a base visual foi extraída para o mod cliente separado **Quill UI** (`com.quillui`,
`0.2.0-alpha.2`), em https://github.com/NetoTTT/quill-ui. O LaborCraft mantém aliases (`LcTheme`,
`LcRender`, `LcTooltips`, `LcButton`, `LcScrollList`) para não reescrever todas as telas de uma vez.
O cliente precisa instalar `quill-ui-0.2.0-alpha.2.jar`; o servidor não precisa. A documentação em inglês
fica em `docs/` no repositório do framework e em `C:\Users\foque\Documents\QuillUI\docs\`.

## Princípio

A UI do LaborCraft deve parecer um **livro-caixa/escritório de campo**, não um menu genérico de Minecraft:
fundo escuro, superfícies em camadas, tipografia compacta, acento âmbar e cores de raridade como
informação — não como decoração.

## Tokens

- **Painel:** `PANEL_BG`, `PANEL_BORDER`, sombra de 4 px.
- **Cabeçalho:** `HEADER_BG`, barra de acento vertical de 3 px, divisor no fim.
- **Superfícies:** `SURFACE` para cards; `SURFACE_ALT` para linhas internas; borda `SURFACE_BORDER`.
- **Texto:** `TITLE` (títulos), `TEXT` (corpo), `TEXT_SOFT` (subtítulos), `MUTED`/`MUTED_DARK` (notas).
- **Acento:** `ACCENT`/`ACCENT_HOVER` (âmbar).
- **Semântica:** `DANGER`/`DANGER_BG`, `SUCCESS`, `INFO`.
- **Espaçamento:** grade de 4 px: `SPACE_1` a `SPACE_6`.
- **Tabs:** `CHIP_BG` + `CHIP_BORDER`; tab ativa usa `PRIMARY`/acento.

## Estrutura de tela

1. **Header** de 48 px: barra de acento, título, subtítulo e divisor.
2. **Tab strip** (quando houver abas) com 4 px de padding interno.
3. **Cards de seção** (`LcRender.section`) para agrupar conteúdo; uma coluna ou duas.
4. **Ações** separadas no rodapé do conteúdo; nunca no meio dos cards.
5. **Footer** com divisor e texto discreto.

## Componentes

- `LcButton`: alias de `QuillButton`; PRIMARY (ação principal), SECONDARY (edição),
  GHOST (navegação), DANGER (destrutivo); `selected` marca estado atual e adiciona barra de acento.
- `LcScrollList`: alias de `QuillScrollList`; linhas com hover/seleção, scrollbar fina.
- `LcTheme`/`LcRender`/`LcTooltips`: aliases de `QuillTheme`/`QuillRender`/`QuillTooltips`.
- Layout novo deve usar `QuillColumn`, `QuillRow` e `QuillGrid`; não criar posições mágicas.
- Cards de status e listas devem usar as mesmas margens de 6/8/12 px.

## Painel Admin como referência

- Tabs: Salário, Cobrança, Expediente, Área/Casa, Mercado, Sistema.
- Cards de duas colunas para configurações; ações no rodapé.
- O painel não muda estado diretamente: botão → comando → servidor → snapshot → refresh.

## Próximas aplicações

1. Hiring: stat cards + lista com cards de oferta.
2. Employee: perfil em card, abas, ações no rodapé.
3. Station: lista/card do posto + mapa/armazenamento consistentes.
4. Profile: stat cards, filtro como chip e linhas com dica visual.
5. Config: linguagem em cards, botão de voltar/close no padrão.

## Dependência do cliente

- `quill-ui-0.2.0-alpha.2.jar` vai em `mods/` ao lado do LaborCraft e do Fabric API.
- O `fabric.mod.json` do LaborCraft **sugere** `quillui`; não há dependência dura para não quebrar
  servidores dedicados, que não carregam classes de tela.
- Se o jar do Quill UI estiver ausente no cliente, as telas falham ao abrir; não há fallback visual.

## Regra de ouro

Se uma cor, margem ou altura for usada em mais de uma tela, ela vira token/componente — não
literal repetido. Componentes reutilizáveis vão para o Quill UI, não para o pacote do LaborCraft.
