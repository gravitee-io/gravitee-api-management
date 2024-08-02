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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.mongodb.management.internal.api.RatingAnswerMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.RatingAnswerMongo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoRatingAnswerRepository implements RatingAnswerRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoRatingAnswerRepository.class);

    @Autowired
    private RatingAnswerMongoRepository internalRatingAnswerRepository;

    @Override
    public List<RatingAnswer> findByRating(String rating) throws TechnicalException {
        LOGGER.debug("Find rating answer by rating [{}]", rating);
        final List<RatingAnswerMongo> ratingAnswersMongo = internalRatingAnswerRepository.findByRating(rating);
        LOGGER.debug("Find rating answer by api [{}] - internalRatingAnswerRepository", rating);
        return ratingAnswersMongo.stream().map(this::map).collect(toList());
    }

    @Override
    public RatingAnswer create(RatingAnswer ratingAnswer) throws TechnicalException {
        LOGGER.debug("Create rating answer for rating [{}] by user [{}]", ratingAnswer.getRating(), ratingAnswer.getUser());
        final RatingAnswer createdRatingAnswer = map(internalRatingAnswerRepository.insert(map(ratingAnswer)));
        LOGGER.debug("Create rating answer for rating [{}] by user [{}] - DONE", ratingAnswer.getRating(), ratingAnswer.getUser());
        return createdRatingAnswer;
    }

    @Override
    public RatingAnswer update(RatingAnswer ratingAnswer) throws TechnicalException {
        if (ratingAnswer == null || ratingAnswer.getId() == null) {
            throw new IllegalStateException("Rating answer to update must specify an id");
        }
        final RatingAnswerMongo ratingAnswerMongo = internalRatingAnswerRepository.findById(ratingAnswer.getId()).orElse(null);
        if (ratingAnswerMongo == null) {
            throw new IllegalStateException(String.format("No rating answer found with id [%s]", ratingAnswer.getId()));
        }
        try {
            ratingAnswerMongo.setUser(ratingAnswer.getUser());
            ratingAnswerMongo.setRating(ratingAnswer.getRating());
            ratingAnswerMongo.setComment(ratingAnswer.getComment());
            ratingAnswerMongo.setCreatedAt(ratingAnswer.getCreatedAt());
            ratingAnswerMongo.setUpdatedAt(ratingAnswer.getUpdatedAt());
            return map(internalRatingAnswerRepository.save(ratingAnswerMongo));
        } catch (Exception e) {
            LOGGER.error("An error occurred while updating rating answer", e);
            throw new TechnicalException("An error occurred while updating rating answer");
        }
    }

    @Override
    public Optional<RatingAnswer> findById(String id) throws TechnicalException {
        LOGGER.debug("Find rating answer by ID [{}]", id);
        final RatingAnswerMongo ratingAnswerMongo = internalRatingAnswerRepository.findById(id).orElse(null);
        LOGGER.debug("Find rating answer by ID [{}] - internalRatingAnswerRepository", id);
        return ofNullable(map(ratingAnswerMongo));
    }

    @Override
    public void delete(final String id) throws TechnicalException {
        try {
            internalRatingAnswerRepository.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occurred while deleting rating answer [{}]", id, e);
            throw new TechnicalException("An error occurred while deleting rating answer");
        }
    }

    @Override
    public List<String> deleteByRating(String ratingId) throws TechnicalException {
        try {
            LOGGER.debug("Delete rating answer by ratingId [{}]", ratingId);
            return internalRatingAnswerRepository.deleteByRating(ratingId).stream().map(RatingAnswerMongo::getId).toList();
        } catch (Exception e) {
            LOGGER.error("An error occurred while deleting rating answer by ratingId [{}]", ratingId, e);
            throw new TechnicalException("An error occurred while deleting rating answer by ratingId");
        }
    }

    private RatingAnswer map(final RatingAnswerMongo ratingAnswerMongo) {
        if (ratingAnswerMongo == null) {
            return null;
        }
        final RatingAnswer ratingAnswer = new RatingAnswer();
        ratingAnswer.setId(ratingAnswerMongo.getId());
        ratingAnswer.setRating(ratingAnswerMongo.getRating());
        ratingAnswer.setUser(ratingAnswerMongo.getUser());
        ratingAnswer.setComment(ratingAnswerMongo.getComment());
        ratingAnswer.setCreatedAt(ratingAnswerMongo.getCreatedAt());
        return ratingAnswer;
    }

    private RatingAnswerMongo map(final RatingAnswer ratingAnswer) {
        final RatingAnswerMongo ratingAnswerMongo = new RatingAnswerMongo();
        ratingAnswerMongo.setId(ratingAnswer.getId());
        ratingAnswerMongo.setRating(ratingAnswer.getRating());
        ratingAnswerMongo.setUser(ratingAnswer.getUser());
        ratingAnswerMongo.setComment(ratingAnswer.getComment());
        ratingAnswerMongo.setCreatedAt(ratingAnswer.getCreatedAt());
        return ratingAnswerMongo;
    }

    @Override
    public Set<RatingAnswer> findAll() throws TechnicalException {
        return internalRatingAnswerRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
