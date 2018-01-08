package com.hykj.ccbrother.utils;

import java.io.*;

public class StreamUtil {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public StreamUtil() {
    }

    public static void io(InputStream in, OutputStream out) throws IOException {
        io((InputStream)in, (OutputStream)out, -1);
    }

    public static void io(InputStream in, OutputStream out, int bufferSize) throws IOException {
        if(bufferSize == -1) {
            bufferSize = 8192;
        }

        byte[] buffer = new byte[bufferSize];

        int amount;
        while((amount = in.read(buffer)) >= 0) {
            out.write(buffer, 0, amount);
        }

    }

    public static void io(Reader in, Writer out) throws IOException {
        io((Reader)in, (Writer)out, -1);
    }

    public static void io(Reader in, Writer out, int bufferSize) throws IOException {
        if(bufferSize == -1) {
            bufferSize = 4096;
        }

        char[] buffer = new char[bufferSize];

        int amount;
        while((amount = in.read(buffer)) >= 0) {
            out.write(buffer, 0, amount);
        }

    }

    public static OutputStream synchronizedOutputStream(OutputStream out) {
        return new StreamUtil.SynchronizedOutputStream(out);
    }

    public static OutputStream synchronizedOutputStream(OutputStream out, Object lock) {
        return new StreamUtil.SynchronizedOutputStream(out, lock);
    }

    public static String readText(InputStream in) throws IOException {
        return readText(in, (String)null, -1);
    }

    public static String readText(InputStream in, String encoding) throws IOException {
        return readText(in, encoding, -1);
    }

    public static String readText(InputStream in, String encoding, int bufferSize) throws IOException {
        InputStreamReader reader = encoding == null?new InputStreamReader(in):new InputStreamReader(in, encoding);
        return readText(reader, bufferSize);
    }

    public static String readText(Reader reader) throws IOException {
        return readText(reader, -1);
    }

    public static String readText(Reader reader, int bufferSize) throws IOException {
        StringWriter writer = new StringWriter();
        io((Reader)reader, (Writer)writer, bufferSize);
        return writer.toString();
    }

    private static class SynchronizedOutputStream extends OutputStream {
        private OutputStream out;
        private Object lock;

        SynchronizedOutputStream(OutputStream out) {
            this(out, out);
        }

        SynchronizedOutputStream(OutputStream out, Object lock) {
            this.out = out;
            this.lock = lock;
        }

        public void write(int datum) throws IOException {
            Object var2 = this.lock;
            synchronized(this.lock) {
                this.out.write(datum);
            }
        }

        public void write(byte[] data) throws IOException {
            Object var2 = this.lock;
            synchronized(this.lock) {
                this.out.write(data);
            }
        }

        public void write(byte[] data, int offset, int length) throws IOException {
            Object var4 = this.lock;
            synchronized(this.lock) {
                this.out.write(data, offset, length);
            }
        }

        public void flush() throws IOException {
            Object var1 = this.lock;
            synchronized(this.lock) {
                this.out.flush();
            }
        }

        public void close() throws IOException {
            Object var1 = this.lock;
            synchronized(this.lock) {
                this.out.close();
            }
        }
    }
}
