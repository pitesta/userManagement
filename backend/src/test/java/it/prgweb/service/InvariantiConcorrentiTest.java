package it.prgweb.service;

import it.prgweb.PrgwebApplication;
import it.prgweb.repository.AppLockRepository;
import it.prgweb.repository.UtenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.CookieManager;
import java.net.CookiePolicy;
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

/** Verifiche HTTP concorrenti per l'ultimo amministratore e timeout del mutex. */
class InvariantiConcorrentiTest {

    @TempDir Path directory;
    @Test
    void dueDegradazioniContemporaneeLascianoUnSoloAmministratore() throws Exception {
        String dbUrl = url("admin", "");
        try (ConfigurableApplicationContext context = avvia(dbUrl)) {
            URI api = endpoint(context);
            HttpClient clientAdmin = clientConCookie();
            long adminA = id(registra(clientAdmin, api, "adminA"));
            accedi(clientAdmin, api, "admin", "admin");
            long adminSeed = idUtente(clientAdmin, api, "admin");
            assertThat(invia(clientAdmin, "POST", path(api, "/api/admin/utente"),
                    "{\"idUtente\":" + adminA + ",\"amministratore\":true}").statusCode()).isEqualTo(200);

            HttpClient clientB = clientConCookie();
            accedi(clientB, api, "admin", "admin");
            CountDownLatch pronti = new CountDownLatch(2);
            CountDownLatch via = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<HttpResponse<String>> reqA = executor.submit(() -> degrada(clientAdmin, api, adminSeed, pronti, via));
                Future<HttpResponse<String>> reqB = executor.submit(() -> degrada(clientB, api, adminA, pronti, via));
                assertThat(pronti.await(10, TimeUnit.SECONDS)).isTrue();
                via.countDown();
                HttpResponse<String> rispostaA = reqA.get(10, TimeUnit.SECONDS);
                int statusA = rispostaA.statusCode();
                HttpResponse<String> rispostaB = reqB.get(10, TimeUnit.SECONDS);
                int statusB = rispostaB.statusCode();
                assertThat(java.util.List.of(statusA, statusB)).containsExactlyInAnyOrder(200, 409);
                HttpResponse<String> conflitto = statusA == 409 ? rispostaA : rispostaB;
                assertThat(conflitto.body()).isEqualTo("{\"error\":\"Deve rimanere almeno un amministratore.\"}");
                assertThat(context.getBean(UtenteRepository.class).countByAmministratoreIsTrue()).isEqualTo(1);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void timeoutDelMutexRisponde500SenzaRegistrazioneParziale() throws Exception {
        try (ConfigurableApplicationContext context = avvia(url("timeout", ";LOCK_TIMEOUT=500"))) {
            AppLockRepository lock = context.getBean(AppLockRepository.class);
            TransactionTemplate transazione = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            CountDownLatch mutexPreso = new CountDownLatch(1);
            CountDownLatch rilascia = new CountDownLatch(1);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> detentore = executor.submit(() -> transazione.execute(status -> {
                lock.acquisisciMutex();
                mutexPreso.countDown();
                try {
                    rilascia.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            try {
                assertThat(mutexPreso.await(10, TimeUnit.SECONDS)).isTrue();
                URI api = endpoint(context);
                HttpResponse<String> risposta = invia(HttpClient.newHttpClient(), "POST",
                        path(api, "/api/register"),
                        "{\"nomeUtente\":\"timeout\",\"password\":\"segreto1\",\"email\":\"timeout@example.com\"}");
                assertThat(risposta.statusCode()).isEqualTo(500);
                assertThat(risposta.body()).isEqualTo("{\"error\":\"Errore interno del database.\"}");
                assertThat(context.getBean(UtenteRepository.class).count()).isEqualTo(1);
                assertThat(context.getBean(UtenteRepository.class).findByNomeUtente("admin")).isPresent();
            } finally {
                rilascia.countDown();
                detentore.get(10, TimeUnit.SECONDS);
                executor.shutdownNow();
            }
        }
    }

    private ConfigurableApplicationContext avvia(String url) {
        return new SpringApplicationBuilder(PrgwebApplication.class)
                .run("--spring.datasource.url=" + url,
                        "--server.port=0",
                        "--logging.level.org.hibernate.SQL=DEBUG");
    }

    private String url(String nome, String parametri) {
        return "jdbc:h2:file:" + directory.resolve(nome).resolve("app").toAbsolutePath()
                + ";DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE" + parametri;
    }

    private static URI endpoint(ConfigurableApplicationContext context) {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        return URI.create("http://127.0.0.1:" + port + "/prgweb");
    }

    private static URI path(URI api, String path) {
        return URI.create(api.toString() + path);
    }

    private static HttpClient clientConCookie() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    private static long id(HttpResponse<String> response) throws Exception {
        return tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response.body()).path("utente").path("idUtente").longValue();
    }

    private static long idUtente(HttpClient client, URI api, String nome) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(path(api, "/api/utenti")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var utenti = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.body()).path("utenti");
        for (var utente : utenti) {
            if (utente.path("nomeUtente").asString().equals(nome)) return utente.path("idUtente").longValue();
        }
        throw new AssertionError("Utente non trovato: " + nome);
    }

    private static HttpResponse<String> registra(HttpClient client, URI api, String nome) throws Exception {
        HttpResponse<String> response = invia(client, "POST", path(api, "/api/register"),
                "{\"nomeUtente\":\"" + nome + "\",\"password\":\"segreto1\",\"email\":\""
                        + nome + "@example.com\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return response;
    }

    private static void accedi(HttpClient client, URI api, String nome) throws Exception {
        accedi(client, api, nome, "segreto1");
    }

    private static void accedi(HttpClient client, URI api, String nome, String password) throws Exception {
        HttpResponse<String> response = invia(client, "POST", path(api, "/api/login"),
                "{\"nomeUtente\":\"" + nome + "\",\"password\":\"" + password + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private static HttpResponse<String> degrada(HttpClient client, URI api, long id,
                                                 CountDownLatch pronti, CountDownLatch via) throws Exception {
        pronti.countDown();
        if (!via.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Latch non rilasciato");
        return invia(client, "POST", path(api, "/api/admin/utente"),
                "{\"idUtente\":" + id + ",\"amministratore\":false}");
    }

    private static HttpResponse<String> invia(HttpClient client, String metodo, URI uri, String body)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
