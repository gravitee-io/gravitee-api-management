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
package io.gravitee.management.service.impl;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.service.RatingService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ApiRatingUnavailableException;
import io.gravitee.management.service.exceptions.RatingAlreadyExistsException;
import io.gravitee.management.service.exceptions.RatingNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingServiceImpl extends AbstractService implements RatingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RatingServiceImpl.class);

    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private RatingAnswerRepository ratingAnswerRepository;

    @Autowired
    private UserService userService;

    @Value("${rating.enabled:false}")
    private boolean enabled;

    @Override
    public RatingEntity create(final NewRatingEntity ratingEntity) {
        if (!enabled) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findByApiAndUser(ratingEntity.getApi(), getAuthenticatedUsername());
            if (ratingOptional.isPresent()) {
                throw new RatingAlreadyExistsException(ratingEntity.getApi(), getAuthenticatedUsername());
            }
            return convert(ratingRepository.create(convert(ratingEntity)));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create rating on api {}", ratingEntity.getApi(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create rating on api " + ratingEntity.getApi(), ex);
        }
    }

    @Override
    public RatingEntity createAnswer(final NewRatingAnswerEntity answerEntity) {
        try {
            final Rating rating = findById(answerEntity.getRatingId());

            final RatingAnswer ratingAnswer = new RatingAnswer();
            ratingAnswer.setId(UUID.toString(UUID.random()));
            ratingAnswer.setRating(answerEntity.getRatingId());
            ratingAnswer.setUser(getAuthenticatedUsername());
            ratingAnswer.setComment(answerEntity.getComment());
            ratingAnswer.setCreatedAt(new Date());
            ratingAnswerRepository.create(ratingAnswer);
            return convert(rating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create a rating answer on rating {}", answerEntity.getRatingId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create a rating answer on rating" + answerEntity.getRatingId(), ex);
        }
    }

    @Override
    public Page<RatingEntity> findByApi(final String api, final Pageable pageable) {
        try {
            final Page<Rating> pageRating = ratingRepository.findByApiPageable(api, pageable);
            final List<RatingEntity> ratingEntities =
                    pageRating.getContent().stream().map(this::convert).collect(toList());
            return new Page<>(ratingEntities, pageRating.getPageNumber(),
                    (int) pageRating.getPageElements(), pageRating.getTotalElements());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find ratings for api {}", api, ex);
            throw new TechnicalManagementException("An error occurred while trying to find ratings for api " + api, ex);
        }
    }

    @Override
    public RatingSummaryEntity findSummaryByApi(final String api) {
        try {
            final List<Rating> ratings = ratingRepository.findByApi(api);
            final RatingSummaryEntity ratingSummary = new RatingSummaryEntity();
            ratingSummary.setApi(api);
            ratingSummary.setNumberOfRatings(ratings.size());
            final OptionalDouble optionalAvg = ratings.stream().mapToInt(Rating::getRate).average();
            if (optionalAvg.isPresent()) {
                ratingSummary.setAverageRate(optionalAvg.getAsDouble());
            }
            ratingSummary.setNumberOfRatingsByRate(ratings.stream().collect(groupingBy(Rating::getRate, counting())));
            return ratingSummary;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find summary rating for api {}", api, ex);
            throw new TechnicalManagementException("An error occurred while trying to find summary rating for api " + api, ex);
        }
    }

    @Override
    public RatingEntity findByApiForConnectedUser(final String api) {
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findByApiAndUser(api, getAuthenticatedUsername());
            if (ratingOptional.isPresent()) {
                return convert(ratingOptional.get());
            }
            return null;
        } catch (final TechnicalException ex) {
            final String message = "An error occurred while trying to find rating for api " + api + " and user " + getAuthenticatedUsername();
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public RatingEntity update(final UpdateRatingEntity ratingEntity) {
        try {
            final Rating rating = findById(ratingEntity.getId());
            if (!rating.getApi().equals(ratingEntity.getApi())) {
                throw new RatingNotFoundException(ratingEntity.getId(), ratingEntity.getApi());
            }
            final Date now = new Date();
            rating.setUpdatedAt(now);
            rating.setRate(ratingEntity.getRate());

            // we can save a title/comment only once
            if (isBlank(rating.getTitle())) {
                rating.setTitle(ratingEntity.getTitle());
            }
            if (isBlank(rating.getComment())) {
                rating.setComment(ratingEntity.getComment());
            }
            return convert(ratingRepository.update(rating));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to update rating {}", ratingEntity.getId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to update rating " + ratingEntity.getId(), ex);
        }
    }

    @Override
    public void delete(final String id) {
        try {
            findById(id);
            ratingRepository.delete(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete rating {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete rating " + id, ex);
        }
    }

    @Override
    public void deleteAnswer(final String ratingId, final String answerId) {
        try {
            findById(ratingId);
            ratingAnswerRepository.delete(answerId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete rating answer {}", answerId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete rating answer " + answerId, ex);
        }
    }

    private Rating findById(String id) {
        if (!enabled) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findById(id);
            if (!ratingOptional.isPresent()) {
                throw new RatingNotFoundException(id);
            }
            return ratingOptional.get();
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find a rating by id {}", id, ex);
            throw new TechnicalManagementException("An error occurred while trying to find a rating by id " + id, ex);
        }
    }

    private RatingEntity convert(final Rating rating) {
        final RatingEntity ratingEntity = new RatingEntity();

        final UserEntity user = userService.findByName(rating.getUser(), false);
        ratingEntity.setUsername(user.getUsername());
        ratingEntity.setFirstname(user.getFirstname());
        ratingEntity.setLastname(user.getLastname());

        ratingEntity.setId(rating.getId());
        ratingEntity.setApi(rating.getApi());
        ratingEntity.setTitle(rating.getTitle());
        ratingEntity.setComment(rating.getComment());
        ratingEntity.setRate(rating.getRate());
        ratingEntity.setCreatedAt(rating.getCreatedAt());
        ratingEntity.setUpdatedAt(rating.getUpdatedAt());

        try {
            final List<RatingAnswer> ratingAnswers = ratingAnswerRepository.findByRating(rating.getId());
            if (ratingAnswers != null) {
                ratingEntity.setAnswers(ratingAnswers.stream()
                        .map(ratingAnswer -> {
                            final RatingAnswerEntity ratingAnswerEntity = new RatingAnswerEntity();
                            ratingAnswerEntity.setId(ratingAnswer.getId());
                            final UserEntity userAnswer = userService.findByName(ratingAnswer.getUser(), false);
                            ratingAnswerEntity.setUsername(userAnswer.getUsername());
                            ratingAnswerEntity.setFirstname(userAnswer.getFirstname());
                            ratingAnswerEntity.setLastname(userAnswer.getLastname());
                            ratingAnswerEntity.setComment(ratingAnswer.getComment());
                            ratingAnswerEntity.setCreatedAt(ratingAnswer.getCreatedAt());
                            return ratingAnswerEntity;
                        })
                        .sorted(comparing(RatingAnswerEntity::getCreatedAt, reverseOrder()))
                        .collect(toList()));
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find rating answers by rating id {}", rating.getId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to find rating answers by rating id " + rating.getId(), ex);
        }
        return ratingEntity;
    }

    private Rating convert(final NewRatingEntity ratingEntity) {
        final Rating rating = new Rating();
        rating.setId(UUID.toString(UUID.random()));
        rating.setApi(ratingEntity.getApi());
        rating.setRate(ratingEntity.getRate());
        rating.setTitle(ratingEntity.getTitle());
        rating.setComment(ratingEntity.getComment());
        rating.setUser(getAuthenticatedUsername());
        final Date now = new Date();
        rating.setCreatedAt(now);
        rating.setUpdatedAt(now);
        return rating;
    }
}
