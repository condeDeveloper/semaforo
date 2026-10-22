package br.com.conde.semaforo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Varre todos os estados alcancaveis e responde as perguntas de seguranca.
 *
 * <p>Tres perguntas, uma varredura: existe estado em que ninguem pode andar
 * (impasse), existe estado com dois processos dentro da secao critica (exclusao
 * quebrada) e existe estado com o buffer fora da faixa que ele deveria respeitar.
 *
 * <p>Toda resposta "sim" vem com o caminho exato desde o inicio. Afirmar que um
 * protocolo quebra sem mostrar a ordem de passos que quebra e opiniao; com o
 * caminho, qualquer um confere na mao.
 */
public final class Explorador {

    private final Sistema sistema;
    private final Maquina maquina;
    private final int teto;

    public Explorador(Sistema sistema) {
        this(sistema, 400_000);
    }

    public Explorador(Sistema sistema, int teto) {
        this.sistema = sistema;
        this.maquina = new Maquina(sistema);
        this.teto = teto;
    }

    /** O grafo inteiro mais as respostas. */
    public record Resultado(Sistema sistema, int estados, int transicoes, int maximoDeItens,
                            List<String> impasse, List<String> exclusaoQuebrada,
                            List<String> bufferQuebrado, Map<Estado, List<Maquina.Movimento>> grafo,
                            Estado inicial) {

        public boolean temImpasse() {
            return impasse != null;
        }

        public boolean exclusaoVale() {
            return exclusaoQuebrada == null;
        }

        public boolean bufferVale() {
            return bufferQuebrado == null;
        }
    }

    public Resultado explorar() {
        Estado inicial = sistema.inicial();
        Map<Estado, List<Maquina.Movimento>> grafo = new HashMap<>();
        Map<Estado, Estado> pai = new HashMap<>();
        Map<Estado, String> comoCheguei = new HashMap<>();

        Deque<Estado> aFazer = new ArrayDeque<>();
        aFazer.add(inicial);
        grafo.put(inicial, null);
        pai.put(inicial, null);

        int transicoes = 0;
        int maximoDeItens = 0;
        List<String> impasse = null;
        List<String> exclusao = null;
        List<String> buffer = null;

        while (!aFazer.isEmpty()) {
            Estado atual = aFazer.poll();
            List<Maquina.Movimento> daqui = maquina.movimentos(atual);
            grafo.put(atual, daqui);
            transicoes += daqui.size();
            maximoDeItens = Math.max(maximoDeItens, atual.itens());

            if (daqui.isEmpty() && impasse == null) {
                impasse = traco(atual, pai, comoCheguei, "ninguem pode andar");
            }
            if (exclusao == null && quantosDentro(atual) > 1) {
                exclusao = traco(atual, pai, comoCheguei,
                        quantosDentro(atual) + " processos dentro da secao ao mesmo tempo");
            }
            if (buffer == null && (atual.itens() < 0 || atual.itens() > sistema.buffer())) {
                buffer = traco(atual, pai, comoCheguei, "buffer com " + atual.itens()
                        + " itens, fora da faixa 0.." + sistema.buffer());
            }

            for (Maquina.Movimento movimento : daqui) {
                Estado destino = movimento.destino();
                if (!grafo.containsKey(destino)) {
                    if (grafo.size() >= teto) {
                        throw new IllegalStateException("passou do teto de " + teto + " estados");
                    }
                    grafo.put(destino, null);
                    pai.put(destino, atual);
                    comoCheguei.put(destino, sistema.nomes().get(movimento.processo())
                            + ": " + movimento.passo().rotulo());
                    aFazer.add(destino);
                }
            }
        }

        return new Resultado(sistema, grafo.size(), transicoes, maximoDeItens,
                impasse, exclusao, buffer, grafo, inicial);
    }

    private int quantosDentro(Estado estado) {
        int dentro = 0;
        for (int processo = 0; processo < sistema.processos(); processo++) {
            if (sistema.dentroDaSecao(estado, processo)) {
                dentro++;
            }
        }
        return dentro;
    }

    private List<String> traco(Estado alvo, Map<Estado, Estado> pai,
                               Map<Estado, String> comoCheguei, String veredito) {
        List<String> caminho = new ArrayList<>();
        Estado passo = alvo;
        while (pai.get(passo) != null) {
            caminho.add(0, comoCheguei.get(passo));
            passo = pai.get(passo);
        }
        caminho.add("=> " + veredito);
        return caminho;
    }
}
