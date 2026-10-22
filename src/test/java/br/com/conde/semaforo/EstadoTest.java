package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("O estado global")
class EstadoTest {

    @Test
    @DisplayName("comeca com mutex em 1, vazios no tamanho do buffer e cheios em zero")
    void inicial() {
        Estado estado = Estado.inicial(4, 3);

        assertThat(estado.valor(Semaforos.MUTEX)).isEqualTo(1);
        assertThat(estado.valor(Semaforos.VAZIOS)).isEqualTo(3);
        assertThat(estado.valor(Semaforos.CHEIOS)).isZero();
        assertThat(estado.itens()).isZero();
        assertThat(estado.processos()).isEqualTo(4);
        for (int processo = 0; processo < 4; processo++) {
            assertThat(estado.bloqueado(processo)).isFalse();
            assertThat(estado.pc(processo)).isZero();
        }
    }

    @Test
    @DisplayName("e imutavel: cada mudanca devolve outro estado e deixa o primeiro intacto")
    void imutavel() {
        Estado inicial = Estado.inicial(2, 1);

        Estado outro = inicial.comItens(1).comValor(Semaforos.MUTEX, 0).comPc(0, 2);

        assertThat(inicial.itens()).isZero();
        assertThat(inicial.valor(Semaforos.MUTEX)).isEqualTo(1);
        assertThat(inicial.pc(0)).isZero();
        assertThat(outro.itens()).isEqualTo(1);
        assertThat(outro.valor(Semaforos.MUTEX)).isZero();
        assertThat(outro.pc(0)).isEqualTo(2);
    }

    @Test
    @DisplayName("dois estados com os mesmos campos sao o mesmo estado para o explorador")
    void igualdade() {
        Estado um = Estado.inicial(2, 2).comItens(1).entrarNaFila(0, Semaforos.CHEIOS);
        Estado outro = Estado.inicial(2, 2).comItens(1).entrarNaFila(0, Semaforos.CHEIOS);

        assertThat(um).isEqualTo(outro).hasSameHashCodeAs(outro);
    }

    @Test
    @DisplayName("a ordem da fila faz parte do estado: trocar quem chegou antes e outro estado")
    void ordemDaFilaImporta() {
        Estado base = Estado.inicial(2, 2);

        Estado umDepoisDoOutro = base.entrarNaFila(0, Semaforos.CHEIOS).entrarNaFila(1, Semaforos.CHEIOS);
        Estado invertido = base.entrarNaFila(1, Semaforos.CHEIOS).entrarNaFila(0, Semaforos.CHEIOS);

        assertThat(umDepoisDoOutro.fila(Semaforos.CHEIOS)).containsExactly(0, 1);
        assertThat(invertido.fila(Semaforos.CHEIOS)).containsExactly(1, 0);
        assertThat(umDepoisDoOutro).isNotEqualTo(invertido);
    }

    @Test
    @DisplayName("sair da fila tira so quem saiu e desmarca o bloqueio dele")
    void sairDaFila() {
        Estado cheia = Estado.inicial(3, 1)
                .entrarNaFila(0, Semaforos.VAZIOS)
                .entrarNaFila(1, Semaforos.VAZIOS)
                .entrarNaFila(2, Semaforos.VAZIOS);

        Estado depois = cheia.sairDaFila(1, Semaforos.VAZIOS);

        assertThat(depois.fila(Semaforos.VAZIOS)).containsExactly(0, 2);
        assertThat(depois.bloqueado(1)).isFalse();
        assertThat(depois.bloqueado(0)).isTrue();
        assertThat(depois.bloqueadoEm(2)).isEqualTo(Semaforos.VAZIOS);
    }

    @Test
    @DisplayName("a fila devolvida e uma copia: mexer nela nao mexe no estado")
    void filaECopia() {
        Estado estado = Estado.inicial(2, 1).entrarNaFila(0, Semaforos.MUTEX);

        int[] copia = estado.fila(Semaforos.MUTEX);
        copia[0] = 99;

        assertThat(estado.fila(Semaforos.MUTEX)).containsExactly(0);
    }
}
