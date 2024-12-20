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
}
