package com.retro.headdbserver.data;

import java.util.List;
/**
 * Data model of a single head from the data base.
 * Holds potential ID, Display Name, Texture, Category, and Tags
 */
// The GSON is evil so we round it up into the JSON schema thats used everywhere in the GUI lookup.
public record HeadData(
        int id,
        String name,
        String texture,
        String category,
        List<String> tags
) {}
