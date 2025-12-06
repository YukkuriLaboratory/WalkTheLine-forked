package games.polarbearbytes.walktheline.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import games.polarbearbytes.walktheline.WalkTheLine;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class for managing the configuration file serialization and deserialization
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "walk-the-line.json");

    private static WalkTheLineConfig config;

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            config = new WalkTheLineConfig(); // default
            saveConfig();
        } else {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, WalkTheLineConfig.class);
            } catch (IOException e) {
                WalkTheLine.LOGGER.error(e.getMessage());
                config = new WalkTheLineConfig(); // fallback
            }
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            WalkTheLine.LOGGER.warn(e.getMessage());
        }
    }

    public static WalkTheLineConfig getConfig() {
        if(config == null) {
            loadConfig();
            WalkTheLine.LOGGER.info("Config loaded!");
        }
        return config;
    }
}