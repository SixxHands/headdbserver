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

/**
 * Database
 */

public final class HeadRepository {
    // We start by turning it into a array when we fetch the GSON so that they do not drop the list in parsing.
    private static final Type ListType = new TypeToken<List<HeadData>>() {}.getType();

    private final Gson _gson = new Gson();
    private final AtomicReference<List<HeadData>> _heads = new AtomicReference<>(List.of());
    // Our list starts here so we call it to the server so we can use it.
    public CompletableFuture<List<HeadData>> refreshAsync(MinecraftServer server) {
        return CompletableFuture.supplyAsync(() -> loadLatest(server)); // We do not use an explicit server parameter otherwise the server is hung by the remote request until it completes.
                                                                        // Shifting to ForkJoinPool is better anyways.So we can do it on a background thread.
    }

    // List the head information.
    public List<HeadData> all() {
        return _heads.get();
    }

    public boolean isReady() {
        return !_heads.get().isEmpty();
    }

    /**
    * Look up a head by numeric ID, optional for if the ID does not exist in the database.
    */
    // Is it optional? Yes because if we dont the ID might not exist and a commmand may choke on itself
    public Optional<HeadData> byId(int id) {
        return _heads.get().stream().filter(h -> h.id() == id).findFirst();
    }

    /**
     * Get a sorted list of all category names.
     * @return
     */
    // Gather all unique category names that we just requested and order them...
    public List<String> categories() {
        return _heads.get().stream()
                .map(HeadData::category)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new)) // (get out of here duplicates we dont need you)
                .stream()
                .sorted(String.CASE_INSENSITIVE_ORDER) // ...alphabetically (and by capitals)
                .toList();
    }

    /**
     * Filter and return all heads in a specific category
     */
    // My big belly wants a category. Put that category on my plate in an array.
    public List<HeadData> byCategory(String category) {
        return _heads.get().stream()
                .filter(h -> h.category() != null && h.category().equalsIgnoreCase(category)) // Ignore the case. It doesnt matter.
                .sorted(Comparator.comparing(HeadData::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Filters heads by matching text {query} to names, catagories, ids, or tags.
     * @param query
     * @return
     */
    // Search. Pretty Shrimple.
    public List<HeadData> search(String query) {
        String q = query.toLowerCase();
        return _heads.get().stream()
                .filter(h -> contains(h.name(), q) // Does a head from the evil GSON contain the same name as the search?
                        || contains(h.category(), q) // Is the search simulaur to a catgoerey?
                        || h.tags().stream().anyMatch(tag -> contains(tag, q)) // Is a tag simular to the search?
                        || Integer.toString(h.id()).equals(q)) 
                .sorted(Comparator.comparing(HeadData::name, String.CASE_INSENSITIVE_ORDER)) // Still dont care about case. frick you case
                .toList(); 
    }
    // Null safe no case search filter just in case something has nothing, prevents missing fields in the database making the search back-off
    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query); // oh no a value equals NOTHING in the database????!1 moving on then... oh and also its lower case now
    }

    /**
     * Core manager for local (server) head database. Load, fetch, make edit or back out cleanly.
     */
    private List<HeadData> loadLatest(MinecraftServer server) {
        // Load the cache where ever it is or make a new one.
        Path cachePath = server.getServerDirectory().resolve(HeadDbConfig.CachePath.get()); // Standard dog fetch
        try {
            Files.createDirectories(cachePath.getParent()); // Standard make file
        } catch (IOException e) {
            HeadDbServerMod.Logger.warn("Could not create head cache directories: {}", cachePath, e); // In case doing either didn't work
        }

        String url = HeadDbConfig.HeadSourceUrl.get(); // Config link that we think the database is at.
        try {
            HttpClient client = HttpClient.newBuilder().build();                                                              // open the standard http request so we can grab the file
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();                                      // this is still part of the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); // and so is this
            if (response.statusCode() >= 200 && response.statusCode() < 300) { //success or not?
                String body = response.body();// extract the json from the the responce 
                List<HeadData> loaded = readHeads(body); // parse it via our anit-evil gson function
                _heads.set(loaded); // New heads? slam dunk them into our cache/memory
                try (Writer writer = Files.newBufferedWriter(cachePath, StandardCharsets.UTF_8)) {
                    writer.write(body); // Im the one that makes the write stream safe
                }
                HeadDbServerMod.Logger.info("Loaded {} heads from remote source.", loaded.size()); // "n" number of heads added type thing
                return loaded;
            }
            HeadDbServerMod.Logger.warn("Remote head source returned HTTP {}", response.statusCode()); // if it didnt work heres the code it gave (likley a 404)
        } catch (Exception e) {
            HeadDbServerMod.Logger.warn("Failed to refresh heads from remote source, falling back to cache.", e); // when it failed also let them know its cache only
        }

        // Gson, read my cached file, and convert it to my list we made for this.
        if (Files.exists(cachePath)) {
            try (Reader reader = Files.newBufferedReader(cachePath, StandardCharsets.UTF_8)) {
                List<HeadData> loaded = _gson.fromJson(reader, ListType);
                _heads.set(loaded == null ? List.of() : List.copyOf(loaded));
                HeadDbServerMod.Logger.info("Loaded {} heads from local cache.", _heads.get().size()); // Gson loaded it.
            } catch (IOException e) {
                HeadDbServerMod.Logger.error("Failed to read local head cache.", e); // Gson failed, major L
            }
        }

        return _heads.get();
    }

    // Parse the raw json string into head related data, but come back an empty list if its incorrect or empty
    private List<HeadData> readHeads(String json) {
        List<HeadData> loaded = _gson.fromJson(json, ListType);
        if (loaded == null) {
            return List.of(); // Re:Empty List
        }
        return List.copyOf(loaded);
    }
}
