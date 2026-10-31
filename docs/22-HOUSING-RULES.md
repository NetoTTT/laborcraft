# Regras de casa

A validação de casa agora tem duas camadas: medição e regras.

## Medição — `HousingScan`

`HousingValidator.scan(...)` faz só o trabalho físico:

- flood fill de 6 direções a partir do ar acima da cama;
- detecção de céu aberto, volume máximo e raio máximo;
- bounds do piso e da altura da cabeça;
- portas, camas canônicas, blocos de conforto e fonte de luz;
- motivo de falha de medição (`not_a_bed`, `no_space`, `open_sky`, `too_large`).

O resultado é imutável e não depende de config de regra.

## Regras — `HousingRule`

Cada exigência é uma regra independente:

```java
HousingRules.register(new HousingRule() {
    public String reason() { return "no_bed"; }

    public boolean passes(HousingScan scan, LaborCraftConfig config) {
        return !scan.beds().isEmpty();
    }
});
```

As regras atuais, nesta ordem:

1. `too_small`
2. `no_door`
3. `no_light`

A primeira que falha vira o `failureReason` legado; todas aparecem em
`HousingReport.rules()` para UIs futuras.

## Relatório — `HousingReport`

Continua com os mesmos acessores (`valid`, `width`, `depth`, `doors`, `beds`, `capacity`,
`comfortBlocks`, `comfortBonus`, `failureReason`, `min`, `max`, `bedList`) e ganhou:

```java
List<HousingRuleResult> rules();
```

O construtor antigo (sem `rules`) continua existindo para compatibilidade.

## Adicionar uma validação

1. Crie a regra e registre em `HousingRules.register(...)`.
2. Adicione a tradução `laborcraft.housing.reason.<reason>` em `en_us.json` e `pt_br.json`.
3. Se a regra precisar de um dado novo do mundo, adicione o campo em `HousingScan` (a única parte
   que conhece o `ServerLevel`).

Exemplos futuros: telhado por material, janelas, altura mínima, cômodos separados, workspace,
armazenamento, caminho até a porta, cama acessível, casa por emprego.
