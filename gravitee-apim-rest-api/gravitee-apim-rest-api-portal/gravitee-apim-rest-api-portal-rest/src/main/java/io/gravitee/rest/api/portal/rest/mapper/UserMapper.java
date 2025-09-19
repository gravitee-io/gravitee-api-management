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
        final User userItem = new User();
        userItem.setEmail(user.getEmail());
        userItem.setDisplayName(user.getDisplayName());
        userItem.setId(user.getId());
        return userItem;
    }

    public User convert(UserEntity user) {
        final User userItem = new User();
        userItem.setEmail(user.getEmail());
        userItem.setFirstName(user.getFirstname());
        userItem.setLastName(user.getLastname());
        userItem.setDisplayName(user.getDisplayName());
        userItem.setId(user.getId());
        userItem.setEditableProfile(IDP_SOURCE_GRAVITEE.equals(user.getSource()) || IDP_SOURCE_MEMORY.equalsIgnoreCase(user.getSource()));
        if (user.getRoles() != null) {
            Map<String, List<String>> userPermissions = user
                .getRoles()
                .stream()
                .filter(role -> RoleScope.ENVIRONMENT.equals(role.getScope()) || RoleScope.ORGANIZATION.equals(role.getScope()))
                .map(UserRoleEntity::getPermissions)
                .map(rolePermissions ->
                    rolePermissions
                        .entrySet()
                        .stream()
                        .collect(
                            Collectors.toMap(Map.Entry::getKey, entry ->
                                new String(entry.getValue())
                                    .chars()
                                    .mapToObj(c -> (char) c)
                                    .map(String::valueOf)
                                    .collect(Collectors.toList())
                            )
                        )
                )
                .reduce(new HashMap<>(), (acc, rolePermissions) -> {
                    acc.putAll(rolePermissions);
                    return acc;
                });
            userItem.setPermissions(objectMapper.convertValue(userPermissions, UserPermissions.class));
        }
        userItem.setCustomFields(user.getCustomFields());
        return userItem;
    }

    public User convert(SearchableUser user) {
        final User userItem = new User();
        userItem.setEmail(user.getEmail());
        userItem.setFirstName(user.getFirstname());
        userItem.setLastName(user.getLastname());
        userItem.setDisplayName(user.getDisplayName());
        userItem.setId(user.getId());
        userItem.setReference(user.getReference());
        return userItem;
    }

    public NewExternalUserEntity convert(RegisterUserInput input) {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setEmail(input.getEmail());
        newExternalUserEntity.setFirstname(input.getFirstname());
        newExternalUserEntity.setLastname(input.getLastname());
        if (input.getCustomFields() != null) {
            newExternalUserEntity.setCustomFields(input.getCustomFields());
        }
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

    public ResetPasswordUserEntity convert(ChangeUserPasswordInput input) {
        ResetPasswordUserEntity changePwdUserEntity = new ResetPasswordUserEntity();
        changePwdUserEntity.setToken(input.getToken());
        changePwdUserEntity.setPassword(input.getPassword());
        changePwdUserEntity.setFirstname(input.getFirstname());
        changePwdUserEntity.setLastname(input.getLastname());
        return changePwdUserEntity;
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
