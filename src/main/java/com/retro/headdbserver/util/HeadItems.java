package com.retro.headdbserver.util;

import com.retro.headdbserver.data.HeadData;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class HeadItems {
    private HeadItems() {}

    public static ItemStack createHead(HeadData head) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);

        String textureValue = encodeTextureData(head.texture());
        String profileName = head.name();
        if (profileName != null) {
            profileName = profileName.replaceAll("[^a-zA-Z0-9_]", "");
            if (profileName.length() > 16) {
                profileName = profileName.substring(0, 16);
            }
        }
        if (profileName == null || profileName.isEmpty()) {
            profileName = "HeadDB";
        }
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("headdb:" + head.id()).getBytes(StandardCharsets.UTF_8)),
                profileName
        );
        profile.getProperties().put("textures", new Property("textures", textureValue));

        stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(head.name()).withStyle(ChatFormatting.GOLD));
        return stack;
    }

    public static ItemStack createGuiHead(HeadData head) {
        ItemStack stack = createHead(head);
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("ID: " + head.id()).withStyle(ChatFormatting.DARK_GRAY),
                Component.literal("Category: " + head.category()).withStyle(ChatFormatting.GRAY),
                Component.literal("Click to buy").withStyle(ChatFormatting.GREEN)
        )));
        return stack;
    }

    private static String encodeTextureData(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return "";
        }

        String value = rawTexture.trim();

        try {
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            if (decoded.startsWith("{") && decoded.contains("\"textures\"")) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
        }

        if (value.startsWith("{") && value.contains("\"textures\"")) {
            return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        if (value.startsWith("http://textures.minecraft.net/texture/")
                || value.startsWith("https://textures.minecraft.net/texture/")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + value.replace("\"", "") + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + value + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static ItemStack createCategoryIcon(HeadData sampleHead, String categoryName) {
        ItemStack stack = createHead(sampleHead);

        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(categoryName).withStyle(ChatFormatting.GOLD));

        stack.set(DataComponents.LORE,
                new ItemLore(List.of(
                        Component.literal("Open category").withStyle(ChatFormatting.GREEN),
                        Component.literal("Category: " + categoryName).withStyle(ChatFormatting.GRAY)
                )));

        return stack;
    }

    public static ItemStack simpleNamed(ItemStack stack, Component name, List<Component> lore) {
        ItemStack copy = stack.copy();
        copy.set(DataComponents.CUSTOM_NAME, name);
        if (!lore.isEmpty()) {
            copy.set(DataComponents.LORE, new ItemLore(lore));
        }
        return copy;
    }
}
