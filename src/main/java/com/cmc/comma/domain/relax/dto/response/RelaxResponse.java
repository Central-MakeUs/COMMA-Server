package com.cmc.comma.domain.relax.dto.response;

import com.cmc.comma.domain.relax.entity.Relax;

public record RelaxResponse(
        Long id,
        String name,
        String description,
        String activeMessage,
        String imageUrl,
        long activeUserCount
) {
    public static RelaxResponse of(Relax relax, long activeUserCount) {
        return new RelaxResponse(
                relax.getId(),
                relax.getName(),
                relax.getDescription(),
                relax.getActiveMessage(),
                relax.getImageUrl(),
                activeUserCount
        );
    }
}