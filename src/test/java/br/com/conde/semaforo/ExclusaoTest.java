package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exclusao mutua: dois dentro da secao ao mesmo tempo")
class ExclusaoTest {

    @ParameterizedTest
    @EnumSource(value = Variante.class, names = {"CERTO", "MUTEX_PRIMEIRO", "V_TROCADO"})
    @DisplayName("com mutex, nenhuma ordem de passos poe dois processos dentro")
    void comMutexNinguemEntraJunto(Variante variante) {
        Explorador.Resultado r = new Explorador(Sistema.de(2, 2, 2, variante)).explorar();

        assertThat(r.exclusaoVale()).isTrue();
    }

    @Test
    @DisplayName("sem mutex dois produtores entram juntos, e bastam quatro passos")
    void semMutexQuebra() {
        Explorador.Resultado r = new Explorador(Sistema.de(2, 2, 2, Variante.SEM_MUTEX)).explorar();

        assertThat(r.exclusaoVale()).isFalse();
        assertThat(r.exclusaoQuebrada()).hasSize(5);
        assertThat(r.exclusaoQuebrada().get(4)).contains("2 processos dentro");
    }

    @Test
    @DisplayName("com UM lugar no buffer, tirar o mutex nao quebra nada: o contador ja serializa")
    void bufferDeUmLugarDispensaOMutex() {
        int[][] tamanhos = {{1, 1}, {1, 2}, {2, 1}, {2, 2}};

        for (int[] quantos : tamanhos) {
            Explorador.Resultado r = new Explorador(
                    Sistema.de(quantos[0], quantos[1], 1, Variante.SEM_MUTEX)).explorar();

            assertThat(r.exclusaoVale())
                    .describedAs("%dp x %dc com buffer 1", quantos[0], quantos[1])
                    .isTrue();
        }
    }

    @Test
    @DisplayName("com DOIS lugares quebra ate com um produtor e um consumidor, em oito passos")
    void doisLugaresJaQuebram() {
        Explorador.Resultado r = new Explorador(Sistema.de(1, 1, 2, Variante.SEM_MUTEX)).explorar();

        assertThat(r.exclusaoVale()).isFalse();
        assertThat(r.exclusaoQuebrada()).hasSize(9);
    }

    @Test
    @DisplayName("dois produtores precisam de metade dos passos que um produtor e um consumidor")
    void doisProdutoresQuebramMaisRapido() {
        int comDois = new Explorador(Sistema.de(2, 1, 2, Variante.SEM_MUTEX))
                .explorar().exclusaoQuebrada().size();
        int comUm = new Explorador(Sistema.de(1, 1, 2, Variante.SEM_MUTEX))
                .explorar().exclusaoQuebrada().size();

        assertThat(comDois).isEqualTo(5);
        assertThat(comUm).isEqualTo(9);
    }

    @Test
    @DisplayName("sem mutex o buffer continua dentro da faixa: o que quebra nao e a contagem")
    void semMutexNaoEstouraOBuffer() {
        for (int tamanho = 1; tamanho <= 3; tamanho++) {
            Explorador.Resultado r = new Explorador(
                    Sistema.de(2, 2, tamanho, Variante.SEM_MUTEX)).explorar();

            assertThat(r.bufferVale())
                    .describedAs("buffer de %d", tamanho)
                    .isTrue();
            assertThat(r.maximoDeItens()).isEqualTo(tamanho);
        }
    }

    @Test
    @DisplayName("a janela de exclusao e de um passo: so o sai mantem o processo dentro")
    void janelaDeUmPasso() {
        Sistema sistema = Sistema.de(1, 1, 1, Variante.CERTO);
        Maquina maquina = new Maquina(sistema);
        Estado estado = sistema.inicial();

        estado = maquina.aplicar(estado, 0);
        assertThat(sistema.dentroDaSecao(estado, 0)).isFalse();
        estado = maquina.aplicar(estado, 0);
        assertThat(sistema.dentroDaSecao(estado, 0)).isFalse();
        estado = maquina.aplicar(estado, 0);
        assertThat(sistema.dentroDaSecao(estado, 0)).isTrue();
        estado = maquina.aplicar(estado, 0);
        assertThat(sistema.dentroDaSecao(estado, 0)).isFalse();
    }
}
