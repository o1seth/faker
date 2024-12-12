package net.java.mproxy.ui;

import net.java.mproxy.Proxy;

import javax.swing.*;
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

        currentLocale = Proxy.getSaveManager().get("locale");
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
        if (Proxy.getSaveManager() == null) {
            throw new IllegalStateException("ViaProxy is not yet initialized");
        }

        currentLocale = locale;
        Proxy.getSaveManager().put("locale", locale);
        Proxy.getSaveManager().save();
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
