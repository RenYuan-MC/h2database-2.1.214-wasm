/*
 * Copyright 2004-2022 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * RenYuan wasm branch: a fully in-memory, writable file system. The database image
 * is passed in from JavaScript and exported back after the engine has closed it.
 */
package org.h2.store.fs.wasm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.h2.store.fs.FilePath;

/**
 * An in-memory file system with a single database image.
 */
public class FilePathWasm extends FilePath {

    /**
     * The scheme of this file system.
     */
    public static final String SCHEME = "wasm";

    /**
     * The name used for the database that is passed in from JavaScript.
     */
    public static final String DATABASE = "wasm:db.mv.db";

    private static final Map<String, Entry> FILES = new LinkedHashMap<>();

    private static volatile String databaseName = DATABASE;

    /**
     * One file in the virtual file system.
     */
    static final class Entry {

        private byte[] data;

        private int length;

        private boolean readOnly;

        private long lastModified;

        Entry(byte[] initial) {
            data = initial.length == 0 ? new byte[1024] : new byte[Math.max(1024, initial.length)];
            System.arraycopy(initial, 0, data, 0, initial.length);
            length = initial.length;
            lastModified = System.currentTimeMillis();
        }

        synchronized int length() {
            return length;
        }

        synchronized long lastModified() {
            return lastModified;
        }

        synchronized void ensureCapacity(int capacity) {
            if (capacity > data.length) {
                int size = data.length;
                while (size < capacity) {
                    size <<= 1;
                }
                byte[] next = new byte[size];
                System.arraycopy(data, 0, next, 0, length);
                data = next;
            }
        }

        synchronized int read(long position, byte[] buffer, int offset, int count) {
            if (position >= length) {
                return -1;
            }
            int n = (int) Math.min(count, length - position);
            System.arraycopy(data, (int) position, buffer, offset, n);
            return n;
        }

        synchronized void write(long position, byte[] buffer, int offset, int count) {
            ensureCapacity((int) (position + count));
            if (position > length) {
                // writing past the end leaves a hole that must read as zeroes
                Arrays.fill(data, length, (int) position, (byte) 0);
            }
            System.arraycopy(buffer, offset, data, (int) position, count);
            if (position + count > length) {
                length = (int) (position + count);
            }
            lastModified = System.currentTimeMillis();
        }

        synchronized void truncate(int newLength) {
            if (newLength < length) {
                length = newLength;
            }
            lastModified = System.currentTimeMillis();
        }

        synchronized byte[] copy() {
            byte[] result = new byte[length];
            System.arraycopy(data, 0, result, 0, length);
            return result;
        }
    }

    /**
     * Load the database image that JavaScript passed in.
     *
     * @param data the database file bytes
     */
    public static void loadDatabase(byte[] data) {
        clear();
        synchronized (FILES) {
            FILES.put(DATABASE, new Entry(data));
        }
        databaseName = DATABASE;
    }

    /**
     * Get the current database image.
     *
     * @return the database file bytes, or {@code null} if none is loaded
     */
    public static byte[] exportDatabase() {
        synchronized (FILES) {
            Entry entry = FILES.get(databaseName);
            return entry != null ? entry.copy() : null;
        }
    }

    /**
     * Drop every file of the virtual file system.
     */
    public static void clear() {
        synchronized (FILES) {
            FILES.clear();
        }
    }

    static Entry entry(String name, boolean create) {
        synchronized (FILES) {
            Entry entry = FILES.get(name);
            if (entry == null && create) {
                entry = new Entry(new byte[0]);
                FILES.put(name, entry);
            }
            return entry;
        }
    }

    static void remove(String name) {
        synchronized (FILES) {
            FILES.remove(name);
        }
    }

    static boolean contains(String name) {
        synchronized (FILES) {
            return FILES.containsKey(name);
        }
    }

    public FilePathWasm() {
    }

    private FilePathWasm(String name) {
        this.name = name;
    }

    @Override
    public FilePath getPath(String path) {
        return new FilePathWasm(path.replace('\\', '/'));
    }

    @Override
    public long size() {
        Entry entry = entry(name, false);
        return entry != null ? entry.length() : 0;
    }

    @Override
    public void moveTo(FilePath newName, boolean atomicReplace) {
        String target = newName.toString();
        synchronized (FILES) {
            Entry entry = FILES.get(name);
            if (entry == null) {
                throw new IllegalStateException("File not found: " + name);
            }
            if (!atomicReplace && FILES.containsKey(target)) {
                throw new IllegalStateException("File exists: " + target);
            }
            FILES.remove(name);
            FILES.put(target, entry);
        }
    }

    @Override
    public boolean createFile() {
        synchronized (FILES) {
            if (FILES.containsKey(name)) {
                return false;
            }
            FILES.put(name, new Entry(new byte[0]));
            return true;
        }
    }

    @Override
    public boolean exists() {
        return contains(name);
    }

    @Override
    public void delete() {
        remove(name);
    }

    @Override
    public List<FilePath> newDirectoryStream() {
        List<FilePath> list = new ArrayList<>();
        String prefix = name.endsWith("/") ? name : name + "/";
        synchronized (FILES) {
            for (String file : FILES.keySet()) {
                if (file.startsWith(prefix)) {
                    String rest = file.substring(prefix.length());
                    if (!rest.isEmpty() && rest.indexOf('/') < 0) {
                        list.add(new FilePathWasm(file));
                    }
                }
            }
        }
        return list;
    }

    @Override
    public FilePath toRealPath() {
        return this;
    }

    @Override
    public FilePath getParent() {
        int index = name.lastIndexOf('/');
        return index < 0 ? null : new FilePathWasm(name.substring(0, index));
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isRegularFile() {
        return exists();
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public long lastModified() {
        Entry entry = entry(name, false);
        return entry != null ? entry.lastModified() : 0L;
    }

    @Override
    public boolean canWrite() {
        Entry entry = entry(name, false);
        return entry != null && !entry.readOnly;
    }

    @Override
    public void createDirectory() {
        // directories are implicit
    }

    @Override
    public java.nio.channels.FileChannel open(String mode) throws java.io.IOException {
        Entry entry = entry(name, true);
        return new FileWasm(entry, "r".equals(mode));
    }

    @Override
    public java.io.OutputStream newOutputStream(boolean append) throws java.io.IOException {
        // RenYuan wasm branch: Channels.newOutputStream() is not implemented by the
        // TeaVM class library, so the streams are built directly on the entry.
        final Entry entry = entry(name, true);
        final FileWasm channel = new FileWasm(entry, false);
        if (append) {
            channel.position(entry.length());
        } else {
            entry.truncate(0);
        }
        return new java.io.OutputStream() {
            private final java.nio.ByteBuffer single = java.nio.ByteBuffer.allocate(1);

            @Override
            public void write(int b) throws java.io.IOException {
                single.clear();
                single.put((byte) b);
                single.flip();
                channel.write(single);
            }

            @Override
            public void write(byte[] b, int off, int len) throws java.io.IOException {
                channel.write(java.nio.ByteBuffer.wrap(b, off, len));
            }

            @Override
            public void close() throws java.io.IOException {
                channel.close();
            }
        };
    }

    @Override
    public java.io.InputStream newInputStream() throws java.io.IOException {
        final FileWasm channel = (FileWasm) open("r");
        return new java.io.InputStream() {

            @Override
            public int read() throws java.io.IOException {
                java.nio.ByteBuffer single = java.nio.ByteBuffer.allocate(1);
                int n = channel.read(single);
                return n <= 0 ? -1 : single.get(0) & 0xff;
            }

            @Override
            public int read(byte[] b, int off, int len) throws java.io.IOException {
                if (len == 0) {
                    return 0;
                }
                return channel.read(java.nio.ByteBuffer.wrap(b, off, len));
            }

            @Override
            public void close() throws java.io.IOException {
                channel.close();
            }
        };
    }

    @Override
    public boolean setReadOnly() {
        Entry entry = entry(name, false);
        if (entry != null) {
            entry.readOnly = true;
        }
        return true;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }
}
