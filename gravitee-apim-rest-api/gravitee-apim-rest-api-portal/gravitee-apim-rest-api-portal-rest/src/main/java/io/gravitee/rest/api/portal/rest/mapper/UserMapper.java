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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.idp.api.identity.SearchableUser;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.portal.rest.model.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserMapper {

    public static final String IDP_SOURCE_GRAVITEE = "gravitee";
    public static final String IDP_SOURCE_MEMORY = "memory";

    private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public User convert(PrimaryOwnerEntity user) {
        return UserMapstructMapper.INSTANCE.primaryOwnerEntityToUser(user);
    }

    public User convert(UserEntity user) {
        return UserMapstructMapper.INSTANCE.userEntityToUser(user);
    }

    public User convert(SearchableUser user) {
        return UserMapstructMapper.INSTANCE.seachableUserToUser(user);
    }

    public NewExternalUserEntity convert(RegisterUserInput input) {
        return UserMapstructMapper.INSTANCE.toNewExternalUserEntity(input);
    }

    public RegisterUserEntity convert(FinalizeRegistrationInput input) {
        return UserMapstructMapper.INSTANCE.toRegisterUserEntity(input);
    }

    public ResetPasswordUserEntity convert(ChangeUserPasswordInput input) {
        return UserMapstructMapper.INSTANCE.toResetPasswordUserEntity(input);
    }

    public UserLinks computeUserLinks(String basePath, Date updateDate) {
        UserLinks userLinks = new UserLinks();
        final String hash = updateDate == null ? "" : String.valueOf(updateDate.getTime());
        userLinks.setAvatar(basePath + "/avatar?" + hash);
        userLinks.setNotifications(basePath + "/notifications");
        userLinks.setSelf(basePath);
        return userLinks;
    }
}
