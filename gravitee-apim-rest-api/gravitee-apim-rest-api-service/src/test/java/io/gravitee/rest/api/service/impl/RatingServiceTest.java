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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.management.model.RatingReferenceType;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.RatingAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.RatingNotFoundException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RatingServiceTest {

    private static final String USER = "my-user";
    private static final String API_ID = "my-api";
    private static final String TITLE = "My title";
    private static final String COMMENT = "My comment";
    private static final String ANSWER = "My answer";
    private static final Byte RATE = 3;
    private static final String RATING_ID = "my-rating";
    private static final String UNKNOWN_RATING_ID = "unknown-rating";

    @InjectMocks
    private RatingService ratingService = new RatingServiceImpl();

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingAnswerRepository ratingAnswerRepository;

    @Mock
    private UserService userService;

    @Mock
    private NewRatingEntity newRatingEntity;

    @Mock
    private UpdateRatingEntity updateRatingEntity;

    @Mock
    private NewRatingAnswerEntity newRatingAnswerEntity;

    @Mock
    private Rating rating;

    @Mock
    private RatingAnswer ratingAnswer;

    @Mock
    private UserEntity user;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService mockParameterService;

    @Mock
    private ApiSearchService mockApiSearchService;

    @Mock
    private NotifierService mockNotifierService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void init() {
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserDetails(USER, "", emptyList()));
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(rating.getId()).thenReturn(RATING_ID);
        when(rating.getReferenceId()).thenReturn(API_ID);
        when(rating.getTitle()).thenReturn(TITLE);
        when(rating.getComment()).thenReturn(COMMENT);
        when(rating.getRate()).thenReturn(RATE);
        when(rating.getUser()).thenReturn(USER);

        when(userService.findById(GraviteeContext.getExecutionContext(), USER)).thenReturn(user);
        when(user.getId()).thenReturn(USER);

        when(
            mockParameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PORTAL_RATING_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
    }

    @Test(expected = RatingAlreadyExistsException.class)
    public void shouldNotCreateBecauseAlreadyExists() throws TechnicalException {
        when(newRatingEntity.getApi()).thenReturn(API_ID);
        when(ratingRepository.findByReferenceIdAndReferenceTypeAndUser(API_ID, RatingReferenceType.API, USER)).thenReturn(of(rating));

        ratingService.create(GraviteeContext.getExecutionContext(), newRatingEntity);
        verify(mockNotifierService, never())
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.NEW_RATING_ANSWER), eq(API_ID), any());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        when(ratingRepository.findByReferenceIdAndReferenceTypeAndUser(API_ID, RatingReferenceType.API, USER)).thenReturn(empty());
        when(ratingRepository.create(any())).thenReturn(rating);
        when(newRatingEntity.getApi()).thenReturn(API_ID);
        when(newRatingEntity.getComment()).thenReturn(COMMENT);
        when(newRatingEntity.getRate()).thenReturn(RATE);
        when(newRatingEntity.getTitle()).thenReturn(TITLE);

        final RatingEntity ratingEntity = ratingService.create(GraviteeContext.getExecutionContext(), newRatingEntity);

        assertEquals(USER, ratingEntity.getUser());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
        verify(mockNotifierService, times(1)).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.NEW_RATING), eq(API_ID), any());
        verify(mockNotifierService, never())
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.NEW_RATING_ANSWER), eq(API_ID), any());
    }

    @Test(expected = RatingNotFoundException.class)
    public void shouldNotCreateAnswerBecauseUnknownRating() throws TechnicalException {
        when(newRatingAnswerEntity.getRatingId()).thenReturn(UNKNOWN_RATING_ID);
        when(ratingRepository.findById(UNKNOWN_RATING_ID)).thenReturn(empty());

        ratingService.createAnswer(GraviteeContext.getExecutionContext(), newRatingAnswerEntity);
        verify(mockNotifierService, times(1)).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.NEW_RATING), eq(API_ID), any());
        verify(mockNotifierService, never())
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.NEW_RATING_ANSWER), eq(API_ID), any());
    }

    @Test
    public void shouldCreateAnswer() throws TechnicalException {
        when(ratingAnswer.getUser()).thenReturn(USER);
        when(ratingAnswer.getComment()).thenReturn(ANSWER);
        when(ratingAnswer.getCreatedAt()).thenReturn(new Date());

        when(ratingAnswerRepository.findByRating(RATING_ID)).thenReturn(singletonList(ratingAnswer));

        when(newRatingAnswerEntity.getRatingId()).thenReturn(RATING_ID);
        when(newRatingAnswerEntity.getComment()).thenReturn(COMMENT);
        when(ratingRepository.findById(RATING_ID)).thenReturn(of(rating));

        final RatingEntity ratingEntity = ratingService.createAnswer(GraviteeContext.getExecutionContext(), newRatingAnswerEntity);

        verify(ratingAnswerRepository).create(any(RatingAnswer.class));

        assertEquals(USER, ratingEntity.getUser());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
        assertEquals(ANSWER, ratingEntity.getAnswers().get(0).getComment());
        assertEquals(USER, ratingEntity.getAnswers().get(0).getUser());
        assertNotNull(ratingEntity.getAnswers().get(0).getCreatedAt());
        verify(mockNotifierService, times(1))
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.NEW_RATING_ANSWER), eq(API_ID), any());
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        final Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(1);
        when(pageable.getPageSize()).thenReturn(1);
        final Page<Rating> pageRating = new Page<>(List.of(rating), 1, 10, 100);

        when(
            ratingRepository.findByReferenceIdAndReferenceTypePageable(
                eq(API_ID),
                eq(RatingReferenceType.API),
                eq(new PageableBuilder().pageNumber(0).pageSize(1).build())
            )
        )
            .thenReturn(pageRating);

        final Page<RatingEntity> pageRatingEntity = ratingService.findByApi(GraviteeContext.getExecutionContext(), API_ID, pageable);

        assertEquals(1, pageRatingEntity.getPageNumber());
        assertEquals(10, pageRatingEntity.getPageElements());
        assertEquals(100, pageRatingEntity.getTotalElements());
        final RatingEntity ratingEntity = pageRatingEntity.getContent().get(0);
        assertEquals(USER, ratingEntity.getUser());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
    }

    @Test
    public void shouldNotFindByApiForConnectedUser() throws TechnicalException {
        when(ratingRepository.findByReferenceIdAndReferenceTypeAndUser(API_ID, RatingReferenceType.API, USER)).thenReturn(empty());
        assertNull(ratingService.findByApiForConnectedUser(GraviteeContext.getExecutionContext(), API_ID));
    }

    @Test
    public void shouldFindByApiForConnectedUser() throws TechnicalException {
        when(ratingRepository.findByReferenceIdAndReferenceTypeAndUser(API_ID, RatingReferenceType.API, USER)).thenReturn(of(rating));

        final RatingEntity ratingEntity = ratingService.findByApiForConnectedUser(GraviteeContext.getExecutionContext(), API_ID);

        assertEquals(USER, ratingEntity.getUser());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
    }

    @Test(expected = RatingNotFoundException.class)
    public void shouldNotUpdateBecauseUnknownRating() throws TechnicalException {
        when(updateRatingEntity.getId()).thenReturn(UNKNOWN_RATING_ID);
        when(ratingRepository.findById(UNKNOWN_RATING_ID)).thenReturn(empty());

        ratingService.update(GraviteeContext.getExecutionContext(), updateRatingEntity);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(ratingRepository.update(any())).thenReturn(rating);

        when(updateRatingEntity.getId()).thenReturn(RATING_ID);
        when(updateRatingEntity.getApi()).thenReturn(API_ID);
        when(updateRatingEntity.getRate()).thenReturn(RATE);
        when(ratingRepository.findById(RATING_ID)).thenReturn(of(rating));

        ratingService.update(GraviteeContext.getExecutionContext(), updateRatingEntity);

        verify(rating).setUpdatedAt(any(Date.class));
        verify(rating).setRate(RATE);
        verify(ratingRepository).update(rating);
    }

    @Test(expected = RatingNotFoundException.class)
    public void shouldNotDeleteBecauseUnknownRating() throws TechnicalException {
        when(ratingRepository.findById(UNKNOWN_RATING_ID)).thenReturn(empty());

        ratingService.delete(GraviteeContext.getExecutionContext(), UNKNOWN_RATING_ID);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        when(ratingRepository.findById(RATING_ID)).thenReturn(of(rating));
        ratingService.delete(GraviteeContext.getExecutionContext(), RATING_ID);
        verify(ratingRepository).delete(RATING_ID);
    }

    @Test
    public void shouldFindSummaryByApiWithNoRatings() throws TechnicalException {
        when(ratingRepository.findByReferenceIdAndReferenceType(API_ID, RatingReferenceType.API)).thenReturn(emptyList());

        final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(GraviteeContext.getExecutionContext(), API_ID);
        assertEquals(API_ID, ratingSummary.getApi());
        assertEquals(0, ratingSummary.getNumberOfRatings());
        assertNull(ratingSummary.getAverageRate());
        assertTrue(ratingSummary.getNumberOfRatingsByRate().isEmpty());
    }

    @Test
    public void shouldFindSummaryByApi() throws TechnicalException {
        final Rating r = new Rating();
        r.setRate(new Byte("4"));

        when(ratingRepository.findByReferenceIdAndReferenceType(API_ID, RatingReferenceType.API)).thenReturn(asList(rating, r));

        final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(GraviteeContext.getExecutionContext(), API_ID);
        assertEquals(API_ID, ratingSummary.getApi());
        assertEquals(2, ratingSummary.getNumberOfRatings());
        assertEquals(3.5, ratingSummary.getAverageRate(), 0);
        assertFalse(ratingSummary.getNumberOfRatingsByRate().isEmpty());
        assertEquals(1, ratingSummary.getNumberOfRatingsByRate().get(new Byte("3")), 0);
        assertEquals(1, ratingSummary.getNumberOfRatingsByRate().get(new Byte("4")), 0);
    }
}
