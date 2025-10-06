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

package net.java.faker.util;

import net.java.faker.ui.Window;
import net.java.faker.ui.tab.GeneralTab;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

public class Util {

    public static ImageIcon getResourceImageIcon(String path) {
        URL url = GeneralTab.class.getResource(path);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url);
    }

    public static Image getResourceImage(String res) {
        try {
            return Toolkit.getDefaultToolkit().getImage(Window.class.getResource(res));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static byte[] getResourceBytes(String res) {
        try {

            InputStream is = Util.class.getResourceAsStream(res);
            if (is == null) {
                return null;
            }
            byte[] arrBuffer = new byte[16384];
            ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());

            int read = 0;
            while ((read = is.read(arrBuffer)) != -1) {
                baos.write(arrBuffer, 0, read);
            }
            is.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
