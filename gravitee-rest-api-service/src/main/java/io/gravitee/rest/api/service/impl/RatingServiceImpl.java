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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.ApiRatingUnavailableException;
import io.gravitee.rest.api.service.exceptions.RatingAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.RatingNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.management.model.RatingReferenceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AuditService auditService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private NotifierService notifierService;

    @Autowired
    private ApiService apiService;

    @Override
    public RatingEntity create(final NewRatingEntity ratingEntity) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findByReferenceIdAndReferenceTypeAndUser(ratingEntity.getApi(), RatingReferenceType.API, getAuthenticatedUsername());
            if (ratingOptional.isPresent()) {
                throw new RatingAlreadyExistsException(ratingEntity.getApi(), getAuthenticatedUsername());
            }
            Rating rating = ratingRepository.create(convert(ratingEntity));
            auditService.createApiAuditLog(rating.getReferenceId(), null, Rating.RatingEvent.RATING_CREATED, rating.getCreatedAt(), null, rating);

            notifierService.trigger(
                    ApiHook.NEW_RATING,
                    rating.getReferenceId(),
                    new NotificationParamsBuilder()
                            .api(apiService.findById(rating.getReferenceId()))
                            .build());

            return convert(rating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create rating on api {}", ratingEntity.getApi(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create rating on api " + ratingEntity.getApi(), ex);
        }
    }

    @Override
    public RatingEntity createAnswer(final NewRatingAnswerEntity answerEntity) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Rating rating = findModelById(answerEntity.getRatingId());

            final RatingAnswer ratingAnswer = new RatingAnswer();
            ratingAnswer.setId(UUID.toString(UUID.random()));
            ratingAnswer.setRating(answerEntity.getRatingId());
            ratingAnswer.setUser(getAuthenticatedUsername());
            ratingAnswer.setComment(answerEntity.getComment());
            ratingAnswer.setCreatedAt(new Date());
            ratingAnswerRepository.create(ratingAnswer);
            auditService.createApiAuditLog(rating.getReferenceId(), null, RatingAnswer.RatingAnswerEvent.RATING_ANSWER_CREATED, ratingAnswer.getCreatedAt(), null, ratingAnswer);

            notifierService.trigger(
                    ApiHook.NEW_RATING_ANSWER,
                    rating.getReferenceId(),
                    new NotificationParamsBuilder()
                            .api(apiService.findById(rating.getReferenceId()))
                            .build());

            return convert(rating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create a rating answer on rating {}", answerEntity.getRatingId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create a rating answer on rating" + answerEntity.getRatingId(), ex);
        }
    }

    @Override
    public RatingEntity findById(String id) {
        return convert(findModelById(id));
    }

    @Override
    public Page<RatingEntity> findByApi(final String api, final Pageable pageable) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Page<Rating> pageRating = ratingRepository.findByReferenceIdAndReferenceTypePageable(api, RatingReferenceType.API,
                    new PageableBuilder().pageNumber(pageable.pageNumber() - 1).pageSize(pageable.pageSize()).build());
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
    public List<RatingEntity> findByApi(String api) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final List<Rating> ratings = ratingRepository.findByReferenceIdAndReferenceType(api, RatingReferenceType.API);
            final List<RatingEntity> ratingEntities = ratings.stream().map(this::convert).collect(toList());
            return ratingEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find ratings for api {}", api, ex);
            throw new TechnicalManagementException("An error occurred while trying to find ratings for api " + api, ex);
        }
    }

    @Override
    public RatingSummaryEntity findSummaryByApi(final String api) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final List<Rating> ratings = ratingRepository.findByReferenceIdAndReferenceType(api, RatingReferenceType.API);
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
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findByReferenceIdAndReferenceTypeAndUser(api, RatingReferenceType.API, getAuthenticatedUsername());
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
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Rating rating = findModelById(ratingEntity.getId());
            final Rating oldRating = new Rating(rating);
            if (!rating.getReferenceId().equals(ratingEntity.getApi())) {
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
            Rating updatedRating = ratingRepository.update(rating);
            auditService.createApiAuditLog(rating.getReferenceId(), null, Rating.RatingEvent.RATING_UPDATED, updatedRating.getUpdatedAt(), oldRating, updatedRating);
            return convert(updatedRating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to update rating {}", ratingEntity.getId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to update rating " + ratingEntity.getId(), ex);
        }
    }

    @Override
    public void delete(final String id) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            Rating rating = findModelById(id);
            ratingRepository.delete(id);
            auditService.createApiAuditLog(rating.getReferenceId(), null, Rating.RatingEvent.RATING_DELETED, new Date(), rating, null);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete rating {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete rating " + id, ex);
        }
    }

    @Override
    public void deleteAnswer(final String ratingId, final String answerId) {
        if (!isEnabled()) {
            throw new ApiRatingUnavailableException();
        }
        try {
            Rating rating = findModelById(ratingId);
            ratingAnswerRepository.delete(answerId);
            auditService.createApiAuditLog(rating.getReferenceId(), null, RatingAnswer.RatingAnswerEvent.RATING_ANSWER_DELETED, new Date(), rating, null);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete rating answer {}", answerId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete rating answer " + answerId, ex);
        }
    }

    @Override
    public boolean isEnabled() {
        return parameterService.findAsBoolean(Key.PORTAL_RATING_ENABLED);
    }

    private Rating findModelById(String id) {
        if (!isEnabled()) {
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

        final UserEntity user = userService.findById(rating.getUser());
        ratingEntity.setUser(user.getId());
        ratingEntity.setUserDisplayName(user.getDisplayName());
        ratingEntity.setId(rating.getId());
        ratingEntity.setApi(rating.getReferenceId());
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
                            final UserEntity userAnswer = userService.findById(ratingAnswer.getUser());
                            ratingAnswerEntity.setUser(userAnswer.getId());

                            if (userAnswer.getFirstname() != null && userAnswer.getLastname() != null) {
                                ratingAnswerEntity.setUserDisplayName(userAnswer.getFirstname() + ' ' + userAnswer.getLastname());
                            } else {
                                ratingAnswerEntity.setUserDisplayName(userAnswer.getEmail());
                            }
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
        rating.setReferenceId(ratingEntity.getApi());
        rating.setReferenceType(RatingReferenceType.API);
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
