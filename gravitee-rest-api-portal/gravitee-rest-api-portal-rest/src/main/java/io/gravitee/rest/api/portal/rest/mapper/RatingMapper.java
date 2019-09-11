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

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingAnswer;
import io.gravitee.rest.api.service.UserService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingMapper {
    
    @Autowired
    UserService userService;
    
    @Autowired
    UserMapper userMapper;
    
    public Rating convert(RatingEntity ratingEntity) {
        final Rating rating = new Rating();

        UserEntity author = userService.findById(ratingEntity.getUser());
        rating.setAuthor(userMapper.convert(author));
        rating.setTitle(ratingEntity.getTitle());
        rating.setComment(ratingEntity.getComment());
        if(ratingEntity.getCreatedAt() != null) {
            rating.setDate(ratingEntity.getCreatedAt().toInstant().atOffset( ZoneOffset.UTC ));
        }
        rating.setId(ratingEntity.getId());
        rating.setValue(Integer.valueOf(ratingEntity.getRate()));
        
        if(ratingEntity.getAnswers() != null) {
            List<RatingAnswer> ratingsAnswer = 
                    ratingEntity.getAnswers().stream()
                        .map(rae -> {
                            UserEntity responseAuthor = userService.findById(rae.getUser());
                            return new RatingAnswer()
                                    .author(userMapper.convert(responseAuthor))
                                    .comment(rae.getComment())
                                    .date(rae.getCreatedAt().toInstant().atOffset( ZoneOffset.UTC ));
                        }
                        )
                        .collect(Collectors.toList());
            rating.setAnswers(ratingsAnswer);
        }
        
        return rating;
    }

}
