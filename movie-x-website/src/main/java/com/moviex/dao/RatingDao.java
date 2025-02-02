package com.moviex.dao;

import com.moviex.entity.Rating;

import java.util.List;
import java.util.UUID;

public interface RatingDao {
    List<Rating> findByUserEmail(String email);

    Rating findByUserIdAndVideoId(UUID userId, UUID videoId);

    Rating create(Rating entity);

    Rating update(Rating entity);
}
