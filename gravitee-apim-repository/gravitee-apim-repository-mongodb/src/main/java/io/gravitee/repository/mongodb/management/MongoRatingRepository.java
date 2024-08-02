/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;
import io.gravitee.repository.mongodb.management.internal.api.RatingMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.RatingMongo;
import io.gravitee.repository.mongodb.management.internal.model.TokenMongo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoRatingRepository implements RatingRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoRatingRepository.class);

    @Autowired
    private RatingMongoRepository internalRatingRepository;

    @Override
    public Optional<Rating> findById(String id) throws TechnicalException {
        LOGGER.debug("Find rating by ID [{}]", id);
        final RatingMongo rating = internalRatingRepository.findById(id).orElse(null);
        LOGGER.debug("Find rating by ID [{}] - Done", id);
        return ofNullable(map(rating));
    }

    @Override
    public Optional<Rating> findByReferenceIdAndReferenceTypeAndUser(
        final String referenceId,
        final RatingReferenceType referenceType,
        final String user
    ) {
        LOGGER.debug("Find rating by ref [{}] and user [{}]", referenceId, user);
        final RatingMongo rating = internalRatingRepository.findByReferenceIdAndReferenceTypeAndUser(
            referenceId,
            referenceType.name(),
            user
        );
        LOGGER.debug("Find rating by ref [{}] and user [{}] - DONE", referenceId, user);
        return ofNullable(map(rating));
    }

    @Override
    public Set<String> findReferenceIdsOrderByRate(RatingCriteria ratingCriteria) throws TechnicalException {
        LOGGER.debug("Find rating by criteria [{}]", ratingCriteria);
        Set<String> ranking = internalRatingRepository.findReferenceIdsOrderByRate(ratingCriteria);
        LOGGER.debug("Find rating by criteria [{}] - DONE", ratingCriteria);
        return ranking;
    }

    @Override
    public Rating create(final Rating rating) throws TechnicalException {
        LOGGER.debug("Create rating for ref [{}] by user [{}]", rating.getReferenceId(), rating.getUser());
        final Rating createdRating = map(internalRatingRepository.insert(map(rating)));
        LOGGER.debug("Create rating for ref [{}] by user [{}] - DONE", rating.getReferenceId(), rating.getUser());
        return createdRating;
    }

    @Override
    public Page<Rating> findByReferenceIdAndReferenceTypePageable(
        final String referenceId,
        final RatingReferenceType referenceType,
        final Pageable pageable
    ) {
        LOGGER.debug("Find rating by ref [{}] with pagination", referenceId);
        final org.springframework.data.domain.Page<RatingMongo> ratingPageMongo =
            internalRatingRepository.findByReferenceIdAndReferenceType(
                referenceId,
                referenceType.name(),
                PageRequest.of(pageable.pageNumber(), pageable.pageSize(), Sort.Direction.DESC, "createdAt")
            );
        final List<Rating> ratings = ratingPageMongo.getContent().stream().map(this::map).collect(toList());
        final Page<Rating> ratingPage = new Page<>(
            ratings,
            ratingPageMongo.getNumber(),
            ratingPageMongo.getNumberOfElements(),
            ratingPageMongo.getTotalElements()
        );
        LOGGER.debug("Find rating by ref [{}] with pagination - DONE", referenceId);
        return ratingPage;
    }

    @Override
    public List<Rating> findByReferenceIdAndReferenceType(final String referenceId, final RatingReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Find rating by ref [{}, {}]", referenceId, referenceType);
        final List<RatingMongo> ratings = internalRatingRepository.findByReferenceIdAndReferenceType(referenceId, referenceType.name());
        LOGGER.debug("Find rating by ID [{}, {}] - Done", referenceId, referenceType);
        return ratings.stream().map(this::map).collect(toList());
    }

    @Override
    public Rating update(final Rating rating) throws TechnicalException {
        if (rating == null || rating.getId() == null) {
            throw new IllegalStateException("Rating to update must specify an id");
        }
        final RatingMongo ratingMongo = internalRatingRepository.findById(rating.getId()).orElse(null);
        if (ratingMongo == null) {
            throw new IllegalStateException(String.format("No rating found with id [%s]", rating.getId()));
        }
        try {
            ratingMongo.setUser(rating.getUser());
            ratingMongo.setRate(rating.getRate());
            ratingMongo.setTitle(rating.getTitle());
            ratingMongo.setComment(rating.getComment());
            ratingMongo.setCreatedAt(rating.getCreatedAt());
            ratingMongo.setUpdatedAt(rating.getUpdatedAt());
            return map(internalRatingRepository.save(ratingMongo));
        } catch (Exception e) {
            LOGGER.error("An error occurred while updating rating", e);
            throw new TechnicalException("An error occurred while updating rating");
        }
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        try {
            internalRatingRepository.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occurred while deleting rating [{}]", id, e);
            throw new TechnicalException("An error occurred while deleting rating");
        }
    }

    private Rating map(final RatingMongo ratingMongo) {
        if (ratingMongo == null) {
            return null;
        }
        final Rating rating = new Rating();
        rating.setId(ratingMongo.getId());
        rating.setReferenceId(ratingMongo.getReferenceId());
        rating.setReferenceType(RatingReferenceType.valueOf(ratingMongo.getReferenceType()));
        rating.setUser(ratingMongo.getUser());
        rating.setRate(ratingMongo.getRate());
        rating.setTitle(ratingMongo.getTitle());
        rating.setComment(ratingMongo.getComment());
        rating.setCreatedAt(ratingMongo.getCreatedAt());
        rating.setUpdatedAt(ratingMongo.getUpdatedAt());
        return rating;
    }

    private RatingMongo map(final Rating rating) {
        final RatingMongo ratingMongo = new RatingMongo();
        ratingMongo.setId(rating.getId());
        ratingMongo.setReferenceId(rating.getReferenceId());
        ratingMongo.setReferenceType(rating.getReferenceType().name());
        ratingMongo.setUser(rating.getUser());
        ratingMongo.setRate(rating.getRate());
        ratingMongo.setTitle(rating.getTitle());
        ratingMongo.setComment(rating.getComment());
        ratingMongo.setCreatedAt(rating.getCreatedAt());
        ratingMongo.setUpdatedAt(rating.getUpdatedAt());
        return ratingMongo;
    }

    @Override
    public Set<Rating> findAll() throws TechnicalException {
        return internalRatingRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, RatingReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("Delete rating by ref type '{}' and ref id '{}'", referenceType, referenceId);
        try {
            final var ratings = internalRatingRepository
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(RatingMongo::getId)
                .toList();
            LOGGER.debug("Delete rating by ref type '{}' and ref id '{}' done", referenceId, referenceType);
            return ratings;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete rating for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete rating by reference", ex);
        }
    }
}
