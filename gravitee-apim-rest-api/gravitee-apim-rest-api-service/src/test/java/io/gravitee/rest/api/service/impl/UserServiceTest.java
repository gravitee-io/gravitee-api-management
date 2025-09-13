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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.RESET_PASSWORD;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_REGISTRATION;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.util.Maps;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    private static final String USER_SOURCE = "usersource";
    private static final String USER_NAME = "tuser";
    private static final String EMAIL = "user@gravitee.io";
    private static final String FIRST_NAME = "The";
    private static final String LAST_NAME = "User";
    private static final String PASSWORD = "gh2gyf8!zjfnz";
    private static final String JWT_SECRET = "VERYSECURE";

    private static final String ORGANIZATION = "organization#Id";
    private static final String ENVIRONMENT = "environment#Id";
    private static final Set<UserRoleEntity> ROLES = Collections.singleton(new UserRoleEntity());

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION, ENVIRONMENT);

    static {
        UserRoleEntity r = ROLES.iterator().next();
        r.setScope(RoleScope.ENVIRONMENT);
        r.setName("USER");
    }

    @InjectMocks
    private UserServiceImpl userService = new UserServiceImpl();

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private NewExternalUserEntity newUser;

    @Mock
    private UpdateUserEntity updateUser;

    @Mock
    private User user;

    @Mock
    private Date date;

    @Mock
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private EmailService emailService;

    @Mock
    private ParameterService mockParameterService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private InvitationService invitationService;

    @Mock
    private PortalNotificationService portalNotificationService;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private GroupService groupService;

    @Mock
    private SocialIdentityProviderEntity identityProvider;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserMetadataService userMetadataService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private UserConverter userConverter;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @Before
    public void setup() {
        when(userConverter.toUser(any(NewExternalUserEntity.class))).thenCallRealMethod();
        when(userConverter.toUserEntity(any(User.class), any())).thenCallRealMethod();
    }

    @Test
    public void shouldFindByUsername() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION)).thenReturn(of(user));

        final UserEntity userEntity = userService.findBySource(ORGANIZATION, USER_SOURCE, USER_NAME, false);

        assertEquals(USER_NAME, userEntity.getId());
        assertEquals(FIRST_NAME, userEntity.getFirstname());
        assertEquals(LAST_NAME, userEntity.getLastname());
        assertEquals(EMAIL, userEntity.getEmail());
        assertEquals(PASSWORD, userEntity.getPassword());
        assertNull(userEntity.getRoles());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldFindByUsernameThrowsError() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION)).thenThrow(new TechnicalException("user not found"));

        final UserEntity userEntity = userService.findBySource(ORGANIZATION, USER_SOURCE, USER_NAME, false);
    }

    @Test
    public void shouldFindByEmail() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(userRepository.findByEmail(EMAIL, ORGANIZATION)).thenReturn(List.of(user));

        final List<UserEntity> optUserEntity = userService.findByEmail(EXECUTION_CONTEXT, EMAIL);

        UserEntity userEntity = optUserEntity.getFirst();
        assertNotNull(userEntity);

        assertEquals(USER_NAME, userEntity.getId());
        assertEquals(FIRST_NAME, userEntity.getFirstname());
        assertEquals(LAST_NAME, userEntity.getLastname());
        assertEquals(EMAIL, userEntity.getEmail());
        assertNull(userEntity.getPassword());
        assertNull(userEntity.getRoles());
    }

    @Test
    public void shouldFindByIdWithRoles() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));
        RoleEntity apiPoRole = new RoleEntity();
        apiPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API)).thenReturn(apiPoRole);

        RoleEntity appPoRole = new RoleEntity();
        appPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION)).thenReturn(appPoRole);

        final UserEntity userEntity = userService.findByIdWithRoles(EXECUTION_CONTEXT, USER_NAME);

        assertEquals(USER_NAME, userEntity.getId());
        assertEquals(FIRST_NAME, userEntity.getFirstname());
        assertEquals(LAST_NAME, userEntity.getLastname());
        assertEquals(EMAIL, userEntity.getEmail());
        assertNull(userEntity.getPassword());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        final UserEntity userEntity = userService.findById(EXECUTION_CONTEXT, USER_NAME);

        assertEquals(USER_NAME, userEntity.getId());
        assertEquals(FIRST_NAME, userEntity.getFirstname());
        assertEquals(LAST_NAME, userEntity.getLastname());
        assertEquals(EMAIL, userEntity.getEmail());
        assertNull(userEntity.getPassword());
        assertNull(userEntity.getRoles());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotFindByIdBecauseNotInCurrentOrganization() throws TechnicalException {
        when(user.getOrganizationId()).thenReturn("ANOTHER_ORGANIZATION");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.findById(EXECUTION_CONTEXT, USER_NAME);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotFindByUsernameBecauseNotExists() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION)).thenReturn(Optional.empty());

        userService.findBySource(ORGANIZATION, USER_SOURCE, USER_NAME, false);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUsernameBecauseTechnicalException() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION)).thenThrow(TechnicalException.class);

        userService.findBySource(ORGANIZATION, USER_SOURCE, USER_NAME, false);
    }

    @Test
    public void shouldComputeRolesToAddToUserFromRoleMapping() throws IOException {
        List<RoleMappingEntity> roleMappingEntities = getRoleMappingEntities();
        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token_body.json"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token_body.json"), Charset.defaultCharset());

        RoleEntity orgAdminRole = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "ADMIN", ORGANIZATION)).thenReturn(Optional.of(orgAdminRole));

        RoleEntity orgUserRole = mockRoleEntity(RoleScope.ORGANIZATION, "USER");
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "USER", ORGANIZATION)).thenReturn(Optional.of(orgUserRole));

        RoleEntity envUserRole = mockRoleEntity(RoleScope.ENVIRONMENT, "USER");
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, "USER", ORGANIZATION)).thenReturn(Optional.of(envUserRole));

        Set<RoleEntity> roleEntitiesForOrganization = userService.computeOrganizationRoles(
            EXECUTION_CONTEXT,
            roleMappingEntities,
            USER_NAME,
            userInfo,
            accessToken,
            idToken
        );

        Map<String, Set<RoleEntity>> roleEntitiesForEnvironments = userService.computeEnvironmentRoles(
            EXECUTION_CONTEXT,
            roleMappingEntities,
            USER_NAME,
            userInfo,
            accessToken,
            idToken
        );

        assertEquals(2, roleEntitiesForOrganization.size());
        assertTrue(roleEntitiesForOrganization.contains(orgAdminRole));
        assertTrue(roleEntitiesForOrganization.contains(orgUserRole));

        assertEquals(1, roleEntitiesForEnvironments.size());
        assertEquals(1, roleEntitiesForEnvironments.get(ENVIRONMENT).size());
        assertTrue(roleEntitiesForEnvironments.get(ENVIRONMENT).contains(envUserRole));
    }

    @Test
    public void setDefaultRolesIfRoleMappingIsNotMatching() throws IOException {
        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token_body.json"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token_body.json"), Charset.defaultCharset());

        RoleMappingEntity role1 = new RoleMappingEntity();
        role1.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_4'}");
        role1.setOrganizations(Collections.singletonList("ADMIN"));

        RoleMappingEntity role2 = new RoleMappingEntity();
        role2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        role2.setEnvironments(Collections.singletonMap(ENVIRONMENT, Collections.singletonList("USER")));

        RoleMappingEntity role3 = new RoleMappingEntity();
        role3.setCondition("{#jsonPath(#profile, '$.job_id') == 'API_BREAKER'}");
        role3.setOrganizations(Collections.singletonList("USER"));
        role3.setEnvironments(Collections.singletonMap(ENVIRONMENT, Collections.singletonList("USER")));
        final List<RoleMappingEntity> rolesMapping = Arrays.asList(role1, role2, role3);

        RoleEntity orgAdminRole = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        when(roleService.findDefaultRoleByScopes(EXECUTION_CONTEXT.getOrganizationId(), RoleScope.ORGANIZATION))
            .thenReturn(List.of(orgAdminRole));

        RoleEntity envUserRole = mockRoleEntity(RoleScope.ENVIRONMENT, "USER");
        when(roleService.findDefaultRoleByScopes(EXECUTION_CONTEXT.getOrganizationId(), RoleScope.ENVIRONMENT))
            .thenReturn(List.of(envUserRole));

        Set<RoleEntity> roleEntitiesForOrganization = userService.computeOrganizationRoles(
            EXECUTION_CONTEXT,
            rolesMapping,
            USER_NAME,
            userInfo,
            accessToken,
            idToken
        );

        Map<String, Set<RoleEntity>> roleEntitiesForEnvironments = userService.computeEnvironmentRoles(
            EXECUTION_CONTEXT,
            rolesMapping,
            USER_NAME,
            userInfo,
            accessToken,
            idToken
        );

        assertEquals(1, roleEntitiesForOrganization.size());
        assertTrue(roleEntitiesForOrganization.contains(orgAdminRole));

        assertEquals(1, roleEntitiesForEnvironments.size());
        assertEquals(1, roleEntitiesForEnvironments.get(ENVIRONMENT).size());
        assertTrue(roleEntitiesForEnvironments.get(ENVIRONMENT).contains(envUserRole));
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        innerShouldCreate(null);
    }

    @Test
    public void shouldCreateWithCustomFields() throws TechnicalException {
        Map<String, Object> customFields = Maps.<String, Object>builder().put("md1", "value1").put("md2", "value2").build();
        innerShouldCreate(customFields);
    }

    @Test(expected = EmailFormatInvalidException.class)
    public void shouldNotCreateNormalUserBadEmail() throws TechnicalException {
        final NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setService(false);
        newPreRegisterUserEntity.setEmail("bad./.email");

        userService.create(EXECUTION_CONTEXT, newPreRegisterUserEntity);
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void shouldNotCreateNormalUserBecauseAlreadyExists() throws TechnicalException {
        final NewPreRegisterUserEntity newPreRegisterUserEntity = mock(NewPreRegisterUserEntity.class);
        when(newPreRegisterUserEntity.isService()).thenReturn(false);
        when(newPreRegisterUserEntity.getEmail()).thenReturn(EMAIL);
        when(newPreRegisterUserEntity.getSource()).thenReturn("gravitee");
        doCallRealMethod().when(newPreRegisterUserEntity).setSourceId(any());
        when(newPreRegisterUserEntity.getSourceId()).thenCallRealMethod();

        when(userRepository.findBySource("gravitee", EMAIL, ORGANIZATION)).thenReturn(Optional.of(new User()));

        userService.create(EXECUTION_CONTEXT, newPreRegisterUserEntity);

        verify(newPreRegisterUserEntity, times(1)).setSourceId(EMAIL);
    }

    @Test
    public void shouldCreateNormalUser() throws TechnicalException {
        final NewPreRegisterUserEntity newPreRegisterUserEntity = mock(NewPreRegisterUserEntity.class);
        when(newPreRegisterUserEntity.isService()).thenReturn(false);
        when(newPreRegisterUserEntity.getEmail()).thenReturn(EMAIL);
        when(newPreRegisterUserEntity.getSource()).thenReturn("gravitee");
        doCallRealMethod().when(newPreRegisterUserEntity).setSourceId(any());
        when(newPreRegisterUserEntity.getSourceId()).thenCallRealMethod();

        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER))
            .thenReturn(1000);
        when(userRepository.findBySource("gravitee", EMAIL, ORGANIZATION)).thenReturn(empty());

        // Mock create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole, boolean autoRegistrationEnabled)
        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());
        mockDefaultEnvironment();

        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getCreatedAt()).thenReturn(date);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.create(any(User.class))).thenReturn(user);
        RoleEntity roleEnvironmentAdmin = mockRoleEntity(RoleScope.ENVIRONMENT, "ADMIN");
        RoleEntity roleOrganizationAdmin = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        when(roleService.findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT))
            .thenReturn(Arrays.asList(roleOrganizationAdmin, roleEnvironmentAdmin));
        RoleEntity roleEnv = mock(RoleEntity.class);
        when(roleEnv.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleEnv.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, ENVIRONMENT, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(List.of(roleEnv)));
        RoleEntity roleOrg = mock(RoleEntity.class);
        when(roleOrg.getScope()).thenReturn(RoleScope.ORGANIZATION);
        when(roleOrg.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, ORGANIZATION, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(List.of(roleOrg)));

        // Come back to our method

        userService.create(EXECUTION_CONTEXT, newPreRegisterUserEntity);

        verify(newPreRegisterUserEntity, times(1)).setSourceId(EMAIL);
        verify(emailService, times(1)).sendAsyncEmailNotification(eq(EXECUTION_CONTEXT), any());
        verify(notifierService, times(1)).trigger(eq(EXECUTION_CONTEXT), eq(PortalHook.USER_CREATED), any());
    }

    @Test(expected = EmailFormatInvalidException.class)
    public void shouldNotCreateServiceUserBadEmail() throws TechnicalException {
        final NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setService(true);
        newPreRegisterUserEntity.setEmail("bad./.email");

        userService.create(EXECUTION_CONTEXT, newPreRegisterUserEntity);
    }

    @Test
    public void shouldCreateServiceUserWithEmail() throws TechnicalException {
        final NewPreRegisterUserEntity newPreRegisterUserEntity = mock(NewPreRegisterUserEntity.class);
        when(newPreRegisterUserEntity.isService()).thenReturn(true);
        when(newPreRegisterUserEntity.getEmail()).thenReturn(EMAIL);
        when(newPreRegisterUserEntity.getSource()).thenReturn("gravitee");
        when(newPreRegisterUserEntity.getLastname()).thenReturn(LAST_NAME);
        doCallRealMethod().when(newPreRegisterUserEntity).setSourceId(any());
        when(newPreRegisterUserEntity.getSourceId()).thenCallRealMethod();

        // Mock create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole, boolean autoRegistrationEnabled)
        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());
        mockDefaultEnvironment();

        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getCreatedAt()).thenReturn(date);
        when(user.getUpdatedAt()).thenReturn(date);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.create(any(User.class))).thenReturn(user);
        RoleEntity roleEnvironmentAdmin = mockRoleEntity(RoleScope.ENVIRONMENT, "ADMIN");
        RoleEntity roleOrganizationAdmin = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        when(roleService.findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT))
            .thenReturn(Arrays.asList(roleOrganizationAdmin, roleEnvironmentAdmin));
        RoleEntity roleEnv = mock(RoleEntity.class);
        when(roleEnv.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleEnv.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, ENVIRONMENT, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(List.of(roleEnv)));
        RoleEntity roleOrg = mock(RoleEntity.class);
        when(roleOrg.getScope()).thenReturn(RoleScope.ORGANIZATION);
        when(roleOrg.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, ORGANIZATION, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(List.of(roleOrg)));

        // Come back to our method

        userService.create(EXECUTION_CONTEXT, newPreRegisterUserEntity);

        verify(newPreRegisterUserEntity, times(1)).setSourceId(LAST_NAME);
        verify(emailService, never()).sendAsyncEmailNotification(eq(EXECUTION_CONTEXT), any());
        verify(notifierService, never()).trigger(eq(EXECUTION_CONTEXT), any(), any());
    }

    @Test
    public void shouldCreateServiceUserWithoutEmail() throws TechnicalException {
        final NewPreRegisterUserEntity newPreRegisterUserEntity = mock(NewPreRegisterUserEntity.class);
        when(newPreRegisterUserEntity.isService()).thenReturn(true);
        when(newPreRegisterUserEntity.getSource()).thenReturn("gravitee");
        when(newPreRegisterUserEntity.getLastname()).thenReturn(LAST_NAME);
        doCallRealMethod().when(newPreRegisterUserEntity).setSourceId(any());
        when(newPreRegisterUserEntity.getSourceId()).thenCallRealMethod();

        // Mock create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole, boolean autoRegistrationEnabled)
        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());
        mockDefaultEnvironment();

        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(null);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getCreatedAt()).thenReturn(date);
        when(user.getUpdatedAt()).thenReturn(date);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.create(any(User.class))).thenReturn(user);
        RoleEntity roleEnvironmentAdmin = mockRoleEntity(RoleScope.ENVIRONMENT, "ADMIN");
        RoleEntity roleOrganizationAdmin = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        when(roleService.findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT))
            .thenReturn(Arrays.asList(roleOrganizationAdmin, roleEnvironmentAdmin));
        RoleEntity roleEnv = mock(RoleEntity.class);
        when(roleEnv.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleEnv.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, ENVIRONMENT, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(List.of(roleEnv)));
        RoleEntity roleOrg = mock(RoleEntity.class);
        when(roleOrg.getScope()).thenReturn(RoleScope.ORGANIZATION);
        when(roleOrg.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, ORGANIZATION, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(List.of(roleOrg)));

        // Come back to our method

        userService.create(EXECUTION_CONTEXT, newPreRegisterUserEntity);

        verify(newPreRegisterUserEntity, times(1)).setSourceId(LAST_NAME);
        verify(emailService, never()).sendAsyncEmailNotification(eq(EXECUTION_CONTEXT), any());
        verify(notifierService, never()).trigger(eq(EXECUTION_CONTEXT), any(), any());
    }

    protected void innerShouldCreate(Map<String, Object> customFields) throws TechnicalException {
        if (customFields != null) {
            when(newUser.getCustomFields()).thenReturn(customFields);
        }

        when(newUser.getEmail()).thenReturn(EMAIL);
        when(newUser.getFirstname()).thenReturn(FIRST_NAME);
        when(newUser.getLastname()).thenReturn(LAST_NAME);
        when(newUser.getSource()).thenReturn(USER_SOURCE);
        when(newUser.getSourceId()).thenReturn(USER_NAME);

        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ORGANIZATION)).thenReturn(Optional.empty());

        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getCreatedAt()).thenReturn(date);
        when(user.getUpdatedAt()).thenReturn(date);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.create(any(User.class))).thenReturn(user);
        RoleEntity roleEnv = mock(RoleEntity.class);
        when(roleEnv.getScope()).thenReturn(RoleScope.ENVIRONMENT);
        when(roleEnv.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ENVIRONMENT, ENVIRONMENT, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(Arrays.asList(roleEnv)));
        RoleEntity roleOrg = mock(RoleEntity.class);
        when(roleOrg.getScope()).thenReturn(RoleScope.ORGANIZATION);
        when(roleOrg.getName()).thenReturn("USER");
        when(membershipService.getRoles(MembershipReferenceType.ORGANIZATION, ORGANIZATION, MembershipMemberType.USER, user.getId()))
            .thenReturn(new HashSet<>(Arrays.asList(roleOrg)));

        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());
        mockDefaultEnvironment();

        if (customFields != null) {
            when(userMetadataService.create(eq(EXECUTION_CONTEXT), any())).thenAnswer(x -> convertNewUserMetadataEntity(x.getArgument(1)));
        }

        final UserEntity createdUserEntity = userService.create(EXECUTION_CONTEXT, newUser, false);

        verify(userRepository)
            .create(
                argThat(userToCreate ->
                    USER_NAME.equals(userToCreate.getSourceId()) &&
                    USER_SOURCE.equals(userToCreate.getSource()) &&
                    EMAIL.equals(userToCreate.getEmail()) &&
                    FIRST_NAME.equals(userToCreate.getFirstname()) &&
                    LAST_NAME.equals(userToCreate.getLastname()) &&
                    userToCreate.getCreatedAt() != null &&
                    userToCreate.getUpdatedAt() != null &&
                    userToCreate.getCreatedAt().equals(userToCreate.getUpdatedAt())
                )
            );

        assertEquals(USER_NAME, createdUserEntity.getId());
        assertEquals(FIRST_NAME, createdUserEntity.getFirstname());
        assertEquals(LAST_NAME, createdUserEntity.getLastname());
        assertEquals(EMAIL, createdUserEntity.getEmail());
        assertNull(createdUserEntity.getPassword());
        assertEquals(ROLES, createdUserEntity.getRoles());
        assertEquals(date, createdUserEntity.getCreatedAt());
        assertEquals(date, createdUserEntity.getUpdatedAt());

        if (customFields != null) {
            verify(userMetadataService, times(2)).create(eq(EXECUTION_CONTEXT), any());
            assertFalse(createdUserEntity.getCustomFields().isEmpty());

            assertEquals(customFields.size(), createdUserEntity.getCustomFields().size());
            assertTrue(createdUserEntity.getCustomFields().keySet().containsAll(customFields.keySet()));
            assertTrue(createdUserEntity.getCustomFields().values().containsAll(customFields.values()));
        } else {
            assertTrue(createdUserEntity.getCustomFields() == null || createdUserEntity.getCustomFields().isEmpty());
        }
    }

    private UserMetadataEntity convertNewUserMetadataEntity(NewUserMetadataEntity entity) {
        UserMetadataEntity metadata = new UserMetadataEntity();
        metadata.setFormat(entity.getFormat());
        metadata.setKey(entity.getName());
        metadata.setName(entity.getName());
        metadata.setValue(entity.getValue());
        metadata.setUserId(entity.getUserId());
        return metadata;
    }

    @Test
    public void shouldUpdateUser() throws TechnicalException {
        final String USER_ID = "myuserid";
        final String USER_EMAIL = "my.user@acme.fr";
        final String GIO_SOURCE = "gravitee";

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setSource(GIO_SOURCE);
        user.setSourceId(USER_EMAIL);
        user.setOrganizationId(ORGANIZATION);

        when(userRepository.update(any(User.class)))
            .thenAnswer(
                new Answer<User>() {
                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        return (User) args[0];
                    }
                }
            );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findBySource(GIO_SOURCE, USER_EMAIL, ORGANIZATION)).thenReturn(Optional.empty());

        when(updateUser.getEmail()).thenReturn(USER_EMAIL);
        String UPDATED_LAST_NAME = LAST_NAME + "updated";
        String UPDATED_FIRST_NAME = FIRST_NAME + "updated";
        when(updateUser.getFirstname()).thenReturn(UPDATED_FIRST_NAME);
        when(updateUser.getLastname()).thenReturn(UPDATED_LAST_NAME);
        userService.update(EXECUTION_CONTEXT, user.getId(), updateUser);

        verify(userRepository)
            .update(
                argThat(userToUpdate ->
                    USER_ID.equals(userToUpdate.getId()) &&
                    GIO_SOURCE.equals(userToUpdate.getSource()) &&
                    USER_EMAIL.equals(userToUpdate.getEmail()) &&
                    USER_EMAIL.equals(userToUpdate.getSourceId()) && // update of sourceId authorized for gravitee source
                    UPDATED_FIRST_NAME.equals(userToUpdate.getFirstname()) &&
                    UPDATED_LAST_NAME.equals(userToUpdate.getLastname())
                )
            );
    }

    @Test
    public void shouldUpdateUser_butNotEmail() throws TechnicalException {
        final String USER_ID = "myuserid";
        final String USER_EMAIL = "my.user@acme.fr";
        final String SOURCE = "gravitee-no-email-update";

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setSource(SOURCE);
        user.setSourceId(USER_ID);
        user.setOrganizationId(ORGANIZATION);

        when(userRepository.update(any(User.class)))
            .thenAnswer(
                new Answer<User>() {
                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        return (User) args[0];
                    }
                }
            );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(updateUser.getEmail()).thenReturn(USER_EMAIL);
        String UPDATED_LAST_NAME = LAST_NAME + "updated";
        String UPDATED_FIRST_NAME = FIRST_NAME + "updated";
        when(updateUser.getFirstname()).thenReturn(UPDATED_FIRST_NAME);
        when(updateUser.getLastname()).thenReturn(UPDATED_LAST_NAME);
        userService.update(EXECUTION_CONTEXT, user.getId(), updateUser);

        verify(userRepository)
            .update(
                argThat(userToUpdate ->
                    USER_ID.equals(userToUpdate.getId()) &&
                    SOURCE.equals(userToUpdate.getSource()) &&
                    USER_EMAIL.equals(userToUpdate.getEmail()) &&
                    USER_ID.equals(userToUpdate.getSourceId()) && //sourceId shouldn't be updated in this case
                    UPDATED_FIRST_NAME.equals(userToUpdate.getFirstname()) &&
                    UPDATED_LAST_NAME.equals(userToUpdate.getLastname())
                )
            );
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void shouldNotUpdateUser_EmailAlreadyInUse() throws TechnicalException {
        final String USER_ID = "myuserid";
        final String USER_EMAIL = "my.user@acme.fr";
        final String GIO_SOURCE = "gravitee";

        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setSource(GIO_SOURCE);
        user.setSourceId(USER_EMAIL);
        user.setOrganizationId(ORGANIZATION);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findBySource(GIO_SOURCE, USER_EMAIL, ORGANIZATION)).thenReturn(Optional.of(new User()));

        when(updateUser.getEmail()).thenReturn(USER_EMAIL);
        String UPDATED_LAST_NAME = LAST_NAME + "updated";
        String UPDATED_FIRST_NAME = FIRST_NAME + "updated";
        when(updateUser.getFirstname()).thenReturn(UPDATED_FIRST_NAME);
        when(updateUser.getLastname()).thenReturn(UPDATED_LAST_NAME);
        userService.update(EXECUTION_CONTEXT, user.getId(), updateUser);

        verify(userRepository, never()).update(any());
    }

    @Test(expected = OrganizationNotFoundException.class)
    public void shouldNotCreateBecauseOrganizationDoesNotExist() throws TechnicalException {
        when(organizationService.findById(ORGANIZATION)).thenThrow(OrganizationNotFoundException.class);

        userService.create(EXECUTION_CONTEXT, newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void shouldNotCreateBecauseExists() throws TechnicalException {
        when(userRepository.findBySource(nullable(String.class), nullable(String.class), nullable(String.class)))
            .thenReturn(of(new User()));

        userService.create(EXECUTION_CONTEXT, newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        when(userRepository.create(any(User.class))).thenThrow(TechnicalException.class);

        userService.create(EXECUTION_CONTEXT, newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotConnectBecauseNotExists() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.empty());

        userService.connect(EXECUTION_CONTEXT, USER_NAME);

        verify(userRepository, never()).create(any());
    }

    @Test
    public void shouldCreateDefaultApplicationWhenNotExistingInEnvironment() throws TechnicalException {
        setField(userService, "defaultApplicationForFirstConnection", true);
        when(user.getLastConnectionAt()).thenReturn(null);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));
        EnvironmentEntity environment1 = new EnvironmentEntity();
        environment1.setId("envId1");
        EnvironmentEntity environment2 = new EnvironmentEntity();
        environment2.setId("envId2");
        EnvironmentEntity environment3 = new EnvironmentEntity();
        environment3.setId("envId3");
        when(environmentService.findByUser(eq(ORGANIZATION), any())).thenReturn(Arrays.asList(environment1, environment2, environment3));

        ApplicationListItem defaultApp = new ApplicationListItem();
        defaultApp.setName("Default application");
        defaultApp.setDescription("My default application");
        defaultApp.setType(ApplicationType.SIMPLE.name());

        userService.connect(EXECUTION_CONTEXT, USER_NAME);

        verify(applicationService, times(1)).create(argThat(context -> context.getEnvironmentId().equals("envId2")), any(), eq(USER_NAME));
        verify(applicationService, times(1)).create(argThat(context -> context.getEnvironmentId().equals("envId3")), any(), eq(USER_NAME));
    }

    @Test
    public void shouldNotCreateDefaultApplicationBecauseDisabled() throws TechnicalException {
        setField(userService, "defaultApplicationForFirstConnection", false);
        when(user.getLastConnectionAt()).thenReturn(null);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(EXECUTION_CONTEXT, USER_NAME);

        verify(applicationService, never()).create(eq(EXECUTION_CONTEXT), any(), eq(USER_NAME));
    }

    @Test
    public void shouldNotCreateDefaultApplicationBecauseAlreadyConnected() throws TechnicalException {
        when(user.getLastConnectionAt()).thenReturn(new Date());
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(EXECUTION_CONTEXT, USER_NAME);

        verify(applicationService, never()).create(eq(EXECUTION_CONTEXT), any(), eq(USER_NAME));
    }

    @Test(expected = UserRegistrationUnavailableException.class)
    public void shouldNotCreateUserIfRegistrationIsDisabled() {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));

        userService.finalizeRegistration(EXECUTION_CONTEXT, userEntity);
    }

    @Test(expected = UserNotFoundException.class)
    public void createNewRegistrationUserThatIsNotCreatedYet() throws TechnicalException {
        when(
            mockParameterService.findAsBoolean(
                EXECUTION_CONTEXT,
                Key.PORTAL_USERCREATION_ENABLED,
                ENVIRONMENT,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(passwordValidator.validate(anyString())).thenReturn(true);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(EXECUTION_CONTEXT, userEntity);
    }

    @Test(expected = UserStateConflictException.class)
    public void createNewRegistrationUserWithResetPasswordAction() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100, RESET_PASSWORD.name()));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(EXECUTION_CONTEXT, userEntity);
    }

    @Test
    public void changePassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(passwordValidator.validate(anyString())).thenReturn(true);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));
        when(userRepository.update(any())).thenAnswer(returnsFirstArg());

        ResetPasswordUserEntity userEntity = new ResetPasswordUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100, RESET_PASSWORD.name()));
        userEntity.setPassword(PASSWORD);

        userService.finalizeResetPassword(EXECUTION_CONTEXT, userEntity);

        verify(auditService)
            .createOrganizationAuditLog(
                eq(EXECUTION_CONTEXT),
                eq(ORGANIZATION),
                anyMap(),
                argThat(evt -> evt.equals(User.AuditEvent.PASSWORD_CHANGED)),
                any(),
                any(),
                any()
            );
    }

    @Test(expected = PasswordFormatInvalidException.class)
    public void changePassword_invalidPwd() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));

        ResetPasswordUserEntity userEntity = new ResetPasswordUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100, RESET_PASSWORD.name()));
        userEntity.setPassword(PASSWORD);

        userService.finalizeResetPassword(EXECUTION_CONTEXT, userEntity);
    }

    @Test(expected = UserStateConflictException.class)
    public void changePassword_withInvalidAction() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        ResetPasswordUserEntity userEntity = new ResetPasswordUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeResetPassword(EXECUTION_CONTEXT, userEntity);
    }

    @Test(expected = PasswordFormatInvalidException.class)
    public void createAlreadyPreRegisteredUser_invalidPassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(
            mockParameterService.findAsBoolean(
                EXECUTION_CONTEXT,
                Key.PORTAL_USERCREATION_ENABLED,
                ENVIRONMENT,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(EXECUTION_CONTEXT, userEntity);
    }

    @Test
    public void createAlreadyPreRegisteredUser() throws TechnicalException {
        when(
            mockParameterService.findAsBoolean(
                EXECUTION_CONTEXT,
                Key.PORTAL_USERCREATION_ENABLED,
                ENVIRONMENT,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(passwordValidator.validate(anyString())).thenReturn(true);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));
        when(userRepository.update(any(User.class))).thenReturn(user);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(EXECUTION_CONTEXT, userEntity);

        verify(userRepository)
            .update(
                argThat(userToCreate ->
                    "CUSTOM_LONG_ID".equals(userToCreate.getId()) &&
                    EMAIL.equals(userToCreate.getEmail()) &&
                    FIRST_NAME.equals(userToCreate.getFirstname()) &&
                    LAST_NAME.equals(userToCreate.getLastname())
                )
            );
    }

    @Test(expected = UserRegistrationUnavailableException.class)
    public void shouldValidateJWTokenAndFail() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis() / 1000 - 100));
        userEntity.setPassword(PASSWORD);

        verify(userRepository, never()).findBySource(USER_SOURCE, USER_NAME, ORGANIZATION);

        userService.finalizeRegistration(EXECUTION_CONTEXT, userEntity);
    }

    @Test
    public void shouldResetPassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER))
            .thenReturn(1000);
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(user.getSourceId()).thenReturn(EMAIL);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        when(auditService.search(eq(EXECUTION_CONTEXT), argThat(arg -> arg.getEvents().contains(User.AuditEvent.PASSWORD_RESET.name()))))
            .thenReturn(mock(MetadataPage.class));

        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(final Authentication authentication) {}
            }
        );

        userService.resetPassword(EXECUTION_CONTEXT, USER_NAME);

        verify(user, never()).setPassword(null);
        verify(userRepository, never()).update(user);
        verify(emailService).sendAsyncEmailNotification(eq(EXECUTION_CONTEXT), any());
    }

    @Test
    public void shouldResetPassword_auditEventNotMatch() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER))
            .thenReturn(1000);
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(user.getSourceId()).thenReturn(EMAIL);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        MetadataPage mdPage = mock(MetadataPage.class);
        AuditEntity entity1 = new AuditEntity();
        entity1.setProperties(Collections.singletonMap("USER", "unknown"));
        when(mdPage.getContent()).thenReturn(Arrays.asList(entity1));
        when(auditService.search(eq(EXECUTION_CONTEXT), argThat(arg -> arg.getEvents().contains(User.AuditEvent.PASSWORD_RESET.name()))))
            .thenReturn(mdPage);

        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(final Authentication authentication) {}
            }
        );

        userService.resetPassword(EXECUTION_CONTEXT, USER_NAME);

        verify(user, never()).setPassword(null);
        verify(userRepository, never()).update(user);
        verify(emailService).sendAsyncEmailNotification(eq(EXECUTION_CONTEXT), any());
    }

    @Test(expected = PasswordAlreadyResetException.class)
    public void shouldNotResetPassword_AlreadyReset() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(user.getSourceId()).thenReturn(EMAIL);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        MetadataPage mdPage = mock(MetadataPage.class);
        AuditEntity entity1 = new AuditEntity();
        entity1.setProperties(Collections.singletonMap("USER", USER_NAME));
        when(mdPage.getContent()).thenReturn(Arrays.asList(entity1));
        when(auditService.search(eq(EXECUTION_CONTEXT), argThat(arg -> arg.getEvents().contains(User.AuditEvent.PASSWORD_RESET.name()))))
            .thenReturn(mdPage);

        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(final Authentication authentication) {}
            }
        );

        userService.resetPassword(EXECUTION_CONTEXT, USER_NAME);

        verify(user, never()).setPassword(null);
        verify(userRepository, never()).update(user);
        verify(emailService, never()).sendAsyncEmailNotification(eq(EXECUTION_CONTEXT), any());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotResetPasswordCauseUserNotFound() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(empty());
        userService.resetPassword(EXECUTION_CONTEXT, USER_NAME);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldFailWhileResettingPassword() throws TechnicalException {
        when(userRepository.findBySource(any(), any(), any())).thenReturn(Optional.empty());
        userService.resetPasswordFromSourceId(EXECUTION_CONTEXT, "my@email.com", "HTTP://MY-RESET-PAGE");
    }

    @Test(expected = UserNotActiveException.class)
    public void shouldFailWhileResettingPasswordWhenUserFoundIsNotActive() throws TechnicalException {
        User user = new User();
        user.setId(USER_NAME);
        user.setSource("gravitee");
        user.setStatus(UserStatus.ARCHIVED);
        when(userRepository.findBySource(any(), any(), any())).thenReturn(Optional.of(user));

        userService.resetPasswordFromSourceId(EXECUTION_CONTEXT, "my@email.com", "HTTP://MY-RESET-PAGE");
    }

    @Test(expected = UserNotInternallyManagedException.class)
    public void shouldFailWhileResettingPasswordWhenUserFoundIsNotInternallyManaged() throws TechnicalException {
        User user = new User();
        user.setId(USER_NAME);
        user.setSource("not gravitee");
        user.setStatus(UserStatus.ACTIVE);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findBySource(any(), any(), any())).thenReturn(Optional.of(user));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        userService.resetPasswordFromSourceId(EXECUTION_CONTEXT, "my@email.com", "HTTP://MY-RESET-PAGE");
    }

    @Test(expected = UserNotInternallyManagedException.class)
    public void shouldNotResetPasswordCauseUserNotInternallyManaged() throws TechnicalException {
        when(user.getSource()).thenReturn("external");
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.resetPassword(EXECUTION_CONTEXT, USER_NAME);
    }

    @Test(expected = ServiceAccountNotManageableException.class)
    public void shouldNotResetPasswordCauseServiceAccountNotManageable() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(user.getSource()).thenReturn("gravitee");
        when(user.getSourceId()).thenReturn("lastname");
        when(user.getLastname()).thenReturn("lastname");
        when(user.getPassword()).thenReturn(null);
        when(user.getOrganizationId()).thenReturn(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.resetPassword(EXECUTION_CONTEXT, USER_NAME);
    }

    private String createJWT(long expirationSeconds, String action) {
        Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);

        Date issueAt = new Date();
        Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(expirationSeconds));

        return JWT
            .create()
            .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
            .withIssuedAt(issueAt)
            .withExpiresAt(Date.from(expireAt))
            .withSubject(USER_NAME)
            .withClaim(JWTHelper.Claims.EMAIL, EMAIL)
            .withClaim(JWTHelper.Claims.FIRSTNAME, FIRST_NAME)
            .withClaim(JWTHelper.Claims.LASTNAME, LAST_NAME)
            .withClaim(JWTHelper.Claims.ACTION, action)
            .sign(algorithm);
        /*
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JWTHelper.Claims.SUBJECT, USER_NAME);
        claims.put(JWTHelper.Claims.EMAIL, EMAIL);
        claims.put(JWTHelper.Claims.FIRSTNAME, FIRST_NAME);
        claims.put(JWTHelper.Claims.LASTNAME, LAST_NAME);
        claims.put(JWTHelper.Claims.ACTION, USER_REGISTRATION);
        claims.put("exp", expirationSeconds);
        return new JWTSigner(JWT_SECRET).sign(claims);
         */
    }

    private String createJWT(long expirationSeconds) {
        return createJWT(expirationSeconds, USER_REGISTRATION.name());
    }

    @Test
    public void shouldUpdateUser_UpdateFields_And_CreateFields() throws Exception {
        final String USER_ID = "userid";
        User user = new User();
        user.setId(USER_ID);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname("john");
        user.setLastname("doe");
        user.setEmail("john.doe@mail.domain");

        when(userRepository.findById(USER_ID)).thenReturn(of(user));

        UpdateUserEntity toUpdate = new UpdateUserEntity();
        toUpdate.setEmail(user.getEmail());
        toUpdate.setFirstname(user.getFirstname());
        toUpdate.setLastname(user.getLastname());
        toUpdate.setCustomFields(
            Maps.<String, Object>builder().put("fieldToUpdate", "valueUpdated").put("fieldToCreate", "newValue").build()
        );

        UserMetadataEntity existingField = new UserMetadataEntity();
        existingField.setValue("value1");
        existingField.setUserId(USER_ID);
        existingField.setFormat(MetadataFormat.STRING);
        existingField.setName("fieldToUpdate");
        existingField.setKey("fieldToUpdate");

        when(userMetadataService.findAllByUserId(USER_ID)).thenReturn(Arrays.asList(existingField));

        userService.update(EXECUTION_CONTEXT, USER_ID, toUpdate);

        verify(userMetadataService)
            .update(
                eq(EXECUTION_CONTEXT),
                argThat(entity ->
                    entity.getKey().equals(existingField.getKey()) &&
                    entity.getName().equals(existingField.getName()) &&
                    entity.getUserId().equals(existingField.getUserId()) &&
                    entity.getValue().equals(toUpdate.getCustomFields().get(existingField.getKey()))
                )
            );

        verify(userMetadataService)
            .create(
                eq(EXECUTION_CONTEXT),
                argThat(entity ->
                    entity.getName().equals("fieldToCreate") &&
                    entity.getUserId().equals(existingField.getUserId()) &&
                    entity.getValue().equals(toUpdate.getCustomFields().get("fieldToCreate"))
                )
            );
    }

    @Test
    public void shouldNotDeleteIfAPIPO() throws TechnicalException {
        User user = new User();
        user.setId(USER_NAME);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));

        RoleEntity apiPoRole = new RoleEntity();
        apiPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API)).thenReturn(apiPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                USER_NAME,
                MembershipReferenceType.API,
                apiPoRole.getId()
            )
        )
            .thenReturn(Set.of(new MembershipEntity()));

        RoleEntity appPoRole = new RoleEntity();
        appPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION)).thenReturn(appPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                USER_NAME,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            )
        )
            .thenReturn(Set.of());

        try {
            userService.delete(EXECUTION_CONTEXT, USER_NAME);
            fail("should throw StillPrimaryOwnerException");
        } catch (StillPrimaryOwnerException e) {
            //success
            verify(membershipService, never()).removeMemberMemberships(EXECUTION_CONTEXT, MembershipMemberType.USER, USER_NAME);
            verify(userRepository, never()).update(any());
            verify(searchEngineService, never()).delete(eq(EXECUTION_CONTEXT), any());
        }
    }

    @Test
    public void shouldNotDeleteIfApplicationPO() throws TechnicalException {
        User user = new User();
        user.setId(USER_NAME);
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));

        RoleEntity apiPoRole = new RoleEntity();
        apiPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API)).thenReturn(apiPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                USER_NAME,
                MembershipReferenceType.API,
                apiPoRole.getId()
            )
        )
            .thenReturn(Set.of());

        RoleEntity appPoRole = new RoleEntity();
        appPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION)).thenReturn(appPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                USER_NAME,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            )
        )
            .thenReturn(Set.of((new MembershipEntity())));

        try {
            userService.delete(EXECUTION_CONTEXT, USER_NAME);
            fail("should throw StillPrimaryOwnerException");
        } catch (StillPrimaryOwnerException e) {
            //success
            verify(membershipService, never()).removeMemberMemberships(EXECUTION_CONTEXT, MembershipMemberType.USER, USER_NAME);
            verify(userRepository, never()).update(any());
            verify(searchEngineService, never()).delete(eq(EXECUTION_CONTEXT), any());
        }
    }

    @Test
    public void shouldDeleteUnanonymize() throws TechnicalException {
        String userId = "userId";
        String firstName = "first";
        String lastName = "last";
        String email = "email";

        User user = new User();
        user.setId(userId);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setEmail(email);
        user.setOrganizationId(ORGANIZATION);
        when(userRepository.findById(userId)).thenReturn(of(user));

        RoleEntity apiPoRole = new RoleEntity();
        apiPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API)).thenReturn(apiPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.API,
                apiPoRole.getId()
            )
        )
            .thenReturn(Set.of());

        RoleEntity appPoRole = new RoleEntity();
        appPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION)).thenReturn(appPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            )
        )
            .thenReturn(Set.of());

        userService.delete(EXECUTION_CONTEXT, userId);

        verify(membershipService, times(1))
            .getMembershipsByMemberAndReferenceAndRole(MembershipMemberType.USER, userId, MembershipReferenceType.API, apiPoRole.getId());
        verify(membershipService, times(1))
            .getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            );

        verify(membershipService, times(1)).removeMemberMemberships(EXECUTION_CONTEXT, MembershipMemberType.USER, userId);
        verify(userRepository, times(1))
            .update(
                argThat(
                    new ArgumentMatcher<User>() {
                        @Override
                        public boolean matches(User user) {
                            return (
                                userId.equals(user.getId()) &&
                                UserStatus.ARCHIVED.equals(user.getStatus()) &&
                                "deleted-sourceId".equals(user.getSourceId()) &&
                                !updatedAt.equals(user.getUpdatedAt()) &&
                                firstName.equals(user.getFirstname()) &&
                                lastName.equals(user.getLastname()) &&
                                email.equals(user.getEmail())
                            );
                        }
                    }
                )
            );
        verify(searchEngineService, times(1)).delete(eq(EXECUTION_CONTEXT), any());
        verify(portalNotificationService, times(1)).deleteAll(user.getId());
        verify(portalNotificationConfigService, times(1)).deleteByUser(user.getId());
        verify(genericNotificationConfigService, times(1)).deleteByUser(eq(user));
        verify(tokenService, times(1)).revokeByUser(EXECUTION_CONTEXT, userId);
    }

    @Test
    public void shouldDeleteAnonymize() throws TechnicalException {
        setField(userService, "anonymizeOnDelete", true);

        String userId = "userId";
        String organizationId = ORGANIZATION;
        String firstName = "first";
        String lastName = "last";
        String email = "email";

        User user = new User();
        user.setId(userId);
        user.setOrganizationId(organizationId);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setEmail(email);
        user.setPicture("picture");
        when(userRepository.findById(userId)).thenReturn(of(user));

        RoleEntity apiPoRole = new RoleEntity();
        apiPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API)).thenReturn(apiPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.API,
                apiPoRole.getId()
            )
        )
            .thenReturn(Set.of());

        RoleEntity appPoRole = new RoleEntity();
        appPoRole.setId("po-role");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION)).thenReturn(appPoRole);
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            )
        )
            .thenReturn(Set.of());

        userService.delete(EXECUTION_CONTEXT, userId);

        verify(membershipService, times(1))
            .getMembershipsByMemberAndReferenceAndRole(MembershipMemberType.USER, userId, MembershipReferenceType.API, apiPoRole.getId());
        verify(membershipService, times(1))
            .getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            );

        verify(membershipService, times(1)).removeMemberMemberships(EXECUTION_CONTEXT, MembershipMemberType.USER, userId);
        verify(userRepository, times(1))
            .update(
                argThat(
                    new ArgumentMatcher<User>() {
                        @Override
                        public boolean matches(User user) {
                            return (
                                userId.equals(user.getId()) &&
                                organizationId.equals(user.getOrganizationId()) &&
                                UserStatus.ARCHIVED.equals(user.getStatus()) &&
                                ("deleted-" + userId).equals(user.getSourceId()) &&
                                !updatedAt.equals(user.getUpdatedAt()) &&
                                "Unknown".equals(user.getFirstname()) &&
                                user.getLastname().isEmpty() &&
                                user.getEmail() == null &&
                                user.getPicture() == null
                            );
                        }
                    }
                )
            );
        verify(searchEngineService, times(1)).delete(eq(EXECUTION_CONTEXT), any());
        verify(portalNotificationService, times(1)).deleteAll(user.getId());
        verify(portalNotificationConfigService, times(1)).deleteByUser(user.getId());
        verify(genericNotificationConfigService, times(1)).deleteByUser(eq(user));
        verify(tokenService, times(1)).revokeByUser(EXECUTION_CONTEXT, userId);
    }

    @Test(expected = EmailRequiredException.class)
    public void shouldThrowEmailRequiredExceptionWhenMissingMailInUserInfo() throws IOException {
        reset(identityProvider);

        Map<String, String> wrongUserProfileMapping = new HashMap<>();
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.EMAIL, "theEmail");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.ID, "theEmail");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.SUB, "sub");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.FIRSTNAME, "given_name");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.LASTNAME, "family_name");
        wrongUserProfileMapping.put(SocialIdentityProviderEntity.UserProfile.PICTURE, "picture");
        when(identityProvider.getUserProfileMapping()).thenReturn(wrongUserProfileMapping);
        when(identityProvider.isEmailRequired()).thenReturn(Boolean.TRUE);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);
    }

    @Test
    public void shouldReturnDefaultGroupsMappingWhenSpelEvaluationExceptionOccurs() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService);
        mockDefaultEnvironment();

        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition("Some Soup");
        condition1.setGroups(Arrays.asList("Example group", "soft user"));

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition3.setGroups(Collections.singletonList("Api consumer"));

        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(condition1, condition2, condition3));
        when(userRepository.create(any())).thenReturn(mockUser());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        verify(groupService, times(1)).findById(EXECUTION_CONTEXT, "Api consumer");

        verify(groupService, never()).findById(EXECUTION_CONTEXT, "Others");
        verify(groupService, never()).findById(EXECUTION_CONTEXT, "Example group");
        verify(groupService, never()).findById(EXECUTION_CONTEXT, "soft user");
    }

    @Test
    public void shouldRefreshExistingUser() throws IOException, TechnicalException {
        reset(identityProvider, userRepository);

        mockDefaultEnvironment();

        User user = mockUser();

        when(userRepository.findBySource(null, user.getSourceId(), ORGANIZATION)).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.update(user)).thenReturn(user);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);
        verify(userRepository, times(1)).update(refEq(user));
    }

    private User mockUser() {
        User user = new User();
        user.setId("janedoe@example.com");
        user.setSource("oauth2");
        user.setSourceId("janedoe@example.com");
        user.setLastname("Doe");
        user.setFirstname("Jane");
        user.setEmail("janedoe@example.com");
        user.setPicture("http://example.com/janedoe/me.jpg");
        user.setOrganizationId(ORGANIZATION);
        return user;
    }

    @Test
    public void shouldCreateNewUser() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, roleService);

        mockDefaultEnvironment();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    public void shouldCreateNewUserWithNullMappings() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, roleService);

        mockDefaultEnvironment();

        when(identityProvider.getRoleMappings()).thenReturn(null);
        when(identityProvider.getGroupMappings()).thenReturn(null);

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    public void shouldCreateNewUserWithNoMatchingGroupsMappingFromUserInfo() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", ORGANIZATION)).thenReturn(Optional.empty());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body_no_matching.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(EXECUTION_CONTEXT),
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromUserInfo() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();
        mockRolesMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn(createdUser.getSource());
        when(userRepository.findBySource(createdUser.getSource(), createdUser.getSourceId(), ORGANIZATION)).thenReturn(Optional.empty());

        //mock group search and association
        when(groupService.findById(EXECUTION_CONTEXT, "Example group")).thenReturn(mockGroupEntity("group_id_1", "Example group"));
        when(groupService.findById(EXECUTION_CONTEXT, "soft user")).thenReturn(mockGroupEntity("group_id_2", "soft user"));
        when(groupService.findById(EXECUTION_CONTEXT, "Api consumer")).thenReturn(mockGroupEntity("group_id_4", "Api consumer"));

        // mock role search
        RoleEntity roleOrganizationAdmin = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        RoleEntity roleOrganizationUser = mockRoleEntity(RoleScope.ORGANIZATION, "USER");
        RoleEntity roleEnvironmentAdmin = mockRoleEntity(RoleScope.ENVIRONMENT, "ADMIN");
        RoleEntity roleApiUser = mockRoleEntity(RoleScope.API, "USER");
        RoleEntity roleApplicationAdmin = mockRoleEntity(RoleScope.APPLICATION, "ADMIN");

        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "ADMIN", ORGANIZATION)).thenReturn(Optional.of(roleOrganizationAdmin));
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "USER", ORGANIZATION)).thenReturn(Optional.of(roleOrganizationUser));
        when(roleService.findDefaultRoleByScopes(ORGANIZATION, RoleScope.API, RoleScope.APPLICATION))
            .thenReturn(Arrays.asList(roleApiUser, roleApplicationAdmin));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, ORGANIZATION)),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        //verify group creations
        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(0))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_3")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, ORGANIZATION)),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"))
                ),
                eq("oauth2")
            );
    }

    @Test
    public void shouldUpdateUserWithGroupMappingWithoutOverridingIfGroupDefined() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();
        mockRolesMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", ORGANIZATION)).thenReturn(Optional.empty());

        //mock group search and association
        when(groupService.findById(EXECUTION_CONTEXT, "Example group")).thenReturn(mockGroupEntity("group_id_1", "Example group"));
        when(groupService.findById(EXECUTION_CONTEXT, "soft user")).thenReturn(mockGroupEntity("group_id_2", "soft user"));
        when(groupService.findById(EXECUTION_CONTEXT, "Api consumer")).thenReturn(mockGroupEntity("group_id_4", "Api consumer"));

        // mock role search
        RoleEntity roleOrganizationAdmin = mockRoleEntity(RoleScope.ORGANIZATION, "ADMIN");
        RoleEntity roleOrganizationUser = mockRoleEntity(RoleScope.ORGANIZATION, "USER");
        RoleEntity roleEnvironmentAdmin = mockRoleEntity(RoleScope.ENVIRONMENT, "ADMIN");
        RoleEntity roleApiUser = mockRoleEntity(RoleScope.API, "USER");
        RoleEntity roleApplicationAdmin = mockRoleEntity(RoleScope.APPLICATION, "ADMIN");

        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "ADMIN", ORGANIZATION)).thenReturn(Optional.of(roleOrganizationAdmin));
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "USER", ORGANIZATION)).thenReturn(Optional.of(roleOrganizationUser));
        when(roleService.findDefaultRoleByScopes(ORGANIZATION, RoleScope.API, RoleScope.APPLICATION))
            .thenReturn(Arrays.asList(roleApiUser, roleApplicationAdmin));

        Membership membership = new Membership();
        membership.setSource("oauth2");
        membership.setReferenceId("membershipId");
        membership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.GROUP);
        final HashSet<Membership> memberships = new HashSet<>();
        memberships.add(membership);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "janedoe@example.com",
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP
            )
        )
            .thenReturn(memberships);

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, ORGANIZATION)),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(Collections.singletonList(mockMemberEntity()));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        //verify group creations
        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(0))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_3")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4")),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "USER")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, ORGANIZATION)),
                eq(new MembershipService.MembershipMember("janedoe@example.com", null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "ADMIN")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.ORGANIZATION, "USER"))
                ),
                eq("oauth2")
            );

        verify(membershipService, times(1))
            .deleteReferenceMemberBySource(
                eq(EXECUTION_CONTEXT),
                eq(MembershipReferenceType.GROUP),
                eq("membershipId"),
                eq(MembershipMemberType.USER),
                eq("janedoe@example.com"),
                eq("oauth2")
            );
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromUserInfoWhenGroupIsNotFound() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn(createdUser.getSource());
        when(userRepository.findBySource(createdUser.getSource(), createdUser.getSourceId(), ORGANIZATION)).thenReturn(Optional.empty());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(EXECUTION_CONTEXT),
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );

        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION);
        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ENVIRONMENT);
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromAccessTokenWhenGroupIsNotFound() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();

        GroupMappingEntity groupCondition1 = new GroupMappingEntity();
        groupCondition1.setCondition("{#jsonPath(#accessToken, '$.custom_access_token') == 'foobar'}");
        groupCondition1.setGroups(Collections.singletonList("Api consumer"));
        GroupMappingEntity groupCondition2 = new GroupMappingEntity();
        groupCondition2.setCondition("{#jsonPath(#accessToken, '$.custom_access_token') == 'unknown'}");
        groupCondition2.setGroups(Collections.singletonList("Api consumer"));

        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(groupCondition1, groupCondition2));

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn(createdUser.getSource());
        when(userRepository.findBySource(createdUser.getSource(), createdUser.getSourceId(), ORGANIZATION)).thenReturn(Optional.empty());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(EXECUTION_CONTEXT),
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );

        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION);
        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ENVIRONMENT);
        verify(groupService, times(1)).findById(EXECUTION_CONTEXT, "Api consumer");
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromIdTokenWhenGroupIsNotFound() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();

        GroupMappingEntity groupCondition1 = new GroupMappingEntity();
        groupCondition1.setCondition("{#jsonPath(#idToken, '$.custom_id_token') == 'foobar'}");
        groupCondition1.setGroups(Collections.singletonList("Api consumer"));
        GroupMappingEntity groupCondition2 = new GroupMappingEntity();
        groupCondition2.setCondition("{#jsonPath(#idToken, '$.custom_id_token') == 'unknown'}");
        groupCondition2.setGroups(Collections.singletonList("Api consumer"));

        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(groupCondition1, groupCondition2));

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn(createdUser.getSource());
        when(userRepository.findBySource(createdUser.getSource(), createdUser.getSourceId(), ORGANIZATION)).thenReturn(Optional.empty());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(EXECUTION_CONTEXT),
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );

        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION);
        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ENVIRONMENT);
        verify(groupService, times(1)).findById(EXECUTION_CONTEXT, "Api consumer");
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromIdTokenWhenGroupIsNull() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);

        when(identityProvider.getId()).thenReturn(createdUser.getSource());
        when(userRepository.findBySource(createdUser.getSource(), createdUser.getSourceId(), ORGANIZATION)).thenReturn(Optional.empty());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, null, null);

        //verify group creations
        verify(membershipService, never())
            .addRoleToMemberOnReference(
                eq(EXECUTION_CONTEXT),
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipMember.class),
                any(MembershipService.MembershipRole.class)
            );

        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ORGANIZATION);
        verify(roleService, times(1)).findDefaultRoleByScopes(ORGANIZATION, RoleScope.ENVIRONMENT);
    }

    @Test
    public void shouldCreateUserWithGroupRolesWhenNoOrgDefaultRoles() throws Exception {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        // Group mapping
        GroupMappingEntity mapping = new GroupMappingEntity();
        mapping.setCondition("true");
        mapping.setGroups(List.of("Group with roles"));
        when(identityProvider.getGroupMappings()).thenReturn(List.of(mapping));
        // No existing user
        when(userRepository.findBySource(any(), any(), eq(ORGANIZATION))).thenReturn(Optional.empty());
        when(identityProvider.getId()).thenReturn("oauth2");

        User createdUser = mockUser();
        when(userRepository.create(any())).thenReturn(createdUser);

        // Mock group with overrides
        GroupEntity group = new GroupEntity();
        group.setId("group-1");
        group.setRoles(Map.of(RoleScope.API, "API_OVERRIDE", RoleScope.APPLICATION, "APP_OVERRIDE"));
        when(groupService.findById(EXECUTION_CONTEXT, "Group with roles")).thenReturn(group);

        // No org default roles
        when(roleService.findDefaultRoleByScopes(eq(ORGANIZATION), any())).thenReturn(Collections.emptyList());
        // Membership update expectation
        when(
            membershipService.updateRolesToMemberOnReferenceBySource(
                eq(EXECUTION_CONTEXT),
                eq(new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group-1")),
                eq(new MembershipService.MembershipMember(createdUser.getId(), null, MembershipMemberType.USER)),
                argThat(roles ->
                    roles.contains(new MembershipService.MembershipRole(RoleScope.API, "API_OVERRIDE")) &&
                    roles.contains(new MembershipService.MembershipRole(RoleScope.APPLICATION, "APP_OVERRIDE"))
                ),
                eq("oauth2")
            )
        )
            .thenReturn(List.of(mockMemberEntity()));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, null, null);
        verify(membershipService).updateRolesToMemberOnReferenceBySource(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotAssignRolesWhenNoOrgOrGroupDefaultRoles() throws Exception {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        // Group mapping
        GroupMappingEntity mapping = new GroupMappingEntity();
        mapping.setCondition("true");
        mapping.setGroups(List.of("Group without roles"));
        when(identityProvider.getGroupMappings()).thenReturn(List.of(mapping));

        // No existing user
        when(userRepository.findBySource(any(), any(), eq(ORGANIZATION))).thenReturn(Optional.empty());
        when(identityProvider.getId()).thenReturn("oauth2");

        User createdUser = mockUser();
        when(userRepository.create(any())).thenReturn(createdUser);

        // Mock group with no roles
        GroupEntity group = new GroupEntity();
        group.setId("group-2");
        group.setRoles(Collections.emptyMap());
        when(groupService.findById(EXECUTION_CONTEXT, "Group without roles")).thenReturn(group);

        // No org default roles
        when(roleService.findDefaultRoleByScopes(eq(ORGANIZATION), any())).thenReturn(Collections.emptyList());

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, null, null);

        // No memberships should be created
        verify(membershipService, never()).updateRolesToMemberOnReferenceBySource(any(), any(), any(), any(), any());
    }

    private void mockDefaultEnvironment() {
        Map<String, String> userProfileMapping = new HashMap<>();
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.EMAIL, "email");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.ID, "email");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.SUB, "sub");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.FIRSTNAME, "given_name");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.LASTNAME, "family_name");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.PICTURE, "picture");
        when(identityProvider.getUserProfileMapping()).thenReturn(userProfileMapping);

        EnvironmentEntity defaultEnv = new EnvironmentEntity();
        defaultEnv.setId(ENVIRONMENT);
        when(environmentService.findByOrganization(ORGANIZATION)).thenReturn(List.of(defaultEnv));
    }

    private void mockGroupsMapping() {
        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition(
            "{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}"
        );
        condition1.setGroups(Arrays.asList("Example group", "soft user"));

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition(
            "{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'" +
            "&& #jsonPath(#accessToken, '$.custom_access_token') == 'foobar' " +
            "&& #jsonPath(#idToken, '$.custom_id_token') == 'foobar'}"
        );
        condition3.setGroups(Collections.singletonList("Api consumer"));

        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(condition1, condition2, condition3));
    }

    private void mockRolesMapping() {
        final List<RoleMappingEntity> roleMappingList = getRoleMappingEntities();
        when(identityProvider.getRoleMappings()).thenReturn(roleMappingList);
    }

    private List<RoleMappingEntity> getRoleMappingEntities() {
        RoleMappingEntity role1 = new RoleMappingEntity();
        role1.setCondition(
            "{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' " +
            "&& #jsonPath(#profile, '$.job_id') != 'API_BREAKER' " +
            "&& #jsonPath(#accessToken, '$.custom_access_token') == 'foobar' " +
            "&& #jsonPath(#idToken, '$.custom_id_token') == 'foobar'}"
        );
        role1.setOrganizations(Collections.singletonList("ADMIN"));

        RoleMappingEntity role2 = new RoleMappingEntity();
        role2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        role2.setOrganizations(Collections.singletonList("USER"));

        RoleMappingEntity role3 = new RoleMappingEntity();
        role3.setCondition(
            "{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'" +
            "&& #jsonPath(#accessToken, '$.custom_access_token') == 'foobar' " +
            "&& #jsonPath(#idToken, '$.custom_id_token') == 'foobar'}"
        );
        role3.setOrganizations(Collections.singletonList("USER"));
        role3.setEnvironments(Collections.singletonMap(ENVIRONMENT, Collections.singletonList("USER")));
        final List<RoleMappingEntity> roleMappingList = Arrays.asList(role1, role2, role3);
        return roleMappingList;
    }

    private RoleEntity mockRoleEntity(RoleScope scope, String name) {
        RoleEntity role = new RoleEntity();
        role.setId(scope.name() + "_" + name);
        role.setScope(scope);
        role.setName(name);
        return role;
    }

    private GroupEntity mockGroupEntity(String id, String name) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        return groupEntity;
    }

    private MemberEntity mockMemberEntity() {
        return mock(MemberEntity.class);
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }

    @Test
    public void shouldSearchUsers_hasResults_ordersUniqueAndPopulateFlags() {
        UserServiceImpl spyUserService = spy(userService);

        // Search engine returns duplicated ids and one unknown id
        List<String> docs = Arrays.asList("u1", "u2", "u1", "u3", "u4", "u3");
        io.gravitee.rest.api.service.impl.search.SearchResult searchResult = new io.gravitee.rest.api.service.impl.search.SearchResult(
            docs,
            42
        );
        when(searchEngineService.search(eq(EXECUTION_CONTEXT), any())).thenReturn(searchResult);

        // Prepare fetched users (u4 is missing on purpose)
        UserEntity ue1 = new UserEntity();
        ue1.setId("u1");
        UserEntity ue2 = new UserEntity();
        ue2.setId("u2");
        UserEntity ue3 = new UserEntity();
        ue3.setId("u3");
        doReturn(new HashSet<>(Arrays.asList(ue1, ue2, ue3))).when(spyUserService).findByIds(eq(EXECUTION_CONTEXT), anyCollection());

        // Mock roles for Primary Owner checks
        RoleEntity apiPORole = mockRoleEntity(RoleScope.API, "PRIMARY_OWNER");
        RoleEntity appPORole = mockRoleEntity(RoleScope.APPLICATION, "PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API)).thenReturn(apiPORole);
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION)).thenReturn(appPORole);

        // Only u2 is primary owner (API). Others not.
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                eq(MembershipMemberType.USER),
                eq("u2"),
                eq(MembershipReferenceType.API),
                eq(apiPORole.getId())
            )
        )
            .thenReturn(
                java.util.Collections.<io.gravitee.rest.api.model.MembershipEntity>singleton(
                    new io.gravitee.rest.api.model.MembershipEntity()
                )
            );
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                eq(MembershipMemberType.USER),
                eq("u1"),
                eq(MembershipReferenceType.API),
                eq(apiPORole.getId())
            )
        )
            .thenReturn(java.util.Collections.<io.gravitee.rest.api.model.MembershipEntity>emptySet());
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                eq(MembershipMemberType.USER),
                eq("u3"),
                eq(MembershipReferenceType.API),
                eq(apiPORole.getId())
            )
        )
            .thenReturn(java.util.Collections.<io.gravitee.rest.api.model.MembershipEntity>emptySet());

        // No application PO for anyone
        when(
            membershipService.getMembershipsByMemberAndReferenceAndRole(
                eq(MembershipMemberType.USER),
                anyString(),
                eq(MembershipReferenceType.APPLICATION),
                eq(appPORole.getId())
            )
        )
            .thenReturn(java.util.Collections.<io.gravitee.rest.api.model.MembershipEntity>emptySet());

        // Tokens per user: u1=1, u2=3, u3=0
        when(tokenService.findByUser("u1")).thenReturn(Collections.singletonList(new io.gravitee.rest.api.model.TokenEntity()));
        when(tokenService.findByUser("u2"))
            .thenReturn(
                Arrays.asList(
                    new io.gravitee.rest.api.model.TokenEntity(),
                    new io.gravitee.rest.api.model.TokenEntity(),
                    new io.gravitee.rest.api.model.TokenEntity()
                )
            );
        when(tokenService.findByUser("u3")).thenReturn(Collections.emptyList());

        io.gravitee.rest.api.model.common.Pageable pageable = new io.gravitee.rest.api.model.common.PageableImpl(2, 5);

        io.gravitee.common.data.domain.Page<UserEntity> page = spyUserService.search(EXECUTION_CONTEXT, "john", pageable);

        // Then: order preserved, duplicates removed, missing id filtered out
        List<UserEntity> content = page.getContent();
        assertEquals(3, content.size());
        assertEquals("u1", content.get(0).getId());
        assertEquals("u2", content.get(1).getId());
        assertEquals("u3", content.get(2).getId());

        // Page metadata
        assertEquals(2, page.getPageNumber());
        assertEquals(5, page.getPageElements());
        assertEquals(42, page.getTotalElements());

        // Flags populated
        assertFalse(content.get(0).isPrimaryOwner());
        assertTrue(content.get(1).isPrimaryOwner());
        assertFalse(content.get(2).isPrimaryOwner());

        assertEquals(1, content.get(0).getNbActiveTokens());
        assertEquals(3, content.get(1).getNbActiveTokens());
        assertEquals(0, content.get(2).getNbActiveTokens());

        // Verify that search was called and populateUserFlags implied calls
        verify(searchEngineService).search(eq(EXECUTION_CONTEXT), any());
        verify(roleService).findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.API);
        verify(roleService).findPrimaryOwnerRoleByOrganization(ORGANIZATION, RoleScope.APPLICATION);
    }
}
