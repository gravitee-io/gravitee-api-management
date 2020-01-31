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

import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.FinalizeRegistrationInput;
import io.gravitee.rest.api.portal.rest.model.RegisterUserInput;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.model.UserLinks;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserMapper {

    public User convert(UserEntity user) {
        final User userItem = new User();
        userItem.setEmail(user.getEmail());
        userItem.setFirstName(user.getFirstname());
        userItem.setLastName(user.getLastname());
        if(StringUtils.isEmpty(user.getFirstname()) && StringUtils.isEmpty(user.getLastname())) {
            userItem.setDisplayName(user.getDisplayName());    
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.capitalize(user.getFirstname()));
            sb.append(" ");
            sb.append(user.getLastname().toUpperCase().charAt(0));
            sb.append(".");
            userItem.setDisplayName(sb.toString());
        }
        userItem.setId(user.getId());
        return userItem;
    }

    public NewExternalUserEntity convert(RegisterUserInput input) {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setEmail(input.getEmail());
        newExternalUserEntity.setFirstname(input.getFirstname());
        newExternalUserEntity.setLastname(input.getLastname());
        return newExternalUserEntity;
    }

    public RegisterUserEntity convert(FinalizeRegistrationInput input) {
        RegisterUserEntity registerUserEntity = new RegisterUserEntity();
        registerUserEntity.setToken(input.getToken());
        registerUserEntity.setPassword(input.getPassword());
        registerUserEntity.setFirstname(input.getFirstname());
        registerUserEntity.setLastname(input.getLastname());
        return registerUserEntity;
    }

    public UserLinks computeUserLinks(String basePath, String picture) {
        UserLinks userLinks = new UserLinks();
        userLinks.setAvatar(basePath + "/avatar" + (picture == null? "" : "?" + picture.hashCode()));
        userLinks.setNotifications(basePath + "/notifications");
        userLinks.setSelf(basePath);
        return userLinks;
    }
}
