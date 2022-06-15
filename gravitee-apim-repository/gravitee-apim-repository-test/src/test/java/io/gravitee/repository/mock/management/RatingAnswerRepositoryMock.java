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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.model.RatingAnswer;
import io.gravitee.repository.mock.AbstractRepositoryMock;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RatingAnswerRepositoryMock extends AbstractRepositoryMock<RatingAnswerRepository> {

    public RatingAnswerRepositoryMock() {
        super(RatingAnswerRepository.class);
    }

    @Override
    protected void prepare(RatingAnswerRepository ratingAnswerRepository) throws Exception {
        final RatingAnswer ratingAnswer = new RatingAnswer();
        ratingAnswer.setId("answer-id");
        ratingAnswer.setRating("rating-id");
        ratingAnswer.setUser("user");
        ratingAnswer.setComment("Answer");
        ratingAnswer.setCreatedAt(parse("11/02/2017"));

        final RatingAnswer ratingAnswer2 = new RatingAnswer();
        ratingAnswer2.setId("answer2-id");
        ratingAnswer2.setRating("rating3-id");
        ratingAnswer2.setUser("admin");
        ratingAnswer2.setComment("Answer2");
        ratingAnswer2.setCreatedAt(parse("11/02/2017"));

        final RatingAnswer newRatingAnswer = new RatingAnswer();
        newRatingAnswer.setId("new-answer-id");
        newRatingAnswer.setRating("new-rating");
        newRatingAnswer.setUser("user");
        newRatingAnswer.setComment("My answer");
        newRatingAnswer.setCreatedAt(parse("11/02/2017"));

        when(ratingAnswerRepository.findByRating("rating-id")).thenReturn(singletonList(ratingAnswer));
        when(ratingAnswerRepository.findByRating("rating3-id")).thenReturn(singletonList(ratingAnswer2));
        when(ratingAnswerRepository.findByRating("new-rating")).thenReturn(singletonList(newRatingAnswer));
        when(ratingAnswerRepository.findById("answer-id")).thenReturn(of(ratingAnswer), empty());
    }
}
