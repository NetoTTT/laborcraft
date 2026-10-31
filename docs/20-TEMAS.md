# Temas da interface

O jogador escolhe o tema no **Perfil**, no card **Tema**. A escolha é do cliente e fica salva em
`config/laborcraft-ui.json`; não é enviada ao servidor.

## Temas disponíveis

| id | Nome | Estilo |
| --- | --- | --- |
| `labor` | Labor | Âmbar escuro padrão. |
| `glass` | Vidro | Painéis translúcidos, bordas claras e destaque ciano (liquid glass). |
| `flat` | Chapado | Cores sólidas, contraste alto, bordas retas e espaçamento curto. |
| `light` | Claro | Fundo claro, texto escuro e destaque âmbar. |
| `forest` | Floresta | Verde profundo com destaque esmeralda. |
| `amethyst` | Ametista | Violeta profundo com destaque ametista. |

O tema **Vidro** usa transparência de verdade (alpha nos roles de painel/superfície). Minecraft não
tem blur de tela, então o efeito é translucidez + brilho frio, não um desfoque real. O tema
**Claro** mantém o `SCREEN_BG` transparente e o painel levemente translúcido, para o mundo aparecer
ao redor como nos outros temas.

Cores de UI são sempre declaradas com `QuillColors.rgb(...)` ou `QuillColors.argb(...)`, nunca com
literais hexadecimais.

## Onde fica

- Paletas: `src/client/java/com/foque/laborcraft/client/ui/theme/LaborCraftTheme.java`
- Persistência: `config/laborcraft-ui.json` (`{"theme": "glass"}`)
- Seleção: `LaborCraftProfileScreen.themeCard()`
- Aplicação: todas as telas passam `LaborCraftTheme.currentData()` para o `QuillScreen`; widgets
  criados no `buildContent` herdam o tema via `QuillThemeScope`.
- HUDs e o grid de itens leem `LaborCraftTheme.currentData()` diretamente.

## Adicionar um tema

1. Adicione uma entrada no enum `LaborCraftTheme` com id e `QuillThemeData`.
2. Monte a paleta com `QuillThemeData.builder().set(role, cor)...build()`.
3. Adicione `laborcraft.ui.theme.<id>` e `laborcraft.ui.theme.<id>.tip` em `en_us.json` e
   `pt_br.json`.
4. Se o tema mudar espaçamentos, passe `QuillThemeMetrics` no builder.

Cores semânticas continuam fora da paleta: zonas do mapa (âmbar/azul), barra de durabilidade e
destaques no mundo.
