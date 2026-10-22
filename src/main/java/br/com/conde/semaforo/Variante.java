package br.com.conde.semaforo;

import java.util.List;

/**
 * As quatro versoes do produtor-consumidor que valem a pena medir.
 *
 * <p>Tres delas sao erros que qualquer um escreve sem perceber, e o ponto do
 * repositorio e que o grafo de estados separa o que acontece em cada um: duas
 * quebram coisas diferentes e uma nao quebra nada.
 */
public enum Variante {

    /** O livro: desce o contador de recurso primeiro, a trava depois. */
    CERTO("certo"),

    /**
     * A trava primeiro, o recurso depois. Parece inofensivo e e o impasse classico:
     * quem segura o mutex vai dormir esperando lugar no buffer, e quem liberaria
     * lugar nao consegue entrar para liberar.
     */
    MUTEX_PRIMEIRO("mutex primeiro"),

    /**
     * Sem trava nenhuma, confiando que os contadores ja ordenam tudo. Eles ordenam
     * a quantidade, nao o acesso: dois produtores podem estar dentro ao mesmo tempo.
     */
    SEM_MUTEX("sem mutex"),

    /**
     * Sobe o contador de recurso antes de soltar a trava. Parece tao errado quanto
     * o {@link #MUTEX_PRIMEIRO}, e nao e: a medida mostra que nao quebra nada.
     */
    V_TROCADO("V trocado");

    private final String rotulo;

    Variante(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** O laco do produtor nesta variante. */
    public List<Passo> produtor() {
        return switch (this) {
            case CERTO -> List.of(
                    Passo.p(Semaforos.VAZIOS),
                    Passo.p(Semaforos.MUTEX),
                    Passo.deposita(),
                    Passo.sai(),
                    Passo.v(Semaforos.MUTEX),
                    Passo.v(Semaforos.CHEIOS));
            case MUTEX_PRIMEIRO -> List.of(
                    Passo.p(Semaforos.MUTEX),
                    Passo.p(Semaforos.VAZIOS),
                    Passo.deposita(),
                    Passo.sai(),
                    Passo.v(Semaforos.MUTEX),
                    Passo.v(Semaforos.CHEIOS));
            case SEM_MUTEX -> List.of(
                    Passo.p(Semaforos.VAZIOS),
                    Passo.deposita(),
                    Passo.sai(),
                    Passo.v(Semaforos.CHEIOS));
            case V_TROCADO -> List.of(
                    Passo.p(Semaforos.VAZIOS),
                    Passo.p(Semaforos.MUTEX),
                    Passo.deposita(),
                    Passo.sai(),
                    Passo.v(Semaforos.CHEIOS),
                    Passo.v(Semaforos.MUTEX));
        };
    }

    /** O laco do consumidor nesta variante. */
    public List<Passo> consumidor() {
        return switch (this) {
            case CERTO -> List.of(
                    Passo.p(Semaforos.CHEIOS),
                    Passo.p(Semaforos.MUTEX),
                    Passo.retira(),
                    Passo.sai(),
                    Passo.v(Semaforos.MUTEX),
                    Passo.v(Semaforos.VAZIOS));
            case MUTEX_PRIMEIRO -> List.of(
                    Passo.p(Semaforos.MUTEX),
                    Passo.p(Semaforos.CHEIOS),
                    Passo.retira(),
                    Passo.sai(),
                    Passo.v(Semaforos.MUTEX),
                    Passo.v(Semaforos.VAZIOS));
            case SEM_MUTEX -> List.of(
                    Passo.p(Semaforos.CHEIOS),
                    Passo.retira(),
                    Passo.sai(),
                    Passo.v(Semaforos.VAZIOS));
            case V_TROCADO -> List.of(
                    Passo.p(Semaforos.CHEIOS),
                    Passo.p(Semaforos.MUTEX),
                    Passo.retira(),
                    Passo.sai(),
                    Passo.v(Semaforos.VAZIOS),
                    Passo.v(Semaforos.MUTEX));
        };
    }
}
