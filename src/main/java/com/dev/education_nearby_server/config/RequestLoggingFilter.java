package com.dev.education_nearby_server.config;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_COMPLETED_MESSAGE =
            "HTTP request completed. method={} path={} status={} durationMs={}";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);

        try {
            filterChain.doFilter(request, response);
            if (request.isAsyncStarted()) {
                registerAsyncListener(request, startNanos, method, path, requestId);
                return;
            }
            logCompletion(response.getStatus(), method, path, startNanos);
        } catch (Exception ex) {
            if (request.isAsyncStarted()) {
                registerAsyncListener(request, startNanos, method, path, requestId);
            } else {
                logCompletion(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, method, path, startNanos);
            }
            throw ex;
        }
    }

    private void registerAsyncListener(
            HttpServletRequest request,
            long startNanos,
            String method,
            String path,
            String requestId
    ) {
        request.getAsyncContext().addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                logAsyncCompletion(event.getSuppliedResponse());
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                logAsyncCompletion(event.getSuppliedResponse());
            }

            @Override
            public void onError(AsyncEvent event) {
                logAsyncCompletion(event.getSuppliedResponse());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                // no-op
            }

            private void logAsyncCompletion(ServletResponse servletResponse) {
                HttpServletResponse httpResponse = servletResponse instanceof HttpServletResponse
                        ? (HttpServletResponse) servletResponse
                        : null;
                int status = httpResponse != null ? httpResponse.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

                if (requestId != null) {
                    MDC.put(RequestIdFilter.MDC_KEY, requestId);
                }
                try {
                    logCompletion(status, method, path, startNanos);
                } finally {
                    if (requestId != null) {
                        MDC.remove(RequestIdFilter.MDC_KEY);
                    }
                }
            }
        });
    }

    private void logCompletion(int status, String method, String path, long startNanos) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        if (status >= 500) {
            log.error(REQUEST_COMPLETED_MESSAGE, method, path, status, durationMs);
        } else if (status >= 400) {
            log.warn(REQUEST_COMPLETED_MESSAGE, method, path, status, durationMs);
        } else {
            log.info(REQUEST_COMPLETED_MESSAGE, method, path, status, durationMs);
        }
    }
}
