package net.java.mproxy.ui.elements;

import net.java.mproxy.ui.Window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LinkLabel extends JLabel {

    public LinkLabel(final String text, final String url) {
        super("<html><a href=\"\">" + text + "</a></html>");
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Window.openURL(url);
            }
        });
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}