<img width="1672" height="941" alt="bw3030-logo" src="https://github.com/user-attachments/assets/e88236af-07bf-41a8-9dbb-f5385e84e51c" />

[![Discord](https://discordapp.com/api/guilds/760851292826107926/widget.png?style=shield)](https://discord.gg/kPaBGwhmjf) [![bStats](https://img.shields.io/bstats/servers/18317)](#)

Desde março de 2023, o desenvolvimento do BedWars2023 começou como um fork do plugin [BedWars1058](https://www.spigotmc.org/resources/bedwars1058-opensource.97320/). O objetivo deste fork é implementar novos recursos e modernizar a base de código.

O plugin é publicado sob a licença open-source GNU GPL 3.0. Você pode ler a licença completa [aqui](https://www.gnu.org/licenses/gpl-3.0.html).

# Descrição
BedWars é um mini-game em que você precisa defender a sua cama e destruir as dos outros.  
Depois que a sua cama é destruída, você não renasce mais.

# Requisitos de sistema
Este software roda em [Spigot](https://www.spigotmc.org/) e NMS.
Forks do Spigot sem código NMS compilado não são suportados.
Os servidores oficialmente suportados são [Spigot](https://www.spigotmc.org/) e [Paper](https://papermc.io/).
É necessário usar Java 11 ou superior para rodar este plugin.

O sistema interno de restauração de mundos é baseado em compactar e descompactar mapas, o que pode ficar
pesado se você ainda usa HDD em 2024 e não tem uma CPU decente. Para um sistema de restauração melhor
e mais rápido, instale o [SlimeWorldManager](https://www.spigotmc.org/resources/slimeworldmanager.69974/) ou o [AdvancedWorldManager](https://www.spigotmc.org/resources/advanced-slimeworldmanager.87209/).
O BedWars2023 vai se integrar a ele e fazer tudo por você.

# Dependências
- Java 11
- Forks do Spigot com NMS

# Instalação básica
- Baixe a release mais recente
- Coloque na pasta plugins
- Reinicie o servidor

# Setups prontos e addons da comunidade

Você encontra uma lista de setups prontos e addons da comunidade [na wiki](https://wiki.tomkeuper.com/docs/BedWars2023/addons)

# Principais recursos

### Flexível | Formas de rodar o plugin:
- **SHARED**: pode rodar junto com outros mini-games na mesma instância do spigot. As partidas só ficam acessíveis por comandos.
- **MULTIARENA**: exige uma instância inteira de servidor para hospedar o mini-game. Protege o mundo do lobby, e as partidas podem ser acessadas por comandos, NPCs, placas e GUIs.
- **BUNGEE-LEGACY**: o antigo modo bungee clássico, em que uma partida ocupa uma instância inteira de servidor. Você é adicionado à partida ao entrar no servidor. O status da arena é exibido como MOTD.
- **BUNGEE**: um modo bungee novo e escalável. Consegue hospedar várias arenas na mesma instância de servidor, clonar e iniciar novas arenas quando necessário para que outros jogadores possam entrar. O servidor pode ser reiniciado automaticamente após uma certa quantidade de partidas jogadas. Isso exige instalar o [BedWarsProxy](https://www.spigotmc.org/resources/bedwarsproxy.66642/) nos seus servidores de lobby para os jogadores conseguirem entrar. E, claro, você pode rodar quantos servidores quiser no modo bungee.

### Idioma | Sistema de idioma por jogador:
- Cada jogador pode receber mensagens, hologramas, GUIs etc. no idioma que preferir. /bw lang.
- Você pode remover ou adicionar novos idiomas.
- Nomes de times, nomes de grupos, conteúdos da loja e muito mais podem ser traduzidos nos seus idiomas.
- Títulos e subtítulos customizados para a [contagem regressiva de início](https://wiki.tomkeuper.com/docs/BedWars2023/configuration/language-configuration#custom-title-sub-title-for-arena-countdown).

### Remoção do lobby | Opcional:
O lobby de espera dentro do mapa pode ser removido assim que a partida começa.

### Grupos de arena | Customização:
- Você pode agrupar arenas por tipo (4v4, 50v50). Pode nomeá-los como quiser.
- Os grupos podem ter layouts de scoreboard, melhorias de time, itens iniciais e configurações de gerador próprios.
- Você pode entrar em mapas por grupo: /bw join Solo, /bw gui Solo.

### Loja | Customização:
- Você pode configurar os itens padrão da compra rápida.
- Você pode adicionar ou remover categorias.
- Você pode adicionar novos itens de loja ou executar comandos na compra.
- Itens permanentes são entregues depois que você renasce.
- Itens permanentes podem ser rebaixáveis, fazendo você perder um nível por morte.
- Itens podem ter peso, então você não consegue comprar um item mais fraco que o atual etc.
- Itens especiais disponíveis: Traça, Defesa dos Sonhos, Ovo das Pontes, TNT Jump e Bola de Fogo Reta.
- A compra rápida está disponível e é sincronizada entre os nós também no modo bungee.

### Melhorias de time | Customização:
- Você pode ter melhorias de time diferentes por grupo de arena.
- Você pode adicionar e remover categorias e conteúdos.
- Você pode criar elementos de melhoria que: encantam itens, dão efeitos de poção (para companheiros de time/ base/ inimigos quando entram na ilha), editam configurações de gerador e mudam a quantidade de dragões da fase de Morte Súbita.
- Você pode adicionar novas armadilhas que: desencantam itens (espada, armadura, arco), dão efeitos de poção (time/ base/ inimigos), removem efeito de poção quando um inimigo entra no alcance da sua ilha e disparam comandos.

### Formas de entrar em uma arena:

- Seletor de arena, que pode ser configurado. /bw gui mostra todos os grupos de arena, enquanto /bw gui Solo mostra as partidas dos grupos Solo e /bw gui Solo+4v4 mostra as partidas dos grupos Solo e 4v4.
- Você também pode entrar em partidas por NPCs, instalando o Citizens.
- Placas de entrada também estão disponíveis, com bloco de status.
- Comandos também podem ser usados. /bw join random te leva para a arena mais cheia, /bw join nomeDoMapa te envia para a arena informada e /bw join nomeDoGrupo+nomeDoGrupo2 te leva para um mapa dos grupos informados.

### Configurações de arena | Customização:
- Você pode definir um nome de exibição customizado, usado em placas, GUIs etc.
- Opção de definir a quantidade mínima/ máxima de jogadores e o tamanho do time.
- Opções de ativar/desativar: permitir espectadores, desativar geradores de times vazios, desativar NPCs de times vazios, desativar o gerenciamento interno de drops, uso de hologramas de cama.
- Alcance de proteção para o spawn do time e para os NPCs do time.
- Raio da ilha (para recursos como disparo de armadilhas e mapa) e raio da borda.
- Morte instantânea no void com base na coordenada Y.
- Você pode criar quantos times quiser.
- Você pode permitir a quebra do mapa, como em uma partida de SkyWars.
- Você pode ativar/desativar a divisão de geradores.
- Regras de jogo customizadas por mapa.
- Geradores ilimitados de ferro/ ouro/ esmeralda (este pode ser ativado pelas melhorias) por time.

### Vip Kick | Privilégio:
Jogadores com a permissão `bw.vip` conseguem entrar em arenas cheias na fase de início. Isso expulsa da partida um jogador sem a permissão `bw.vip`.

### Estatísticas de jogador:
- O plugin não fornece hologramas de ranking, mas você pode usar o ajLeaderboards ou o LeaderHeads para isso, com os placeholders que disponibilizamos.
- Os jogadores podem ver suas estatísticas pela GUI interna de stats, que é customizável e acessada por /bw stats.

### Sistema de party:
- Fornecemos um sistema de party interno, básico e funcional, para jogar com seus amigos no mesmo time ou arena.
- Também suportamos o Parties do AlessioDP e o Party and Friends do Simonsator, que podem ser uma solução melhor se você for uma rede grande.

### Sistema anti-AFK:
Jogadores inativos por mais de 45 segundos não conseguem pegar itens dos geradores.

### Itens de entrada customizados:
- Você pode adicionar e remover os itens que recebe ao entrar no servidor (apenas no multi-arena) e os itens que recebe ao entrar em uma partida na fase de início/ espera ou ao entrar como espectador.
- Os itens de entrada podem executar comandos.

### Sistema de restauração de mapa:
- O adaptador de restauração padrão do BedWars2023 é baseado em descarregar o mapa, descompactar um backup e carregá-lo de novo. Isso pode ser pesado para servidores com hardware fraco. Recomendamos processadores gamer e um SSD.
- Para melhorar o desempenho, adicionamos suporte ao SlimeWorldManager, que carrega os mapas muito mais rápido e com menos impacto de performance graças ao formato slime. Recomendamos fortemente instalar este plugin. Nenhuma conversão manual é necessária. O BedWars2023 cuida de tudo. Veja como instalá-lo aqui.
- Você também pode implementar o seu próprio adaptador de mapa pela API.
- Pode parecer mais pesado que outros plugins porque não acompanhamos apenas os blocos modificados. Precisamos restaurar o mapa inteiro, porque os donos de servidor podem permitir que os jogadores destruam os mapas, como em uma partida de SkyWars. Regiões como geradores, NPCs e spawns de time ficam protegidas.

### Re-Join | Recurso:
Se você for desconectado, ou se sair de uma partida (configurável), pode voltar a ela por comando ou entrando no servidor novamente. Também está disponível no modo bungee escalável.

### TNT Jump | Recurso:
- Os jogadores conseguem fazer tnt jump com valores configuráveis.
- Jogadores com tnt no inventário ficam com uma partícula vermelha na cabeça (configurável).

### Eventos sazonais:
- Especial de Halloween. É ativado automaticamente com base no fuso horário da sua máquina e fornece efeitos legais.

# Bibliotecas de terceiros
- [bStats](https://bstats.org/getting-started/include-metrics)
- [Commons IO](https://mvnrepository.com/artifact/commons-io/commons-io)
- [HikariCP](https://mvnrepository.com/artifact/com.zaxxer/HikariCP)
- [IridiumColorAPI](https://nexus.iridiumdevelopment.net/#browse/browse:maven-releases:com%2Firidium%2FIridiumColorAP)
- [SlimJar](https://github.com/slimjar/slimjar)
- [SLF4J](http://www.slf4j.org/)
- [Flow-NBT](https://github.com/SpongePowered/nbt)
- [Jedis](https://github.com/redis/jedis)
- [CloudNet](https://cloudnetservice.eu/docs/3.3/api/start/)
- [H2](https://www.h2database.com/html/main.html)
- [VipFeatures](https://gitlab.com/andrei1058/VipFeatures)

# Contato
[![Discord Server](https://discordapp.com/api/guilds/760851292826107926/widget.png?style=banner3)](https://discord.gg/kPaBGwhmjf)
