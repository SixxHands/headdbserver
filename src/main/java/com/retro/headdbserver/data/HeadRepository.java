package com.retro.headdbserver.data;

import com.retro.headdbserver.HeadDbServerMod;
import com.retro.headdbserver.config.HeadDbConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public final class HeadRepository {
    private static final Type ListType = new TypeToken<List<HeadData>>() {}.getType();

    private final Gson _gson = new Gson();
    private final AtomicReference<List<HeadData>> _heads = new AtomicReference<>(List.of());
    public CompletableFuture<List<HeadData>> refreshAsync(MinecraftServer server) {
        return CompletableFuture.supplyAsync(() -> loadLatest(server));
    }

    public List<HeadData> all() {
        return _heads.get();
    }

    public boolean isReady() {
        return !_heads.get().isEmpty();
    }

    public Optional<HeadData> byId(int id) {
        return _heads.get().stream().filter(h -> h.id() == id).findFirst();
    }

    public List<String> categories() {
        return _heads.get().stream()
                .map(HeadData::category)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new))
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<HeadData> byCategory(String category) {
        return _heads.get().stream()
                .filter(h -> h.category() != null && h.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(HeadData::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<HeadData> search(String query) {
        String q = query.toLowerCase();
        return _heads.get().stream()
                .filter(h -> contains(h.name(), q)
                        || contains(h.category(), q)
                        || h.tags().stream().anyMatch(tag -> contains(tag, q))
                        || Integer.toString(h.id()).equals(q)) 
                .sorted(Comparator.comparing(HeadData::name, String.CASE_INSENSITIVE_ORDER))
                .toList(); 
    }
    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

        private List<HeadData> loadLatest(MinecraftServer server) {
        Path cachePath = server.getServerDirectory().resolve(HeadDbConfig.CachePath.get());
        try {
            Files.createDirectories(cachePath.getParent());
        } catch (IOException e) {
            HeadDbServerMod.Logger.warn("Could not create head cache directories: {}", cachePath, e);
        }

        String url = HeadDbConfig.HeadSourceUrl.get();
        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                List<HeadData> loaded = readHeads(body);
                _heads.set(loaded);
                try (Writer writer = Files.newBufferedWriter(cachePath, StandardCharsets.UTF_8)) {
                    writer.write(body);
                }
                HeadDbServerMod.Logger.info("Loaded {} heads from remote source.", loaded.size());
                return loaded;
            }
            HeadDbServerMod.Logger.warn("Remote head source returned HTTP {}", response.statusCode());
        } catch (Exception e) {
            HeadDbServerMod.Logger.warn("Failed to refresh heads from remote source, falling back to cache.", e);
        }

        if (Files.exists(cachePath)) {
            try (Reader reader = Files.newBufferedReader(cachePath, StandardCharsets.UTF_8)) {
                List<HeadData> loaded = _gson.fromJson(reader, ListType);
                _heads.set(loaded == null ? List.of() : List.copyOf(loaded));
                HeadDbServerMod.Logger.info("Loaded {} heads from local cache.", _heads.get().size());
            } catch (IOException e) {
                HeadDbServerMod.Logger.error("Failed to read local head cache.", e);
            }
        }

        return _heads.get();
    }

    private List<HeadData> readHeads(String json) {
        List<HeadData> loaded = _gson.fromJson(json, ListType);
        if (loaded == null) {
            return List.of();
        }
        return List.copyOf(loaded);
    }
}
