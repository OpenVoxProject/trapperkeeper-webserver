package com.puppetlabs.trapperkeeper.services.webserver.jetty.utils;

import ch.qos.logback.access.jetty.JettyServerAdapter;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.access.jetty.RequestWrapper;
import ch.qos.logback.access.jetty.ResponseWrapper;
import ch.qos.logback.access.common.spi.AccessEvent;
import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.spi.FilterReply;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.util.Iterator;

/**
 * Provide an alternative RequestLogImpl implementation that is compatible with Jetty 12.
 * Uses RequestWrapper and ResponseWrapper to convert core Jetty types to servlet types.
 */
public class ModifiedRequestLogImpl extends RequestLogImpl {
    // unfortunately, this field is private in RequestLogImpl, so we provide our own version of it, and recreate
    // all the functions in the RequestLogImpl that use it
    AppenderAttachableImpl<IAccessEvent> aai = new AppenderAttachableImpl<IAccessEvent>();

    /**
     * log - override the log funciton in RequestLogImpl for the sole purpose of changing the type of the `makeJettyServerAdapter`
     *       to make it compatible with jetty.
     * @param jettyRequest
     * @param jettyResponse
     */
    @Override
    public void log(Request jettyRequest, Response jettyResponse) {
        JettyServerAdapter adapter = this.makeJettyServerAdapter(jettyRequest, jettyResponse);
        // Wrap core Jetty types in servlet wrappers for AccessEvent
        RequestWrapper requestWrapper = new RequestWrapper(jettyRequest);
        ResponseWrapper responseWrapper = new ResponseWrapper(jettyResponse);
        IAccessEvent accessEvent = new AccessEvent(this, requestWrapper, responseWrapper, adapter);
        if (this.getFilterChainDecision(accessEvent) != FilterReply.DENY) {
            this.aai.appendLoopOnAppenders(accessEvent);
        }
    }

    /**
     * Provide a new JettyServerAdapter that is compatible with Jetty 12.
     * @param jettyRequest the Jetty 12 core Request
     * @param jettyResponse the Jetty 12 core Response
     * @return a JettyServerAdapter for the request/response pair
     */
    private JettyServerAdapter makeJettyServerAdapter(Request jettyRequest, Response jettyResponse) {
        return new ModifiedJettyModernServerAdapter(jettyRequest, jettyResponse);
    }

    @Override
    public void stop() {
        this.aai.detachAndStopAllAppenders();
        super.stop();
    }

    @Override
    public void addAppender(Appender<IAccessEvent> newAppender) {
        aai.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<IAccessEvent>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<IAccessEvent> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<IAccessEvent> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<IAccessEvent> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }
}
