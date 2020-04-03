package nl.matsv.viabackwards.api.data;

import nl.matsv.viabackwards.ViaBackwards;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.MappingDataLoader;
import us.myles.ViaVersion.util.GsonUtil;
import us.myles.viaversion.libs.gson.*;

import java.io.*;
import java.util.Map;

public class VBMappingDataLoader {

    public static JsonObject loadFromDataDir(String name) {
        File file = new File(ViaBackwards.getPlatform().getDataFolder(), name);
        if (!file.exists()) return loadData(name);

        // Load the file from the platform's directory if present
        try (FileReader reader = new FileReader(file)) {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } catch (JsonSyntaxException e) {
            ViaBackwards.getPlatform().getLogger().warning(name + " is badly formatted!");
            e.printStackTrace();
            ViaBackwards.getPlatform().getLogger().warning("Falling back to resource's file!");
            return loadData(name);
        } catch (IOException | JsonIOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonObject loadData(String name) {
        InputStream stream = VBMappingDataLoader.class.getClassLoader().getResourceAsStream("assets/viabackwards/data/" + name);
        InputStreamReader reader = new InputStreamReader(stream);
        try {
            return GsonUtil.getGson().fromJson(reader, JsonObject.class);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
                // Ignored
            }
        }
    }

    public static void mapIdentifiers(short[] output, JsonObject oldIdentifiers, JsonObject newIdentifiers, JsonObject diffIdentifiers) {
        mapIdentifiers(output, oldIdentifiers, newIdentifiers, diffIdentifiers, true);
    }

    public static void mapIdentifiers(short[] output, JsonObject oldIdentifiers, JsonObject newIdentifiers, JsonObject diffIdentifiers, boolean warnOnMissing) {
        for (Map.Entry<String, JsonElement> entry : oldIdentifiers.entrySet()) {
            String key = entry.getValue().getAsString();
            Map.Entry<String, JsonElement> value = MappingDataLoader.findValue(newIdentifiers, key);
            if (value == null) {
                // Search in diff mappings
                if (diffIdentifiers != null) {
                    JsonPrimitive diffValue = diffIdentifiers.getAsJsonPrimitive(key);
                    if (diffValue == null && key.contains("[")) {
                        diffValue = diffIdentifiers.getAsJsonPrimitive(key.substring(0, key.indexOf('[')));
                    }
                    if (diffValue == null) {
                        if (warnOnMissing && !Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                            ViaBackwards.getPlatform().getLogger().warning("No diff key for " + entry.getValue() + " :( ");
                        }
                        continue;
                    }
                    value = MappingDataLoader.findValue(newIdentifiers, diffValue.getAsString());
                }
                if (value == null) {
                    if (warnOnMissing && !Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        ViaBackwards.getPlatform().getLogger().warning("No key for " + entry.getValue() + " :( ");
                    }
                    continue;
                }
            }
            output[Integer.parseInt(entry.getKey())] = Short.parseShort(value.getKey());
        }
    }

    public static void mapIdentifiers(short[] output, JsonArray oldIdentifiers, JsonArray newIdentifiers, JsonObject diffIdentifiers, boolean warnOnMissing) {
        int i = -1;
        for (JsonElement oldIdentifier : oldIdentifiers) {
            i++;
            String key = oldIdentifier.getAsString();
            Integer index = MappingDataLoader.findIndex(newIdentifiers, key);
            if (index == null) {
                // Search in diff mappings
                if (diffIdentifiers != null) {
                    JsonPrimitive diffValue = diffIdentifiers.getAsJsonPrimitive(key);
                    if (diffValue == null) {
                        if (warnOnMissing && !Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                            ViaBackwards.getPlatform().getLogger().warning("No diff key for " + key + " :( ");
                        }
                        continue;
                    }
                    String mappedName = diffValue.getAsString();
                    if (mappedName.isEmpty()) continue; // "empty" remaps

                    index = MappingDataLoader.findIndex(newIdentifiers, mappedName);
                }
                if (index == null) {
                    if (warnOnMissing && !Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        ViaBackwards.getPlatform().getLogger().warning("No key for " + key + " :( ");
                    }
                    continue;
                }
            }
            output[i] = index.shortValue();
        }
    }
}
