package br.com.conde.semaforo;

import java.util.ArrayList;
import java.util.List;

/**
 * A funcao de transicao: dado um estado, quem pode andar e para onde.
 *
 * <p>Nao ha relogio, nao ha escalonador e nao ha preferencia. Se dois processos
 * podem executar o proximo passo, o explorador segue os dois caminhos. E por isso
 * que o resultado vale para qualquer escalonador real: nenhum deles faz nada que
 * nao esteja aqui dentro.
 */
public final class Maquina {

    private final Sistema sistema;

    public Maquina(Sistema sistema) {
        this.sistema = sistema;
    }

    public Sistema sistema() {
        return sistema;
    }

    /** Um movimento possivel: quem andou, que passo executou e no que deu. */
    public record Movimento(int processo, Passo passo, Estado destino) {
    }

    /**
     * Todos os movimentos possiveis a partir de um estado.
     *
     * <p>Lista vazia quer dizer que ninguem pode andar: ou todo mundo esta
     * bloqueado em algum semaforo, que e o impasse, ou nao ha processo nenhum.
     */
    public List<Movimento> movimentos(Estado estado) {
        List<Movimento> saida = new ArrayList<>();
        for (int processo = 0; processo < sistema.processos(); processo++) {
            if (estado.bloqueado(processo)) {
                continue;
            }
            Passo passo = sistema.passoAtual(estado, processo);
            switch (passo.tipo()) {
                case P -> saida.add(new Movimento(processo, passo, descer(estado, processo, passo.semaforo())));
                case V -> subir(estado, processo, passo.semaforo(), passo, saida);
                case DEPOSITA -> saida.add(new Movimento(processo, passo,
                        avancar(estado.comItens(estado.itens() + 1), processo)));
                case RETIRA -> saida.add(new Movimento(processo, passo,
                        avancar(estado.comItens(estado.itens() - 1), processo)));
                case SAI -> saida.add(new Movimento(processo, passo, avancar(estado, processo)));
            }
        }
        return saida;
    }

    /** Executa o passo de um processo escolhido, quando ele e o unico que interessa. */
    public Estado aplicar(Estado estado, int processo) {
        for (Movimento movimento : movimentos(estado)) {
            if (movimento.processo() == processo) {
                return movimento.destino();
            }
        }
        throw new IllegalStateException("processo " + processo + " nao pode andar em " + estado);
    }

    private Estado descer(Estado estado, int processo, int semaforo) {
        if (estado.valor(semaforo) > 0) {
            return avancar(estado.comValor(semaforo, estado.valor(semaforo) - 1), processo);
        }
        // Dorme na fila. O contador de programa NAO avanca aqui: quem avanca e o V,
        // na hora de acordar. E isso que faz o P ser indivisivel do ponto de vista
        // de quem espera: ou ele passou, ou ele esta dormindo no proprio P.
        return estado.entrarNaFila(processo, semaforo);
    }

    private void subir(Estado estado, int processo, int semaforo, Passo passo, List<Movimento> saida) {
        int[] fila = estado.fila(semaforo);
        if (fila.length == 0) {
            saida.add(new Movimento(processo, passo,
                    avancar(estado.comValor(semaforo, estado.valor(semaforo) + 1), processo)));
            return;
        }
        int[] candidatos = sistema.disciplina() == Semaforos.Disciplina.FIFO
                ? new int[]{fila[0]}
                : fila;
        for (int acordado : candidatos) {
            Estado destino = estado.sairDaFila(acordado, semaforo);
            destino = avancar(destino, acordado);
            destino = avancar(destino, processo);
            saida.add(new Movimento(processo, passo, destino));
        }
    }

    private Estado avancar(Estado estado, int processo) {
        int tamanho = sistema.programa(processo).size();
        return estado.comPc(processo, (estado.pc(processo) + 1) % tamanho);
    }
}
