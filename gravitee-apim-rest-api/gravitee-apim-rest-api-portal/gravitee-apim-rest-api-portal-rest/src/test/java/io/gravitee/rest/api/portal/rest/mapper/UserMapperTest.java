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

import static org.junit.Assert.*;

import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.portal.rest.model.FinalizeRegistrationInput;
import io.gravitee.rest.api.portal.rest.model.RegisterUserInput;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.portal.rest.model.UserLinks;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
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

    private static final String DEV_ENVIRONMENT_ID = "dev-environment-id";
    private static final String TEST_ENVIRONMENT_ID = "test-environment-id";

    @InjectMocks
    private UserMapper userMapper;

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

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
        GraviteeContext.setCurrentEnvironment(DEV_ENVIRONMENT_ID);
        userEntity.setEnvRoles(Map.of(DEV_ENVIRONMENT_ID, Set.of(userRoleEntityEnvironment)));
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
    public void should_not_apply_flat_environment_roles_when_env_roles_is_not_populated() {
        UserRoleEntity environmentRole = environmentRole("env-role-id", Map.of("APPLICATION", new char[] { 'C' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(environmentRole)));
        userEntity.setEnvRoles(null);
        GraviteeContext.setCurrentEnvironment(DEV_ENVIRONMENT_ID);

        User user = userMapper.convert(userEntity);
        assertNull(user.getPermissions().getAPPLICATION());
    }

    @Test
    public void should_not_apply_environment_roles_when_current_environment_is_not_set() {
        UserRoleEntity flatRole = environmentRole("flat-role-id", Map.of("APPLICATION", new char[] { 'C' }));
        UserRoleEntity defaultEnvRole = environmentRole("default-env-role-id", Map.of("APPLICATION", new char[] { 'R' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(flatRole)));
        userEntity.setEnvRoles(Map.of(GraviteeContext.getDefaultEnvironment(), Set.of(defaultEnvRole)));

        GraviteeContext.setCurrentEnvironment(null);
        User user = userMapper.convert(userEntity);
        assertNull(user.getPermissions().getAPPLICATION());
    }

    @Test
    public void should_not_apply_environment_roles_when_current_environment_absent_from_env_roles() {
        UserRoleEntity devRole = environmentRole("le-group-role-id", Map.of("APPLICATION", new char[] { 'C', 'R' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(devRole)));
        // envRoles is populated but has no entry for the current environment.
        userEntity.setEnvRoles(Map.of(DEV_ENVIRONMENT_ID, Set.of(devRole)));

        GraviteeContext.setCurrentEnvironment(TEST_ENVIRONMENT_ID);
        User testUser = userMapper.convert(userEntity);
        assertNull(testUser.getPermissions().getAPPLICATION());
    }

    @Test
    public void should_not_apply_environment_permissions_from_flat_roles_when_env_roles_has_current_environment_entry() {
        UserRoleEntity devRole = environmentRole("le-group-role-id", Map.of("APPLICATION", new char[] { 'C', 'R' }));
        UserRoleEntity testRole = environmentRole("ue-group-role-id", Map.of("APPLICATION", new char[] { 'R' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(devRole, testRole)));
        userEntity.setEnvRoles(Map.of(DEV_ENVIRONMENT_ID, Set.of(devRole), TEST_ENVIRONMENT_ID, Set.of()));

        GraviteeContext.setCurrentEnvironment(TEST_ENVIRONMENT_ID);
        User testUser = userMapper.convert(userEntity);
        assertNull(testUser.getPermissions().getAPPLICATION());
    }

    @Test
    public void should_include_organization_and_current_environment_permissions() {
        UserRoleEntity orgRole = organizationRole("org-role-id", Map.of("USER", new char[] { 'R' }));
        UserRoleEntity envRole = environmentRole("env-role-id", Map.of("APPLICATION", new char[] { 'C' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(orgRole, envRole)));
        userEntity.setEnvRoles(Map.of(DEV_ENVIRONMENT_ID, Set.of(envRole)));

        GraviteeContext.setCurrentEnvironment(DEV_ENVIRONMENT_ID);
        User user = userMapper.convert(userEntity);

        assertTrue(user.getPermissions().getUSER().contains("R"));
        assertTrue(user.getPermissions().getAPPLICATION().contains("C"));
    }

    @Test
    public void should_union_permissions_from_multiple_roles_in_same_environment() {
        UserRoleEntity createRole = environmentRole("create-role-id", Map.of("APPLICATION", new char[] { 'C' }));
        UserRoleEntity readRole = environmentRole("read-role-id", Map.of("APPLICATION", new char[] { 'R' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(createRole, readRole)));
        userEntity.setEnvRoles(Map.of(DEV_ENVIRONMENT_ID, Set.of(createRole, readRole)));

        GraviteeContext.setCurrentEnvironment(DEV_ENVIRONMENT_ID);
        User user = userMapper.convert(userEntity);

        assertTrue(user.getPermissions().getAPPLICATION().contains("C"));
        assertTrue(user.getPermissions().getAPPLICATION().contains("R"));
    }

    @Test
    public void should_use_only_current_environment_roles_when_user_has_multiple_environment_memberships() {
        UserRoleEntity devRole = environmentRole("le-group-role-id", Map.of("APPLICATION", new char[] { 'C', 'R' }));
        UserRoleEntity testRole = environmentRole("ue-group-role-id", Map.of("APPLICATION", new char[] { 'R' }));

        UserEntity userEntity = baseUserEntity();
        userEntity.setRoles(new HashSet<>(Set.of(devRole, testRole)));
        userEntity.setEnvRoles(Map.of(DEV_ENVIRONMENT_ID, Set.of(devRole), TEST_ENVIRONMENT_ID, Set.of(testRole)));

        GraviteeContext.setCurrentEnvironment(DEV_ENVIRONMENT_ID);
        User devUser = userMapper.convert(userEntity);
        assertTrue(devUser.getPermissions().getAPPLICATION().contains("C"));

        GraviteeContext.setCurrentEnvironment(TEST_ENVIRONMENT_ID);
        User testUser = userMapper.convert(userEntity);
        assertFalse(testUser.getPermissions().getAPPLICATION().contains("C"));
        assertTrue(testUser.getPermissions().getAPPLICATION().contains("R"));
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

    private static UserEntity baseUserEntity() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        UserEntity userEntity = new UserEntity();
        userEntity.setCreatedAt(nowDate);
        userEntity.setEmail(USER_EMAIL);
        userEntity.setFirstname(USER_FIRSTNAME);
        userEntity.setId(USER_ID);
        userEntity.setLastConnectionAt(nowDate);
        userEntity.setLastname(USER_LASTNAME);
        userEntity.setPassword(USER_PASSWORD);
        userEntity.setPicture(USER_PICTURE);
        userEntity.setSource(USER_SOURCE);
        userEntity.setSourceId(USER_SOURCE_ID);
        userEntity.setStatus(USER_STATUS);
        userEntity.setUpdatedAt(nowDate);
        return userEntity;
    }

    private static UserRoleEntity environmentRole(String roleId, Map<String, char[]> permissions) {
        UserRoleEntity role = new UserRoleEntity();
        role.setId(roleId);
        role.setScope(RoleScope.ENVIRONMENT);
        role.setPermissions(permissions);
        return role;
    }

    private static UserRoleEntity organizationRole(String roleId, Map<String, char[]> permissions) {
        UserRoleEntity role = new UserRoleEntity();
        role.setId(roleId);
        role.setScope(RoleScope.ORGANIZATION);
        role.setPermissions(permissions);
        return role;
    }
}
