package it.prgweb.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Fallback per il routing HTML5 di Angular: le URL senza estensione che non
 * appartengono alle API vengono servite con index.html.
 */
@WebFilter(urlPatterns = "/*")
public class SpaFallbackFilter implements Filter {

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
            return;
        }
        chain.doFilter(request, response);
    }
}
