/*
 * Copyright 2004-2022 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: read-write file channel over an in-memory file.
 */
package org.h2.store.fs.wasm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import org.h2.store.fs.FakeFileChannel;
import org.h2.store.fs.FileBaseDefault;

/**
 * A file channel backed by a {@link FilePathWasm.Entry}.
 */
final class FileWasm extends FileBaseDefault {

    private final FilePathWasm.Entry entry;

    private final boolean readOnly;

    private volatile boolean closed;

    FileWasm(FilePathWasm.Entry entry, boolean readOnly) {
        this.entry = entry;
        this.readOnly = readOnly;
    }

    @Override
    public long size() {
        return entry.length();
    }

    @Override
    protected void implTruncate(long newLength) throws IOException {
        if (readOnly) {
            throw new NonWritableChannelException();
        }
        if (closed) {
            throw new ClosedChannelException();
        }
        if (newLength < 0) {
            throw new IOException("negative length");
        }
        entry.truncate((int) newLength);
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        if (closed) {
            throw new ClosedChannelException();
        }
        if (position < 0) {
            throw new IOException("negative position");
        }
        int length = entry.length();
        if (position >= length) {
            return -1;
        }
        int count = (int) Math.min(dst.remaining(), length - position);
        byte[] buffer = new byte[count];
        int n = entry.read(position, buffer, 0, count);
        if (n <= 0) {
            return -1;
        }
        dst.put(buffer, 0, n);
        return n;
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        if (readOnly) {
            throw new NonWritableChannelException();
        }
        if (closed) {
            throw new ClosedChannelException();
        }
        if (position < 0) {
            throw new IOException("negative position");
        }
        int count = src.remaining();
        if (count == 0) {
            return 0;
        }
        byte[] buffer = new byte[count];
        src.get(buffer);
        entry.write(position, buffer, 0, count);
        return count;
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) {
        // RenYuan wasm branch: there is a single process and a single database image,
        // so locking always succeeds and does nothing.
        return new FileLock(FakeFileChannel.INSTANCE, position, size, shared) {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public void release() {
            }
        };
    }

    @Override
    protected void implCloseChannel() {
        closed = true;
    }
}
