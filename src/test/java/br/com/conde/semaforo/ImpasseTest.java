package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Impasse: ninguem pode andar")
class ImpasseTest {

    @ParameterizedTest
    @EnumSource(value = Variante.class, names = {"CERTO", "SEM_MUTEX", "V_TROCADO"})
    @DisplayName("nenhuma ordem de passos trava o protocolo quando o recurso vem antes da trava")
    void semImpasse(Variante variante) {
        Explorador.Resultado r = new Explorador(Sistema.de(2, 2, 2, variante)).explorar();

        assertThat(r.temImpasse()).isFalse();
    }

    @Test
    @DisplayName("trocar a ordem dos dois P trava o sistema, e o caminho tem cinco passos")
    void mutexPrimeiroTrava() {
        Explorador.Resultado r = new Explorador(Sistema.de(2, 2, 2, Variante.MUTEX_PRIMEIRO)).explorar();

        assertThat(r.temImpasse()).isTrue();
        assertThat(r.impasse()).hasSize(6);
        assertThat(r.impasse().get(r.impasse().size() - 1)).contains("ninguem pode andar");
        assertThat(r.impasse()).anyMatch(linha -> linha.contains("P(mutex)"));
    }

    @Test
    @DisplayName("o impasse aparece com um produtor e um consumidor so, no menor sistema que existe")
    void bastaUmDeCada() {
        Explorador.Resultado r = new Explorador(Sistema.de(1, 1, 1, Variante.MUTEX_PRIMEIRO)).explorar();

        assertThat(r.temImpasse()).isTrue();
        assertThat(r.estados()).isEqualTo(32);
    }

    @Test
    @DisplayName("aumentar o buffer nao salva o protocolo errado: o impasse continua")
    void bufferMaiorNaoSalva() {
        for (int tamanho = 1; tamanho <= 4; tamanho++) {
            Explorador.Resultado r = new Explorador(
                    Sistema.de(1, 1, tamanho, Variante.MUTEX_PRIMEIRO)).explorar();

            assertThat(r.temImpasse())
                    .describedAs("buffer de %d", tamanho)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("o estado travado tem todo mundo dormindo, e e isso que o traco prova")
    void todosDormindo() {
        Sistema sistema = Sistema.de(1, 1, 1, Variante.MUTEX_PRIMEIRO);
        Explorador.Resultado r = new Explorador(sistema).explorar();
        Maquina maquina = new Maquina(sistema);

        long travados = r.grafo().keySet().stream()
                .filter(estado -> maquina.movimentos(estado).isEmpty())
                .peek(estado -> {
                    for (int p = 0; p < sistema.processos(); p++) {
                        assertThat(estado.bloqueado(p)).isTrue();
                    }
                })
                .count();

        assertThat(travados).isPositive();
    }
}
