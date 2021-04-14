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

import static io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper.usersURL;

import io.gravitee.rest.api.model.RatingAnswerEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingAnswer;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.UserService;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    public Rating convert(RatingEntity ratingEntity, UriInfo uriInfo) {
        final Rating rating = new Rating();
        UserEntity authorEntity = userService.findById(ratingEntity.getUser());
        User author = userMapper.convert(authorEntity);
        author.setLinks(
            userMapper.computeUserLinks(usersURL(uriInfo.getBaseUriBuilder(), authorEntity.getId()), authorEntity.getUpdatedAt())
        );
        rating.setAuthor(author);
        rating.setTitle(ratingEntity.getTitle());
        rating.setComment(ratingEntity.getComment());
        if (ratingEntity.getCreatedAt() != null) {
            rating.setDate(ratingEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        rating.setId(ratingEntity.getId());
        rating.setValue(Integer.valueOf(ratingEntity.getRate()));

        if (ratingEntity.getAnswers() != null) {
            List<RatingAnswer> ratingsAnswer = ratingEntity
                .getAnswers()
                .stream()
                .sorted(Comparator.comparing(RatingAnswerEntity::getCreatedAt))
                .map(
                    rae -> {
                        UserEntity answerAuthorEntity = userService.findById(rae.getUser());
                        User answerAuthor = userMapper.convert(answerAuthorEntity);
                        answerAuthor.setLinks(
                            userMapper.computeUserLinks(
                                usersURL(uriInfo.getBaseUriBuilder(), answerAuthorEntity.getId()),
                                answerAuthorEntity.getUpdatedAt()
                            )
                        );
                        return new RatingAnswer()
                            .id(rae.getId())
                            .author(answerAuthor)
                            .comment(rae.getComment())
                            .date(rae.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
                    }
                )
                .collect(Collectors.toList());
            rating.setAnswers(ratingsAnswer);
        }

        return rating;
    }
}
