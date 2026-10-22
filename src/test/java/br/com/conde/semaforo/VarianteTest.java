package br.com.conde.semaforo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Os quatro protocolos")
class VarianteTest {

    @ParameterizedTest
    @EnumSource(Variante.class)
    @DisplayName("todo protocolo tem exatamente uma secao critica, de um passo")
    void umaSecaoSo(Variante variante) {
        for (List<Passo> codigo : List.of(variante.produtor(), variante.consumidor())) {
            assertThat(codigo).filteredOn(passo -> passo.tipo() == Passo.Tipo.SAI).hasSize(1);
        }
    }

    @ParameterizedTest
    @EnumSource(Variante.class)
    @DisplayName("todo produtor deposita uma vez e todo consumidor retira uma vez")
    void umMovimentoPorVolta(Variante variante) {
        assertThat(variante.produtor()).filteredOn(p -> p.tipo() == Passo.Tipo.DEPOSITA).hasSize(1);
        assertThat(variante.produtor()).filteredOn(p -> p.tipo() == Passo.Tipo.RETIRA).isEmpty();
        assertThat(variante.consumidor()).filteredOn(p -> p.tipo() == Passo.Tipo.RETIRA).hasSize(1);
        assertThat(variante.consumidor()).filteredOn(p -> p.tipo() == Passo.Tipo.DEPOSITA).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Variante.class, names = {"CERTO", "MUTEX_PRIMEIRO", "V_TROCADO"})
    @DisplayName("quem usa mutex desce e sobe o mutex uma vez por volta")
    void mutexEquilibrado(Variante variante) {
        for (List<Passo> codigo : List.of(variante.produtor(), variante.consumidor())) {
            assertThat(contar(codigo, Passo.Tipo.P, Semaforos.MUTEX)).isEqualTo(1);
            assertThat(contar(codigo, Passo.Tipo.V, Semaforos.MUTEX)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("sem mutex nao tem mutex nenhum, e por isso tem dois passos a menos")
    void semMutex() {
        assertThat(Variante.SEM_MUTEX.produtor()).hasSize(4);
        assertThat(Variante.SEM_MUTEX.consumidor()).hasSize(4);
        assertThat(contar(Variante.SEM_MUTEX.produtor(), Passo.Tipo.P, Semaforos.MUTEX)).isZero();
        assertThat(contar(Variante.SEM_MUTEX.consumidor(), Passo.Tipo.V, Semaforos.MUTEX)).isZero();
    }

    @Test
    @DisplayName("a unica diferenca entre o certo e o mutex primeiro sao os dois primeiros passos")
    void mutexPrimeiroSoTrocaAOrdem() {
        List<Passo> certo = Variante.CERTO.produtor();
        List<Passo> trocado = Variante.MUTEX_PRIMEIRO.produtor();

        assertThat(certo).hasSameSizeAs(trocado);
        assertThat(certo.get(0)).isEqualTo(trocado.get(1));
        assertThat(certo.get(1)).isEqualTo(trocado.get(0));
        assertThat(certo.subList(2, certo.size())).isEqualTo(trocado.subList(2, trocado.size()));
    }

    @Test
    @DisplayName("a unica diferenca entre o certo e o V trocado sao os dois ultimos passos")
    void vTrocadoSoTrocaAOrdem() {
        List<Passo> certo = Variante.CERTO.consumidor();
        List<Passo> trocado = Variante.V_TROCADO.consumidor();

        assertThat(certo.subList(0, 4)).isEqualTo(trocado.subList(0, 4));
        assertThat(certo.get(4)).isEqualTo(trocado.get(5));
        assertThat(certo.get(5)).isEqualTo(trocado.get(4));
    }

    @Test
    @DisplayName("o rotulo do passo diz o nome do semaforo, que e o que aparece no contraexemplo")
    void rotulos() {
        assertThat(Passo.p(Semaforos.VAZIOS).rotulo()).isEqualTo("P(vazios)");
        assertThat(Passo.v(Semaforos.CHEIOS).rotulo()).isEqualTo("V(cheios)");
        assertThat(Passo.deposita().rotulo()).isEqualTo("deposita");
    }

    private long contar(List<Passo> codigo, Passo.Tipo tipo, int semaforo) {
        return codigo.stream().filter(p -> p.tipo() == tipo && p.semaforo() == semaforo).count();
    }
}
