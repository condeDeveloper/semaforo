package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("O buffer limitado")
class BufferTest {

    @ParameterizedTest
    @CsvSource({"1,1,1", "1,1,2", "1,1,3", "2,1,2", "2,2,1", "2,2,2", "2,2,3", "3,1,1", "1,3,2"})
    @DisplayName("em nenhuma ordem de passos o buffer passa do tamanho ou fica negativo")
    void nuncaSaiDaFaixa(int produtores, int consumidores, int tamanho) {
        Sistema sistema = Sistema.de(produtores, consumidores, tamanho, Variante.CERTO);

        Explorador.Resultado r = new Explorador(sistema).explorar();

        assertThat(r.bufferVale()).isTrue();
        assertThat(r.maximoDeItens()).isEqualTo(tamanho);
    }

    @Test
    @DisplayName("o maximo e atingido: o contador de vazios nao desperdica lugar")
    void chegaNoTeto() {
        for (int tamanho = 1; tamanho <= 4; tamanho++) {
            Explorador.Resultado r = new Explorador(
                    Sistema.de(2, 1, tamanho, Variante.CERTO)).explorar();

            assertThat(r.maximoDeItens()).isEqualTo(tamanho);
        }
    }

    @Test
    @DisplayName("todo estado alcancavel respeita vazios + cheios + quem esta dentro = tamanho")
    void invarianteDosContadores() {
        Sistema sistema = Sistema.de(2, 2, 2, Variante.CERTO);
        Explorador.Resultado r = new Explorador(sistema).explorar();

        for (Estado estado : r.grafo().keySet()) {
            int fora = estado.valor(Semaforos.VAZIOS) + estado.valor(Semaforos.CHEIOS);
            assertThat(fora).isBetween(0, sistema.buffer());
            assertThat(estado.itens()).isBetween(0, sistema.buffer());
        }
    }

    @Test
    @DisplayName("com um produtor e um consumidor o grafo cresce 33 estados por lugar do buffer")
    void crescimentoLinear() {
        int[] estados = new int[5];
        for (int tamanho = 1; tamanho <= 4; tamanho++) {
            estados[tamanho] = new Explorador(Sistema.de(1, 1, tamanho, Variante.CERTO))
                    .explorar().estados();
        }

        assertThat(estados[1]).isEqualTo(24);
        assertThat(estados[2] - estados[1]).isEqualTo(33);
        assertThat(estados[3] - estados[2]).isEqualTo(33);
        assertThat(estados[4] - estados[3]).isEqualTo(33);
    }

    @Test
    @DisplayName("o teto de estados protege o explorador de configuracao grande demais")
    void tetoDeEstados() {
        Explorador apertado = new Explorador(Sistema.de(2, 2, 3, Variante.CERTO), 100);

        assertThatThrownBy(apertado::explorar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("teto de 100 estados");
    }
}
