package it.prgweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Applicazione Spring Boot standalone (fat jar con Tomcat embedded). */
@SpringBootApplication
public class PrgwebApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrgwebApplication.class, args);
    }
}
