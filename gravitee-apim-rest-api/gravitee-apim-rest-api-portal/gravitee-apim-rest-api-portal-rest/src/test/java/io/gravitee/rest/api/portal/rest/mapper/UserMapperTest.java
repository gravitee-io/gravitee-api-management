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

import io.gravitee.rest.api.idp.api.identity.SearchableUser;
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
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
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

    private static final String SEARCHABLE_USER_DISPLAY_NAME = "my-searchable-user-display-name";
    private static final String SEARCHABLE_USER_REFERENCE = "my-searchable-user-reference";

    private static final String ENVIRONMENT_DEV = "dev";
    private static final String ENVIRONMENT_TEST = "test";

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
        userEntity.setRoles(new HashSet<>(Arrays.asList(userRoleEntityOrganization)));
        userEntity.setEnvRoles(Map.of(ENVIRONMENT_DEV, Set.of(userRoleEntityEnvironment)));
        userEntity.setSource(USER_SOURCE);
        userEntity.setSourceId(USER_SOURCE_ID);
        userEntity.setStatus(USER_STATUS);
        userEntity.setUpdatedAt(nowDate);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_DEV);

        // Test
        User responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(USER_ID, responseUser.getId());
        assertEquals(USER_EMAIL, responseUser.getEmail());
        assertEquals(USER_FIRSTNAME, responseUser.getFirstName());
        assertEquals(USER_LASTNAME, responseUser.getLastName());
        assertEquals(USER_FIRSTNAME + ' ' + USER_LASTNAME, responseUser.getDisplayName());
        assertTrue(responseUser.getPermissions().getAPPLICATION().containsAll(Arrays.asList("C")));
        assertTrue(responseUser.getPermissions().getUSER().containsAll(Arrays.asList("C", "R", "U", "D")));
    }

    @Test
    public void shouldOnlyConvertEnvironmentPermissionsOfCurrentEnvironment() {
        // init
        UserRoleEntity devRole = new UserRoleEntity();
        devRole.setId("dev-role-id");
        devRole.setScope(RoleScope.ENVIRONMENT);
        devRole.setPermissions(Map.of("APPLICATION", new char[] { 'C', 'R', 'U', 'D' }));

        UserRoleEntity testRole = new UserRoleEntity();
        testRole.setId("test-role-id");
        testRole.setScope(RoleScope.ENVIRONMENT);
        testRole.setPermissions(Map.of("APPLICATION", new char[] { 'R' }));

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        userEntity.setRoles(new HashSet<>());
        userEntity.setEnvRoles(Map.of(ENVIRONMENT_DEV, Set.of(devRole), ENVIRONMENT_TEST, Set.of(testRole)));

        // Test on 'test' environment: only the read-only role must be taken into account
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_TEST);
        User responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(Arrays.asList("R"), responseUser.getPermissions().getAPPLICATION());

        // Test on 'dev' environment: only the full role must be taken into account
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_DEV);
        responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(Arrays.asList("C", "R", "U", "D"), responseUser.getPermissions().getAPPLICATION());
    }

    @Test
    public void shouldUnionPermissionsOfRolesSharingTheSameKey() {
        // init
        UserRoleEntity organizationRole = new UserRoleEntity();
        organizationRole.setId("org-role-id");
        organizationRole.setScope(RoleScope.ORGANIZATION);
        organizationRole.setPermissions(Map.of("APPLICATION", new char[] { 'R' }));

        UserRoleEntity environmentRole = new UserRoleEntity();
        environmentRole.setId("env-role-id");
        environmentRole.setScope(RoleScope.ENVIRONMENT);
        environmentRole.setPermissions(Map.of("APPLICATION", new char[] { 'C', 'R' }));

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID);
        userEntity.setRoles(new HashSet<>(Arrays.asList(organizationRole)));
        userEntity.setEnvRoles(Map.of(ENVIRONMENT_DEV, Set.of(environmentRole)));

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_DEV);

        // Test
        User responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(2, responseUser.getPermissions().getAPPLICATION().size());
        assertTrue(responseUser.getPermissions().getAPPLICATION().containsAll(Arrays.asList("C", "R")));
    }

    @Test
    public void testConvertSearchableUser() {
        // init
        SearchableUser searchableUser = Mockito.mock(SearchableUser.class);

        Mockito.when(searchableUser.getDisplayName()).thenReturn(SEARCHABLE_USER_DISPLAY_NAME);
        Mockito.when(searchableUser.getEmail()).thenReturn(USER_EMAIL);
        Mockito.when(searchableUser.getFirstname()).thenReturn(USER_FIRSTNAME);
        Mockito.when(searchableUser.getId()).thenReturn(USER_ID);
        Mockito.when(searchableUser.getLastname()).thenReturn(USER_LASTNAME);
        Mockito.when(searchableUser.getReference()).thenReturn(SEARCHABLE_USER_REFERENCE);

        // Test
        User responseUser = userMapper.convert(searchableUser);
        assertNotNull(responseUser);
        assertEquals(USER_ID, responseUser.getId());
        assertEquals(USER_EMAIL, responseUser.getEmail());
        assertEquals(USER_FIRSTNAME, responseUser.getFirstName());
        assertEquals(USER_LASTNAME, responseUser.getLastName());
        assertEquals(SEARCHABLE_USER_DISPLAY_NAME, responseUser.getDisplayName());
        assertEquals(SEARCHABLE_USER_REFERENCE, responseUser.getReference());
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
}
