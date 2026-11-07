package it.prgweb.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * Configurazione web: CORS per il dev server Angular :4200 con credenziali
 * e fallback SPA per le GET senza estensione fuori da /api/**.
 */
@Configuration
public class WebConfig {

    private static final String ORIGIN_DEV = "http://localhost:4200";

    @Bean
    public WebMvcConfigurer corsConfig() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(ORIGIN_DEV)
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("Content-Type", "Accept")
                        .allowCredentials(true);
            }
        };
    }

    /** Fallback SPA registrato a mano (nessuna servlet legacy nel runtime). */
    @Bean
    public FilterRegistrationBean<Filter> spaFallbackFilter() {
        Filter fallback = new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                String path = req.getRequestURI().substring(req.getContextPath().length());
                boolean daServireConSpa = "GET".equalsIgnoreCase(req.getMethod())
                        && !path.startsWith("/api/")
                        && !path.contains(".")
                        && !path.equals("/")
                        && !path.isEmpty();
                if (daServireConSpa) {
                    req.getRequestDispatcher("/index.html").forward(req, res);
                } else {
                    chain.doFilter(request, response);
                }
            }
        };
        FilterRegistrationBean<Filter> registrazione = new FilterRegistrationBean<>(fallback);
        registrazione.addUrlPatterns("/*");
        return registrazione;
    }
}
