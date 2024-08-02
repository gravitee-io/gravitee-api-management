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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.management.model.RatingReferenceType;
import java.util.*;
import org.junit.Before;
import org.junit.Test;

public class RatingRepositoryTest extends AbstractManagementRepositoryTest {

    private final Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));

    @Override
    protected String getTestCasesPath() {
        return "/data/rating-tests/";
    }

    @Before
    public void init() {
        cal.set(Calendar.YEAR, 2017);
        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.DAY_OF_MONTH, 11);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<Rating> ratingOptional = ratingRepository.findById("rating-id");

        assertTrue(ratingOptional.isPresent());
        final Rating rating = ratingOptional.get();
        assertEquals("api", rating.getReferenceId());
        assertEquals(RatingReferenceType.API, rating.getReferenceType());
        assertEquals("user", rating.getUser());
        assertEquals("title", rating.getTitle());
        assertEquals("My comment", rating.getComment());
        assertTrue(compareDate(cal.getTime(), rating.getCreatedAt()));
        assertEquals(1, rating.getRate());

        final List<RatingAnswer> ratingAnswers = ratingAnswerRepository.findByRating(rating.getId());
        final RatingAnswer ratingAnswer = ratingAnswers.get(0);
        assertEquals("user", ratingAnswer.getUser());
        assertEquals("Answer", ratingAnswer.getComment());
        assertTrue(compareDate(cal.getTime(), ratingAnswer.getCreatedAt()));
    }

    @Test
    public void shouldFindByApiAndUser() throws Exception {
        final Optional<Rating> ratingOptional = ratingRepository.findByReferenceIdAndReferenceTypeAndUser(
            "api",
            RatingReferenceType.API,
            "user"
        );

        assertTrue(ratingOptional.isPresent());
        final Rating rating = ratingOptional.get();
        assertEquals("api", rating.getReferenceId());
        assertEquals(RatingReferenceType.API, rating.getReferenceType());
        assertEquals("user", rating.getUser());
        assertEquals("title", rating.getTitle());
        assertEquals("My comment", rating.getComment());
        assertTrue(compareDate(cal.getTime(), rating.getCreatedAt()));
        assertEquals(1, rating.getRate());

        final List<RatingAnswer> ratingAnswers = ratingAnswerRepository.findByRating(rating.getId());
        final RatingAnswer ratingAnswer = ratingAnswers.get(0);
        assertEquals("user", ratingAnswer.getUser());
        assertEquals("Answer", ratingAnswer.getComment());
        assertTrue(compareDate(cal.getTime(), ratingAnswer.getCreatedAt()));
    }

    @Test
    public void shouldFindByApiPageable() throws Exception {
        Page<Rating> ratingPage = ratingRepository.findByReferenceIdAndReferenceTypePageable(
            "api",
            RatingReferenceType.API,
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );

        assertEquals(0, ratingPage.getPageNumber());
        assertEquals(2, ratingPage.getPageElements());
        assertEquals(3, ratingPage.getTotalElements());

        assertEquals(2, ratingPage.getContent().size());
        assertEquals("rating4-id", ratingPage.getContent().get(0).getId());
        assertEquals("rating-id", ratingPage.getContent().get(1).getId());

        ratingPage =
            ratingRepository.findByReferenceIdAndReferenceTypePageable(
                "api",
                RatingReferenceType.API,
                new PageableBuilder().pageNumber(1).pageSize(2).build()
            );

        assertEquals(1, ratingPage.getPageNumber());
        assertEquals(1, ratingPage.getPageElements());
        assertEquals(3, ratingPage.getTotalElements());

        assertEquals(1, ratingPage.getContent().size());
        assertEquals("rating2-id", ratingPage.getContent().get(0).getId());
    }

    @Test
    public void shouldFindByReferenceIdAndReferenceType() throws Exception {
        final List<Rating> ratings = ratingRepository.findByReferenceIdAndReferenceType("api", RatingReferenceType.API);
        assertEquals(3, ratings.size());
        assertEquals(1, ratings.stream().filter(rating -> "rating-id".equals(rating.getId())).count());
        assertEquals(1, ratings.stream().filter(rating -> "rating2-id".equals(rating.getId())).count());
        assertEquals(1, ratings.stream().filter(rating -> "rating4-id".equals(rating.getId())).count());
    }

    @Test
    public void shouldFindReferenceIdsOrderByRate() throws Exception {
        final Set<String> ranking = ratingRepository.findReferenceIdsOrderByRate(new RatingCriteria.Builder().build());
        assertEquals(ranking, new LinkedHashSet<>(Arrays.asList("api", "api2")));
    }

    @Test
    public void shouldFindReferenceIdsOrderByRateWithCriteria() throws Exception {
        final Set<String> ranking = ratingRepository.findReferenceIdsOrderByRate(
            new RatingCriteria.Builder().referenceIds("api").referenceType(RatingReferenceType.API).gt(1).build()
        );
        assertEquals(ranking, new LinkedHashSet<>(Arrays.asList("api")));
    }

    @Test
    public void shouldCreate() throws Exception {
        final Rating rating = new Rating();
        rating.setId("new-rating");
        rating.setReferenceId("api");
        rating.setReferenceType(RatingReferenceType.API);
        rating.setUser("user");
        rating.setTitle("title");
        rating.setComment("comment");
        rating.setRate(new Byte("5"));
        rating.setCreatedAt(cal.getTime());
        rating.setUpdatedAt(cal.getTime());

        final RatingAnswer ratingAnswer = new RatingAnswer();
        ratingAnswer.setId("new-answer-id");
        ratingAnswer.setRating(rating.getId());
        ratingAnswer.setUser("user");
        ratingAnswer.setComment("My answer");
        ratingAnswer.setCreatedAt(cal.getTime());

        ratingAnswerRepository.create(ratingAnswer);

        assertFalse(ratingRepository.findById("new-rating").isPresent());
        ratingRepository.create(rating);
        Optional<Rating> optional = ratingRepository.findById("new-rating");
        assertTrue("Rating saved not found", optional.isPresent());

        final Rating ratingSaved = optional.get();
        assertEquals("Invalid rating api.", rating.getReferenceId(), ratingSaved.getReferenceId());
        assertEquals("Invalid rating api.", rating.getReferenceType(), ratingSaved.getReferenceType());
        assertEquals("Invalid rating user.", rating.getUser(), ratingSaved.getUser());
        assertEquals("Invalid rating title.", rating.getTitle(), ratingSaved.getTitle());
        assertEquals("Invalid rating comment.", rating.getComment(), ratingSaved.getComment());
        assertEquals("Invalid rating rate.", rating.getRate(), ratingSaved.getRate());
        assertTrue("Invalid rating created date.", compareDate(rating.getCreatedAt(), ratingSaved.getCreatedAt()));
        assertTrue("Invalid rating updated date.", compareDate(rating.getUpdatedAt(), ratingSaved.getUpdatedAt()));
        assertEquals("Invalid rating answers.", singletonList(ratingAnswer), ratingAnswerRepository.findByRating(rating.getId()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        final Rating rating = new Rating();
        rating.setId("rating-id");
        rating.setReferenceId("api");
        rating.setReferenceType(RatingReferenceType.API);
        rating.setUser("user10");
        rating.setTitle("title10");
        rating.setComment("comment10");
        rating.setRate(new Byte("3"));
        rating.setCreatedAt(cal.getTime());
        rating.setUpdatedAt(cal.getTime());

        final Rating ratingSaved = ratingRepository.update(rating);
        assertEquals("Invalid rating api.", rating.getReferenceId(), ratingSaved.getReferenceId());
        assertEquals("Invalid rating api.", rating.getReferenceType(), ratingSaved.getReferenceType());
        assertEquals("Invalid rating user.", rating.getUser(), ratingSaved.getUser());
        assertEquals("Invalid rating title.", rating.getTitle(), ratingSaved.getTitle());
        assertEquals("Invalid rating comment.", rating.getComment(), ratingSaved.getComment());
        assertEquals("Invalid rating rate.", rating.getRate(), ratingSaved.getRate());
        assertTrue("Invalid rating created date.", compareDate(rating.getCreatedAt(), ratingSaved.getCreatedAt()));
        assertTrue("Invalid rating updated date.", compareDate(rating.getUpdatedAt(), ratingSaved.getUpdatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(ratingRepository.findById("rating3-id").isPresent());
        ratingRepository.delete("rating3-id");
        assertFalse("Rating not deleted", ratingRepository.findById("rating3-id").isPresent());
    }

    @Test
    public void shouldDeleteAnswer() throws Exception {
        assertTrue(ratingAnswerRepository.findById("answer-id").isPresent());
        ratingAnswerRepository.delete("answer-id");
        assertFalse("Rating answer not deleted", ratingAnswerRepository.findById("answer-id").isPresent());
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final var beforeDeletion = ratingRepository
            .findByReferenceIdAndReferenceType("ToBeDeleted", RatingReferenceType.API)
            .stream()
            .map(Rating::getId)
            .toList();
        final var deleted = ratingRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", RatingReferenceType.API);
        final var nbAfterDeletion = ratingRepository.findByReferenceIdAndReferenceType("ToBeDeleted", RatingReferenceType.API).size();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertTrue(beforeDeletion.containsAll(deleted));
        assertEquals(0, nbAfterDeletion);
    }

    @Test
    public void should_delete_answer_by_rating() throws Exception {
        final var beforeDeletion = ratingAnswerRepository.findByRating("ToBeDeleted").stream().map(RatingAnswer::getId).toList();
        final var deleted = ratingAnswerRepository.deleteByRating("ToBeDeleted");
        final var nbAfterDeletion = ratingAnswerRepository.findByRating("ToBeDeleted").size();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertTrue(beforeDeletion.containsAll(deleted));
        assertEquals(0, nbAfterDeletion);
    }
}
