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
package io.gravitee.management.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.gravitee.common.http.MediaType;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.function.JsonPathFunction;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.*;
import io.gravitee.management.model.configuration.identity.GroupMappingEntity;
import io.gravitee.management.model.configuration.identity.RoleMappingEntity;
import io.gravitee.management.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.management.rest.utils.BlindTrustManager;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.RoleService;
import io.gravitee.management.service.SocialIdentityProviderService;
import io.gravitee.management.service.exceptions.GroupNotFoundException;
import io.gravitee.management.service.exceptions.RoleNotFoundException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.Api;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Singleton
@Path("/auth/oauth2/{identity}")
@Api(tags = {"Authentication"})
public class OAuth2AuthenticationResource extends AbstractAuthenticationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthenticationResource.class);

    private final static String TEMPLATE_ENGINE_PROFILE_ATTRIBUTE = "profile";

    @Autowired
    private SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private Environment environment;

    private Client client;

    // Dirty hack: only used to force class loading
    static {
        try {
            LOGGER.trace("Loading class to initialize properly JsonPath Cache provider: " +
                    Class.forName(JsonPathFunction.class.getName()));
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static final String ACCESS_TOKEN_PROPERTY = "access_token";

    @PostConstruct
    public void initClient() throws NoSuchAlgorithmException, KeyManagementException {
        final boolean trustAllEnabled = environment.getProperty("security.trustAll", Boolean.class, false);
        final ClientBuilder builder = ClientBuilder.newBuilder();
        if (trustAllEnabled) {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, new TrustManager[]{new BlindTrustManager()}, null);
            builder.sslContext(sc);
        }

        this.client = builder.build();
    }

    @POST
    @Path("exchange")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenExchange(
            @PathParam(value = "identity") final String identity,
            @QueryParam(value = "token") final String token,
            @Context final HttpServletResponse servletResponse) throws IOException {
        SocialIdentityProviderEntity identityProvider = socialIdentityProviderService.findById(identity);

        if (identityProvider != null) {
            if (identityProvider.getTokenIntrospectionEndpoint() != null) {
                // Step1. Check the token by invoking the introspection endpoint
                final MultivaluedStringMap introspectData = new MultivaluedStringMap();
                introspectData.add(TOKEN, token);
                Response response = client
                        //TODO: what is the correct introspection URL here ?
                        .target(identityProvider.getTokenIntrospectionEndpoint())
                        .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                        .header(HttpHeaders.AUTHORIZATION,
                                String.format("Basic %s",
                                        Base64.getEncoder().encodeToString(
                                                (identityProvider.getClientId() + ':' + identityProvider.getClientSecret()).getBytes())))
                        .post(Entity.form(introspectData));
                introspectData.clear();

                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    JsonNode introspectPayload = response.readEntity(JsonNode.class);
                    boolean active = introspectPayload.path("active").asBoolean(true);

                    if (active) {
                        return authenticateUser(identityProvider, servletResponse, token, null);
                    } else {
                        return Response
                                .status(Response.Status.UNAUTHORIZED)
                                .entity(introspectPayload)
                                .build();
                    }
                } else {
                    LOGGER.error("Token exchange failed with status {}: {}\n{}", response.getStatus(), response.getStatusInfo(), getResponseEntityAsString(response));
                }

                return Response
                        .status(response.getStatusInfo())
                        .entity(response.getEntity())
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Token exchange is not supported for this identity provider")
                        .build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response exchangeAuthorizationCode(
            @PathParam(value = "identity") String identity,
            @Valid @NotNull final Payload payload,
            @Context final HttpServletResponse servletResponse) throws IOException {
        SocialIdentityProviderEntity identityProvider = socialIdentityProviderService.findById(identity);

        if (identityProvider != null) {
            // Step 1. Exchange authorization code for access token.
            final MultivaluedStringMap accessData = new MultivaluedStringMap();
            accessData.add(CLIENT_ID_KEY, payload.getClientId());
            accessData.add(REDIRECT_URI_KEY, payload.getRedirectUri());
            accessData.add(CLIENT_SECRET, identityProvider.getClientSecret());
            accessData.add(CODE_KEY, payload.getCode());
            accessData.add(GRANT_TYPE_KEY, AUTH_CODE);

            Response response = client.target(identityProvider.getTokenEndpoint())
                    .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.form(accessData));
            accessData.clear();

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final String accessToken = (String) getResponseEntity(response).get(ACCESS_TOKEN_PROPERTY);
                return authenticateUser(identityProvider, servletResponse, accessToken, payload.getState());
            } else {
                LOGGER.error("Exchange authorization code failed with status {}: {}\n{}", response.getStatus(), response.getStatusInfo(), getResponseEntityAsString(response));
            }
            return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Retrieve profile information about the authenticated oauth end-user and authenticate it in Gravitee.
     *
     * @return
     */
    private Response authenticateUser(final SocialIdentityProviderEntity socialProvider,
                                      final HttpServletResponse servletResponse,
                                      final String accessToken,
                                      final String state) throws IOException {
        // Step 2. Retrieve profile information about the authenticated end-user.
        Response response = client
                .target(socialProvider.getUserInfoEndpoint())
                .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, String.format(socialProvider.getAuthorizationHeader(), accessToken))
                .get();


        // Step 3. Process the authenticated user.
        final String userInfo = getResponseEntityAsString(response);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return processUser(socialProvider, servletResponse, userInfo, state);
        } else {
            LOGGER.error("User info failed with status {}: {}\n{}", response.getStatus(), response.getStatusInfo(), userInfo);

        }

        return Response.status(response.getStatusInfo()).build();
    }

    private Response processUser(final SocialIdentityProviderEntity socialProvider, final HttpServletResponse servletResponse, final String userInfo, final String state) {
        Map<String, String> attrs = extractUserProfileAttributes(socialProvider.getUserProfileMapping(), userInfo);

        String email = attrs.get(SocialIdentityProviderEntity.UserProfile.EMAIL);
        if (email == null && socialProvider.isEmailRequired()) {
            throw new BadRequestException("No public email linked to your account");
        }

        boolean created = false;
        UserEntity user;

        // Compute group and role mappings
        // This is done BEFORE updating or creating the user account to ensure this one is properly created with correct
        // information (ie. mappings)
        Set<GroupEntity> userGroups = computeUserGroupsFromProfile(email, socialProvider.getGroupMappings(), userInfo);
        Set<RoleEntity> userRoles = computeUserRolesFromProfile(email, socialProvider.getRoleMappings(), userInfo);

        try {
            user = userService.findBySource(socialProvider.getId(), attrs.get(SocialIdentityProviderEntity.UserProfile.ID), false);

            // Update user information from its user info profile
            UpdateUserEntity updatedUser = new UpdateUserEntity();

            // User email is invariant
            updatedUser.setEmail(email);

            if (attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME) != null) {
                updatedUser.setLastname(attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME));
            }
            if (attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME) != null) {
                updatedUser.setFirstname(attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME));
            }
            if (attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE) != null) {
                updatedUser.setPicture(attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE));
            }

            user = userService.update(user.getId(), updatedUser);
        } catch (UserNotFoundException unfe) {
            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setEmail(email);
            newUser.setSource(socialProvider.getId());

            if (attrs.get(SocialIdentityProviderEntity.UserProfile.ID) != null) {
                newUser.setSourceId(attrs.get(SocialIdentityProviderEntity.UserProfile.ID));
            }
            if (attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME) != null) {
                newUser.setLastname(attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME));
            }
            if (attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME) != null) {
                newUser.setFirstname(attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME));
            }
            if (attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE) != null) {
                newUser.setPicture(attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE));
            }

            user = userService.create(newUser, true);
            created = true;
        }

        // Memberships must be refresh only when it is a user creation context or mappings should be synced during
        // later authentication
        List<MembershipService.Membership> groupMemberships = refreshUserGroups(user.getId(), socialProvider.getId(), userGroups);
        List<MembershipService.Membership> roleMemberships = refreshUserRoles(user.getId(), socialProvider.getId(), userRoles);

        if (created || socialProvider.isSyncMappings()) {
            refreshUserMemberships(user.getId(), socialProvider.getId(), groupMemberships, MembershipReferenceType.GROUP);
            refreshUserMemberships(user.getId(), socialProvider.getId(), roleMemberships,
                    MembershipReferenceType.MANAGEMENT, MembershipReferenceType.PORTAL);
        }

        final Set<RoleEntity> roles =
                membershipService.getRoles(MembershipReferenceType.PORTAL, singleton(MembershipDefaultReferenceId.DEFAULT.name()), user.getId(), RoleScope.PORTAL);
        roles.addAll(membershipService.getRoles(MembershipReferenceType.MANAGEMENT, singleton(MembershipDefaultReferenceId.DEFAULT.name()), user.getId(), RoleScope.MANAGEMENT));

        final Set<GrantedAuthority> authorities = new HashSet<>();
        if (!roles.isEmpty()) {
            authorities.addAll(commaSeparatedStringToAuthorityList(roles.stream()
                    .map(r -> r.getScope().name() + ':' + r.getName()).collect(Collectors.joining(","))));
        }

        //set user to Authentication Context
        UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
        userDetails.setEmail(email);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        return connectUser(user.getId(), state, servletResponse);
    }

    private Map<String, String> extractUserProfileAttributes(Map<String, String> userProfileMapping, String userInfo) {
        TemplateEngine templateEngine = TemplateEngine.templateEngine();
        templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

        ReadContext userInfoPath = JsonPath.parse(userInfo);
        HashMap<String, String> map = new HashMap<>(userProfileMapping.size());

        for (Map.Entry<String, String> entry : userProfileMapping.entrySet()) {
            String field = entry.getKey();
            String mapping = entry.getValue();

            if (mapping != null && !mapping.isEmpty()) {
                try {
                    if (mapping.contains("{#")) {
                        map.put(field, templateEngine.convert(mapping));
                    } else {
                        map.put(field, userInfoPath.read(mapping).toString());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Using mapping: \"{}\", no fields are located in {}", mapping, userInfo);
                }
            }
        }

        return map;
    }

    private List<MembershipService.Membership> refreshUserGroups(String userId, String identityProviderId, Collection<GroupEntity> userGroups) {
        List<MembershipService.Membership> memberships = new ArrayList<>();

        // Get the default group roles from system
        List<RoleEntity> roleEntities = roleService.findDefaultRoleByScopes(RoleScope.API, RoleScope.APPLICATION);

        // Add groups to user
        for (GroupEntity groupEntity : userGroups) {
            for (RoleEntity roleEntity : roleEntities) {
                String defaultRole = roleEntity.getName();

                // If defined, get the override default role at the group level
                if (groupEntity.getRoles() != null) {
                    String groupDefaultRole = groupEntity.getRoles().get(io.gravitee.management.model.permissions.RoleScope.valueOf(roleEntity.getScope().name()));
                    if (groupDefaultRole != null) {
                        defaultRole = groupDefaultRole;
                    }
                }

                MembershipService.Membership membership = new MembershipService.Membership(
                        new MembershipService.MembershipReference(MembershipReferenceType.GROUP, groupEntity.getId()),
                        new MembershipService.MembershipUser(userId, null),
                        new MembershipService.MembershipRole(mapScope(roleEntity.getScope()), defaultRole));

                membership.setSource(identityProviderId);

                memberships.add(membership);
            }
        }

        return memberships;
    }

    private List<MembershipService.Membership> refreshUserRoles(String userId, String identityProviderId, Collection<RoleEntity> userRoles) {
        return userRoles.stream()
                .map(roleEntity -> {
                    MembershipService.Membership membership = new MembershipService.Membership(
                            new MembershipService.MembershipReference(
                            io.gravitee.management.model.permissions.RoleScope.MANAGEMENT == roleEntity.getScope() ?
                                    MembershipReferenceType.MANAGEMENT : MembershipReferenceType.PORTAL,
                            MembershipDefaultReferenceId.DEFAULT.name()),
                            new MembershipService.MembershipUser(userId, null),
                            new MembershipService.MembershipRole(
                                    RoleScope.valueOf(roleEntity.getScope().name()),
                                    roleEntity.getName()));

                    membership.setSource(identityProviderId);

                    return membership;
                }).collect(Collectors.toList());
    }

    /**
     * Refresh user memberships.
     *
     * @param userId User identifier.
     * @param identityProviderId The identity provider used to authenticate the user.
     * @param memberships List of memberships to associate to the user
     * @param types The types of user memberships to manage
     */
    private void refreshUserMemberships(String userId, String identityProviderId, List<MembershipService.Membership> memberships,
                                        MembershipReferenceType ... types) {
        // Get existing memberships for a given type
        List<UserMembership> userMemberships = new ArrayList<>();

        for (MembershipReferenceType type : types) {
            userMemberships.addAll(membershipService.findUserMembership(userId, type));
        }

        // Delete existing memberships
        userMemberships.forEach(membership -> {
            // Consider only membership "created by" the identity provider
            if (identityProviderId.equals(membership.getSource())) {
                membershipService.deleteMember(
                        MembershipReferenceType.valueOf(membership.getType()),
                        membership.getReference(),
                        userId);
            }
        });

        // Create updated memberships
        memberships.forEach(membership -> membershipService.addOrUpdateMember(
                membership.getReference(),
                membership.getUser(),
                membership.getRole(),
                membership.getSource(),
                false));
    }

    /**
     * Calculate the list of groups to associate to a user according to its OIDC profile (ie. UserInfo)
     *
     * @param userId
     * @param mappings
     * @param userInfo
     * @return
     */
    private Set<GroupEntity> computeUserGroupsFromProfile(String userId, List<GroupMappingEntity> mappings, String userInfo) {
        if (mappings == null || mappings.isEmpty()) {
            return Collections.emptySet();
        }

        Set<GroupEntity> groups = new HashSet<>();

        for (GroupMappingEntity mapping : mappings) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(userId, match, mapping.getCondition());

            // Get groups
            if (match) {
                for (String groupName : mapping.getGroups()) {
                    try {
                        groups.add(groupService.findById(groupName));
                    } catch (GroupNotFoundException gnfe) {
                        LOGGER.error("Unable to create user, missing group in repository : {}", groupName);
                    }
                }
            }
        }

        return groups;
    }

    /**
     * Calculate the list of roles to associate to a user according to its OIDC profile (ie. UserInfo)
     *
     * @param userId
     * @param mappings
     * @param userInfo
     * @return
     */
    private Set<RoleEntity> computeUserRolesFromProfile(String userId, List<RoleMappingEntity> mappings, String userInfo) {
        if (mappings == null || mappings.isEmpty()) {
            return Collections.emptySet();
        }

        Set<RoleEntity> roles = new HashSet<>();

        for (RoleMappingEntity mapping : mappings) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(userId, match, mapping.getCondition());

            // Get roles
            if (match) {
                if (mapping.getPortal() != null) {
                    try {
                        RoleEntity roleEntity = roleService.findById(RoleScope.PORTAL, mapping.getPortal());
                        roles.add(roleEntity);
                    } catch (RoleNotFoundException rnfe) {
                        LOGGER.error("Unable to create user, missing role in repository : {}", mapping.getPortal());
                    }
                }

                if (mapping.getManagement() != null) {
                    try {
                        RoleEntity roleEntity = roleService.findById(RoleScope.MANAGEMENT, mapping.getManagement());
                        roles.add(roleEntity);
                    } catch (RoleNotFoundException rnfe) {
                        LOGGER.error("Unable to create user, missing role in repository : {}", mapping.getManagement());
                    }
                }
            }
        }

        return roles;
    }

    private void trace(String userId, boolean match, String condition) {
        if (LOGGER.isDebugEnabled()) {
            if (match) {
                LOGGER.debug("the expression {} match on {} user's info ", condition, userId);
            } else {
                LOGGER.debug("the expression {} didn't match {} on user's info ", condition, userId);
            }
        }
    }

    private RoleScope mapScope(io.gravitee.management.model.permissions.RoleScope scope) {
        if (io.gravitee.management.model.permissions.RoleScope.API == scope) {
            return RoleScope.API;
        } else {
            return RoleScope.APPLICATION;
        }
    }

}
