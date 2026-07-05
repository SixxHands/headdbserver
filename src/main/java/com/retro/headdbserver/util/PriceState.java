package com.retro.headdbserver.util;

import com.retro.headdbserver.HeadDbServerMod;
import com.retro.headdbserver.config.HeadDbConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PriceState {
    private Item _item = Items.AIR;
    private int _count = 0;

    public Item item() {
        return _item;
    }

    public int count() {
        return _count;
    }

    public boolean isFree() {
        return _item == Items.AIR || _count <= 0;
    }

    public void set(Item item, int count) {
        _item = item == null ? Items.AIR : item;
        _count = Math.max(0, count);
        if (_item == Items.AIR || _count <= 0) {
            clear();
        }
    }

    public void clear() {
        _item = Items.AIR;
        _count = 0;
    }

    public void load(Path serverDirectory) {
        applyDefaultsFromConfig();

        Path file = serverDirectory.resolve("config/headdbserver/price.properties");
        if (!Files.exists(file)) {
            save(serverDirectory);
            return;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            HeadDbServerMod.Logger.error("Failed to load HeadDB price state from {}", file, e);
            return;
        }

        String rawItem = properties.getProperty("item", "minecraft:air");
        int rawCount;
        try {
            rawCount = Integer.parseInt(properties.getProperty("count", "0"));
        } catch (NumberFormatException e) {
            rawCount = 0;
        }

        Item loadedItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rawItem)).orElse(Items.AIR);
        set(loadedItem, rawCount);
    }

    public void save(Path serverDirectory) {
        Path file = serverDirectory.resolve("config/headdbserver/price.properties");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            HeadDbServerMod.Logger.error("Failed to create HeadDB config directory for {}", file, e);
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("item", BuiltInRegistries.ITEM.getKey(_item).toString());
        properties.setProperty("count", Integer.toString(_count));

        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "HeadDB Server live price override");
        } catch (IOException e) {
            HeadDbServerMod.Logger.error("Failed to save HeadDB price state to {}", file, e);
        }
    }

    private void applyDefaultsFromConfig() {
        String raw = HeadDbConfig.PriceItem.get();
        Item defaultItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(raw)).orElse(Items.AIR);
        int defaultCount = HeadDbConfig.PriceCount.get();
        set(defaultItem, defaultCount);
    }
}
