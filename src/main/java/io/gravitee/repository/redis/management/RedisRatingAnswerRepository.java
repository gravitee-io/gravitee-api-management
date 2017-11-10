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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.redis.management.internal.RatingAnswerRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisRatingAnswerRepository implements RatingAnswerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRatingAnswerRepository.class);

    @Autowired
    private RatingAnswerRedisRepository internalRatingAnswerRepository;

    @Override
    public List<RatingAnswer> findByRating(String rating) throws TechnicalException {
        LOGGER.debug("Find rating answer by rating [{}]", rating);
        final List<RatingAnswer> ratingAnswers = internalRatingAnswerRepository.findByRating(rating);
        LOGGER.debug("Find rating answer by api [{}] - internalRatingAnswerRepository", rating);
        return ratingAnswers;
    }

    @Override
    public RatingAnswer create(RatingAnswer ratingAnswer) throws TechnicalException {
        LOGGER.debug("Create rating answer for rating [{}] by user [{}]", ratingAnswer.getRating(), ratingAnswer.getUser());
        final RatingAnswer createdRatingAnswer = internalRatingAnswerRepository.saveOrUpdate(ratingAnswer);
        LOGGER.debug("Create rating answer for rating [{}] by user [{}] - DONE", ratingAnswer.getRating(), ratingAnswer.getUser());
        return createdRatingAnswer;
    }

    @Override
    public RatingAnswer update(RatingAnswer ratingAnswer) throws TechnicalException {
        if (ratingAnswer == null || ratingAnswer.getId() == null) {
            throw new IllegalStateException("Rating answer to update must specify an id");
        }
        final RatingAnswer ratingAnswerToUpdate = internalRatingAnswerRepository.findById(ratingAnswer.getId());
        if (ratingAnswerToUpdate == null) {
            throw new IllegalStateException(String.format("No rating answer found with id [%s]", ratingAnswerToUpdate.getId()));
        }
        try {
            ratingAnswer.setUser(ratingAnswer.getUser());
            ratingAnswer.setRating(ratingAnswer.getRating());
            ratingAnswer.setComment(ratingAnswer.getComment());
            ratingAnswer.setCreatedAt(ratingAnswer.getCreatedAt());
            ratingAnswer.setUpdatedAt(ratingAnswer.getUpdatedAt());
            return internalRatingAnswerRepository.saveOrUpdate(ratingAnswer);
        } catch (Exception e) {
            LOGGER.error("An error occurred while updating rating answer", e);
            throw new TechnicalException("An error occurred while updating rating answer");
        }
    }

    @Override
    public Optional<RatingAnswer> findById(String id) throws TechnicalException {
        LOGGER.debug("Find rating answer by ID [{}]", id);
        final RatingAnswer ratingAnswer = internalRatingAnswerRepository.findById(id);
        LOGGER.debug("Find rating answer by ID [{}] - internalRatingAnswerRepository", id);
        return ofNullable(ratingAnswer);
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        try {
            internalRatingAnswerRepository.delete(id);
        } catch (Exception e) {
            LOGGER.error("An error occurred while deleting rating answer [{}]", id, e);
            throw new TechnicalException("An error occurred while deleting rating answer");
        }
    }
}
