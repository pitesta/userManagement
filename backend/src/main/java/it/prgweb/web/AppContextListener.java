package it.prgweb.web;

import it.prgweb.db.Database;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Crea lo schema SQLite all'avvio dell'applicazione. */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Database.init();
            sce.getServletContext().log("SQLite pronto: " + Database.getPath());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Inizializzazione database fallita (" + Database.getPath() + ")", e);
        }
    }
}
