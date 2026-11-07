package it.prgweb.repository;

import it.prgweb.config.DatabaseTemporaneoFisso;
import it.prgweb.PrgwebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 3.3: il lock sulla riga app_lock si acquisisce dentro la transazione
 * e le transazioni concorrenti si serializzano (una attende sul
 * SELECT ... FOR UPDATE fino al commit, poi ottiene la riga).
 */
@SpringBootTest(classes = PrgwebApplication.class)
class AppLockRepositoryTest extends DatabaseTemporaneoFisso {

    @Autowired
    private AppLockRepository appLockRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void lockAcquisitoDentroLaStessaTransazione() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Transazione A: prende il lock e lo tiene.
            CountDownLatch lockPrese = new CountDownLatch(1);
            CountDownLatch rilasciaLock = new CountDownLatch(1);
            var futurA = executor.submit(() -> template.execute(stato -> {
                appLockRepository.acquisisciMutex();
                lockPrese.countDown();
                try {
                    rilasciaLock.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));

            assertThat(lockPrese.await(10, TimeUnit.SECONDS)).isTrue();

            // Transazione B: chiede lo stesso lock; deve attendere finché A
            // non commetta (nessun secondo detentore contemporaneo).
            CountDownLatch bAvviata = new CountDownLatch(1);
            var futurB = executor.submit(() -> {
                bAvviata.countDown();
                return template.execute(stato -> {
                    appLockRepository.acquisisciMutex();
                    return "RITORNATO";
                });
            });
            assertThat(bAvviata.await(10, TimeUnit.SECONDS)).isTrue();

            TimeUnit.MILLISECONDS.sleep(300);
            assertThat(futurB.isDone()).isFalse().as("B deve attendere finché A detiene il lock");

            rilasciaLock.countDown();
            futurA.get(10, TimeUnit.SECONDS);
            assertThat(futurB.get(10, TimeUnit.SECONDS)).isEqualTo("RITORNATO");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void accessiSerialiStessaThread() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute(stato -> {
            appLockRepository.acquisisciMutex();
            return null;
        });
        template.execute(stato -> {
            appLockRepository.acquisisciMutex();
            return null;
        });
        assertThat(utenteRepository.count()).isEqualTo(1);
        assertThat(utenteRepository.findByNomeUtente("admin")).isPresent();
    }
}
