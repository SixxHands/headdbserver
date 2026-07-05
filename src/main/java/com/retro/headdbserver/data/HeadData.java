package com.retro.headdbserver.data;

import java.util.List;
public record HeadData(
        int id,
        String name,
        String texture,
        String category,
        List<String> tags
) {}
