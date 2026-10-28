package oreexcavation.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import oreexcavation.core.OreExcavation;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

public class JsonHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static JsonArray GetArray(@Nonnull JsonObject json, @Nonnull String id) {
        JsonElement element = json.get(id);
        return element instanceof JsonArray ? element.getAsJsonArray() : new JsonArray();
    }

    public static JsonObject GetObject(@Nonnull JsonObject json, @Nonnull String id) {
        JsonElement element = json.get(id);
        return element instanceof JsonObject ? element.getAsJsonObject() : new JsonObject();
    }

    public static String GetString(@Nonnull JsonObject json, @Nonnull String id, @Nonnull String def) {
        JsonElement element = json.get(id);
        return element instanceof JsonPrimitive && element.getAsJsonPrimitive().isString() ? element.getAsString() : def;
    }

    public static Number GetNumber(@Nonnull JsonObject json, @Nonnull String id, @Nonnull Number def) {
        JsonElement element = json.get(id);
        try {
            return element instanceof JsonPrimitive ? element.getAsNumber() : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean GetBoolean(@Nonnull JsonObject json, @Nonnull String id, boolean def) {
        JsonElement element = json.get(id);
        try {
            return element instanceof JsonPrimitive ? element.getAsBoolean() : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static JsonElement getElement(@Nonnull JsonObject json, @Nonnull String id, @Nonnull Supplier<JsonElement> def) {
        JsonElement element = json.get(id);
        return element != null ? element : def.get();
    }

    public static JsonObject ReadFromFile(File file) {
        if (file == null || !file.exists()) return new JsonObject();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return json == null ? new JsonObject() : json;
        } catch (Exception e) {
            OreExcavation.logger.error("An error occured while loading JSON from file:", e);
            createBackup(file, "malformed");
            return new JsonObject();
        }
    }

    public static void WriteToFile(File file, JsonObject json) {
        File tmp = new File(file.getAbsolutePath() + ".tmp");
        try {
            if (tmp.exists()) {
                if (!tmp.delete()) throw new IOException("Unable to delete old temp file!");
            } else if (tmp.getParentFile() != null && !tmp.getParentFile().exists() && !tmp.getParentFile().mkdirs()) {
                throw new IOException("Unable to create parent directory!");
            }
        } catch (IOException e) {
            OreExcavation.logger.error("An error occured while writing JSON to file (Directory Setup): " + e);
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(tmp);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
            writer.flush();
        } catch (IOException e) {
            OreExcavation.logger.error("An error occured while writing JSON to file (File Write): " + e);
            return;
        }

        try (FileInputStream fis = new FileInputStream(tmp);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            GSON.fromJson(reader, json.getClass());
        } catch (Exception e) {
            OreExcavation.logger.error("An error occured while writing JSON to file (Validation Check): " + e);
            return;
        }

        try {
            if (file.exists() && !file.delete()) throw new IOException("Failed to delete old file!");
            if (!tmp.renameTo(file)) throw new IOException("Failed to rename temp file!");
        } catch (IOException e) {
            OreExcavation.logger.error("An error occured while writing JSON to file (Temp Copy): " + e);
        }
    }

    public static void createBackup(File file, String prefix) {
        String name = prefix + "_" + file.getName();
        String ext = getExtension(file);
        String parent = file.getParent();
        File backup = new File(parent, name + ext);
        int i = 0;
        while (backup.exists()) {
            backup = new File(parent, name + "_" + i++ + ext);
        }
        try {
            copyTo(file, backup);
            OreExcavation.logger.info("Backup created at: " + backup.getAbsolutePath());
        } catch (IOException e) {
            OreExcavation.logger.error("Unable to create backup! Data loss likely!", e);
        }
    }

    public static void copyTo(File source, File target) throws IOException {
        if (source.exists()) {
            if (target.getParentFile() != null && !target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
                throw new IOException("Failed to create parent directory!");
            }
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String getExtension(File file) {
        String path = file.getAbsolutePath();
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx) : "";
    }
}
