package net.java.mproxy.ui;

import net.java.mproxy.Proxy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class I18n {
    private static final String DEFAULT_LOCALE = "en_US";
    private static Map<String, Properties> LOCALES = new LinkedHashMap<>();
    private static String currentLocale;

    static {

//        if (ViaProxy.getSaveManager() == null) {
//            throw new IllegalStateException("ViaProxy is not yet initialized");
//        }

        try {
            for (Map.Entry<Path, byte[]> entry : getFilesInDirectory("assets/faker/lang").entrySet()) {
                final Properties properties = new Properties();
                properties.load(new InputStreamReader(new ByteArrayInputStream(entry.getValue()), StandardCharsets.UTF_8));
                LOCALES.put(entry.getKey().getFileName().toString().replace(".properties", ""), properties);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load translations", e);
        }
        LOCALES = LOCALES.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().getProperty("language.name")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> newValue, LinkedHashMap::new));

        currentLocale = Proxy.getConfig().locale.get();
        if (currentLocale == null || !LOCALES.containsKey(currentLocale)) {
            final String systemLocale = Locale.getDefault().getLanguage() + '_' + Locale.getDefault().getCountry();
            if (LOCALES.containsKey(systemLocale)) {
                currentLocale = systemLocale;
            } else {
                for (Map.Entry<String, Properties> entry : LOCALES.entrySet()) {
                    if (entry.getKey().startsWith(Locale.getDefault().getLanguage() + '_')) {
                        currentLocale = entry.getKey();
                        break;
                    }
                }
            }
        }

        final int totalTranslation = LOCALES.get(DEFAULT_LOCALE).size();
        for (Properties properties : LOCALES.values()) {
            final int translated = properties.size();
            final float percentage = (float) translated / totalTranslation * 100;
            properties.put("language.completion", (int) Math.floor(percentage) + "%");
        }
    }

    static WeakHashMap<Object, String> links = new WeakHashMap<>();
    static WeakHashMap<Component, String> tooltipLinks = new WeakHashMap<>();

    public static void update() {
        for (Map.Entry<Object, String> e : links.entrySet()) {
            if (e.getKey() instanceof JLabel jLabel) {
                jLabel.setText(get(e.getValue()));
            }
            if (e.getKey() instanceof AbstractButton abstractButton) {
                abstractButton.setText(get(e.getValue()));
            }
            if (e.getKey() instanceof TitledBorder titledBorder) {
                titledBorder.setTitle(get(e.getValue()));
            }
            if (e.getKey() instanceof UITab tab) {
                tab.getOwner().setTitleAt(tab.getIndex(), get(e.getValue()));
            }
        }
        for (Map.Entry<Component, String> e : tooltipLinks.entrySet()) {
            if (e.getKey() instanceof JLabel jLabel) {
                jLabel.setToolTipText(get(e.getValue()));
            }
            if (e.getKey() instanceof AbstractButton abstractButton) {
                abstractButton.setToolTipText(get(e.getValue()));
            }
        }
    }

    public static void link(UITab tab, final String key) {
        tab.getOwner().setTitleAt(tab.getIndex(), get(key));
        links.put(tab, key);
    }

    public static void link(TitledBorder border, final String key) {
        border.setTitle(get(key));
        links.put(border, key);
    }

    public static void link(Component component, final String key) {
        if (component instanceof JLabel jLabel) {
            jLabel.setText(get(key));
            links.put(component, key);
        }
        if (component instanceof AbstractButton abstractButton) {
            abstractButton.setText(get(key));
            links.put(component, key);
        }
    }

    public static void linkTooltip(Component component, final String key) {
        if (component instanceof JLabel jLabel) {
            jLabel.setToolTipText(get(key));
            tooltipLinks.put(component, key);
        }
        if (component instanceof AbstractButton abstractButton) {
            abstractButton.setToolTipText(get(key));
            tooltipLinks.put(component, key);
        }
    }

    public static String get(final String key) {
        return getSpecific(currentLocale, key);
    }

    public static String get(final String key, String... args) {
        return String.format(getSpecific(currentLocale, key), (Object[]) args);
    }

    public static String getSpecific(final String locale, final String key) {

        Properties properties = LOCALES.get(locale);
        if (properties == null) {
            properties = LOCALES.get(DEFAULT_LOCALE);
        }
        String value = properties.getProperty(key);
        if (value == null) {
            value = LOCALES.get(DEFAULT_LOCALE).getProperty(key);
        }
        if (value == null) {
            return "Missing translation for key: " + key;
        }
        return value;
    }

    public static String getCurrentLocale() {
        return currentLocale;
    }

    public static void setLocale(final String locale) {
        currentLocale = locale;
        Proxy.getConfig().locale.set(locale);
        Proxy.getConfig().save();
    }

    public static Collection<String> getAvailableLocales() {
        return LOCALES.keySet();
    }

    private static Map<Path, byte[]> getFilesInDirectory(final String assetPath) throws IOException, URISyntaxException {
        final Path path = getPath(I18n.class.getClassLoader().getResource(assetPath).toURI());
        return getFilesInPath(path);
    }

    @SuppressWarnings({"DuplicateExpressions", "resource"})
    private static Path getPath(final URI uri) throws IOException {
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException e) {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
            return Paths.get(uri);
        }
    }

    private static Map<Path, byte[]> getFilesInPath(final Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toMap(f -> f, f -> {
                        try {
                            return Files.readAllBytes(f);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }, (u, v) -> {
                        throw new IllegalStateException("Duplicate key");
                    }, LinkedHashMap::new));
        }
    }
}
