/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.redis.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.redis.management.internal.RatingRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisRatingRepository implements RatingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRatingRepository.class);

    @Autowired
    private RatingRedisRepository internalRatingRepository;

    @Override
    public Optional<Rating> findById(String id) throws TechnicalException {
        LOGGER.debug("Find rating by ID [{}]", id);
        final Rating rating = internalRatingRepository.findById(id);
        LOGGER.debug("Find rating by ID [{}] - Done", id);
        return ofNullable(rating);
    }

    @Override
    public Optional<Rating> findByApiAndUser(final String api, final String user) throws TechnicalException {
        LOGGER.debug("Find rating by api [{}] and user [{}]", api, user);
        final Rating rating = internalRatingRepository.findByApiAndUser(api, user);
        LOGGER.debug("Find rating by api [{}] and user [{}] - DONE", api, user);
        return ofNullable(rating);
    }

    @Override
    public Rating create(final Rating rating) throws TechnicalException {
        LOGGER.debug("Create rating for api [{}] by user [{}]", rating.getApi(), rating.getUser());
        final Rating createdRating = internalRatingRepository.saveOrUpdate(rating);
        LOGGER.debug("Create rating for api [{}] by user [{}] - DONE", rating.getApi(), rating.getUser());
        return createdRating;
    }

    @Override
    public Page<Rating> findByApiPageable(final String api, final Pageable pageable) throws TechnicalException {
        LOGGER.debug("Find rating by api [{}] with pagination", api);
        final Page<Rating> ratingPage =
                internalRatingRepository.findByApi(api, new PageRequest(pageable.pageNumber(), pageable.pageSize(), Sort.Direction.DESC, "createdAt"));
        LOGGER.debug("Find rating by api [{}] with pagination - DONE", api);
        return ratingPage;
    }

    @Override
    public List<Rating> findByApi(final String api) throws TechnicalException {
        LOGGER.debug("Find rating by api [{}]", api);
        final List<Rating> ratings = internalRatingRepository.findByApi(api);
        LOGGER.debug("Find rating by api [{}] - DONE", api);
        return ratings;
    }

    @Override
    public Rating update(final Rating rating) throws TechnicalException {
        if (rating == null || rating.getId() == null) {
            throw new IllegalStateException("Rating to update must specify an id");
        }
        final Rating ratingToUpdate = internalRatingRepository.findById(rating.getId());
        if (ratingToUpdate == null) {
            throw new IllegalStateException(String.format("No rating found with id [%s]", rating.getId()));
        }
        try {
            ratingToUpdate.setApi(rating.getApi());
            ratingToUpdate.setUser(rating.getUser());
            ratingToUpdate.setRate(rating.getRate());
            ratingToUpdate.setTitle(rating.getTitle());
            ratingToUpdate.setComment(rating.getComment());
            ratingToUpdate.setCreatedAt(rating.getCreatedAt());
            ratingToUpdate.setUpdatedAt(rating.getUpdatedAt());
            return internalRatingRepository.saveOrUpdate(rating);
        } catch (Exception e) {
            LOGGER.error("An error occurred while updating rating", e);
            throw new TechnicalException("An error occurred while updating rating");
        }
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        try {
            internalRatingRepository.delete(id);
        } catch (Exception e) {
            LOGGER.error("An error occurred while deleting rating [{}]", id, e);
            throw new TechnicalException("An error occurred while deleting rating");
        }
    }
}
