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
package io.gravitee.repository.config.mock;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RatingRepositoryMock extends AbstractRepositoryMock<RatingRepository> {

    public RatingRepositoryMock() {
        super(RatingRepository.class);
    }

    @Override
    void prepare(RatingRepository ratingRepository) throws Exception {
        final Rating rating = mockRating("rating-id", "api", RatingReferenceType.API, "user", "title", "My comment", "1");
        final Rating rating2 = mockRating("rating2-id", "api", RatingReferenceType.API, "toto", "title2", "My comment2", "5");
        final Rating rating3 = mockRating("rating3-id", "api2", RatingReferenceType.API, "admin", "title3", "My comment3", "2");
        final Rating rating4 = mockRating("rating4-id", "api", RatingReferenceType.API, "admin", "title4", "My comment4", "2");
        final Rating newRating = mockRating("new-rating", "api", RatingReferenceType.API, "user", "title", "comment", "5");

        when(ratingRepository.findById("rating-id")).thenReturn(of(rating));
        when(ratingRepository.findById("new-rating")).thenReturn(empty(), of(newRating));
        when(ratingRepository.findByReferenceIdAndReferenceTypePageable(eq("api"), eq(RatingReferenceType.API), any(Pageable.class)))
            .thenReturn(
                new io.gravitee.common.data.domain.Page<>(asList(rating4, rating), 0, 2, 3),
                new io.gravitee.common.data.domain.Page<>(asList(rating2), 1, 1, 3)
            );

        final Rating updatedRating = mockRating("rating-id", "api", RatingReferenceType.API, "user10", "title10", "comment10", "3");
        when(ratingRepository.update(any(Rating.class))).thenReturn(updatedRating);

        when(ratingRepository.findById("rating3-id")).thenReturn(of(rating3), empty());

        when(ratingRepository.findByReferenceIdAndReferenceTypeAndUser("api", RatingReferenceType.API, "user")).thenReturn(of(rating));
        when(ratingRepository.findByReferenceIdAndReferenceType("api", RatingReferenceType.API))
            .thenReturn(asList(rating, rating2, rating4));

        when(ratingRepository.findReferenceIdsOrderByRate(eq(new RatingCriteria.Builder().build())))
            .thenReturn(new LinkedHashSet<>(Arrays.asList("api", "api2")));

        when(
            ratingRepository.findReferenceIdsOrderByRate(
                eq(new RatingCriteria.Builder().referenceType(RatingReferenceType.API).gt(1).referenceIds("api").build())
            )
        )
            .thenReturn(new LinkedHashSet<>(Arrays.asList("api")));
    }

    private Rating mockRating(
        final String id,
        final String referenceId,
        final RatingReferenceType referenceType,
        final String user,
        final String title,
        final String comment,
        final String rate
    ) {
        final Rating rating = mock(Rating.class);
        when(rating.getId()).thenReturn(id);
        when(rating.getReferenceId()).thenReturn(referenceId);
        when(rating.getReferenceType()).thenReturn(referenceType);
        when(rating.getUser()).thenReturn(user);
        when(rating.getTitle()).thenReturn(title);
        when(rating.getComment()).thenReturn(comment);
        when(rating.getRate()).thenReturn(new Byte(rate));
        when(rating.getCreatedAt()).thenReturn(parse("11/02/2017"));
        when(rating.getUpdatedAt()).thenReturn(parse("11/02/2017"));
        return rating;
    }
}
