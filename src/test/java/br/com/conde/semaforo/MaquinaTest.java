package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A funcao de transicao")
class MaquinaTest {

    private final Sistema sistema = Sistema.de(2, 2, 2, Variante.CERTO);
    private final Maquina maquina = new Maquina(sistema);

    @Test
    @DisplayName("no estado inicial todo processo pode andar")
    void todosPodemAndar() {
        List<Maquina.Movimento> movimentos = maquina.movimentos(sistema.inicial());

        assertThat(movimentos).hasSize(4);
        assertThat(movimentos).extracting(Maquina.Movimento::processo).containsExactly(0, 1, 2, 3);
    }

    @Test
    @DisplayName("P desce o semaforo quando ha valor sobrando")
    void pDesce() {
        Estado depois = maquina.aplicar(sistema.inicial(), 0);

        assertThat(depois.valor(Semaforos.VAZIOS)).isEqualTo(1);
        assertThat(depois.pc(0)).isEqualTo(1);
        assertThat(depois.bloqueado(0)).isFalse();
    }

    @Test
    @DisplayName("P em semaforo zerado dorme na fila e o contador de programa NAO anda")
    void pDorme() {
        Estado depois = maquina.aplicar(sistema.inicial(), 2);

        assertThat(depois.bloqueadoEm(2)).isEqualTo(Semaforos.CHEIOS);
        assertThat(depois.pc(2)).isZero();
        assertThat(depois.fila(Semaforos.CHEIOS)).containsExactly(2);
        assertThat(maquina.movimentos(depois)).extracting(Maquina.Movimento::processo).doesNotContain(2);
    }

    @Test
    @DisplayName("V com a fila vazia sobe o valor; com fila, nao sobe: passa a vez direto")
    void vPassaOBastao() {
        Estado comFila = maquina.aplicar(sistema.inicial(), 2);
        Estado produtorAndou = comFila;
        for (int i = 0; i < 6; i++) {
            produtorAndou = maquina.aplicar(produtorAndou, 0);
        }

        assertThat(produtorAndou.valor(Semaforos.CHEIOS)).isZero();
        assertThat(produtorAndou.bloqueado(2)).isFalse();
        assertThat(produtorAndou.pc(2)).isEqualTo(1);
        assertThat(produtorAndou.fila(Semaforos.CHEIOS)).isEmpty();
    }

    @Test
    @DisplayName("com fila FIFO o V acorda um so, e e o que chegou primeiro")
    void fifoAcordaUmSo() {
        Sistema fifo = Sistema.de(1, 2, 1, Variante.CERTO);
        Maquina m = new Maquina(fifo);
        Estado estado = fifo.inicial();
        estado = m.aplicar(estado, 1);
        estado = m.aplicar(estado, 2);
        assertThat(estado.fila(Semaforos.CHEIOS)).containsExactly(1, 2);

        for (int i = 0; i < 6; i++) {
            estado = m.aplicar(estado, 0);
        }

        assertThat(estado.bloqueado(1)).isFalse();
        assertThat(estado.bloqueado(2)).isTrue();
    }

    @Test
    @DisplayName("com fila livre o V vira dois caminhos: cada candidato e um ramo do grafo")
    void livreRamifica() {
        Sistema livre = Sistema.de(1, 2, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE);
        Maquina m = new Maquina(livre);
        Estado estado = livre.inicial();
        estado = m.aplicar(estado, 1);
        estado = m.aplicar(estado, 2);
        for (int i = 0; i < 5; i++) {
            estado = m.aplicar(estado, 0);
        }

        List<Maquina.Movimento> doV = m.movimentos(estado).stream()
                .filter(movimento -> movimento.processo() == 0).toList();

        assertThat(doV).hasSize(2);
        assertThat(doV.get(0).destino()).isNotEqualTo(doV.get(1).destino());
    }

    @Test
    @DisplayName("depositar e retirar mexem no buffer e passam pela secao critica")
    void bufferAndaComOsPassos() {
        Estado estado = sistema.inicial();
        estado = maquina.aplicar(estado, 0);
        estado = maquina.aplicar(estado, 0);
        assertThat(sistema.dentroDaSecao(estado, 0)).isFalse();

        estado = maquina.aplicar(estado, 0);

        assertThat(estado.itens()).isEqualTo(1);
        assertThat(sistema.dentroDaSecao(estado, 0)).isTrue();
    }
}
