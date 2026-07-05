package com.retro.headdbserver.data;

import com.retro.headdbserver.HeadDbServerMod;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Yeah this dosent work at all so like either we fix it or we treat it like a soggy side of fries and keep it dead.
// PERSONALLY Id like to keep it dead bc I dont want to deal with storing player data
public final class PlayerFavoritesStore {
    private Connection connection;

    public void open(MinecraftServer server) {
        try {
            Class.forName("org.sqlite.JDBC"); // what is this we dont need you (Im too lazy to delete this)

            // this is the path to the favs database, create it or fetch it
            Path dbPath = server.getServerDirectory().resolve("config/headdbserver/favorites.db");
            java.nio.file.Files.createDirectories(dbPath.getParent());

            // Connect or fetch them loser
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());

            // try player favs by UI and back out if nothing works
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS favorites (" +
                                "player_uuid TEXT NOT NULL," +
                                "head_id INTEGER NOT NULL," +
                                "PRIMARY KEY(player_uuid, head_id)" +
                                ")"
                );
            }
        } catch (Exception e) {
            HeadDbServerMod.Logger.error("Failed to open favorites database.", e);
        }
    }

    // Don't crash the game if closing out of faves fails, just say you croaked
    public void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException e) {
            HeadDbServerMod.Logger.warn("Failed to close favorites database cleanly.", e);
        } finally {
            connection = null;
        }
    }

    // List faves for player based on head ID number
    public List<Integer> getFavorites(UUID playerId) {
        if (connection == null) return List.of();
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT head_id FROM favorites WHERE player_uuid = ? ORDER BY head_id")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            HeadDbServerMod.Logger.warn("Failed to read favorites for {}", playerId, e);
        }
        return out;
    }

    // Check SQ table if this player has favorited a head with this ID.
    public boolean isFavorite(UUID playerId, int headId) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM favorites WHERE player_uuid = ? AND head_id = ?")) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, headId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            HeadDbServerMod.Logger.warn("Failed checking favorite state for {} / {}", playerId, headId, e);
            return false;
        }
    }

    // Add a fav
    public void addFavorite(UUID playerId, int headId) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO favorites(player_uuid, head_id) VALUES(?, ?)")) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, headId);
            ps.executeUpdate();
        } catch (SQLException e) {
            HeadDbServerMod.Logger.warn("Failed to add favorite {} for {}", headId, playerId, e);
        }
    }

    // Delete a fav
    public void removeFavorite(UUID playerId, int headId) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM favorites WHERE player_uuid = ? AND head_id = ?")) {
            ps.setString(1, playerId.toString());
            ps.setInt(2, headId);
            ps.executeUpdate();
        } catch (SQLException e) {
            HeadDbServerMod.Logger.warn("Failed to remove favorite {} for {}", headId, playerId, e);
        }
    }
}
