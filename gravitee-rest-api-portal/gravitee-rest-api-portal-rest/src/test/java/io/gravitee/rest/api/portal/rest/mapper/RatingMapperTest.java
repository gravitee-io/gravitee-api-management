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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.RatingAnswerEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.portal.rest.model.Author;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingAnswer;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RatingMapperTest {

    private static final String API = "my-api";
    private static final String RATING = "my-rating";

    private RatingEntity ratingEntity;

    @InjectMocks
    private RatingMapper ratingMapper;
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        ratingEntity = new RatingEntity();
       
        RatingAnswerEntity ratingAnswerEntity = new RatingAnswerEntity();
        ratingAnswerEntity.setComment(RATING);
        ratingAnswerEntity.setCreatedAt(nowDate);
        ratingAnswerEntity.setId(RATING);
        ratingAnswerEntity.setUser(RATING);
        ratingAnswerEntity.setUserDisplayName(RATING);
        
        ratingEntity.setAnswers(Arrays.asList(ratingAnswerEntity));
        ratingEntity.setApi(API);
        ratingEntity.setComment(RATING);
        ratingEntity.setCreatedAt(nowDate);
        ratingEntity.setId(RATING);
        ratingEntity.setRate((byte)1);
        ratingEntity.setTitle(RATING);
        ratingEntity.setUpdatedAt(nowDate);
        ratingEntity.setUser(RATING);
        ratingEntity.setUserDisplayName(RATING);
        
        Rating responseRating = ratingMapper.convert(ratingEntity);
        assertNotNull(responseRating);
        
        List<RatingAnswer> answers = responseRating.getAnswers();
        assertNotNull(answers);
        assertEquals(1, answers.size());
        RatingAnswer ratingAnswer = answers.get(0);
        assertNotNull(ratingAnswer);
        assertEquals(RATING, ratingAnswer.getComment());
        assertEquals(now.toEpochMilli(), ratingAnswer.getDate().toInstant().toEpochMilli());
        Author ratingAnswerAuthor = ratingAnswer.getAuthor();
        assertNotNull(ratingAnswerAuthor);
        assertEquals(RATING, ratingAnswerAuthor.getId());
        assertEquals(RATING, ratingAnswerAuthor.getName());
        //APIPortal:[test]:add a test for ratingAnswer author's email
        assertNull(ratingAnswerAuthor.getEmail());
        
        Author author = responseRating.getAuthor();
        assertNotNull(author);
        assertEquals(RATING, author.getId());
        assertEquals(RATING, author.getName());
        //APIPortal:[test]:add a test for rating author's email
        assertNull(author.getEmail());
        
        assertEquals(RATING, responseRating.getComment());
        assertEquals(now.toEpochMilli(), responseRating.getDate().toInstant().toEpochMilli());
        assertEquals(RATING, responseRating.getId());
        assertEquals(Integer.valueOf(1), responseRating.getValue());
    }
 
    @Test
    public void testMinimalConvert() {
        Rating responseRating = ratingMapper.convert(new RatingEntity());
        assertNotNull(responseRating);
        assertNull(responseRating.getAnswers());
        assertNull(responseRating.getDate());
    }
}
