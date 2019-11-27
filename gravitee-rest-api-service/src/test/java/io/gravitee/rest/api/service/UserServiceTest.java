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
package io.gravitee.rest.api.service;

import com.auth0.jwt.JWTSigner;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.UserServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_REGISTRATION;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    private static final String USER_SOURCE = "usersource";
    private static final String USER_NAME = "tuser";
    private static final String EMAIL = "user@gravitee.io";
    private static final String FIRST_NAME = "The";
    private static final String LAST_NAME = "User";
    private static final String PASSWORD = "gh2gyf8!zjfnz";
    private static final String JWT_SECRET = "VERYSECURE";;
    private static final String ENVIRONMENT = "DEFAULT";
    private static final Set<UserRoleEntity> ROLES = Collections.singleton(new UserRoleEntity());
    static {
        UserRoleEntity r = ROLES.iterator().next();
        r.setScope(io.gravitee.rest.api.model.permissions.RoleScope.PORTAL);
        r.setName("USER");
    }

    @InjectMocks
    private UserServiceImpl userService = new UserServiceImpl();

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private RoleService roleService;
    @Mock
    private MembershipService membershipService;
    @Mock
    private ConfigurableEnvironment environment;
    @Mock
    private NewExternalUserEntity newUser;
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
    private ApiService apiService;
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

    @Test
    public void shouldFindByUsername() throws TechnicalException {
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ENVIRONMENT)).thenReturn(of(user));

        final UserEntity userEntity = userService.findBySource(USER_SOURCE, USER_NAME, false);

        assertEquals(USER_NAME, userEntity.getId());
        assertEquals(FIRST_NAME, userEntity.getFirstname());
        assertEquals(LAST_NAME, userEntity.getLastname());
        assertEquals(EMAIL, userEntity.getEmail());
        assertEquals(PASSWORD, userEntity.getPassword());
        assertEquals(null, userEntity.getRoles());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotFindByUsernameBecauseNotExists() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ENVIRONMENT)).thenReturn(Optional.empty());

        userService.findBySource(USER_SOURCE, USER_NAME, false);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUsernameBecauseTechnicalException() throws TechnicalException {
        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ENVIRONMENT)).thenThrow(TechnicalException.class);

        userService.findBySource(USER_SOURCE, USER_NAME, false);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        when(newUser.getEmail()).thenReturn(EMAIL);
        when(newUser.getFirstname()).thenReturn(FIRST_NAME);
        when(newUser.getLastname()).thenReturn(LAST_NAME);
        when(newUser.getSource()).thenReturn(USER_SOURCE);
        when(newUser.getSourceId()).thenReturn(USER_NAME);

        when(userRepository.findBySource(USER_SOURCE, USER_NAME, ENVIRONMENT)).thenReturn(Optional.empty());

        when(user.getId()).thenReturn(USER_NAME);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getFirstname()).thenReturn(FIRST_NAME);
        when(user.getLastname()).thenReturn(LAST_NAME);
        when(user.getPassword()).thenReturn(PASSWORD);
        when(user.getCreatedAt()).thenReturn(date);
        when(user.getUpdatedAt()).thenReturn(date);
        when(userRepository.create(any(User.class))).thenReturn(user);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.PORTAL);
        when(role.getName()).thenReturn("USER");
        when(membershipService.getRole(
                MembershipReferenceType.PORTAL,
                MembershipDefaultReferenceId.DEFAULT.name(),
                user.getId(),
                RoleScope.PORTAL)).thenReturn(role);

        final UserEntity createdUserEntity = userService.create(newUser, false);

        verify(userRepository).create(argThat(userToCreate -> USER_NAME.equals(userToCreate.getSourceId()) &&
            USER_SOURCE.equals(userToCreate.getSource()) &&
            EMAIL.equals(userToCreate.getEmail()) &&
            FIRST_NAME.equals(userToCreate.getFirstname()) &&
            LAST_NAME.equals(userToCreate.getLastname()) &&
            userToCreate.getCreatedAt() != null &&
            userToCreate.getUpdatedAt() != null &&
            userToCreate.getCreatedAt().equals(userToCreate.getUpdatedAt())));

        assertEquals(USER_NAME, createdUserEntity.getId());
        assertEquals(FIRST_NAME, createdUserEntity.getFirstname());
        assertEquals(LAST_NAME, createdUserEntity.getLastname());
        assertEquals(EMAIL, createdUserEntity.getEmail());
        assertEquals(PASSWORD, createdUserEntity.getPassword());
        assertEquals(ROLES, createdUserEntity.getRoles());
        assertEquals(date, createdUserEntity.getCreatedAt());
        assertEquals(date, createdUserEntity.getUpdatedAt());
    }

    @Test(expected = UserAlreadyExistsException.class)
    public void shouldNotCreateBecauseExists() throws TechnicalException {
        when(userRepository.findBySource(nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(of(new User()));

        userService.create(newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        when(userRepository.create(any(User.class))).thenThrow(TechnicalException.class);

        userService.create(newUser, false);

        verify(userRepository, never()).create(any());
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotConnectBecauseNotExists() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.empty());

        userService.connect(USER_NAME);

        verify(userRepository, never()).create(any());
    }

    @Test
    public void shouldCreateDefaultApplication() throws TechnicalException {
        setField(userService, "defaultApplicationForFirstConnection", true);
        when(user.getLastConnectionAt()).thenReturn(null);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(USER_NAME);

        verify(applicationService, times(1)).create(any(), eq(USER_NAME));
    }

    @Test
    public void shouldNotCreateDefaultApplicationBecauseDisabled() throws TechnicalException {
        setField(userService, "defaultApplicationForFirstConnection", false);
        when(user.getLastConnectionAt()).thenReturn(null);
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(USER_NAME);

        verify(applicationService, never()).create(any(), eq(USER_NAME));
    }

    @Test
    public void shouldNotCreateDefaultApplicationBecauseAlreadyConnected() throws TechnicalException {
        when(user.getLastConnectionAt()).thenReturn(new Date());
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.connect(USER_NAME);

        verify(applicationService, never()).create(any(), eq(USER_NAME));
    }

    @Test(expected = UserRegistrationUnavailableException.class)
    public void shouldNotCreateUserIfRegistrationIsDisabled() {
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.FALSE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis()/1000 + 100));

        userService.finalizeRegistration(userEntity);
    }

    @Test(expected = UserNotFoundException.class)
    public void createNewRegistrationUserThatIsNotCreatedYet() throws TechnicalException {
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.TRUE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis()/1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(userEntity);

    }

    @Test
    public void createAlreadyPreRegisteredUser() throws TechnicalException {
        when(mockParameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)).thenReturn(Boolean.TRUE);
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        User user = new User();
        user.setId("CUSTOM_LONG_ID");
        user.setEmail(EMAIL);
        user.setFirstname(FIRST_NAME);
        user.setLastname(LAST_NAME);
        when(userRepository.findById(USER_NAME)).thenReturn(Optional.of(user));
        when(userRepository.update(any(User.class))).thenReturn(user);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis()/1000 + 100));
        userEntity.setPassword(PASSWORD);

        userService.finalizeRegistration(userEntity);

        verify(userRepository).update(argThat(userToCreate -> "CUSTOM_LONG_ID".equals(userToCreate.getId()) &&
                EMAIL.equals(userToCreate.getEmail()) &&
                FIRST_NAME.equals(userToCreate.getFirstname()) &&
                LAST_NAME.equals(userToCreate.getLastname()) &&
                !StringUtils.isEmpty(userToCreate.getPassword())));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldValidateJWTokenAndFail() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);

        RegisterUserEntity userEntity = new RegisterUserEntity();
        userEntity.setToken(createJWT(System.currentTimeMillis()/1000 - 100));
        userEntity.setPassword(PASSWORD);

        verify(userRepository, never()).findBySource(USER_SOURCE, USER_NAME, ENVIRONMENT);

        userService.finalizeRegistration(userEntity);
    }

    @Test
    public void shouldResetPassword() throws TechnicalException {
        when(environment.getProperty("jwt.secret")).thenReturn(JWT_SECRET);
        when(user.getId()).thenReturn(USER_NAME);
        when(user.getSource()).thenReturn("gravitee");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.resetPassword(USER_NAME);

        verify(user).setPassword(null);
        verify(userRepository).update(user);
    }

    @Test(expected = UserNotFoundException.class)
    public void shouldNotResetPasswordCauseUserNotFound() throws TechnicalException {
        when(userRepository.findById(USER_NAME)).thenReturn(empty());
        userService.resetPassword(USER_NAME);
    }

    @Test(expected = UserNotInternallyManagedException.class)
    public void shouldNotResetPasswordCauseUserNotInternallyManaged() throws TechnicalException {
        when(user.getSource()).thenReturn("external");
        when(userRepository.findById(USER_NAME)).thenReturn(of(user));

        userService.resetPassword(USER_NAME);
    }

    private String createJWT(long expirationSeconds) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JWTHelper.Claims.SUBJECT, USER_NAME);
        claims.put(JWTHelper.Claims.EMAIL, EMAIL);
        claims.put(JWTHelper.Claims.FIRSTNAME, FIRST_NAME);
        claims.put(JWTHelper.Claims.LASTNAME, LAST_NAME);
        claims.put(JWTHelper.Claims.ACTION, USER_REGISTRATION);
        claims.put("exp", expirationSeconds);
        return new JWTSigner(JWT_SECRET).sign(claims);
    }

    @Test
    public void shouldNotDeleteIfAPIPO() throws TechnicalException {
        ApiEntity apiEntity = mock(ApiEntity.class);
        PrimaryOwnerEntity primaryOwnerEntity = mock(PrimaryOwnerEntity.class);
        when(apiEntity.getPrimaryOwner()).thenReturn(primaryOwnerEntity);
        when(primaryOwnerEntity.getId()).thenReturn(USER_NAME);
        when(apiService.findByUser(USER_NAME, null)).thenReturn(Collections.singleton(apiEntity));

        try {
            userService.delete(USER_NAME);
            fail("should throw StillPrimaryOwnerException");
        } catch (StillPrimaryOwnerException e) {
            //success
            verify(membershipService, never()).removeUser(USER_NAME);
            verify(userRepository, never()).update(any());
            verify(searchEngineService, never()).delete(any(), eq(false));
        }

    }

    @Test
    public void shouldNotDeleteIfApplicationPO() throws TechnicalException {
        ApplicationListItem applicationListItem = mock(ApplicationListItem.class);
        PrimaryOwnerEntity primaryOwnerEntity = mock(PrimaryOwnerEntity.class);
        when(applicationListItem.getPrimaryOwner()).thenReturn(primaryOwnerEntity);
        when(primaryOwnerEntity.getId()).thenReturn(USER_NAME);
        when(applicationService.findByUser(USER_NAME)).thenReturn(Collections.singleton(applicationListItem));

        try {
            userService.delete(USER_NAME);
            fail("should throw StillPrimaryOwnerException");
        } catch (StillPrimaryOwnerException e) {
            //success
            verify(membershipService, never()).removeUser(USER_NAME);
            verify(userRepository, never()).update(any());
            verify(searchEngineService, never()).delete(any(), eq(false));
        }

    }

    @Test
    public void shouldDeleteUnanonymize() throws TechnicalException {
        String userId = "userId";
        String firstName = "first";
        String lastName = "last";
        String email = "email";
        when(apiService.findByUser(userId, null)).thenReturn(Collections.emptySet());
        when(applicationService.findByUser(userId)).thenReturn(Collections.emptySet());
        User user = new User();
        user.setId(userId);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setEmail(email);
        when(userRepository.findById(userId)).thenReturn(of(user));

        userService.delete(userId);

        verify(apiService, times(1)).findByUser(userId, null);
        verify(applicationService, times(1)).findByUser(userId);
        verify(membershipService, times(1)).removeUser(userId);
        verify(userRepository, times(1)).update(argThat(new ArgumentMatcher<User>() {
            @Override
            public boolean matches(User user) {
                return userId.equals(user.getId())
                        && UserStatus.ARCHIVED.equals(user.getStatus())
                        && "deleted-sourceId".equals(user.getSourceId())
                        && !updatedAt.equals(user.getUpdatedAt())
                        && firstName.equals(user.getFirstname())
                        && lastName.equals(user.getLastname())
                        && email.equals(user.getEmail());
            }
        }));
        verify(searchEngineService, times(1)).delete(any(), eq(false));
        verify(portalNotificationService, times(1)).deleteAll(user.getId());
        verify(portalNotificationConfigService, times(1)).deleteByUser(user.getId());
        verify(genericNotificationConfigService, times(1)).deleteByUser(eq(user));
    }

    @Test
    public void shouldDeleteAnonymize() throws TechnicalException {
        setField(userService, "anonymizeOnDelete", true);

        String userId = "userId";
        String firstName = "first";
        String lastName = "last";
        String email = "email";
        when(apiService.findByUser(userId, null)).thenReturn(Collections.emptySet());
        when(applicationService.findByUser(userId)).thenReturn(Collections.emptySet());
        User user = new User();
        user.setId(userId);
        user.setSourceId("sourceId");
        Date updatedAt = new Date(1234567890L);
        user.setUpdatedAt(updatedAt);
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setEmail(email);
        user.setPicture("picture");
        when(userRepository.findById(userId)).thenReturn(of(user));

        userService.delete(userId);

        verify(apiService, times(1)).findByUser(userId, null);
        verify(applicationService, times(1)).findByUser(userId);
        verify(membershipService, times(1)).removeUser(userId);
        verify(userRepository, times(1)).update(argThat(new ArgumentMatcher<User>() {
            @Override
            public boolean matches(User user) {
                return userId.equals(user.getId())
                        && UserStatus.ARCHIVED.equals(user.getStatus())
                        && ("deleted-" + userId).equals(user.getSourceId())
                        && !updatedAt.equals(user.getUpdatedAt())
                        && "Unknown".equals(user.getFirstname())
                        && user.getLastname().isEmpty()
                        && user.getEmail() == null
                        && user.getPicture() == null;
            }
        }));
        verify(searchEngineService, times(1)).delete(any(), eq(false));
        verify(portalNotificationService, times(1)).deleteAll(user.getId());
        verify(portalNotificationConfigService, times(1)).deleteByUser(user.getId());
        verify(genericNotificationConfigService, times(1)).deleteByUser(eq(user));
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
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
    }
    
    @Test(expected = SpelEvaluationException.class)
    public void shouldSpelEvaluationExceptionWhenWrongELGroupsMapping() throws IOException, TechnicalException {
        reset(identityProvider, userRepository);
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

        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", "DEFAULT")).thenReturn(Optional.empty());

        
        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
    }

    @Test
    public void shouldRefreshExistingUser() throws IOException, TechnicalException {
        reset(identityProvider, userRepository);

        mockDefaultEnvironment();
        
        User user = mockUser();

        when(userRepository.findBySource(null,user.getSourceId(), "DEFAULT")).thenReturn(Optional.of(user));
        when(userRepository.findById(user.getSourceId())).thenReturn(Optional.of(user));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
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
        return user;
    }

    @Test
    public void shouldCreateNewUser() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, roleService);

        mockDefaultEnvironment();
        
        when(roleService.findDefaultRoleByScopes(RoleScope.MANAGEMENT,RoleScope.PORTAL)).thenReturn(Arrays.asList(mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.PORTAL,"USER")));

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);
        
        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);
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
        when(userRepository.findBySource("oauth2", "janedoe@example.com", "DEFAULT")).thenReturn(Optional.empty());

        RoleEntity rolePortalUser = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.PORTAL,"USER");
        RoleEntity roleManagementAdmin = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.MANAGEMENT,"ADMIN");

        when(roleService.findDefaultRoleByScopes(RoleScope.MANAGEMENT,RoleScope.PORTAL)).thenReturn(Arrays.asList(rolePortalUser, roleManagementAdmin));


        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body_no_matching.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);

        //verify group creations
        verify(membershipService, times(2)).addOrUpdateMember(
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipUser.class),
                any(MembershipService.MembershipRole.class));
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromUserInfo() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();
        mockRolesMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);
        
        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", "DEFAULT")).thenReturn(Optional.empty());

        //mock group search and association
        when(groupService.findById("Example group")).thenReturn(mockGroupEntity("group_id_1","Example group"));
        when(groupService.findById("soft user")).thenReturn(mockGroupEntity("group_id_2","soft user"));
        when(groupService.findById("Api consumer")).thenReturn(mockGroupEntity("group_id_4","Api consumer"));

        // mock role search
        RoleEntity rolePortalUser = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.PORTAL,"USER");
        RoleEntity roleManagementAdmin = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.MANAGEMENT,"ADMIN");
        RoleEntity roleApiUser = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.API,"USER");
        RoleEntity roleApplicationAdmin = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION,"ADMIN");

        when(roleService.findById(RoleScope.PORTAL, "USER")).thenReturn(rolePortalUser);
        when(roleService.findById(RoleScope.MANAGEMENT, "ADMIN")).thenReturn(roleManagementAdmin);
        when(roleService.findDefaultRoleByScopes(RoleScope.API,RoleScope.APPLICATION)).thenReturn(Arrays.asList(roleApiUser,roleApplicationAdmin));
        when(roleService.findDefaultRoleByScopes(RoleScope.MANAGEMENT,RoleScope.PORTAL)).thenReturn(Arrays.asList(rolePortalUser, roleManagementAdmin));

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"))).thenReturn(mockMemberEntity());

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"))).thenReturn(mockMemberEntity());

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))).thenReturn(mockMemberEntity());

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"))).thenReturn(mockMemberEntity());

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"))).thenReturn(mockMemberEntity());

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.MANAGEMENT, MembershipDefaultReferenceId.DEFAULT.name()),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.MANAGEMENT, "ADMIN"))).thenReturn(mockMemberEntity());

        when(membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.PORTAL, MembershipDefaultReferenceId.DEFAULT.name()),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.PORTAL, "USER"))).thenReturn(mockMemberEntity());

        
        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);

      //verify group creations
        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"));

        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_1"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"));

        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"));

        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_2"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"));

        verify(membershipService, times(0)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_3"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"));

        verify(membershipService, times(0)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_3"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"));

        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"));

        verify(membershipService, times(1)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, "group_id_4"),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "ADMIN"));

        verify(membershipService, times(2)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.MANAGEMENT, MembershipDefaultReferenceId.DEFAULT.name()),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.MANAGEMENT, "ADMIN"));

        verify(membershipService, times(2)).addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.PORTAL, MembershipDefaultReferenceId.DEFAULT.name()),
                new MembershipService.MembershipUser("janedoe@example.com", null),
                new MembershipService.MembershipRole(RoleScope.PORTAL, "USER"));
    }

    @Test
    public void shouldCreateNewUserWithGroupsMappingFromUserInfoWhenGroupIsNotFound() throws IOException, TechnicalException {
        reset(identityProvider, userRepository, groupService, roleService, membershipService);
        mockDefaultEnvironment();
        mockGroupsMapping();

        User createdUser = mockUser();
        when(userRepository.create(any(User.class))).thenReturn(createdUser);
        
        when(identityProvider.getId()).thenReturn("oauth2");
        when(userRepository.findBySource("oauth2", "janedoe@example.com", "DEFAULT")).thenReturn(Optional.empty());

        RoleEntity rolePortalUser = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.PORTAL,"USER");
        RoleEntity roleManagementAdmin = mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope.MANAGEMENT,"ADMIN");

        when(roleService.findDefaultRoleByScopes(RoleScope.MANAGEMENT,RoleScope.PORTAL)).thenReturn(Arrays.asList(rolePortalUser, roleManagementAdmin));

        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        userService.createOrUpdateUserFromSocialIdentityProvider(identityProvider, userInfo);


        //verify group creations
        verify(membershipService, times(2)).addOrUpdateMember(
                any(MembershipService.MembershipReference.class),
                any(MembershipService.MembershipUser.class),
                any(MembershipService.MembershipRole.class));
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
    }

    private void mockGroupsMapping() {
        GroupMappingEntity condition1 = new GroupMappingEntity();
        condition1.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition1.setGroups(Arrays.asList("Example group", "soft user"));

        GroupMappingEntity condition2 = new GroupMappingEntity();
        condition2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        condition2.setGroups(Collections.singletonList("Others"));

        GroupMappingEntity condition3 = new GroupMappingEntity();
        condition3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        condition3.setGroups(Collections.singletonList("Api consumer"));
        
        when(identityProvider.getGroupMappings()).thenReturn(Arrays.asList(condition1, condition2, condition3));
    }

    private void mockRolesMapping() {
        RoleMappingEntity role1 = new RoleMappingEntity();
        role1.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_5' && #jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        role1.setManagement("ADMIN");
        role1.setPortal("USER");

        RoleMappingEntity role2 = new RoleMappingEntity();
        role2.setCondition("{#jsonPath(#profile, '$.identity_provider_id') == 'idp_6'}");
        role2.setPortal("USER");

        RoleMappingEntity role3 = new RoleMappingEntity();
        role3.setCondition("{#jsonPath(#profile, '$.job_id') != 'API_BREAKER'}");
        role3.setPortal("USER");
        when(identityProvider.getRoleMappings()).thenReturn(Arrays.asList(role1, role2, role3));

    }

    private RoleEntity mockRoleEntity(io.gravitee.rest.api.model.permissions.RoleScope scope, String name) {
        RoleEntity role = new RoleEntity();
        role.setScope(scope);
        role.setName(name);
        return role;
    }

    private GroupEntity mockGroupEntity( String id, String name) {
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
}
