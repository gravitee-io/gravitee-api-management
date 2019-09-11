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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.RatingAnswerEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingAnswer;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.UserService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RatingMapperTest {

    private static final String RATING_API = "my-rating-api";
    private static final String RATING_ID = "my-rating-id";
    private static final String RATING_COMMENT = "my-rating-comment";
    private static final String RATING_AUTHOR = "my-rating-author";
    private static final String RATING_AUTHOR_DISPLAY_NAME = "my-rating-author-display-name";
    private static final String RATING_TITLE = "my-rating-title";

    private static final String RATING_RESPONSE_AUTHOR = "my-rating-response-author";
    private static final String RATING_RESPONSE_COMMENT = "my-rating-response-comment";
    private static final String RATING_RESPONSE_AUTHOR_DISPLAY_NAME = "my-rating-response-author-display-name";
    private static final String RATING_RESPONSE_ID = "my-rating-response_ID";

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private RatingMapper ratingMapper;
    
    @Test
    public void testConvert() {
        reset(userService);
        reset(userMapper);
        
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        RatingEntity ratingEntity = new RatingEntity();
       
        RatingAnswerEntity ratingAnswerEntity = new RatingAnswerEntity();
        ratingAnswerEntity.setComment(RATING_RESPONSE_COMMENT);
        ratingAnswerEntity.setCreatedAt(nowDate);
        ratingAnswerEntity.setId(RATING_RESPONSE_ID);
        ratingAnswerEntity.setUser(RATING_RESPONSE_AUTHOR);
        ratingAnswerEntity.setUserDisplayName(RATING_RESPONSE_AUTHOR_DISPLAY_NAME);
        
        ratingEntity.setAnswers(Arrays.asList(ratingAnswerEntity));
        ratingEntity.setApi(RATING_API);
        ratingEntity.setComment(RATING_COMMENT);
        ratingEntity.setCreatedAt(nowDate);
        ratingEntity.setId(RATING_ID);
        ratingEntity.setRate((byte)1);
        ratingEntity.setTitle(RATING_TITLE);
        ratingEntity.setUpdatedAt(nowDate);
        ratingEntity.setUser(RATING_AUTHOR);
        ratingEntity.setUserDisplayName(RATING_AUTHOR_DISPLAY_NAME);
        
        UserEntity authorEntity = new UserEntity();
        authorEntity.setId(RATING_AUTHOR);
        UserEntity responseAuthorEntity = new UserEntity();
        responseAuthorEntity.setId(RATING_RESPONSE_AUTHOR);
        
        User author = new User();
        author.setId(RATING_AUTHOR);
        User responseAuthor = new User();
        responseAuthor.setId(RATING_RESPONSE_AUTHOR);
        
        doReturn(authorEntity).when(userService).findById(RATING_AUTHOR);
        doReturn(responseAuthorEntity).when(userService).findById(RATING_RESPONSE_AUTHOR);
        doReturn(author).when(userMapper).convert(authorEntity);
        doReturn(responseAuthor).when(userMapper).convert(responseAuthorEntity);
        
        Rating responseRating = ratingMapper.convert(ratingEntity);
        assertNotNull(responseRating);
        
        List<RatingAnswer> answers = responseRating.getAnswers();
        assertNotNull(answers);
        assertEquals(1, answers.size());
        RatingAnswer ratingAnswer = answers.get(0);
        assertNotNull(ratingAnswer);
        assertEquals(RATING_RESPONSE_COMMENT, ratingAnswer.getComment());
        assertEquals(now.toEpochMilli(), ratingAnswer.getDate().toInstant().toEpochMilli());
        assertEquals(responseAuthor, ratingAnswer.getAuthor());
        
        assertEquals(author, responseRating.getAuthor());
        assertEquals(RATING_COMMENT, responseRating.getComment());
        assertEquals(RATING_TITLE, responseRating.getTitle());
        assertEquals(now.toEpochMilli(), responseRating.getDate().toInstant().toEpochMilli());
        assertEquals(RATING_ID, responseRating.getId());
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
