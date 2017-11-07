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
package io.gravitee.management.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.*;
import io.gravitee.management.service.exceptions.RatingAlreadyExistsException;
import io.gravitee.management.service.exceptions.RatingNotFoundException;
import io.gravitee.management.service.impl.RatingServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingAnswer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

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

    @Before
    public void init() {
        setField(ratingService, "enabled", true);

        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserDetails(USER, "", emptyList()));
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(rating.getId()).thenReturn(RATING_ID);
        when(rating.getApi()).thenReturn(API_ID);
        when(rating.getTitle()).thenReturn(TITLE);
        when(rating.getComment()).thenReturn(COMMENT);
        when(rating.getRate()).thenReturn(RATE);
        when(rating.getUser()).thenReturn(USER);

        when(userService.findByName(USER, false)).thenReturn(user);
        when(user.getUsername()).thenReturn(USER);
    }

    @Test(expected = RatingAlreadyExistsException.class)
    public void shouldNotCreateBecauseAlreadyExists() throws TechnicalException {
        when(newRatingEntity.getApi()).thenReturn(API_ID);
        when(ratingRepository.findByApiAndUser(API_ID, USER)).thenReturn(of(rating));

        ratingService.create(newRatingEntity);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        when(ratingRepository.findByApiAndUser(API_ID, USER)).thenReturn(empty());
        when(ratingRepository.create(any())).thenReturn(rating);
        when(newRatingEntity.getApi()).thenReturn(API_ID);
        when(newRatingEntity.getComment()).thenReturn(COMMENT);
        when(newRatingEntity.getRate()).thenReturn(RATE);
        when(newRatingEntity.getTitle()).thenReturn(TITLE);

        final RatingEntity ratingEntity = ratingService.create(newRatingEntity);

        assertEquals(USER, ratingEntity.getUsername());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
    }

    @Test(expected = RatingNotFoundException.class)
    public void shouldNotCreateAnswerBecauseUnknownRating() throws TechnicalException {
        when(newRatingAnswerEntity.getRatingId()).thenReturn(UNKNOWN_RATING_ID);
        when(ratingRepository.findById(UNKNOWN_RATING_ID)).thenReturn(empty());

        ratingService.createAnswer(newRatingAnswerEntity);
    }

    @Test
    public void shouldCreateAnswer() throws TechnicalException {
        final Rating updatedRating = mock(Rating.class);
        when(updatedRating.getApi()).thenReturn(API_ID);
        when(updatedRating.getTitle()).thenReturn(TITLE);
        when(updatedRating.getComment()).thenReturn(COMMENT);
        when(updatedRating.getRate()).thenReturn(RATE);
        when(updatedRating.getUser()).thenReturn(USER);

        when(ratingAnswer.getUser()).thenReturn(USER);
        when(ratingAnswer.getComment()).thenReturn(ANSWER);
        when(ratingAnswer.getCreatedAt()).thenReturn(new Date());

        when(ratingAnswerRepository.findByRating(RATING_ID)).thenReturn(singletonList(ratingAnswer));

        when(newRatingAnswerEntity.getRatingId()).thenReturn(RATING_ID);
        when(newRatingAnswerEntity.getComment()).thenReturn(COMMENT);
        when(ratingRepository.findById(RATING_ID)).thenReturn(of(rating));

        final RatingEntity ratingEntity = ratingService.createAnswer(newRatingAnswerEntity);

        verify(ratingAnswerRepository).create(any(RatingAnswer.class));

        assertEquals(USER, ratingEntity.getUsername());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
        assertEquals(ANSWER, ratingEntity.getAnswers().get(0).getComment());
        assertEquals(USER, ratingEntity.getAnswers().get(0).getUsername());
        assertNotNull(ratingEntity.getAnswers().get(0).getCreatedAt());
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        final Pageable pageable = mock(Pageable.class);
        final Page<Rating> pageRating = mock(Page.class);
        when(pageRating.getPageNumber()).thenReturn(1);
        when(pageRating.getPageElements()).thenReturn(10L);
        when(pageRating.getTotalElements()).thenReturn(100L);
        when(pageRating.getContent()).thenReturn(singletonList(rating));

        when(ratingRepository.findByApiPageable(API_ID, pageable)).thenReturn(pageRating);

        final Page<RatingEntity> pageRatingEntity = ratingService.findByApi(API_ID, pageable);

        assertEquals(1, pageRatingEntity.getPageNumber());
        assertEquals(10, pageRatingEntity.getPageElements());
        assertEquals(100, pageRatingEntity.getTotalElements());
        final RatingEntity ratingEntity = pageRatingEntity.getContent().get(0);
        assertEquals(USER, ratingEntity.getUsername());
        assertEquals(API_ID, ratingEntity.getApi());
        assertEquals(TITLE, ratingEntity.getTitle());
        assertEquals(COMMENT, ratingEntity.getComment());
        assertEquals(RATE, ratingEntity.getRate(), 0);
        assertEquals(ratingEntity.getCreatedAt(), ratingEntity.getUpdatedAt());
    }

    @Test
    public void shouldNotFindByApiForConnectedUser() throws TechnicalException {
        when(ratingRepository.findByApiAndUser(API_ID, USER)).thenReturn(empty());
        assertNull(ratingService.findByApiForConnectedUser(API_ID));
    }

    @Test
    public void shouldFindByApiForConnectedUser() throws TechnicalException {
        when(ratingRepository.findByApiAndUser(API_ID, USER)).thenReturn(of(rating));

        final RatingEntity ratingEntity = ratingService.findByApiForConnectedUser(API_ID);

        assertEquals(USER, ratingEntity.getUsername());
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

        ratingService.update(updateRatingEntity);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(ratingRepository.update(any())).thenReturn(rating);

        when(updateRatingEntity.getId()).thenReturn(RATING_ID);
        when(updateRatingEntity.getApi()).thenReturn(API_ID);
        when(updateRatingEntity.getRate()).thenReturn(RATE);
        when(ratingRepository.findById(RATING_ID)).thenReturn(of(rating));

        ratingService.update(updateRatingEntity);

        verify(rating).setUpdatedAt(any(Date.class));
        verify(rating).setRate(RATE);
        verify(ratingRepository).update(rating);
    }

    @Test(expected = RatingNotFoundException.class)
    public void shouldNotDeleteBecauseUnknownRating() throws TechnicalException {
        when(updateRatingEntity.getId()).thenReturn(UNKNOWN_RATING_ID);
        when(ratingRepository.findById(UNKNOWN_RATING_ID)).thenReturn(empty());

        ratingService.delete(UNKNOWN_RATING_ID);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        when(ratingRepository.findById(RATING_ID)).thenReturn(of(rating));
        ratingService.delete(RATING_ID);
        verify(ratingRepository).delete(RATING_ID);
    }

    @Test
    public void shouldFindSummaryByApiWithNoRatings() throws TechnicalException {
        when(ratingRepository.findByApi(API_ID)).thenReturn(emptyList());

        final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(API_ID);
        assertEquals(API_ID, ratingSummary.getApi());
        assertEquals(0, ratingSummary.getNumberOfRatings());
        assertNull(ratingSummary.getAverageRate());
        assertTrue(ratingSummary.getNumberOfRatingsByRate().isEmpty());
    }

    @Test
    public void shouldFindSummaryByApi() throws TechnicalException {
        final Rating r = new Rating();
        r.setRate(new Byte("4"));

        when(ratingRepository.findByApi(API_ID)).thenReturn(asList(rating, r));

        final RatingSummaryEntity ratingSummary = ratingService.findSummaryByApi(API_ID);
        assertEquals(API_ID, ratingSummary.getApi());
        assertEquals(2, ratingSummary.getNumberOfRatings());
        assertEquals(3.5, ratingSummary.getAverageRate(), 0);
        assertFalse(ratingSummary.getNumberOfRatingsByRate().isEmpty());
        assertEquals(1, ratingSummary.getNumberOfRatingsByRate().get(new Byte("3")), 0);
        assertEquals(1, ratingSummary.getNumberOfRatingsByRate().get(new Byte("4")), 0);
    }
}