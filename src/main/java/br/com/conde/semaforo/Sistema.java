package br.com.conde.semaforo;

import java.util.ArrayList;
import java.util.List;

/**
 * A configuracao medida: quantos produtores, quantos consumidores, que tamanho
 * tem o buffer, qual versao do protocolo e como o {@code V} escolhe quem acordar.
 *
 * <p>Os processos sao numerados com os produtores primeiro. Cada um roda o seu
 * laco para sempre: o contador de programa volta a zero no fim.
 */
public record Sistema(int produtores, int consumidores, int buffer,
                      Variante variante, Semaforos.Disciplina disciplina) {

    public Sistema {
        if (produtores < 1 || consumidores < 1) {
            throw new IllegalArgumentException("precisa de pelo menos um de cada");
        }
        if (buffer < 1) {
            throw new IllegalArgumentException("buffer precisa de pelo menos um lugar");
        }
    }

    public static Sistema de(int produtores, int consumidores, int buffer, Variante variante) {
        return new Sistema(produtores, consumidores, buffer, variante, Semaforos.Disciplina.FIFO);
    }

    public Sistema com(Semaforos.Disciplina outra) {
        return new Sistema(produtores, consumidores, buffer, variante, outra);
    }

    public Sistema com(Variante outra) {
        return new Sistema(produtores, consumidores, buffer, outra, disciplina);
    }

    public int processos() {
        return produtores + consumidores;
    }

    public boolean ehProdutor(int processo) {
        return processo < produtores;
    }

    public List<Passo> programa(int processo) {
        return ehProdutor(processo) ? variante.produtor() : variante.consumidor();
    }

    public Passo passoAtual(Estado estado, int processo) {
        return programa(processo).get(estado.pc(processo));
    }

    /**
     * O processo esta dentro da secao critica?
     *
     * <p>Ele entra quando executa {@code deposita} ou {@code retira} e so sai no
     * passo {@code sai}. Enquanto o contador de programa aponta para o {@code sai},
     * ele ainda esta la dentro: e essa janela de um passo que o explorador procura
     * ver ocupada por dois processos ao mesmo tempo.
     */
    public boolean dentroDaSecao(Estado estado, int processo) {
        List<Passo> codigo = programa(processo);
        int pc = estado.pc(processo);
        return pc < codigo.size() && codigo.get(pc).tipo() == Passo.Tipo.SAI;
    }

    public Estado inicial() {
        return Estado.inicial(processos(), buffer);
    }

    /** Os nomes dos processos, para o traco do contraexemplo ficar legivel. */
    public List<String> nomes() {
        List<String> nomes = new ArrayList<>();
        for (int i = 0; i < produtores; i++) {
            nomes.add("produtor" + (i + 1));
        }
        for (int i = 0; i < consumidores; i++) {
            nomes.add("consumidor" + (i + 1));
        }
        return nomes;
    }

    @Override
    public String toString() {
        return produtores + "p x " + consumidores + "c, buffer " + buffer
                + ", " + variante.rotulo() + ", fila " + disciplina;
    }
}
