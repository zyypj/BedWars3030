# Contribuindo com o BedWars2023
👍 Antes de mais nada, obrigado por reservar um tempo para contribuir!

A seguir está um conjunto de diretrizes para contribuir com o BedWars2023.
São, na maior parte, diretrizes e não regras. Use o seu bom senso e fique à vontade
para propor mudanças neste documento em um pull request.

### O que eu devo saber antes de começar?
#### O BedWars2023 e a sua estrutura

![Structure](.github/assets/contributing/structure.png)  
Temos um módulo para a API pública chamado `bedwars-api`, onde expomos
algumas partes do mini-game. A mágica acontece no módulo `bedwars-plugin`,
e os que começam com `versionsupport_` são usados para tratar o código
específico de cada versão. Como o BedWars2023 tem suporte a múltiplas versões e não
depende de Reflection, precisamos criar um novo módulo para cada nova versão do MC.
O `versionsupport_common` define o que precisa ser tratado de forma diferente
por cada suporte de versão do MC.

## Como eu posso contribuir?
#### Reportando bugs

Ao [criar um relatório de bug](https://github.com/zyypj/BedWars3030/issues/new), inclua o máximo de detalhes possível.
Preencha o template obrigatório; as informações pedidas nos ajudam a resolver os problemas mais rápido.

> **Nota:** Se você encontrar uma issue **Closed** que pareça ser exatamente o que você está enfrentando, abra uma nova issue e inclua um link para a issue original no corpo da nova.

### Sugerindo melhorias
Sugestões de melhoria são acompanhadas como issues do GitHub, então você precisa
[abrir uma nova issue](https://github.com/zyypj/BedWars3030/issues/new)
e fornecer as seguintes informações:
- Use um título claro e descritivo para a issue, que identifique a sugestão.
- Forneça uma descrição passo a passo da melhoria sugerida, com o máximo de detalhes possível.
- Descreva o comportamento atual e explique qual comportamento você esperava ver no lugar, e por quê.
- Explique por que essa melhoria seria útil.
- Informe qual versão do BedWars você está usando.


### A sua primeira contribuição de código
Não sabe por onde começar a contribuir com o BedWars2023?
Você pode começar olhando estas issues `beginner` e `help-wanted`:
- [Issues para iniciantes](beginner) - issues que devem exigir apenas algumas linhas de código e um ou dois testes.
- [Issues com ajuda desejada](help-wanted) - issues um pouco mais complexas que as de iniciante.
  
  
#### Desenvolvimento local
Antes de começar, certifique-se de ter `git`, `java` e `maven` instalados.
O plugin pode ser desenvolvido localmente clonando este repositório, aplicando as suas
alterações e compilando o jar com `mvn clean install`.


## Pull Requests
Siga todas as instruções do template para que a sua contribuição seja
considerada pelos mantenedores:
1. Copie o template correto para a sua contribuição:
  - 🐛 Está corrigindo um bug? Copie o template [daqui](.github/templates/contributing/bug_fix.md).
  - 📈 Está melhorando a performance? Copie o template [daqui](.github/templates/contributing/performance_improvement.md).
  - 📝 Está atualizando a documentação? Copie o template [daqui](.github/templates/contributing/documentation.md).
  - 💻 Está mudando alguma funcionalidade? Copie o template [daqui](.github/templates/contributing/feature_change.md).
2. Substitua este texto pelo conteúdo do template
3. Preencha todas as seções do template
4. Clique em "Create pull request"

# Notas adicionais

### Labels de issues e pull requests
| Nome da label             | Descrição                                                                                                                    |  
|---------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `enhancement`             | Pedidos de novos recursos.                                                                                                   |
| `bug`                     | Bugs confirmados ou relatos com grande chance de serem bugs.                                                                 |
| `question`                | Perguntas, mais do que relatos de bug ou pedidos de recurso (ex.: como eu faço X).                                           |
| `feedback`                | Feedback geral, mais do que relatos de bug ou pedidos de recurso.                                                            |
| `help-wanted`             | O time agradeceria a ajuda da comunidade para resolver estas issues.                                                         |
| `beginner`                | Issues menos complexas, boas como primeira contribuição para quem quer começar a colaborar.                                  |
| `more-information-needed` | É preciso reunir mais informações sobre estes problemas ou pedidos de recurso (ex.: passos para reproduzir).                  |
| `needs-reproduction`      | Provavelmente bugs, mas que ainda não foram reproduzidos de forma confiável.                                                 |
| `duplicate`               | Issues que são duplicatas de outras, ou seja, já foram reportadas antes.                                                     |
| `wontfix`                 | O time decidiu não corrigir estas issues por enquanto, seja porque funcionam como esperado, seja por algum outro motivo.      |
| `invalid`                 | Issues que não são válidas (ex.: erros do usuário).                                                                          |

### Labels de pull request


| Nome da label      | Descrição                                                                                  |
|--------------------|--------------------------------------------------------------------------------------------|
| `work-in-progress` | Pull requests que ainda estão em andamento; mais mudanças virão.                           |
| `needs-review`     | Pull requests que precisam de revisão de código e aprovação dos mantenedores ou            |
| core team.         |                                                                                            |
| `under-review`     | Pull requests em revisão pelos mantenedores.                                                |
| `requires-changes` | Pull requests que precisam ser atualizados com base nos comentários da revisão e revisados novamente. |
| `needs-testing`    | Pull requests que precisam de teste manual.                                                |
