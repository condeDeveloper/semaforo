# semaforo

Semáforos de Dijkstra escritos do zero em Java 21, e o produtor-consumidor
verificado pela varredura de **todos os entrelaçamentos possíveis**.

Não há uma `Thread` neste repositório. Cada linha do protocolo vira um passo
atômico numerado, o estado global inteiro cabe num objeto imutável e a execução
vira um grafo: rodar o programa é caminhar nesse grafo, e *testar* o programa é
visitar o grafo inteiro. É a diferença entre "não deu problema nas mil vezes que
rodei" e "não existe ordem de passos que dê problema".

```
mvn -B -ntp verify
mvn -B -ntp exec:java -Dexec.args="protocolos"   # (ou filas, buffer, mutex)
```

## O que é medido

Quatro versões do mesmo produtor-consumidor, três delas erradas de um jeito que
qualquer um escreve sem perceber:

| versão | o que muda |
|---|---|
| **certo** | `P(vazios)` e depois `P(mutex)`: o recurso antes da trava |
| **mutex primeiro** | a trava antes do recurso |
| **sem mutex** | só os contadores `vazios` e `cheios`, sem trava nenhuma |
| **V trocado** | sobe `cheios` antes de soltar o `mutex` |

E três perguntas respondidas por varredura, não por amostra: existe estado em que
**ninguém pode andar** (impasse), existe estado com **dois processos dentro da
seção crítica** (exclusão quebrada) e existe estado com o **buffer fora da faixa**.
Mais uma quarta, que não é sobre estado nenhum e sim sobre ciclo: existe uma volta
que o sistema pode repetir para sempre, trabalhando, com um processo específico
dormindo (**inanição**).

## Protocolos

2 produtores, 2 consumidores, buffer de 2 lugares, fila FIFO:

| versão | estados | arestas | impasse | exclusão | buffer | máximo |
|---|---:|---:|---|---|---|---:|
| certo | 723 | 1.968 | não | ok | ok | 2 |
| mutex primeiro | 1.065 | 2.336 | **SIM** | ok | ok | 2 |
| sem mutex | 329 | 968 | não | **QUEBRA** | ok | 2 |
| V trocado | 665 | 1.740 | não | ok | ok | 2 |

O impasse do `mutex primeiro` sai em cinco passos, e o caminho é este:

```
consumidor1: P(mutex)
produtor1:   P(mutex)
produtor2:   P(mutex)
consumidor1: P(cheios)
consumidor2: P(mutex)
=> ninguem pode andar
```

O consumidor pegou a trava e foi dormir esperando item. Quem produziria o item
precisa da trava para entrar. Ninguém acorda ninguém.

## O mutex só faz falta a partir de dois lugares

Esta é a tabela que mudou o que eu achava do assunto. Protocolo **sem mutex**:

| sistema | estados | exclusão | passos até quebrar |
|---|---:|---|---:|
| 1p x 1c, buffer 1 | 16 | ok | - |
| 1p x 1c, buffer 2 | 32 | **QUEBRA** | 8 |
| 1p x 1c, buffer 3 | 48 | **QUEBRA** | 8 |
| 2p x 1c, buffer 1 | 46 | ok | - |
| 2p x 1c, buffer 2 | 104 | **QUEBRA** | 4 |
| 2p x 2c, buffer 1 | 130 | ok | - |
| 2p x 2c, buffer 2 | 329 | **QUEBRA** | 4 |
| 2p x 2c, buffer 3 | 576 | **QUEBRA** | 4 |

**Com um lugar só no buffer, tirar a trava não quebra nada**, nem com dois
produtores e dois consumidores: o contador `vazios` vale 1 e já funciona como a
própria trava. O defeito nasce no segundo lugar do buffer, e aí basta um produtor
e um consumidor para dois processos ficarem dentro ao mesmo tempo.

E, nos dois casos, **o buffer continua dentro da faixa**: nunca passa do tamanho,
nunca fica negativo. O que tirar o mutex quebra não é a contagem, é o acesso. Os
contadores ordenam *quantos*, não *quem*.

## Quem o V acorda decide quem espera para sempre

Dijkstra não fixou qual processo o `V` tira da fila, e a escolha não muda nada do
que o programa calcula. Muda quem passa fome. Protocolo certo, 3 produtores, 1
consumidor, buffer 1:

| disciplina | estados | arestas | inanição | quem passa fome |
|---|---:|---:|---|---|
| FIFO | 248 | 538 | não | - |
| livre | 248 | 556 | **SIM** | produtor1, produtor2, produtor3 |

**Os estados alcançáveis são exatamente os mesmos.** O que a escolha livre
acrescenta são 18 arestas, 3,3% a mais, e é só disso que a inanição precisa: uma
volta de 70 estados que o sistema repete para sempre, produzindo e consumindo,
com um produtor dormindo na fila de `vazios` e nunca sendo o escolhido.

A inanição aqui tem uma condição de corte: o ciclo só conta se for **justo**, ou
seja, se todo processo que em algum ponto da volta *podia* andar andou pelo menos
uma vez. Sem essa exigência qualquer protocolo "tem inanição", porque sempre dá
para imaginar um escalonador que ignora alguém de propósito. O que a fila livre
permite é outra coisa: todo mundo anda, o sistema progride, e mesmo assim existe
um processo que nunca é a vez dele.

## O buffer

Protocolo certo, fila FIFO:

| sistema | estados | arestas | máximo de itens | limite |
|---|---:|---:|---:|---:|
| 1p x 1c, buffer 1 | 24 | 36 | 1 | 1 |
| 1p x 1c, buffer 2 | 57 | 96 | 2 | 2 |
| 1p x 1c, buffer 3 | 90 | 156 | 3 | 3 |
| 2p x 1c, buffer 2 | 210 | 471 | 2 | 2 |
| 2p x 2c, buffer 1 | 210 | 488 | 1 | 1 |
| 2p x 2c, buffer 2 | 723 | 1.968 | 2 | 2 |
| 2p x 2c, buffer 3 | 1.572 | 4.560 | 3 | 3 |

O máximo bate com o limite em toda linha: o contador não desperdiça lugar nem
deixa passar. Com um produtor e um consumidor o grafo cresce **33 estados por
lugar** do buffer, exatamente, e isso também está preso num teste.

## O V trocado não é defeito

Subir `cheios` antes de soltar o `mutex` parece tão errado quanto trocar a ordem
dos `P`, e não é: não trava, não quebra exclusão, não estoura o buffer. Ainda
tem **58 estados a menos** que a versão do livro, porque acordar o consumidor
antes de soltar a trava corta caminhos em que ele tentaria entrar e dormiria de
novo. Simetria aparente não é simetria: trocar a ordem dos `P` é fatal e trocar a
ordem dos `V` é inofensivo, porque `P` pode bloquear e `V` nunca bloqueia.

## O que as medidas me corrigiram

Quatro coisas que eu escrevi antes de medir, e que o teste derrubou:

1. **"Sem mutex, um produtor e um consumidor não quebram nada."** Quebram, em oito
   passos. O que decide não é a quantidade de processos, é o **tamanho do buffer**:
   com um lugar não quebra nem com quatro processos, com dois lugares quebra com
   dois. Eu estava olhando para o lado errado do problema.
2. **"O impasse do menor sistema tem 18 estados."** Tem 32. Chutar número de
   estado de cabeça não funciona: o que cresce o grafo não é o programa, é a
   combinação dos contadores de programa com as filas.
3. **"Inanição precisa de três concorrentes."** Bastam **dois na mesma fila**. E o
   espelho disso é mais interessante: com um produtor e um consumidor não existe
   inanição em buffer nenhum, porque ninguém disputa fila com ninguém. Quem passa
   fome é só quem divide fila: dois produtores não fazem o consumidor esperar.
4. **"Onde há impasse não há inanição, o sistema para antes."** O mesmo grafo tem
   os dois. São defeitos independentes: uma parte do grafo trava e outra parte
   gira para sempre deixando alguém de fora.

## Como está organizado

- `Passo` e `Semaforos`: a instrução atômica e os três semáforos.
- `Variante`: as quatro versões do protocolo, como listas de passos.
- `Estado`: o estado global inteiro, imutável, com igualdade por valor.
- `Maquina`: a função de transição. `P` que não passa dorme na fila e **não**
  avança o contador de programa: quem avança é o `V`, na hora de acordar.
- `Explorador`: a varredura, com o caminho exato de todo defeito encontrado.
- `Inanicao`: componentes fortemente conexos (Tarjan iterativo) no subgrafo em
  que o faminto está bloqueado, mais o teste de justiça.
- `Medidor`: as quatro tabelas acima. Todas rodam no CI, nos três sistemas.

70 testes.
