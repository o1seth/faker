/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
