package br.com.conde.semaforo;

/**
 * Os tres semaforos do produtor-consumidor, e a disciplina da fila.
 *
 * <p>{@code mutex} comeca em 1 e protege o buffer. {@code vazios} comeca com o
 * tamanho do buffer e conta lugar livre. {@code cheios} comeca em 0 e conta item
 * pronto. Os dois ultimos nao sao travas: sao contadores de recurso, e e por isso
 * que um produtor pode ficar preso em {@code P(vazios)} enquanto o buffer esta
 * cheio sem que ninguem esteja "dentro" de nada.
 */
public final class Semaforos {

    public static final int MUTEX = 0;
    public static final int VAZIOS = 1;
    public static final int CHEIOS = 2;
    public static final int QUANTOS = 3;

    private Semaforos() {
    }

    public static String nome(int semaforo) {
        return switch (semaforo) {
            case MUTEX -> "mutex";
            case VAZIOS -> "vazios";
            case CHEIOS -> "cheios";
            default -> throw new IllegalArgumentException("semaforo " + semaforo);
        };
    }

    /**
     * Quem o {@code V} acorda quando ha mais de um processo na fila.
     *
     * <p>Dijkstra nao fixou isso, e a escolha nao muda nada do que o programa
     * calcula: muda quem espera para sempre. O explorador trata {@link #LIVRE}
     * como ramificacao, ou seja, cada escolha vira um caminho separado do grafo.
     */
    public enum Disciplina {
        /** Acorda o que esta na fila ha mais tempo. Uma escolha so, nada se ramifica. */
        FIFO,
        /** Acorda qualquer um da fila. Cada candidato vira um ramo do grafo. */
        LIVRE
    }
}
