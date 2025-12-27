package com.puppetlabs.trapperkeeper.services.webserver.jetty.utils;

import java.util.Map;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import org.slf4j.MDC;

/**
 * A Jetty 12 Handler.Wrapper implementation that imbues Request objects
 * with a copy of the SLF4J Mapped Diagnostic Context (MDC) and handles clearing
 * the MDC after each request.
 *
 * In Jetty 12, RequestLogHandler was removed. This replaces MDCRequestLogHandler
 * from Jetty 10 with the new Handler.Wrapper pattern.
 *
 * The MDC is captured after the wrapped handler completes (but before the
 * response callback finishes), ensuring that any MDC values set during request
 * handling are available to the access logger.
 *
 * Patterned after:
 *   https://github.com/jetty-project/jetty-and-logback-example/blob/master/jetty-slf4j-mdc-handler/src/main/java/org/eclipse/jetty/examples/logging/MDCHandler.java
 */
public class MDCHandler extends Handler.Wrapper {
    public static final String MDC_ATTR = "com.puppetlabs.trapperkeeper.services.webserver.jetty12.MDC";

    public MDCHandler(Handler handler) {
        super(handler);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        // Save any existing MDC context to restore after the request
        Map<String, String> savedContext = MDC.getCopyOfContextMap();

        // Wrap the callback to capture MDC after handler completes
        Callback wrappedCallback = new Callback() {
            @Override
            public void succeeded() {
                try {
                    // Capture MDC after handler has run but before response completes
                    // This ensures MDC values set during request handling are available
                    Map<String, String> mdcPropertyMap = MDC.getCopyOfContextMap();
                    request.setAttribute(MDC_ATTR, mdcPropertyMap);
                } finally {
                    // Clean up MDC for this thread to prevent contamination
                    if (savedContext != null) {
                        MDC.setContextMap(savedContext);
                    } else {
                        MDC.clear();
                    }
                }
                callback.succeeded();
            }

            @Override
            public void failed(Throwable x) {
                try {
                    // Still capture MDC on failure for logging purposes
                    Map<String, String> mdcPropertyMap = MDC.getCopyOfContextMap();
                    request.setAttribute(MDC_ATTR, mdcPropertyMap);
                } finally {
                    // Clean up MDC for this thread
                    if (savedContext != null) {
                        MDC.setContextMap(savedContext);
                    } else {
                        MDC.clear();
                    }
                }
                callback.failed(x);
            }
        };

        return super.handle(request, response, wrappedCallback);
    }
}
