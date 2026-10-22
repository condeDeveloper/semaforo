package br.com.conde.semaforo;

import java.util.Arrays;

/**
 * O estado global do sistema, inteiro, num objeto imutavel.
 *
 * <p>Esta classe e o motivo de o repositorio nao ter uma thread. Se o estado cabe
 * aqui dentro e a transicao e uma funcao pura, entao "rodar o programa" vira
 * caminhar num grafo, e perguntas que um teste com threads so responde "nao
 * aconteceu desta vez" viram perguntas que a varredura responde para sempre.
 *
 * <p>O que entra: o contador de programa de cada processo, em qual semaforo cada
 * um esta bloqueado (ou -1), o valor de cada semaforo, a fila de cada semaforo e
 * quantos itens o buffer tem. O que NAO entra, de proposito: quantas voltas cada
 * processo ja deu. Sem isso o conjunto de estados e finito e os processos sao
 * lacos infinitos, que e o que eles sao de verdade; com isso o grafo cresceria
 * sem parar e nao daria para procurar ciclo de inanicao.
 */
public final class Estado {

    private final int[] pc;
    private final int[] bloqueadoEm;
    private final int[] valor;
    private final int[][] fila;
    private final int itens;
    private final int cacheHash;

    public Estado(int[] pc, int[] bloqueadoEm, int[] valor, int[][] fila, int itens) {
        this.pc = pc;
        this.bloqueadoEm = bloqueadoEm;
        this.valor = valor;
        this.fila = fila;
        this.itens = itens;
        this.cacheHash = calcularHash();
    }

    /** O estado inicial: todos no comeco do laco, buffer vazio, filas vazias. */
    public static Estado inicial(int processos, int tamanhoDoBuffer) {
        int[] pc = new int[processos];
        int[] bloqueado = new int[processos];
        Arrays.fill(bloqueado, -1);
        int[] valor = new int[Semaforos.QUANTOS];
        valor[Semaforos.MUTEX] = 1;
        valor[Semaforos.VAZIOS] = tamanhoDoBuffer;
        valor[Semaforos.CHEIOS] = 0;
        int[][] fila = new int[Semaforos.QUANTOS][0];
        return new Estado(pc, bloqueado, valor, fila, 0);
    }

    public int pc(int processo) {
        return pc[processo];
    }

    public int bloqueadoEm(int processo) {
        return bloqueadoEm[processo];
    }

    public boolean bloqueado(int processo) {
        return bloqueadoEm[processo] >= 0;
    }

    public int valor(int semaforo) {
        return valor[semaforo];
    }

    public int[] fila(int semaforo) {
        return fila[semaforo].clone();
    }

    public int itens() {
        return itens;
    }

    public int processos() {
        return pc.length;
    }

    public Estado comPc(int processo, int novoPc) {
        int[] outro = pc.clone();
        outro[processo] = novoPc;
        return new Estado(outro, bloqueadoEm, valor, fila, itens);
    }

    public Estado comItens(int novosItens) {
        return new Estado(pc, bloqueadoEm, valor, fila, novosItens);
    }

    public Estado comValor(int semaforo, int novoValor) {
        int[] outro = valor.clone();
        outro[semaforo] = novoValor;
        return new Estado(pc, bloqueadoEm, outro, fila, itens);
    }

    /** Poe o processo na fila do semaforo e marca que ele esta bloqueado la. */
    public Estado entrarNaFila(int processo, int semaforo) {
        int[][] outras = fila.clone();
        int[] atual = fila[semaforo];
        int[] nova = Arrays.copyOf(atual, atual.length + 1);
        nova[atual.length] = processo;
        outras[semaforo] = nova;
        int[] bloqueio = bloqueadoEm.clone();
        bloqueio[processo] = semaforo;
        return new Estado(pc, bloqueio, valor, outras, itens);
    }

    /** Tira o processo da fila do semaforo e desmarca o bloqueio dele. */
    public Estado sairDaFila(int processo, int semaforo) {
        int[] atual = fila[semaforo];
        int[] nova = new int[atual.length - 1];
        int k = 0;
        for (int quem : atual) {
            if (quem != processo) {
                nova[k++] = quem;
            }
        }
        int[][] outras = fila.clone();
        outras[semaforo] = nova;
        int[] bloqueio = bloqueadoEm.clone();
        bloqueio[processo] = -1;
        return new Estado(pc, bloqueio, valor, outras, itens);
    }

    private int calcularHash() {
        int h = Arrays.hashCode(pc);
        h = 31 * h + Arrays.hashCode(bloqueadoEm);
        h = 31 * h + Arrays.hashCode(valor);
        h = 31 * h + Arrays.deepHashCode(fila);
        return 31 * h + itens;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Estado e)) {
            return false;
        }
        return itens == e.itens
                && Arrays.equals(pc, e.pc)
                && Arrays.equals(bloqueadoEm, e.bloqueadoEm)
                && Arrays.equals(valor, e.valor)
                && Arrays.deepEquals(fila, e.fila);
    }

    @Override
    public int hashCode() {
        return cacheHash;
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder("pc=").append(Arrays.toString(pc));
        texto.append(" bloq=").append(Arrays.toString(bloqueadoEm));
        texto.append(" sem=").append(Arrays.toString(valor));
        texto.append(" itens=").append(itens);
        for (int s = 0; s < Semaforos.QUANTOS; s++) {
            if (fila[s].length > 0) {
                texto.append(' ').append(Semaforos.nome(s)).append(Arrays.toString(fila[s]));
            }
        }
        return texto.toString();
    }
}
