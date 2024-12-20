package net.java.faker.util.logging;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class LoggerFileStream extends PrintStream {

    protected final String name;
    final FileOutputStream fileOutputStream;

    public LoggerFileStream(final String name, final OutputStream out, final File file) {
        super(out);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (Exception ignored) {

        }
        this.fileOutputStream = fileOutputStream;
        this.name = name;
    }

    @Override
    public void flush() {
        super.flush();
        try {
            if (fileOutputStream != null) {
                fileOutputStream.flush();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        try {
            if (fileOutputStream != null) {
                fileOutputStream.write(buf, off, len);
            }
        } catch (Exception ignored) {
        }
    }


}
