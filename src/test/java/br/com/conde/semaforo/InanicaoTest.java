package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inanicao: o sistema anda e um processo nunca e a vez dele")
class InanicaoTest {

    private List<Inanicao.Achado> procurar(Sistema sistema) {
        Explorador.Resultado r = new Explorador(sistema).explorar();
        return new Inanicao(sistema, r).procurar();
    }

    @Test
    @DisplayName("com fila FIFO ninguem passa fome, em nenhuma volta do grafo")
    void fifoNaoDeixaNinguemDeFora() {
        List<Inanicao.Achado> achados = procurar(Sistema.de(3, 1, 1, Variante.CERTO));

        assertThat(achados).isEmpty();
    }

    @Test
    @DisplayName("com fila livre os tres produtores podem passar fome, um de cada vez")
    void livreDeixa() {
        Sistema livre = Sistema.de(3, 1, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE);

        List<Inanicao.Achado> achados = procurar(livre);

        assertThat(achados).hasSize(3);
        assertThat(achados).extracting(Inanicao.Achado::nome)
                .containsExactly("produtor1", "produtor2", "produtor3");
    }

    @Test
    @DisplayName("bastam DOIS na mesma fila: com um de cada lado ninguem passa fome")
    void precisaDeDoisNaMesmaFila() {
        for (int buffer = 1; buffer <= 3; buffer++) {
            Sistema umDeCada = Sistema.de(1, 1, buffer, Variante.CERTO)
                    .com(Semaforos.Disciplina.LIVRE);

            assertThat(procurar(umDeCada))
                    .describedAs("buffer de %d", buffer)
                    .isEmpty();
        }

        Sistema doisProdutores = Sistema.de(2, 1, 1, Variante.CERTO)
                .com(Semaforos.Disciplina.LIVRE);

        assertThat(procurar(doisProdutores)).hasSize(2);
    }

    @Test
    @DisplayName("quem passa fome e so quem disputa fila: dois produtores nao afetam o consumidor")
    void soQuemDisputa() {
        Sistema sistema = Sistema.de(2, 1, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE);

        assertThat(procurar(sistema)).extracting(Inanicao.Achado::nome)
                .containsExactly("produtor1", "produtor2");

        Sistema espelho = Sistema.de(1, 2, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE);

        assertThat(procurar(espelho)).extracting(Inanicao.Achado::nome)
                .containsExactly("consumidor1", "consumidor2");
    }

    @Test
    @DisplayName("a disciplina nao muda os estados alcancaveis, so os caminhos entre eles")
    void mesmosEstadosOutrosCaminhos() {
        Explorador.Resultado fifo = new Explorador(Sistema.de(3, 1, 1, Variante.CERTO)).explorar();
        Explorador.Resultado livre = new Explorador(
                Sistema.de(3, 1, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE)).explorar();

        assertThat(livre.estados()).isEqualTo(fifo.estados());
        assertThat(livre.transicoes()).isGreaterThan(fifo.transicoes());
        assertThat(livre.grafo().keySet()).isEqualTo(fifo.grafo().keySet());
    }

    @Test
    @DisplayName("o ciclo encontrado e mesmo um ciclo, e o faminto esta bloqueado em todos os estados")
    void oCicloSeSustenta() {
        Sistema livre = Sistema.de(3, 1, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE);
        Explorador.Resultado r = new Explorador(livre).explorar();

        Inanicao.Achado achado = new Inanicao(livre, r).procurar().get(0);

        assertThat(achado.ciclo().get(0)).contains("estados que o sistema pode repetir");
        assertThat(achado.processo()).isZero();
    }

    @Test
    @DisplayName("impasse e inanicao sao defeitos independentes: o mesmo grafo tem os dois")
    void impasseEInanicaoConvivem() {
        Sistema travado = Sistema.de(2, 2, 2, Variante.MUTEX_PRIMEIRO)
                .com(Semaforos.Disciplina.LIVRE);
        Explorador.Resultado r = new Explorador(travado).explorar();

        assertThat(r.temImpasse()).isTrue();
        assertThat(new Inanicao(travado, r).procurar()).isNotEmpty();
    }
}
