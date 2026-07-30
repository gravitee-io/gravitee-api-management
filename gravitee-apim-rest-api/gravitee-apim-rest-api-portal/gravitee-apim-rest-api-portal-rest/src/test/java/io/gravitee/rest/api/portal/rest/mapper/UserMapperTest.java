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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.portal.rest.model.FinalizeRegistrationInput;
import io.gravitee.rest.api.portal.rest.model.RegisterUserInput;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.model.UserLinks;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class UserMapperTest {

    private static final String USER_EMAIL = "my-user-email";
    private static final String USER_FIRSTNAME = "my-user-firstname";
    private static final String USER_LASTNAME = "my-user-lastname";
    private static final String USER_TOKEN = "my-user-token";
    private static final String USER_ID = "my-user-id";
    private static final String USER_PASSWORD = "my-user-password";
    private static final String USER_PICTURE = "my-user-picture";
    private static final String USER_SOURCE = "my-user-source";
    private static final String USER_SOURCE_ID = "my-user-source-id";
    private static final String USER_STATUS = "my-user-status";

    private static final String SEARCH_USER_DISPLAY_NAME = "my-search-user-display-name";
    private static final String SEARCH_USER_REFERENCE = "my-search-user-reference";

    @InjectMocks
    private UserMapper userMapper;

    @Test
    public void testConvertUserEntity() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        // init
        UserEntity userEntity = new UserEntity();

        userEntity.setCreatedAt(nowDate);
        userEntity.setEmail(USER_EMAIL);
        userEntity.setFirstname(USER_FIRSTNAME);
        userEntity.setId(USER_ID);
        userEntity.setLastConnectionAt(nowDate);
        userEntity.setLastname(USER_LASTNAME);
        userEntity.setPassword(USER_PASSWORD);
        userEntity.setPicture(USER_PICTURE);
        userEntity.setRoles(null);
        userEntity.setSource(USER_SOURCE);
        userEntity.setSourceId(USER_SOURCE_ID);
        userEntity.setStatus(USER_STATUS);
        userEntity.setUpdatedAt(nowDate);

        // Test
        User responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(USER_ID, responseUser.getId());
        assertEquals(USER_EMAIL, responseUser.getEmail());
        assertEquals(USER_FIRSTNAME, responseUser.getFirstName());
        assertEquals(USER_LASTNAME, responseUser.getLastName());
        assertEquals(USER_FIRSTNAME + ' ' + USER_LASTNAME, responseUser.getDisplayName());
    }

    @Test
    public void testConvertUserEntityWithPermissions() throws Exception {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        // init
        UserEntity userEntity = new UserEntity();
        UserRoleEntity userRoleEntityOrganization = new UserRoleEntity();
        userRoleEntityOrganization.setId("org-id");
        userRoleEntityOrganization.setScope(RoleScope.ORGANIZATION);
        HashMap<String, char[]> organizationPermissions = new HashMap<>();
        organizationPermissions.put("USER", new char[] { 'C', 'R', 'U', 'D' });
        organizationPermissions.put("ENVIRONMENT", new char[] { 'C', 'R', 'U', 'D' });
        userRoleEntityOrganization.setPermissions(organizationPermissions);

        UserRoleEntity userRoleEntityEnvironment = new UserRoleEntity();
        userRoleEntityEnvironment.setScope(RoleScope.ENVIRONMENT);
        userRoleEntityEnvironment.setId("env-id");
        HashMap<String, char[]> environmentPermissions = new HashMap<>();
        environmentPermissions.put("APPLICATION", new char[] { 'C' });
        userRoleEntityEnvironment.setPermissions(environmentPermissions);

        userEntity.setCreatedAt(nowDate);
        userEntity.setEmail(USER_EMAIL);
        userEntity.setFirstname(USER_FIRSTNAME);
        userEntity.setId(USER_ID);
        userEntity.setLastConnectionAt(nowDate);
        userEntity.setLastname(USER_LASTNAME);
        userEntity.setPassword(USER_PASSWORD);
        userEntity.setPicture(USER_PICTURE);
        userEntity.setRoles(new HashSet<>(Arrays.asList(userRoleEntityOrganization, userRoleEntityEnvironment)));
        userEntity.setSource(USER_SOURCE);
        userEntity.setSourceId(USER_SOURCE_ID);
        userEntity.setStatus(USER_STATUS);
        userEntity.setUpdatedAt(nowDate);

        // Test
        User responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(USER_ID, responseUser.getId());
        assertEquals(USER_EMAIL, responseUser.getEmail());
        assertEquals(USER_FIRSTNAME, responseUser.getFirstName());
        assertEquals(USER_LASTNAME, responseUser.getLastName());
        assertEquals(USER_FIRSTNAME + ' ' + USER_LASTNAME, responseUser.getDisplayName());
        assertTrue(responseUser.getPermissions().getAPPLICATION().containsAll(Arrays.asList("C")));
    }

    @Test
    void should_scope_permissions_to_current_environment() {
        UserRoleEntity organizationRole = role("organization-role", RoleScope.ORGANIZATION, "USER", 'R');
        UserRoleEntity devRole = role("dev-role", RoleScope.ENVIRONMENT, "APPLICATION", 'C', 'R');
        UserRoleEntity testRole = role("test-role", RoleScope.ENVIRONMENT, "APPLICATION", 'R');
        UserEntity userEntity = new UserEntity();
        userEntity.setRoles(Set.of(organizationRole, devRole, testRole));
        userEntity.setEnvRoles(Map.of("DEV", Set.of(devRole), "TEST", Set.of(testRole)));

        User devUser = userMapper.convertForEnvironment(userEntity, "DEV");
        User testUser = userMapper.convertForEnvironment(userEntity, "TEST");

        assertThat(devUser.getPermissions().getAPPLICATION()).containsExactlyInAnyOrder("C", "R");
        assertThat(devUser.getPermissions().getUSER()).containsExactly("R");
        assertThat(testUser.getPermissions().getAPPLICATION()).containsExactly("R");
        assertThat(testUser.getPermissions().getUSER()).containsExactly("R");
    }

    @Test
    void should_union_permissions_from_roles_in_current_environment() {
        UserRoleEntity createRole = role("create-role", RoleScope.ENVIRONMENT, "APPLICATION", 'C');
        UserRoleEntity updateRole = role("update-role", RoleScope.ENVIRONMENT, "APPLICATION", 'R', 'U');
        UserEntity userEntity = new UserEntity();
        userEntity.setRoles(Set.of(createRole, updateRole));
        userEntity.setEnvRoles(Map.of("DEV", Set.of(createRole, updateRole)));

        User user = userMapper.convertForEnvironment(userEntity, "DEV");

        assertThat(user.getPermissions().getAPPLICATION()).containsExactlyInAnyOrder("C", "R", "U");
    }

    @Test
    void should_not_fall_back_to_flat_environment_roles_when_scoped_roles_are_missing() {
        UserRoleEntity organizationRole = role("organization-role", RoleScope.ORGANIZATION, "USER", 'R');
        UserRoleEntity environmentRole = role("environment-role", RoleScope.ENVIRONMENT, "APPLICATION", 'C');
        UserEntity userEntity = new UserEntity();
        userEntity.setRoles(Set.of(organizationRole, environmentRole));

        User user = userMapper.convertForEnvironment(userEntity, "DEV");

        assertThat(user.getPermissions().getUSER()).containsExactly("R");
        assertThat(user.getPermissions().getAPPLICATION()).isNull();
    }

    @Test
    public void testConvertSearchUser() {
        // init
        io.gravitee.apim.core.user.model.User searchUser = io.gravitee.apim.core.user.model.User.builder()
            .id(USER_ID)
            .reference(SEARCH_USER_REFERENCE)
            .displayName(SEARCH_USER_DISPLAY_NAME)
            .firstName(USER_FIRSTNAME)
            .lastName(USER_LASTNAME)
            .email(USER_EMAIL)
            .editableProfile(true)
            .permissions(Map.of("APPLICATION", List.of("C", "R"), "USER", List.of("R")))
            .build();

        // Test
        User responseUser = userMapper.convert(searchUser);
        assertNotNull(responseUser);
        assertEquals(USER_ID, responseUser.getId());
        assertEquals(USER_EMAIL, responseUser.getEmail());
        assertEquals(USER_FIRSTNAME, responseUser.getFirstName());
        assertEquals(USER_LASTNAME, responseUser.getLastName());
        assertEquals(SEARCH_USER_DISPLAY_NAME, responseUser.getDisplayName());
        assertEquals(SEARCH_USER_REFERENCE, responseUser.getReference());
        assertTrue(responseUser.getEditableProfile());
        assertEquals(List.of("C", "R"), responseUser.getPermissions().getAPPLICATION());
        assertEquals(List.of("R"), responseUser.getPermissions().getUSER());
    }

    @Test
    public void testConvertRegisterUserInput() {
        // init
        RegisterUserInput input = new RegisterUserInput();

        input.setEmail(USER_EMAIL);
        input.setFirstname(USER_FIRSTNAME);
        input.setLastname(USER_LASTNAME);

        // Test
        NewExternalUserEntity newExternalUserEntity = userMapper.convert(input);
        assertNotNull(newExternalUserEntity);
        assertEquals(USER_EMAIL, newExternalUserEntity.getEmail());
        assertEquals(USER_FIRSTNAME, newExternalUserEntity.getFirstname());
        assertEquals(USER_LASTNAME, newExternalUserEntity.getLastname());
    }

    @Test
    public void testConvertFinalizeRegistrationInput() {
        // init
        FinalizeRegistrationInput input = new FinalizeRegistrationInput();

        input.setToken(USER_TOKEN);
        input.setPassword(USER_PASSWORD);
        input.setFirstname(USER_FIRSTNAME);
        input.setLastname(USER_LASTNAME);

        // Test
        RegisterUserEntity registerUserEntity = userMapper.convert(input);
        assertNotNull(registerUserEntity);
        assertEquals(USER_TOKEN, registerUserEntity.getToken());
        assertEquals(USER_PASSWORD, registerUserEntity.getPassword());
        assertEquals(USER_FIRSTNAME, registerUserEntity.getFirstname());
        assertEquals(USER_LASTNAME, registerUserEntity.getLastname());
    }

    @Test
    public void testUserLinks() {
        String basePath = "/user";

        UserLinks links = userMapper.computeUserLinks(basePath, null);

        assertNotNull(links);

        assertEquals(basePath, links.getSelf());
        assertEquals(basePath + "/avatar?", links.getAvatar());
        assertEquals(basePath + "/notifications", links.getNotifications());
        assertEquals(basePath, links.getSelf());
    }

    private static UserRoleEntity role(String id, RoleScope scope, String permission, char... actions) {
        UserRoleEntity role = new UserRoleEntity();
        role.setId(id);
        role.setScope(scope);
        role.setPermissions(Map.of(permission, actions));
        return role;
    }
}
