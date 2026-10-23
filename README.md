# LaborCraft

> Nome provisório (codinome). O nome final, o `modid` e o `group` ainda podem mudar.

Mod Fabric para Minecraft **26.3** de automação de trabalho: você **contrata aldeões** (e, no futuro, outros NPCs) para executar empregos repetitivos — colher e replantar plantações, usar composteira e farinha de osso, guardar em baús — e **paga um salário** por dia, semana ou mês.

A ideia central: o trabalhador não é uma máquina burra, é um funcionário. Existe contrato, existe salário, existe risco de não receber e parar de trabalhar.

## Premissa

1. **Trabalho, não magia.** O NPC trabalha dentro de uma área de serviço definida pelo dono, seguindo regras de colheita/replantio/depósito como um jogador faria.
2. **Contrato e salário.** Todo trabalhador tem um contrato com período de cobrança (diário/semanal/mensal), valor, escalonamento e consequências por falta de pagamento.
3. **Modular por princípio.** O mod não sabe o que é "dinheiro": ele fala com um *provedor de moeda*. Funciona com moeda própria, com scoreboard, com itens (esmeralda) ou com qualquer mod de economia do server/modpack.
4. **Modular por princípio (2).** O mod não sabe só fazer fazenda: empregos são registrados por tipo. Fazendeiro é o primeiro; minerador e outros vêm depois, sem reescrever o núcleo.
5. **Compatibilidade primeiro.** Sem dependência obrigatória de outros mods, sem mixin em código de terceiros se der para evitar, tags para plantações/baús, e tudo que der para ser *data-driven* via datapack.
6. **Servidor manda.** Preços, períodos e regras são decididos pelo dono do servidor ou pelo modpack; o jogador só decide os detalhes que o servidor permitir.

Detalhes completos em [`docs/00-VISAO.md`](docs/00-VISAO.md).

## Status

Esqueleto funcional (M0). Compila e gera `.jar`, com a espinha dorsal já desenhada:

- configuração em JSON (`config/laborcraft.json`),
- registro de provedores de moeda + provedor interno (`config/laborcraft-ledger.json`),
- registro de empregos (fazendeiro),
- bloco `laborcraft:workstation` (Posto de Trabalho) + item + receita de 8 tábuas + baú,
- entidade `laborcraft:worker`, contrato de trabalhador e loja pública de contratação,
- telas de Contratação (loja) e Empregado, com atribuição de emprego e dispensa,
- loja com lista rolável de ofertas (`LcScrollList`) e tela de Perfil do jogador (`/laborcraft profile`),
- contrato com dados do trabalhador (nome, emprego, fome, humor e manutenção); dispensar devolve o contrato em vez de deixar o NPC solto,
- modelo de contrato e período de cobrança,
- modelo do trabalhador: perfil, maestria por emprego, estados e regras base de aquisição/moradia,
- modelo de área (posto + raio por tier + andares), validação de casa e expediente de trabalho,
- docs de arquitetura, empregos, configuração, trabalhador, área/casa/horário, compatibilidade e UI.

Ainda **não** existe: IA de trabalho do fazendeiro, cobrança automática de salário e o perfil completo dentro do contrato. A base de UI, a loja e os comandos administrativos já funcionam (`/laborcraft ui`, `/laborcraft hire`, `/laborcraft profile`, `/laborcraft admin reload|status|set balance`).

## Requisitos

- JDK **25** (Minecraft 26.3 exige Java 25; o build roda o JVM 25).
- Nada de Gradle instalado: o wrapper cuida disso (Gradle 9.8.2, Loom 1.19.3).

## Como compilar

No Windows, use o script (ele acha o JDK 25, roda o wrapper e evita o console "travado"):

```bat
build.cmd build
build.cmd runClient
build.cmd runServer
```

O script redireciona a saída para `build-out.log`, usa `--no-daemon` e imprime no fim. Isso é necessário porque o **daemon do Gradle herda o stdout**, então um pipe direto (`gradlew.bat build | ...`) nunca recebe EOF e parece travado para sempre.

Se `JAVA_HOME` não for um JDK 25+, passe explicitamente:

```bat
set VJ_JAVA_HOME=C:\Program Files\Java\jdk-25
build.cmd build
```

Ou direto pelo wrapper, redirecionando você mesmo:

```bat
gradlew.bat build --console=plain --no-daemon > build-out.log 2>&1
```

O `.jar` sai em `build/libs/laborcraft-<versao>.jar`.

## Versões alvo

| Item | Versão |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.164.0+26.3 |
| Loom | 1.19.3 |
| Gradle | 9.8.2 |
| Java | 25 |

Tudo isso vive em `gradle.properties`. Política de atualização em [`docs/04-ROADMAP.md`](docs/04-ROADMAP.md).

## Estrutura

```
src/main/java/com/foque/laborcraft/
  LaborCraft.java            entrada do mod
  block/                      posto de trabalho (bloco + item)
  config/                     config do servidor/modpack
  economy/                    SPI de moeda + registros + ledger interno
  job/                        tipos de emprego e registro
  contract/                   contrato e período de cobrança
  worker/                     perfil do trabalhador, maestria e estados
  area/                       posto, área de serviço e formas
  housing/                    relatório de validação de moradia
  schedule/                   expediente (dia/noite/custom)
  command/                    comandos do servidor (admin, contrato)
src/client/java/com/foque/laborcraft/client/
  ui/                         tema, componentes e telas nativas
docs/                         desenho, decisões e roadmap
```

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [docs/00-VISAO.md](docs/00-VISAO.md) | premissa, o que o mod é e não é, loop de jogo |
| [docs/01-ARQUITETURA.md](docs/01-ARQUITETURA.md) | camadas, SPI de moeda, empregos, contratos, dados |
| [docs/02-EMPREGOS.md](docs/02-EMPREGOS.md) | fazendeiro em detalhe, minerador depois, tiers |
| [docs/03-CONFIGURACAO.md](docs/03-CONFIGURACAO.md) | salário, período, escalonamento, permissões |
| [docs/07-TRABALHADOR.md](docs/07-TRABALHADOR.md) | entidade, ovo/contrato, maestria, moradia, pathfinding |
| [docs/08-AREA-CASA-HORARIO.md](docs/08-AREA-CASA-HORARIO.md) | posto, área por tier, validação de casa, andares, expediente |
| [docs/09-UI.md](docs/09-UI.md) | API de GUI do 26.3, componentes e regra command-first |
| [docs/10-CONTRATACAO-E-PERFIL.md](docs/10-CONTRATACAO-E-PERFIL.md) | ofertas roláveis, limite, dispensa com contrato e perfil |
| [docs/04-ROADMAP.md](docs/04-ROADMAP.md) | marcos M0–M5 e política de portabilidade |
| [docs/05-DECISOES.md](docs/05-DECISOES.md) | decisões abertas e ADRs |
| [docs/06-COMPATIBILIDADE.md](docs/06-COMPATIBILIDADE.md) | moedas, tags, datapacks, mods de terceiros |

## Licença

CC0-1.0 (herdada do template do Fabric). Pode ser trocada antes da primeira release pública.
