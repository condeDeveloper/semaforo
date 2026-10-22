package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os numeros publicados no README, presos aqui.
 *
 * <p>Um README com numero solto envelhece calado: alguem mexe no modelo, a tabela
 * fica mentindo e ninguem percebe. Com os numeros aqui dentro, mexer no modelo sem
 * refazer a conta quebra o CI nos tres sistemas.
 */
@DisplayName("Os numeros do README")
class MedidasTest {

    private Explorador.Resultado explorar(Variante variante) {
        return new Explorador(Sistema.de(2, 2, 2, variante)).explorar();
    }

    @Test
    @DisplayName("tabela dos protocolos: estados e arestas de cada versao")
    void tabelaDosProtocolos() {
        assertThat(explorar(Variante.CERTO).estados()).isEqualTo(723);
        assertThat(explorar(Variante.CERTO).transicoes()).isEqualTo(1968);
        assertThat(explorar(Variante.MUTEX_PRIMEIRO).estados()).isEqualTo(1065);
        assertThat(explorar(Variante.MUTEX_PRIMEIRO).transicoes()).isEqualTo(2336);
        assertThat(explorar(Variante.SEM_MUTEX).estados()).isEqualTo(329);
        assertThat(explorar(Variante.SEM_MUTEX).transicoes()).isEqualTo(968);
        assertThat(explorar(Variante.V_TROCADO).estados()).isEqualTo(665);
        assertThat(explorar(Variante.V_TROCADO).transicoes()).isEqualTo(1740);
    }

    @Test
    @DisplayName("o V trocado tem MENOS estados que o certo, e nao quebra nada")
    void vTrocadoNaoQuebraENaoCusta() {
        Explorador.Resultado trocado = explorar(Variante.V_TROCADO);
        Explorador.Resultado certo = explorar(Variante.CERTO);

        assertThat(trocado.temImpasse()).isFalse();
        assertThat(trocado.exclusaoVale()).isTrue();
        assertThat(trocado.bufferVale()).isTrue();
        assertThat(trocado.estados()).isLessThan(certo.estados());
        assertThat(certo.estados() - trocado.estados()).isEqualTo(58);
    }

    @Test
    @DisplayName("tabela das filas: mesmos 248 estados, 18 arestas a mais na livre")
    void tabelaDasFilas() {
        Explorador.Resultado fifo = new Explorador(Sistema.de(3, 1, 1, Variante.CERTO)).explorar();
        Explorador.Resultado livre = new Explorador(
                Sistema.de(3, 1, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE)).explorar();

        assertThat(fifo.estados()).isEqualTo(248);
        assertThat(livre.estados()).isEqualTo(248);
        assertThat(fifo.transicoes()).isEqualTo(538);
        assertThat(livre.transicoes()).isEqualTo(556);
        assertThat(livre.transicoes() - fifo.transicoes()).isEqualTo(18);
    }

    @Test
    @DisplayName("tabela do buffer: estados de cada configuracao")
    void tabelaDoBuffer() {
        assertThat(estados(1, 1, 1)).isEqualTo(24);
        assertThat(estados(1, 1, 2)).isEqualTo(57);
        assertThat(estados(1, 1, 3)).isEqualTo(90);
        assertThat(estados(2, 1, 2)).isEqualTo(210);
        assertThat(estados(2, 2, 1)).isEqualTo(210);
        assertThat(estados(2, 2, 2)).isEqualTo(723);
        assertThat(estados(2, 2, 3)).isEqualTo(1572);
    }

    @Test
    @DisplayName("tabela do mutex: com um lugar nao quebra, com dois quebra em 4 ou 8 passos")
    void tabelaDoMutex() {
        for (int[] caso : List.of(new int[]{1, 1}, new int[]{2, 1}, new int[]{2, 2})) {
            Explorador.Resultado r = new Explorador(
                    Sistema.de(caso[0], caso[1], 1, Variante.SEM_MUTEX)).explorar();
            assertThat(r.exclusaoVale()).isTrue();
        }

        assertThat(passosAteQuebrar(1, 1, 2)).isEqualTo(8);
        assertThat(passosAteQuebrar(1, 1, 3)).isEqualTo(8);
        assertThat(passosAteQuebrar(2, 1, 2)).isEqualTo(4);
        assertThat(passosAteQuebrar(2, 2, 2)).isEqualTo(4);
        assertThat(passosAteQuebrar(2, 2, 3)).isEqualTo(4);
    }

    @Test
    @DisplayName("o medidor roda inteiro sem estourar nem lancar")
    void medidorRoda() {
        Medidor.protocolos();
        Medidor.filas();
        Medidor.buffer();
        Medidor.mutex();
    }

    private int estados(int produtores, int consumidores, int buffer) {
        return new Explorador(Sistema.de(produtores, consumidores, buffer, Variante.CERTO))
                .explorar().estados();
    }

    private int passosAteQuebrar(int produtores, int consumidores, int buffer) {
        return new Explorador(Sistema.de(produtores, consumidores, buffer, Variante.SEM_MUTEX))
                .explorar().exclusaoQuebrada().size() - 1;
    }
}
