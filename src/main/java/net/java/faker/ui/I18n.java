package net.java.faker.ui;

import net.java.faker.Proxy;
import net.java.faker.util.Util;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class I18n {
    private static final String DEFAULT_LOCALE = "en_US";
    private static Map<String, Properties> LOCALES = new LinkedHashMap<>();
    private static String currentLocale;

    static {
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
        for (Map.Entry<String, Properties> locale : LOCALES.entrySet()) {
            locale.getValue().entrySet().removeIf(line -> line.getValue().toString().isEmpty());
        }
        final int totalTranslation = LOCALES.get(DEFAULT_LOCALE).size();
        for (Properties properties : LOCALES.values()) {
            final int translated = properties.size();
            final float percentage = (float) translated / totalTranslation * 100;
            properties.put("language.completion", (int) Math.floor(percentage) + "%");
        }
    }

    private static class KeyAndConsumer {
        final String key;
        final BiConsumer<Object, String> consumer;

        @SuppressWarnings("unchecked")
        KeyAndConsumer(String key, BiConsumer<?, String> consumer) {
            this.key = key;
            this.consumer = (BiConsumer<Object, String>) consumer;
        }
    }

    static WeakHashMap<Object, KeyAndConsumer> links = new WeakHashMap<>();

    static WeakHashMap<Object, KeyAndConsumer> tooltipLinks = new WeakHashMap<>();

    public static void update() {
        for (Map.Entry<Object, KeyAndConsumer> e : links.entrySet()) {
            KeyAndConsumer keyAndConsumer = e.getValue();
            Object component = e.getKey();
            keyAndConsumer.consumer.accept(component, keyAndConsumer.key);
        }
        for (Map.Entry<Object, KeyAndConsumer> e : tooltipLinks.entrySet()) {
            KeyAndConsumer keyAndConsumer = e.getValue();
            Object component = e.getKey();
            keyAndConsumer.consumer.accept(component, keyAndConsumer.key);
        }
    }

    public static void link(UITab tab, final String key) {
        BiConsumer<UITab, String> consumer = (t, s) -> t.getOwner().setTitleAt(t.getIndex(), get(s));
        links.put(tab, new KeyAndConsumer(key, consumer));
        consumer.accept(tab, key);
    }

    public static void link(TitledBorder border, final String key) {
        BiConsumer<TitledBorder, String> consumer = (t, s) -> t.setTitle(get(s));
        links.put(border, new KeyAndConsumer(key, consumer));
        consumer.accept(border, key);
    }

    public static void link(JLabel component, final String key, BiConsumer<JLabel, String> consumer) {
        links.put(component, new KeyAndConsumer(key, consumer));
        consumer.accept(component, key);
    }

    public static void link(Component component, final String key) {
        if (component instanceof JLabel jLabel) {
            BiConsumer<JLabel, String> consumer = (t, s) -> t.setText(get(s));
            links.put(jLabel, new KeyAndConsumer(key, consumer));
            consumer.accept(jLabel, key);
        }
        if (component instanceof AbstractButton abstractButton) {
            BiConsumer<AbstractButton, String> consumer = (t, s) -> t.setText(get(s));
            links.put(abstractButton, new KeyAndConsumer(key, consumer));
            consumer.accept(abstractButton, key);
        }
    }

    public static void link(MenuItem menuItem, final String key) {
        BiConsumer<MenuItem, String> consumer = (t, s) -> t.setLabel(get(s));
        links.put(menuItem, new KeyAndConsumer(key, consumer));
        consumer.accept(menuItem, key);
    }

    public static void linkTooltip(Component component, final String key) {
        if (component instanceof JLabel jLabel) {
            BiConsumer<JLabel, String> consumer = (t, s) -> t.setToolTipText(get(s));
            tooltipLinks.put(component, new KeyAndConsumer(key, consumer));
            consumer.accept(jLabel, key);
        }
        if (component instanceof AbstractButton abstractButton) {
            BiConsumer<AbstractButton, String> consumer = (t, s) -> t.setToolTipText(get(s));
            tooltipLinks.put(component, new KeyAndConsumer(key, consumer));
            consumer.accept(abstractButton, key);
        }
        if (component instanceof JTextComponent jTextComponent) {
            BiConsumer<JTextComponent, String> consumer = (t, s) -> t.setToolTipText(get(s));
            tooltipLinks.put(component, new KeyAndConsumer(key, consumer));
            consumer.accept(jTextComponent, key);
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
        URL url = I18n.class.getClassLoader().getResource(assetPath);
        if (url == null) {
            return null;
        }
        final Path path = getPath(url.toURI());
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
