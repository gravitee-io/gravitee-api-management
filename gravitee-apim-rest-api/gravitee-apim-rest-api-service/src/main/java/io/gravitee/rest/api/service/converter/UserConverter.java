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
package io.gravitee.rest.api.service.converter;

import io.gravitee.common.util.Maps;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserMetadataEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class UserConverter {

    public User toUser(NewExternalUserEntity newExternalUserEntity) {
        if (newExternalUserEntity == null) {
            return null;
        }
        User user = new User();
        user.setEmail(newExternalUserEntity.getEmail());
        user.setFirstname(newExternalUserEntity.getFirstname());
        user.setLastname(newExternalUserEntity.getLastname());
        user.setSource(newExternalUserEntity.getSource());
        user.setSourceId(newExternalUserEntity.getSourceId());
        user.setStatus(UserStatus.ACTIVE);
        user.setPicture(newExternalUserEntity.getPicture());
        user.setNewsletterSubscribed(newExternalUserEntity.getNewsletter());
        return user;
    }

    public User toUser(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        User user = new User();
        user.setId(userEntity.getId());
        user.setEmail(userEntity.getEmail());
        user.setFirstname(userEntity.getFirstname());
        user.setLastname(userEntity.getLastname());
        user.setSource(userEntity.getSource());
        user.setSourceId(userEntity.getSourceId());
        if (userEntity.getStatus() != null) {
            user.setStatus(UserStatus.valueOf(userEntity.getStatus()));
        }
        return user;
    }

    public UserEntity toUserEntity(User user) {
        return toUserEntity(user, null);
    }

    public UserEntity toUserEntity(User user, List<UserMetadataEntity> customUserFields) {
        if (user == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();

        userEntity.setId(user.getId());
        userEntity.setSource(user.getSource());
        userEntity.setSourceId(user.getSourceId());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstname(user.getFirstname());
        userEntity.setLastname(user.getLastname());
        userEntity.setPassword(user.getPassword());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());
        userEntity.setLastConnectionAt(user.getLastConnectionAt());
        userEntity.setFirstConnectionAt(user.getFirstConnectionAt());
        userEntity.setPicture(user.getPicture());
        userEntity.setReferenceType(GraviteeContext.ReferenceContextType.ORGANIZATION.name());
        userEntity.setReferenceId(user.getOrganizationId());

        if (user.getStatus() != null) {
            userEntity.setStatus(user.getStatus().name());
        }

        userEntity.setLoginCount(user.getLoginCount());
        userEntity.setNewsletterSubscribed(user.getNewsletterSubscribed());

        if (customUserFields != null && !customUserFields.isEmpty()) {
            Maps.MapBuilder builder = Maps.builder();
            for (UserMetadataEntity meta : customUserFields) {
                builder.put(meta.getKey(), meta.getValue());
            }
            userEntity.setCustomFields(builder.build());
        }
        return userEntity;
    }
}
