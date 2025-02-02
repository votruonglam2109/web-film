package com.moviex.mapper;

import com.moviex.dto.VideoDto;
import com.moviex.entity.History;
import com.moviex.entity.Video;
import com.moviex.utils.PriceFormatUtils;
import com.moviex.utils.TimeFormatUtils;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VideoMapper {

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
                .paymentType(entity.getPaymentType())
                .views(entity.getViews())
                .share(entity.getShare())
                .director(entity.getDirector())
                .actor(entity.getActor())
                .category(entity.getCategory().getName())
                .categorySlug(entity.getCategory().getSlug())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .formattedPrice(PriceFormatUtils.toFomattedString(entity.getPrice()))
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
                .timeAgo(TimeFormatUtils.getTimeAgoString(entity.getCreatedAt()))
                .build();
    }
}
