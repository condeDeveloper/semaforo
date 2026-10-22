package br.com.conde.semaforo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Procura inanicao: um processo que pode esperar para sempre sem que o sistema
 * trave.
 *
 * <p>Impasse e todo mundo parado; inanicao e o sistema andando bem, trabalho
 * sendo feito, e um processo especifico que nunca e a vez dele. Por isso a
 * pergunta nao e sobre estado nenhum: e sobre <b>ciclo</b>. Existe uma volta no
 * grafo que o sistema pode repetir para sempre com o processo X dormindo?
 *
 * <p>A volta so conta se for <b>justa</b>. Sem essa exigencia qualquer protocolo
 * "tem inanicao", porque sempre da para imaginar um escalonador que ignora um
 * processo de proposito. A regra usada aqui: dentro do ciclo, todo processo que
 * em algum momento <i>podia</i> andar precisa ter andado pelo menos uma vez. Um
 * processo bloqueado na fila de um semaforo nao conta como podendo andar, e e
 * exatamente essa brecha que a inanicao de verdade usa.
 */
public final class Inanicao {

    private final Sistema sistema;
    private final Explorador.Resultado resultado;

    public Inanicao(Sistema sistema, Explorador.Resultado resultado) {
        this.sistema = sistema;
        this.resultado = resultado;
    }

    /** O achado: quem passa fome e em que volta do grafo. */
    public record Achado(int processo, String nome, List<String> ciclo) {
    }

    /** O primeiro processo que consegue passar fome, ou vazio se nenhum consegue. */
    public List<Achado> procurar() {
        List<Achado> achados = new ArrayList<>();
        for (int processo = 0; processo < sistema.processos(); processo++) {
            Achado achado = procurarPara(processo);
            if (achado != null) {
                achados.add(achado);
            }
        }
        return achados;
    }

    private Achado procurarPara(int faminto) {
        Set<Estado> permitidos = new HashSet<>();
        for (Estado estado : resultado.grafo().keySet()) {
            if (estado.bloqueado(faminto)) {
                permitidos.add(estado);
            }
        }
        if (permitidos.isEmpty()) {
            return null;
        }
        for (List<Estado> componente : componentes(permitidos)) {
            if (temCiclo(componente, permitidos) && justo(componente, permitidos, faminto)) {
                return new Achado(faminto, sistema.nomes().get(faminto), descrever(componente));
            }
        }
        return null;
    }

    /** Componentes fortemente conexos do subgrafo, por Tarjan iterativo. */
    private List<List<Estado>> componentes(Set<Estado> permitidos) {
        Map<Estado, Integer> indice = new HashMap<>();
        Map<Estado, Integer> menor = new HashMap<>();
        Set<Estado> naPilha = new HashSet<>();
        Deque<Estado> pilha = new ArrayDeque<>();
        List<List<Estado>> saida = new ArrayList<>();
        int[] contador = {0};

        for (Estado raiz : permitidos) {
            if (indice.containsKey(raiz)) {
                continue;
            }
            Deque<Estado> caminho = new ArrayDeque<>();
            Deque<Integer> posicao = new ArrayDeque<>();
            caminho.push(raiz);
            posicao.push(0);
            indice.put(raiz, contador[0]);
            menor.put(raiz, contador[0]++);
            pilha.push(raiz);
            naPilha.add(raiz);

            while (!caminho.isEmpty()) {
                Estado atual = caminho.peek();
                List<Estado> vizinhos = vizinhos(atual, permitidos);
                int i = posicao.pop();
                if (i < vizinhos.size()) {
                    posicao.push(i + 1);
                    Estado proximo = vizinhos.get(i);
                    if (!indice.containsKey(proximo)) {
                        indice.put(proximo, contador[0]);
                        menor.put(proximo, contador[0]++);
                        pilha.push(proximo);
                        naPilha.add(proximo);
                        caminho.push(proximo);
                        posicao.push(0);
                    } else if (naPilha.contains(proximo)) {
                        menor.put(atual, Math.min(menor.get(atual), indice.get(proximo)));
                    }
                } else {
                    caminho.pop();
                    if (!caminho.isEmpty()) {
                        Estado anterior = caminho.peek();
                        menor.put(anterior, Math.min(menor.get(anterior), menor.get(atual)));
                    }
                    if (menor.get(atual).equals(indice.get(atual))) {
                        List<Estado> componente = new ArrayList<>();
                        Estado quem;
                        do {
                            quem = pilha.pop();
                            naPilha.remove(quem);
                            componente.add(quem);
                        } while (!quem.equals(atual));
                        saida.add(componente);
                    }
                }
            }
        }
        return saida;
    }

    private List<Estado> vizinhos(Estado estado, Set<Estado> permitidos) {
        List<Estado> saida = new ArrayList<>();
        List<Maquina.Movimento> movimentos = resultado.grafo().get(estado);
        if (movimentos == null) {
            return saida;
        }
        for (Maquina.Movimento movimento : movimentos) {
            if (permitidos.contains(movimento.destino())) {
                saida.add(movimento.destino());
            }
        }
        return saida;
    }

    private boolean temCiclo(List<Estado> componente, Set<Estado> permitidos) {
        if (componente.size() > 1) {
            return true;
        }
        Estado unico = componente.get(0);
        return vizinhos(unico, permitidos).contains(unico);
    }

    /**
     * Dentro do componente, todo processo que em algum estado podia andar andou
     * pelo menos uma vez por uma transicao que fica dentro do componente.
     */
    private boolean justo(List<Estado> componente, Set<Estado> permitidos, int faminto) {
        Set<Estado> dentro = new HashSet<>(componente);
        Set<Integer> podiaAndar = new HashSet<>();
        Set<Integer> andou = new HashSet<>();
        for (Estado estado : componente) {
            for (Maquina.Movimento movimento : resultado.grafo().get(estado)) {
                podiaAndar.add(movimento.processo());
                if (dentro.contains(movimento.destino())) {
                    andou.add(movimento.processo());
                }
            }
        }
        if (podiaAndar.contains(faminto)) {
            return false;
        }
        podiaAndar.removeAll(andou);
        return podiaAndar.isEmpty() && !andou.isEmpty();
    }

    private List<String> descrever(List<Estado> componente) {
        List<String> linhas = new ArrayList<>();
        linhas.add("volta de " + componente.size() + " estados que o sistema pode repetir para sempre");
        int quantos = Math.min(4, componente.size());
        for (int i = 0; i < quantos; i++) {
            linhas.add("  " + componente.get(i));
        }
        if (componente.size() > quantos) {
            linhas.add("  ...");
        }
        return linhas;
    }
}
