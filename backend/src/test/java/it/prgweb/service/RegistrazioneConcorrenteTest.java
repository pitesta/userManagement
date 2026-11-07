package it.prgweb.service;

import it.prgweb.PrgwebApplication;
import it.prgweb.repository.UtenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Due richieste HTTP simultanee, ripetute con un database appena inizializzato a ogni run. */
class RegistrazioneConcorrenteTest {

    @TempDir Path directory;

    @Test
    void dueRegistrazioniSimultaneeCreanoEsattamenteUnAmministratore() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        for (int run = 0; run < 5; run++) {
            final int runNumber = run;
            Path dbDir = directory.resolve("run-" + runNumber);
            String dbUrl = "jdbc:h2:file:" + dbDir.resolve("app").toAbsolutePath()
                    + ";DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE";
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(PrgwebApplication.class)
                    .run("--spring.datasource.url=" + dbUrl,
                            "--server.port=0",
                            "--logging.level.org.hibernate.SQL=DEBUG")) {
                int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
                URI uri = URI.create("http://127.0.0.1:" + port + "/prgweb/api/register");
                CountDownLatch pronti = new CountDownLatch(2);
                CountDownLatch via = new CountDownLatch(1);
                ExecutorService executor = Executors.newFixedThreadPool(2);
                try {
                    Future<HttpResponse<String>> prima = executor.submit(() -> registra(client, uri,
                            "utenteA" + runNumber, pronti, via));
                    Future<HttpResponse<String>> seconda = executor.submit(() -> registra(client, uri,
                            "utenteB" + runNumber, pronti, via));
                    assertThat(pronti.await(10, TimeUnit.SECONDS)).isTrue();
                    via.countDown();

                    HttpResponse<String> risultatoA = prima.get(20, TimeUnit.SECONDS);
                    HttpResponse<String> risultatoB = seconda.get(20, TimeUnit.SECONDS);
                    assertThat(risultatoA.statusCode()).isEqualTo(201);
                    assertThat(risultatoB.statusCode()).isEqualTo(201);

                    boolean adminA = mapper.readTree(risultatoA.body()).path("utente")
                            .path("amministratore").booleanValue();
                    boolean adminB = mapper.readTree(risultatoB.body()).path("utente")
                            .path("amministratore").booleanValue();
                    assertThat(adminA).isFalse();
                    assertThat(adminB).isFalse();
                    assertThat(context.getBean(UtenteRepository.class).count()).isEqualTo(3);
                    assertThat(context.getBean(UtenteRepository.class).countByAmministratoreIsTrue()).isEqualTo(1);
                } finally {
                    executor.shutdownNow();
                }
            }
        }
    }

    private static HttpResponse<String> registra(HttpClient client, URI uri, String nome,
                                                  CountDownLatch pronti, CountDownLatch via) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"nomeUtente\":\"" + nome
                        + "\",\"password\":\"segreto1\",\"email\":\"" + nome + "@example.com\"}"))
                .build();
        pronti.countDown();
        if (!via.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Latch di avvio non rilasciato");
        }
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
