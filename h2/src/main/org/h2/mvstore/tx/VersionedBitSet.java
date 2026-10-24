/*
 * Copyright 2004-2022 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore.tx;

import java.util.BitSet;

/**
 * Class VersionedBitSet extends standard BitSet to add a version field.
 * This will allow bit set and version to be changed atomically.
 */
final class VersionedBitSet extends BitSet {
    private static final long serialVersionUID = 1L;

    private long version;

    public VersionedBitSet() {}

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public VersionedBitSet clone() {
        // RenYuan wasm branch: TeaVM's BitSet.clone() creates a plain BitSet and loses
        // the subclass, so the state is copied explicitly instead of casting the result.
        VersionedBitSet copy = new VersionedBitSet();
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
            copy.set(i);
        }
        copy.version = version;
        return copy;
    }
}
