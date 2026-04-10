package com.puppetlabs.trapperkeeper.services.webserver.jetty10.utils;

import ch.qos.logback.access.joran.JoranConfigurator;
import ch.qos.logback.access.jetty.JettyServerAdapter;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.processor.AppenderDeclarationAnalyser;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.color.ConverterSupplierByClassName;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.spi.FilterReply;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Provide an alternative RequestLogImpl implementation that is compatible with Jetty 10
 */
public class ModifiedRequestLogImpl extends RequestLogImpl {
    // unfortunately, this field is private in RequestLogImpl, so we provide our own version of it, and recreate
    // all the functions in the RequestLogImpl that use it
    AppenderAttachableImpl<IAccessEvent> aai = new AppenderAttachableImpl<>();

    /**
     * Bridges the binary incompatibility between logback-access 1.3.x and logback-core 1.4+
     * by substituting {@link PatchedPatternLayoutEncoder} via a custom {@link JoranConfigurator}.
     */
    @Override
    protected void configure() {
        URL configUrl = getConfigurationFileURL();
        if (configUrl == null) {
            getStatusManager().add(new ErrorStatus("Could not find configuration file for logback-access", this));
            return;
        }

        // logback-core 1.4+ replaced PATTERN_RULE_REGISTRY (Map<String,String>) with
        // PATTERN_RULE_REGISTRY_FOR_SUPPLIERS (Map<String,Supplier<DynamicConverter>>).
        // Migrate any String entries (e.g. from MDCAccessLogConverter) into the supplier
        // registry before Joran runs; skip non-String values from prior configure() calls.
        Object existingRegistry = getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (existingRegistry instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) existingRegistry;
            Map<String, Supplier<DynamicConverter>> supplierRegistry = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    String key = (String) entry.getKey();
                    String className = (String) entry.getValue();
                    ConverterSupplierByClassName supplier = new ConverterSupplierByClassName(key, className);
                    supplier.setContext(this);
                    supplierRegistry.put(key, supplier);
                }
                // skip non-String entries (e.g. Supplier lambdas from a prior run)
            }
            if (!supplierRegistry.isEmpty()) {
                // Merge into any existing supplier registry
                @SuppressWarnings("unchecked")
                Map<String, Supplier<DynamicConverter>> existing =
                    (Map<String, Supplier<DynamicConverter>>) getObject(CoreConstants.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS);
                if (existing == null) {
                    putObject(CoreConstants.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS, supplierRegistry);
                } else {
                    existing.putAll(supplierRegistry);
                }
            }
            // Reset to an empty String map; caterForLegacyConverterMaps expects
            // String values and will fail on any Supplier values left in the registry.
            putObject(CoreConstants.PATTERN_RULE_REGISTRY, new HashMap<String, String>());
        }

        final JoranConfigurator configurator = getJoranConfigurator();
        try {
            configurator.doConfigure(configUrl);
        } catch (JoranException e) {
            // errors already added to status manager
        }
        if (getName() == null) {
            setName("LogbackRequestLog");
        }
    }

    private JoranConfigurator getJoranConfigurator() {
        JoranConfigurator configurator = new JoranConfigurator() {
            @Override
            protected void addDefaultNestedComponentRegistryRules(DefaultNestedComponentRegistry registry) {
                registry.add(AppenderBase.class, "layout", PatchedPatternLayout.class);
                registry.add(AppenderBase.class, "encoder", PatchedPatternLayoutEncoder.class);
                registry.add(UnsynchronizedAppenderBase.class, "encoder", PatchedPatternLayoutEncoder.class);
            }

            @Override
            protected void addModelHandlerAssociations(DefaultProcessor processor) {
                super.addModelHandlerAssociations(processor);
                // logback-core 1.5.x's AppenderRefModelHandler requires AppenderDeclarationAnalyser
                // to be registered as a first-phase analyser for AppenderModel. logback-access 1.3.x's
                // ModelClassToModelHandlerLinker was compiled against 1.3.x and doesn't register it.
                processor.addAnalyser(AppenderModel.class,
                        () -> new AppenderDeclarationAnalyser(getContext()));
            }
        };
        configurator.setContext(this);
        return configurator;
    }


    /**
     * log - override the log funciton in RequestLogImpl for the sole purpose of changing the type of the `makeJettyServerAdapter`
     *       to make it compatible with jetty10.
     * @param jettyRequest
     * @param jettyResponse
     */
    @Override
    public void log(Request jettyRequest, Response jettyResponse) {
        JettyServerAdapter adapter = this.makeJettyServerAdapter(jettyRequest, jettyResponse);
        IAccessEvent accessEvent = new AccessEvent(this, jettyRequest, jettyResponse, adapter);
        if (this.getFilterChainDecision(accessEvent) != FilterReply.DENY) {
            this.aai.appendLoopOnAppenders(accessEvent);
        }
    }

    /**
     * Provide a new JettyServerAdapter that is compatible with Jetty 10.
     * @param jettyRequest
     * @param jettyResponse
     * @return
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
