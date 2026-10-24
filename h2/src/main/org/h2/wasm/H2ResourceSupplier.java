/*
 * Copyright 2004-2024 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.wasm;

import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

/**
 * Declares classpath resources that have to be embedded into the WebAssembly module.
 * TeaVM only ships resources that are listed by a registered {@link ResourceSupplier}.
 */
public final class H2ResourceSupplier implements ResourceSupplier {
    private static final String[] RESOURCES = {
            "org/h2/res/_messages_en.prop",
            "org/h2/res/help.csv",
            "org/h2/res/javadoc.properties"
    };

    @Override
    public String[] supplyResources(ResourceSupplierContext context) {
        return RESOURCES.clone();
    }
}
