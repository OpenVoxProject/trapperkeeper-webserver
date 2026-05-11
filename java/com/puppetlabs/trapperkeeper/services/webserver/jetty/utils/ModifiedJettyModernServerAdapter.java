package com.puppetlabs.trapperkeeper.services.webserver.jetty.utils;

import ch.qos.logback.access.jetty.JettyServerAdapter;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom JettyServerAdapter for Jetty 12 compatibility.
 * This may not be needed with logback-access-jetty12 but is kept for compatibility.
 */
public class ModifiedJettyModernServerAdapter extends JettyServerAdapter {
    // these are package private in the JettyServerAdapter, unfortunately
    Request request;
    Response response;

    public ModifiedJettyModernServerAdapter(Request jettyRequest, Response jettyResponse) {
        super(jettyRequest, jettyResponse);
        this.response = jettyResponse;
        this.request = jettyRequest;
    }

    /**
     * buildResponseMap
     * This is a replacement of the buildResponseLog in JettyServerAdapter to
     * make it compatible with Jetty 12 responses.
     * @return a map of the headers.
     */
    @Override
    public Map<String, String> buildResponseHeaderMap() {
        Map<String, String> responseHeaderMap = new HashMap<String,String>();

        for (HttpField httpField : this.response.getHeaders()) {
            String key = httpField.getName();
            String value = httpField.getValue();
            responseHeaderMap.put(key, value);
        }

        return responseHeaderMap;
    }
}
