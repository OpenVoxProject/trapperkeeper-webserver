package com.puppetlabs.trapperkeeper.services.webserver.jetty10.utils;

import ch.qos.logback.access.PatternLayout;
import ch.qos.logback.core.pattern.DynamicConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bridges logback-access 1.3.x's {@link PatternLayout} with logback-core 1.4+.
 *
 * <ul>
 *   <li>Implements {@code getDefaultConverterSupplierMap()} (added abstract in 1.4) returning empty.</li>
 *   <li>Filters {@code getDefaultConverterMap()} to String-only entries, since logback-core 1.5.x's
 *       {@code Parser.DEFAULT_COMPOSITE_CONVERTER_MAP} is {@code Map<String,Supplier>} and
 *       pollutes the {@code Map<String,String>}, causing {@code migrateFromStringMapToSupplierMap}
 *       to throw {@link ClassCastException}.</li>
 *   <li>Sanitizes {@code PATTERN_RULE_REGISTRY} before {@code super.start()} for the same reason.</li>
 * </ul>
 */
public class PatchedPatternLayout extends PatternLayout {

    @Override
    public Map<String, Supplier<DynamicConverter>> getDefaultConverterSupplierMap() {
        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, String> getDefaultConverterMap() {
        Map<String, String> clean = new HashMap<>();
        Map raw = super.getDefaultConverterMap();
        for (Object key : raw.keySet()) {
            Object value = raw.get(key);
            if (key instanceof String && value instanceof String) {
                clean.put((String) key, (String) value);
            }
        }
        return clean;
    }

    @Override
    public void start() {
        // Sanitize PATTERN_RULE_REGISTRY before super.start() calls caterForLegacyConverterMaps;
        // logback-core 1.5.x may have written Supplier values into it, causing ClassCastException.
        if (getContext() != null) {
            Object reg = getContext().getObject(ch.qos.logback.core.CoreConstants.PATTERN_RULE_REGISTRY);
            if (reg instanceof java.util.Map) {
                java.util.Map<?, ?> rawMap = (java.util.Map<?, ?>) reg;
                if (rawMap.values().stream().anyMatch(v -> !(v instanceof String))) {
                    getContext().putObject(ch.qos.logback.core.CoreConstants.PATTERN_RULE_REGISTRY,
                                          new java.util.HashMap<String, String>());
                }
            }
        }
        super.start();
    }
}
