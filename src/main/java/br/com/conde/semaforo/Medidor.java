package br.com.conde.semaforo;

import java.util.List;
import java.util.Locale;

/**
 * Imprime as tabelas do README. Cada numero publicado sai daqui, e as mesmas
 * contas estao presas em testes: se uma mudar, o CI quebra.
 *
 * <p>Uso: {@code mvn exec:java -Dexec.args="protocolos"} (ou {@code filas},
 * {@code buffer}, ou nada para as tres).
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String tabela = args.length > 0 ? args[0] : "tudo";
        boolean tudo = tabela.equals("tudo");
        if (tudo || tabela.equals("protocolos")) {
            protocolos();
        }
        if (tudo || tabela.equals("filas")) {
            filas();
        }
        if (tudo || tabela.equals("buffer")) {
            buffer();
        }
        if (tudo || tabela.equals("mutex")) {
            mutex();
        }
    }

    /** As quatro versoes do protocolo, no mesmo tamanho de sistema. */
    public static void protocolos() {
        System.out.println();
        System.out.println("PROTOCOLOS  (2 produtores, 2 consumidores, buffer 2, fila FIFO)");
        System.out.printf(Locale.ROOT, "%-16s %8s %8s %10s %10s %10s %8s%n",
                "versao", "estados", "arestas", "impasse", "exclusao", "buffer", "maximo");
        for (Variante variante : Variante.values()) {
            Sistema sistema = Sistema.de(2, 2, 2, variante);
            Explorador.Resultado r = new Explorador(sistema).explorar();
            System.out.printf(Locale.ROOT, "%-16s %8d %8d %10s %10s %10s %8d%n",
                    variante.rotulo(), r.estados(), r.transicoes(),
                    r.temImpasse() ? "SIM" : "nao",
                    r.exclusaoVale() ? "ok" : "QUEBRA",
                    r.bufferVale() ? "ok" : "QUEBRA",
                    r.maximoDeItens());
        }
        System.out.println();
        for (Variante variante : List.of(Variante.MUTEX_PRIMEIRO, Variante.SEM_MUTEX)) {
            Explorador.Resultado r = new Explorador(Sistema.de(2, 2, 2, variante)).explorar();
            List<String> caminho = r.temImpasse() ? r.impasse() : r.exclusaoQuebrada();
            System.out.println("contraexemplo de " + variante.rotulo() + ":");
            for (String linha : caminho) {
                System.out.println("  " + linha);
            }
            System.out.println();
        }
    }

    /** Quem o V acorda muda quem espera para sempre. */
    public static void filas() {
        System.out.println();
        System.out.println("FILAS  (protocolo certo, 3 produtores, 1 consumidor, buffer 1)");
        System.out.printf(Locale.ROOT, "%-10s %8s %8s %12s %s%n",
                "disciplina", "estados", "arestas", "inanicao", "quem passa fome");
        for (Semaforos.Disciplina disciplina : Semaforos.Disciplina.values()) {
            Sistema sistema = Sistema.de(3, 1, 1, Variante.CERTO).com(disciplina);
            Explorador.Resultado r = new Explorador(sistema).explorar();
            List<Inanicao.Achado> achados = new Inanicao(sistema, r).procurar();
            String quem = achados.isEmpty() ? "-"
                    : String.join(", ", achados.stream().map(Inanicao.Achado::nome).toList());
            System.out.printf(Locale.ROOT, "%-10s %8d %8d %12s %s%n",
                    disciplina, r.estados(), r.transicoes(),
                    achados.isEmpty() ? "nao" : "SIM", quem);
        }
        Sistema livre = Sistema.de(3, 1, 1, Variante.CERTO).com(Semaforos.Disciplina.LIVRE);
        Explorador.Resultado r = new Explorador(livre).explorar();
        List<Inanicao.Achado> achados = new Inanicao(livre, r).procurar();
        if (!achados.isEmpty()) {
            System.out.println();
            System.out.println("a volta que deixa " + achados.get(0).nome() + " dormindo:");
            for (String linha : achados.get(0).ciclo()) {
                System.out.println("  " + linha);
            }
        }
        System.out.println();
    }

    /**
     * Quando o mutex faz falta de verdade.
     *
     * <p>A resposta nao e "sempre": com um lugar so no buffer, o proprio contador
     * de vazios serializa os produtores, e tirar a trava nao quebra nada.
     */
    public static void mutex() {
        System.out.println();
        System.out.println("MUTEX  (protocolo sem mutex, fila FIFO: quando o defeito aparece)");
        System.out.printf(Locale.ROOT, "%-18s %8s %12s %10s%n",
                "sistema", "estados", "exclusao", "passos");
        int[][] casos = {{1, 1, 1}, {1, 1, 2}, {1, 1, 3}, {2, 1, 1}, {2, 1, 2},
                {2, 2, 1}, {2, 2, 2}, {2, 2, 3}};
        for (int[] caso : casos) {
            Sistema sistema = Sistema.de(caso[0], caso[1], caso[2], Variante.SEM_MUTEX);
            Explorador.Resultado r = new Explorador(sistema).explorar();
            System.out.printf(Locale.ROOT, "%-18s %8d %12s %10s%n",
                    caso[0] + "p x " + caso[1] + "c, buffer " + caso[2],
                    r.estados(),
                    r.exclusaoVale() ? "ok" : "QUEBRA",
                    r.exclusaoVale() ? "-" : String.valueOf(r.exclusaoQuebrada().size() - 1));
        }
        System.out.println();
    }

    /** O buffer nunca passa do tamanho, e o grafo cresce com ele. */
    public static void buffer() {
        System.out.println();
        System.out.println("BUFFER  (protocolo certo, fila FIFO)");
        System.out.printf(Locale.ROOT, "%-18s %8s %8s %8s %8s%n",
                "sistema", "estados", "arestas", "maximo", "limite");
        int[][] casos = {{1, 1, 1}, {1, 1, 2}, {1, 1, 3}, {2, 1, 2}, {2, 2, 1}, {2, 2, 2}, {2, 2, 3}};
        for (int[] caso : casos) {
            Sistema sistema = Sistema.de(caso[0], caso[1], caso[2], Variante.CERTO);
            Explorador.Resultado r = new Explorador(sistema).explorar();
            System.out.printf(Locale.ROOT, "%-18s %8d %8d %8d %8d%n",
                    caso[0] + "p x " + caso[1] + "c, buffer " + caso[2],
                    r.estados(), r.transicoes(), r.maximoDeItens(), caso[2]);
        }
        System.out.println();
    }
}
