package com.filmweb.mapper;

import com.filmweb.dto.VideoDto;
import com.filmweb.entity.History;
import com.filmweb.entity.Video;
import com.filmweb.utils.TimeFormatter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;


@ApplicationScoped
public class VideoMapper {
    @Inject
    private TimeFormatter timeFormatter;

    public VideoDto toDto(Video entity) {
        if (entity == null) {
            return null;
        }
        return VideoDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .slug(entity.getSlug())
                .href(entity.getHref())
                .poster(entity.getPoster())
                .views(entity.getViews())
                .share(entity.getShare())
                .director(entity.getDirector())
                .actor(entity.getActor())
                .category(entity.getCategory().getName())
                .categorySlug(entity.getCategory().getSlug())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .likeQuantity(
                   entity.getHistories() != null ?
                    entity.getHistories().stream()
                        .filter(History::getIsLiked)
                        .toList()
                        .size() :
                       0
                )
                .commentQuantity(entity.getComments() != null ? entity.getComments().size() : 0)
                .timeAgo(timeFormatter.getTimeAgoString(entity.getCreatedAt()))
                .build();
    }
}
