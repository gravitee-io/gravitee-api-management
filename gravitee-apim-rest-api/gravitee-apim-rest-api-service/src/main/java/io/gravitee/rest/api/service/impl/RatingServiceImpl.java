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
package io.gravitee.rest.api.service.impl;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.management.model.RatingReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiRatingUnavailableException;
import io.gravitee.rest.api.service.exceptions.RatingAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.RatingAnswerNotFoundException;
import io.gravitee.rest.api.service.exceptions.RatingNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingServiceImpl extends AbstractService implements RatingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RatingServiceImpl.class);

    @Lazy
    @Autowired
    private RatingRepository ratingRepository;

    @Lazy
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
    private ApiSearchService apiSearchService;

    @Override
    public RatingEntity create(ExecutionContext executionContext, final NewRatingEntity ratingEntity) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findByReferenceIdAndReferenceTypeAndUser(
                ratingEntity.getApi(),
                RatingReferenceType.API,
                getAuthenticatedUsername()
            );
            if (ratingOptional.isPresent()) {
                throw new RatingAlreadyExistsException(ratingEntity.getApi(), getAuthenticatedUsername());
            }
            Rating rating = ratingRepository.create(convert(ratingEntity));
            auditService.createApiAuditLog(
                executionContext,
                rating.getReferenceId(),
                null,
                Rating.RatingEvent.RATING_CREATED,
                rating.getCreatedAt(),
                null,
                rating
            );

            notifierService.trigger(
                executionContext,
                ApiHook.NEW_RATING,
                rating.getReferenceId(),
                new NotificationParamsBuilder().api(apiSearchService.findGenericById(executionContext, rating.getReferenceId())).build()
            );

            return convert(executionContext, rating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create rating on api {}", ratingEntity.getApi(), ex);
            throw new TechnicalManagementException("An error occurred while trying to create rating on api " + ratingEntity.getApi(), ex);
        }
    }

    @Override
    public RatingEntity createAnswer(ExecutionContext executionContext, final NewRatingAnswerEntity answerEntity) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Rating rating = findModelById(executionContext, answerEntity.getRatingId());

            final RatingAnswer ratingAnswer = new RatingAnswer();
            ratingAnswer.setId(UuidString.generateRandom());
            ratingAnswer.setRating(answerEntity.getRatingId());
            ratingAnswer.setUser(getAuthenticatedUsername());
            ratingAnswer.setComment(answerEntity.getComment());
            ratingAnswer.setCreatedAt(new Date());
            ratingAnswerRepository.create(ratingAnswer);
            auditService.createApiAuditLog(
                executionContext,
                rating.getReferenceId(),
                null,
                RatingAnswer.RatingAnswerEvent.RATING_ANSWER_CREATED,
                ratingAnswer.getCreatedAt(),
                null,
                ratingAnswer
            );

            notifierService.trigger(
                executionContext,
                ApiHook.NEW_RATING_ANSWER,
                rating.getReferenceId(),
                new NotificationParamsBuilder().api(apiSearchService.findGenericById(executionContext, rating.getReferenceId())).build()
            );

            return convert(executionContext, rating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to create a rating answer on rating {}", answerEntity.getRatingId(), ex);
            throw new TechnicalManagementException(
                "An error occurred while trying to create a rating answer on rating" + answerEntity.getRatingId(),
                ex
            );
        }
    }

    @Override
    public RatingEntity findById(ExecutionContext executionContext, String id) {
        return convert(executionContext, findModelById(executionContext, id));
    }

    @Override
    public RatingAnswerEntity findAnswerById(ExecutionContext executionContext, String answerId) {
        try {
            RatingAnswer ratingAnswer = ratingAnswerRepository
                .findById(answerId)
                .orElseThrow(() -> new RatingAnswerNotFoundException(answerId));
            return convert(executionContext, ratingAnswer);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find rating answer by answer id {}", answerId, ex);
            throw new TechnicalManagementException("An error occurred while trying to find rating answer by answer id " + answerId, ex);
        }
    }

    @NotNull
    private RatingAnswerEntity convert(ExecutionContext executionContext, RatingAnswer ratingAnswer) {
        final RatingAnswerEntity ratingAnswerEntity = new RatingAnswerEntity();
        ratingAnswerEntity.setId(ratingAnswer.getId());
        ratingAnswerEntity.setRating(ratingAnswer.getRating());
        final UserEntity userAnswer = userService.findById(executionContext, ratingAnswer.getUser());
        ratingAnswerEntity.setUser(userAnswer.getId());

        if (userAnswer.getFirstname() != null && userAnswer.getLastname() != null) {
            ratingAnswerEntity.setUserDisplayName(userAnswer.getFirstname() + ' ' + userAnswer.getLastname());
        } else {
            ratingAnswerEntity.setUserDisplayName(userAnswer.getEmail());
        }
        ratingAnswerEntity.setComment(ratingAnswer.getComment());
        ratingAnswerEntity.setCreatedAt(ratingAnswer.getCreatedAt());
        return ratingAnswerEntity;
    }

    @Override
    public Page<RatingEntity> findByApi(ExecutionContext executionContext, final String api, final Pageable pageable) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            return ratingRepository
                .findByReferenceIdAndReferenceTypePageable(api, RatingReferenceType.API, convert(pageable))
                .map(rating -> convert(executionContext, rating));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find ratings for api {}", api, ex);
            throw new TechnicalManagementException("An error occurred while trying to find ratings for api " + api, ex);
        }
    }

    @Override
    public List<RatingEntity> findByApi(ExecutionContext executionContext, String api) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final List<Rating> ratings = ratingRepository.findByReferenceIdAndReferenceType(api, RatingReferenceType.API);
            return ratings.stream().map(rating -> convert(executionContext, rating)).collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find ratings for api {}", api, ex);
            throw new TechnicalManagementException("An error occurred while trying to find ratings for api " + api, ex);
        }
    }

    @Override
    public RatingSummaryEntity findSummaryByApi(ExecutionContext executionContext, final String api) {
        if (!isEnabled(executionContext)) {
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
    public Set<String> findReferenceIdsOrderByRate(ExecutionContext executionContext, Collection<String> apis) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            return ratingRepository.findReferenceIdsOrderByRate(new RatingCriteria.Builder().referenceIds(apis).build());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to compute ranking for apis {}", apis, ex);
            throw new TechnicalManagementException("An error occurred while trying to compute ranking for apis " + apis, ex);
        }
    }

    @Override
    public RatingEntity findByApiForConnectedUser(ExecutionContext executionContext, final String api) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Optional<Rating> ratingOptional = ratingRepository.findByReferenceIdAndReferenceTypeAndUser(
                api,
                RatingReferenceType.API,
                getAuthenticatedUsername()
            );
            if (ratingOptional.isPresent()) {
                return convert(executionContext, ratingOptional.get());
            }
            return null;
        } catch (final TechnicalException ex) {
            final String message =
                "An error occurred while trying to find rating for api " + api + " and user " + getAuthenticatedUsername();
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public RatingEntity update(ExecutionContext executionContext, final UpdateRatingEntity ratingEntity) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            final Rating rating = findModelById(executionContext, ratingEntity.getId());
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
            auditService.createApiAuditLog(
                executionContext,
                rating.getReferenceId(),
                null,
                Rating.RatingEvent.RATING_UPDATED,
                updatedRating.getUpdatedAt(),
                oldRating,
                updatedRating
            );
            return convert(executionContext, updatedRating);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to update rating {}", ratingEntity.getId(), ex);
            throw new TechnicalManagementException("An error occurred while trying to update rating " + ratingEntity.getId(), ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, final String id) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            Rating rating = findModelById(executionContext, id);
            ratingRepository.delete(id);
            auditService.createApiAuditLog(
                executionContext,
                rating.getReferenceId(),
                null,
                Rating.RatingEvent.RATING_DELETED,
                new Date(),
                rating,
                null
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete rating {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete rating " + id, ex);
        }
    }

    @Override
    public void deleteAnswer(ExecutionContext executionContext, final String ratingId, final String answerId) {
        if (!isEnabled(executionContext)) {
            throw new ApiRatingUnavailableException();
        }
        try {
            Rating rating = findModelById(executionContext, ratingId);
            ratingAnswerRepository.delete(answerId);
            auditService.createApiAuditLog(
                executionContext,
                rating.getReferenceId(),
                null,
                RatingAnswer.RatingAnswerEvent.RATING_ANSWER_DELETED,
                new Date(),
                rating,
                null
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete rating answer {}", answerId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete rating answer " + answerId, ex);
        }
    }

    @Override
    public boolean isEnabled(ExecutionContext executionContext) {
        return parameterService.findAsBoolean(executionContext, Key.PORTAL_RATING_ENABLED, ParameterReferenceType.ENVIRONMENT);
    }

    private Rating findModelById(ExecutionContext executionContext, String id) {
        if (!isEnabled(executionContext)) {
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

    private RatingEntity convert(ExecutionContext executionContext, final Rating rating) {
        final RatingEntity ratingEntity = new RatingEntity();

        final UserEntity user = userService.findById(executionContext, rating.getUser());
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
                ratingEntity.setAnswers(
                    ratingAnswers
                        .stream()
                        .map(ratingAnswer -> {
                            return convert(executionContext, ratingAnswer);
                        })
                        .sorted(comparing(RatingAnswerEntity::getCreatedAt, reverseOrder()))
                        .collect(toList())
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurred while trying to find rating answers by rating id {}", rating.getId(), ex);
            throw new TechnicalManagementException(
                "An error occurred while trying to find rating answers by rating id " + rating.getId(),
                ex
            );
        }
        return ratingEntity;
    }

    private Rating convert(final NewRatingEntity ratingEntity) {
        final Rating rating = new Rating();
        rating.setId(UuidString.generateRandom());
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
