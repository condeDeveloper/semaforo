package br.com.conde.semaforo;

/**
 * Uma instrucao atomica de um processo.
 *
 * <p>O modelo inteiro depende desta palavra: <b>atomica</b>. Cada passo acontece
 * por inteiro ou nao acontece, e entre dois passos de um processo qualquer outro
 * processo pode executar quantos passos quiser. E so isso que o explorador faz:
 * tentar todas as ordens possiveis de passos.
 *
 * <p>Um {@code P} que nao pode passar nao vira "espera ocupada": o processo entra
 * na fila do semaforo e para de ter transicao, ate que um {@code V} o acorde.
 * Essa e a diferenca entre o semaforo de Dijkstra e uma trava de giro, e ela muda
 * o que o grafo de estados responde sobre inanicao.
 */
public record Passo(Tipo tipo, int semaforo, String rotulo) {

    public enum Tipo {
        /** Desce o semaforo, ou bloqueia o processo na fila dele. */
        P,
        /** Sobe o semaforo, ou acorda alguem da fila dele. */
        V,
        /** Poe um item no buffer. */
        DEPOSITA,
        /** Tira um item do buffer. */
        RETIRA,
        /** Sai da secao critica. Enquanto o processo esta neste passo, ele ainda esta dentro. */
        SAI
    }

    public static Passo p(int semaforo) {
        return new Passo(Tipo.P, semaforo, "P(" + Semaforos.nome(semaforo) + ")");
    }

    public static Passo v(int semaforo) {
        return new Passo(Tipo.V, semaforo, "V(" + Semaforos.nome(semaforo) + ")");
    }

    public static Passo deposita() {
        return new Passo(Tipo.DEPOSITA, -1, "deposita");
    }

    public static Passo retira() {
        return new Passo(Tipo.RETIRA, -1, "retira");
    }

    public static Passo sai() {
        return new Passo(Tipo.SAI, -1, "sai da secao");
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
