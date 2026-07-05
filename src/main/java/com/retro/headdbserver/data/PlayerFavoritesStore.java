package com.retro.headdbserver.data;

import com.retro.headdbserver.HeadDbServerMod;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerFavoritesStore {
    private Connection connection;

    public void open(MinecraftServer server) {
        try {
            Class.forName("org.sqlite.JDBC");

            Path dbPath = server.getServerDirectory().resolve("config/headdbserver/favorites.db");
            java.nio.file.Files.createDirectories(dbPath.getParent());

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());

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
