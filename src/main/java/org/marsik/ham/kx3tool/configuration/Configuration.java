package org.marsik.ham.kx3tool.configuration;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static final String APP_DIR_NAME = "org.marsik.ham.kx3tool";
    private static final String CONFIGURATION_FILE_NAME = "configuration.properties";
    private Properties properties = new Properties();

    public Configuration() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("default.properties")) {
            properties.load(is);
        } catch (IOException e) {
            logger.error("Could not load the default config: {}", e);
        }

        Path configFile = configHome().resolve(CONFIGURATION_FILE_NAME);
        try (InputStream is = Files.newInputStream(configFile)) {
            properties.load(is);
        } catch (IOException e) {
            logger.warn("Config file {} is not readable.", configFile);
        }
    }

    public Macro getMacro(int id) {
        String name = properties.getProperty("macro." + id + ".name", "Macro " + id);
        String value = properties.getProperty("macro." + id + ".value", "");
        return new Macro(name, value);
    }

    public void setMacro(int id, Macro macro) {
        properties.put("macro." + id + ".name", macro.getName());
        properties.put("macro." + id + ".value", macro.getValue());
        flush();
    }

    private void flush() {
        Path root = configHome();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            logger.error("Could not create configuration directory: {}", e);
            return;
        }

        try (OutputStream os = Files.newOutputStream(root.resolve(CONFIGURATION_FILE_NAME))) {
            properties.store(os, "Configuration for the application");
        } catch (IOException e) {
            logger.error("Could not write configuration: {}", e);
        }
    }

    private static Path configHome() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("XDG_CONFIG_HOME")) {
            return Paths.get(env.get("XDG_CONFIG_HOME"), APP_DIR_NAME);
        } else if (env.containsKey("APPDATA")) {
            return Paths.get(env.get("APPDATA"), APP_DIR_NAME);
        } else if ("Mac OS X".equals(System.getProperty("os.name"))) {
            return Paths.get(env.get("HOME"), "Library", "Preferences", APP_DIR_NAME);
        } else if ("Linux".equals(System.getProperty("os.name"))) {
            return Paths.get(env.get("HOME"), ".config", APP_DIR_NAME);
        } else {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    private static Path dataHome() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("XDG_DATA_HOME")) {
            return Paths.get(env.get("XDG_DATA_HOME"), APP_DIR_NAME);
        } else if (env.containsKey("APPDATA")) {
            return Paths.get(env.get("APPDATA"), APP_DIR_NAME);
        } else if ("Mac OS X".equals(System.getProperty("os.name"))) {
            return Paths.get(env.get("HOME"), "Library", "Application Support", APP_DIR_NAME);
        } else if ("Linux".equals(System.getProperty("os.name"))) {
            return Paths.get(env.get("HOME"), ".local", "share", APP_DIR_NAME);
        } else {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    public static Path logbookFile() {
        return dataHome().resolve("logbook.h2");
    }
}
