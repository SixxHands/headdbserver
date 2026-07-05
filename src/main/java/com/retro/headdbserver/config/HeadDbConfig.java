package com.retro.headdbserver.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HeadDbConfig {
    public static final ModConfigSpec Settings;

    public static final ModConfigSpec.ConfigValue<String> HeadSourceUrl;
    public static final ModConfigSpec.ConfigValue<String> CachePath;
    public static final ModConfigSpec.ConfigValue<String> PriceItem;
    public static final ModConfigSpec.IntValue PriceCount;
    public static final ModConfigSpec.BooleanValue AllowCreativeBypass;
    public static final ModConfigSpec.IntValue PageSize;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("database");
        HeadSourceUrl = builder
                .comment("Remote heads.json source.")
                .define("head_source_url", "https://raw.githubusercontent.com/TheSilentPro/heads/refs/heads/main/heads.json");
        CachePath = builder
                .comment("Relative path inside the server directory where the downloaded heads cache is stored.")
                .define("cache_path", "config/headdbserver/heads_cache.json");
        builder.pop();
        builder.push("pricing");
        PriceItem = builder
                .comment("Default item required for purchases. Use minecraft:air for free heads.")
                .define("price_item", "minecraft:air");
        PriceCount = builder
                .comment("Default amount required for purchases.")
                .defineInRange("price_count", 0, 0, 9999);
        AllowCreativeBypass = builder
                .comment("Allow players in creative mode to bypass payment requirements.")
                .define("allow_creative_bypass", true);
        builder.pop();
        builder.push("menu");
        PageSize = builder
                .comment("How many head slots per page. The menu currently reserves room for nav and utility buttons.")
                .defineInRange("page_size", 28, 1, 45);
        builder.pop();

        Settings = builder.build();
    }

    private HeadDbConfig() {}
}
