package com.puppetlabs.trapperkeeper.services.webserver.jetty10.utils;

import ch.qos.logback.access.PatternLayoutEncoder;

/**
 * Bridges logback-access 1.3.x and logback-core 1.4+. Extends {@link PatternLayoutEncoder}
 * to preserve cast compatibility, but overrides start() to install {@link PatchedPatternLayout}
 * directly rather than letting the parent overwrite it with a plain PatternLayout.
 */
public class PatchedPatternLayoutEncoder extends PatternLayoutEncoder {

    @Override
    public void start() {
        PatchedPatternLayout patchedLayout = new PatchedPatternLayout();
        patchedLayout.setContext(context);
        patchedLayout.setPattern(getPattern());
        patchedLayout.start();
        this.layout = patchedLayout;
        this.started = true; // bypass super.start() to avoid overwriting our layout
    }
}
