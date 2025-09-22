package me.lssupportteam.ipdynamic.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonUtils {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .disableHtmlEscaping()
            .create();

    private static final Map<String, ReadWriteLock> fileLocks = new HashMap<>();
    private static final SimpleDateFormat BACKUP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private static ReadWriteLock getLock(File file) {
        synchronized (fileLocks) {
            return fileLocks.computeIfAbsent(file.getAbsolutePath(), k -> new ReentrantReadWriteLock());
        }
    }

    /**
     * Creates the backup directory structure inside data folder
     */
    private static File getBackupDir(File dataFile) {
        File dataDir = dataFile.getParentFile();
        File backupDir = new File(dataDir, "backup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }

    /**
     * Gets the backup file path with timestamp
     */
    private static File getBackupFile(File originalFile) {
        File backupDir = getBackupDir(originalFile);
        String timestamp = BACKUP_DATE_FORMAT.format(new Date());
        String backupName = originalFile.getName().replace(".json", "_" + timestamp + ".json");
        return new File(backupDir, backupName);
    }

    /**
     * Gets the latest backup file for restoration
     */
    private static File getLatestBackupFile(File originalFile) {
        File backupDir = getBackupDir(originalFile);
        String baseName = originalFile.getName().replace(".json", "");

        File[] backupFiles = backupDir.listFiles((dir, name) ->
            name.startsWith(baseName + "_") && name.endsWith(".json"));

        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }

        // Find the most recent backup
        File latestBackup = backupFiles[0];
        for (File backup : backupFiles) {
            if (backup.lastModified() > latestBackup.lastModified()) {
                latestBackup = backup;
            }
        }

        return latestBackup;
    }

    public static <T> T loadData(File file, Type type, Logger logger) {
        if (!file.exists()) {
            if (logger != null) {
                logger.info("File " + file.getName() + " does not exist. Creating new file.");
            }
            return null;
        }

        ReadWriteLock lock = getLock(file);
        lock.readLock().lock();
        try {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                T data = GSON.fromJson(jsonReader, type);

                // Successfully loaded data silently
                return data;
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error loading data from " + file.getName(), e);
            }


            // Try to restore from latest backup in data/backup folder
            File latestBackup = getLatestBackupFile(file);
            if (latestBackup != null && latestBackup.exists()) {
                try {
                    Files.copy(latestBackup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    if (logger != null) {
                        logger.info("Restored " + file.getName() + " from backup: " + latestBackup.getName());
                    }
                    return loadData(file, type, null);
                } catch (IOException backupError) {
                    if (logger != null) {
                        logger.log(Level.SEVERE, "Failed to restore from backup", backupError);
                    }
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public static <T> boolean saveData(File file, T data, Logger logger) {
        if (data == null) {
            if (logger != null) {
                logger.warning("Cannot save null data to " + file.getName());
            }
            return false;
        }

        ReadWriteLock lock = getLock(file);
        lock.writeLock().lock();
        try {

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }


            // Create backup before saving to data/backup folder
            if (file.exists()) {
                File backupFile = getBackupFile(file);
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }


            File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                JsonWriter jsonWriter = new JsonWriter(writer);
                jsonWriter.setIndent("  ");
                GSON.toJson(data, data.getClass(), jsonWriter);
                jsonWriter.flush();
            }


            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Successfully saved data silently
            return true;

        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error saving data to " + file.getName(), e);
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static Type getTypeTokenMap(Class<?> keyClass, Class<?> valueClass) {
        return TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
    }


    private static class UUIDTypeAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public JsonElement serialize(UUID uuid, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(uuid.toString());
        }

        @Override
        public UUID deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                return UUID.fromString(json.getAsString());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid UUID format: " + json.getAsString(), e);
            }
        }
    }

    public static Gson getGson() {
        return GSON;
    }

    public static boolean validateJsonFile(File file) {
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonParser parser = new JsonParser();
            parser.parse(reader);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean mergeJsonFiles(File source, File destination, Logger logger) {
        try {
            JsonElement sourceElement = loadJsonElement(source);
            JsonElement destElement = loadJsonElement(destination);

            if (sourceElement == null || destElement == null) {
                return false;
            }

            if (sourceElement.isJsonObject() && destElement.isJsonObject()) {
                JsonObject merged = mergeJsonObjects(destElement.getAsJsonObject(), sourceElement.getAsJsonObject());
                return saveJsonElement(destination, merged, logger);
            }

            return false;
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error merging JSON files", e);
            }
            return false;
        }
    }

    private static JsonElement loadJsonElement(File file) {
        if (!file.exists()) return null;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean saveJsonElement(File file, JsonElement element, Logger logger) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.setIndent("  ");
            GSON.toJson(element, jsonWriter);
            return true;
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Error saving JSON element", e);
            }
            return false;
        }
    }

    private static JsonObject mergeJsonObjects(JsonObject base, JsonObject overlay) {
        JsonObject result = new JsonObject();


        for (Map.Entry<String, JsonElement> entry : base.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }


        for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (result.has(key) && result.get(key).isJsonObject() && value.isJsonObject()) {

                result.add(key, mergeJsonObjects(result.get(key).getAsJsonObject(), value.getAsJsonObject()));
            } else {
                result.add(key, value);
            }
        }

        return result;
    }
}