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
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import io.gravitee.common.http.MediaType;
import io.gravitee.el.TemplateEngine;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.*;
import io.gravitee.management.rest.resource.auth.oauth2.AuthorizationServerConfigurationParser;
import io.gravitee.management.rest.resource.auth.oauth2.ExpressionMapping;
import io.gravitee.management.rest.resource.auth.oauth2.ServerConfiguration;
import io.gravitee.management.rest.resource.auth.oauth2.UserMapping;
import io.gravitee.management.security.authentication.AuthenticationProvider;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.RoleService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Singleton
@Path("/auth/oauth2")
@Api(tags = {"Authentication"})
public class OAuth2AuthenticationResource extends AbstractAuthenticationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2AuthenticationResource.class);

    static class UserProfile {
        public static final String ID = "id";
        public static final String FIRSTNAME = "firstName";
        public static final String LASTNAME = "lastName";
        public static final String PICTURE = "picture";
        public static final String EMAIL = "email";
        public static final String USERNAME = "username";
    }

    @Inject
    @Named("oauth2")
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private GroupService groupService;

    @Autowired
    private RoleService roleService;

    @Autowired
    protected MembershipService membershipService;

    private AuthorizationServerConfigurationParser authorizationServerConfigurationParser = new AuthorizationServerConfigurationParser();

    private Client client;

    private ServerConfiguration serverConfiguration;

    public OAuth2AuthenticationResource() {
        this.client = ClientBuilder.newClient();
    }

    @PostConstruct
    public void init() {
        serverConfiguration = authorizationServerConfigurationParser.parseConfiguration(authenticationProvider.configuration());
    }

    @POST
    @Path("exchange")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenExchange(@QueryParam(value = "token") String token,
                                  @Context final HttpServletResponse servletResponse) throws IOException {
        // Step1. Check the token by invoking the introspection endpoint
        final MultivaluedStringMap introspectData = new MultivaluedStringMap();
        introspectData.add(TOKEN, token);
        Response response = client
                .target(serverConfiguration.getTokenIntrospectionEndpoint())
                .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION,
                        String.format("Basic %s",
                                Base64.getEncoder().encodeToString(
                                        (serverConfiguration.getClientId() + ':' + serverConfiguration.getClientSecret()).getBytes())))
                .post(Entity.form(introspectData));
        introspectData.clear();

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonNode introspectPayload = response.readEntity(JsonNode.class);
            boolean active = introspectPayload.path("active").asBoolean(true);

            if (active) {
                return authenticateUser(token, servletResponse);
            } else {
                return Response
                        .status(Response.Status.UNAUTHORIZED)
                        .entity(introspectPayload)
                        .build();
            }
        }

        return Response
                .status(response.getStatusInfo())
                .entity(response.getEntity())
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response exchangeAuthorizationCode(@Valid final Payload payload,
                                              @Context final HttpServletResponse servletResponse) throws IOException {
        // Step 1. Exchange authorization code for access token.
        final MultivaluedStringMap accessData = new MultivaluedStringMap();
        accessData.add(CLIENT_ID_KEY, payload.getClientId());
        accessData.add(REDIRECT_URI_KEY, payload.getRedirectUri());
        accessData.add(CLIENT_SECRET, serverConfiguration.getClientSecret());
        accessData.add(CODE_KEY, payload.getCode());
        accessData.add(GRANT_TYPE_KEY, AUTH_CODE);
        Response response = client.target(serverConfiguration.getTokenEndpoint())
                .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(accessData));
        accessData.clear();

        final String accessToken = (String) getResponseEntity(response).get(serverConfiguration.getAccessTokenProperty());
        return authenticateUser(accessToken, servletResponse);
    }

    /**
     * Retrieve profile information about the authenticated oauth end-user and authenticate it in Gravitee.
     *
     * @return
     */
    private Response authenticateUser(String accessToken, final HttpServletResponse servletResponse) throws IOException {
        // Step 2. Retrieve profile information about the authenticated end-user.
        Response response = client
                .target(serverConfiguration.getUserInfoEndpoint())
                .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION,
                        String.format(
                                serverConfiguration.getAuthorizationHeader(),
                                accessToken))
                .get();

        // Step 3. Process the authenticated user.
        final String userInfo = getResponseEntityAsString(response);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return processUser(userInfo, servletResponse);
        }

        return Response.status(response.getStatusInfo()).build();
    }

    private Response processUser(String userInfo, final HttpServletResponse servletResponse) {
        HashMap<String, String> attrs = getUserProfileAttrs(userInfo);

        String username = attrs.get(UserProfile.EMAIL);
        if (username == null) {
            if (serverConfiguration.getUserMapping().isEmailRequired()) {
                throw new BadRequestException("No public email linked to your account");
            } else {
                username = attrs.get(UserProfile.USERNAME);
                if (username == null) {
                    username = attrs.get(UserProfile.ID);
                    if (username == null) {
                        throw new BadRequestException("No public email nor username nor ID linked to your account");
                    }
                }
            }
        }

        //set user to Authentication Context
        UserDetails userDetails = new UserDetails(username, "", Collections.emptyList());
        userDetails.setEmail(attrs.get(UserProfile.EMAIL));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        try {
            UserEntity registeredUser = userService.findByUsername(username, false);
            userDetails.setUsername(registeredUser.getId());
        } catch (UserNotFoundException unfe) {
            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setUsername(username);
            newUser.setEmail(attrs.get(UserProfile.EMAIL));
            newUser.setSource(AuthenticationSource.OAUTH2.getName());

            if (attrs.get(UserProfile.ID) != null) {
                newUser.setSourceId(attrs.get(UserProfile.ID));
            }
            if (attrs.get(UserProfile.LASTNAME) != null) {
                newUser.setLastname(attrs.get(UserProfile.LASTNAME));
            }
            if (attrs.get(UserProfile.FIRSTNAME) != null) {
                newUser.setFirstname(attrs.get(UserProfile.FIRSTNAME));
            }
            if (attrs.get(UserProfile.PICTURE) != null) {
                newUser.setPicture(attrs.get(UserProfile.PICTURE));
            }

            List<ExpressionMapping> groupsMapping = serverConfiguration.getGroupsMapping();
            List<ExpressionMapping> rolesMapping = serverConfiguration.getRolesMapping();

            if (!groupsMapping.isEmpty()) {
                // Can fail if a group in config does not exist in gravitee --> HTTP 500
                Set<GroupEntity> groupsToAdd = getGroupsToAddUser(username, groupsMapping, userInfo);

                UserEntity createdUser = userService.create(newUser, true);
                userDetails.setUsername(createdUser.getId());

                addUserToApiAndAppGroupsWithDefaultRole(createdUser.getId(), groupsToAdd);
            } else {
                UserEntity createdUser = userService.create(newUser, true);
                userDetails.setUsername(createdUser.getId());
            }

            if (!rolesMapping.isEmpty()) {
                Set<RoleEntity> rolesToAdd = getRolesToAddUser(username, rolesMapping, userInfo);
                addRolesToUser(userDetails.getUsername(), rolesToAdd);
            }
        }

        // User refresh
        UpdateUserEntity user = new UpdateUserEntity();
        user.setUsername(username);

        if (attrs.get(UserProfile.LASTNAME) != null) {
            user.setLastname(attrs.get(UserProfile.LASTNAME));
        }
        if (attrs.get(UserProfile.FIRSTNAME) != null) {
            user.setFirstname(attrs.get(UserProfile.FIRSTNAME));
        }
        if (attrs.get(UserProfile.PICTURE) != null) {
            user.setPicture(attrs.get(UserProfile.PICTURE));
        }

        UserEntity updatedUser = userService.update(user);

        return connectUser(updatedUser.getId(), servletResponse);
    }

    private HashMap<String, String> getUserProfileAttrs(String userInfo) {
        ReadContext userInfoPath = JsonPath.parse(userInfo);
        HashMap<String, String> map = new HashMap<>();

        UserMapping userMapping = serverConfiguration.getUserMapping();
        String emailMap = userMapping.getEmail();
        String idMap = userMapping.getId();
        String lastNameMap = userMapping.getLastname();
        String firstNameMap = userMapping.getFirstname();
        String pictureMap = userMapping.getPicture();
        String usernameMap = userMapping.getUsername();

        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put(UserProfile.EMAIL, emailMap);
        hashMap.put(UserProfile.ID, idMap);
        hashMap.put(UserProfile.LASTNAME, lastNameMap);
        hashMap.put(UserProfile.FIRSTNAME, firstNameMap);
        hashMap.put(UserProfile.PICTURE, pictureMap);
        hashMap.put(UserProfile.USERNAME, usernameMap);

        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            String field = entry.getKey();
            String mapping = entry.getValue();

            if (mapping != null) {
                try {
                    map.put(field, userInfoPath.read(mapping));
                } catch (PathNotFoundException e) {
                    LOGGER.error("Using json-path: \"{}\", no fields are located in {}", mapping, userInfo);
                }
            }
        }

        return map;
    }

    private void addUserToApiAndAppGroupsWithDefaultRole(String userId, Collection<GroupEntity> groupsToAdd) {
        // Get the default role from system
        List<RoleEntity> roleEntities = roleService.findDefaultRoleByScopes(RoleScope.API, RoleScope.APPLICATION);

        // Add groups to user
        for (GroupEntity groupEntity : groupsToAdd) {
            for (RoleEntity roleEntity : roleEntities) {
                String defaultRole = roleEntity.getName();

                // If defined, get the override default role at the group level
                if (groupEntity.getRoles() != null) {
                    String groupDefaultRole = groupEntity.getRoles().get(io.gravitee.management.model.permissions.RoleScope.valueOf(roleEntity.getScope().name()));
                    if (groupDefaultRole != null) {
                        defaultRole = groupDefaultRole;
                    }
                }

                membershipService.addOrUpdateMember(
                        new MembershipService.MembershipReference(MembershipReferenceType.GROUP, groupEntity.getId()),
                        new MembershipService.MembershipUser(userId, null),
                        new MembershipService.MembershipRole(mapScope(roleEntity.getScope()), defaultRole));
            }
        }
    }

    private void addRolesToUser(String userId, Collection<RoleEntity> rolesToAdd) {
        // add roles to user
        for (RoleEntity roleEntity : rolesToAdd) {
            membershipService.addOrUpdateMember(
                    new MembershipService.MembershipReference(
                            io.gravitee.management.model.permissions.RoleScope.MANAGEMENT == roleEntity.getScope() ?
                                    MembershipReferenceType.MANAGEMENT : MembershipReferenceType.PORTAL,
                            MembershipDefaultReferenceId.DEFAULT.name()),
                    new MembershipService.MembershipUser(userId, null),
                    new MembershipService.MembershipRole(
                            RoleScope.valueOf(roleEntity.getScope().name()),
                            roleEntity.getName()));
        }
    }

    private Set<GroupEntity> getGroupsToAddUser(String username, List<ExpressionMapping> mappings, String userInfo) {
        Set<GroupEntity> groupsToAdd = new HashSet<>();

        for (ExpressionMapping mapping : mappings) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable("profile", userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(username, match, mapping);

            //get groups
            if (match) {
                for (String groupName : mapping.getValues()) {
                    List<GroupEntity> groupEntities = groupService.findByName(groupName);

                    if (groupEntities.isEmpty()) {
                        LOGGER.error("Unable to create user, missing group in repository : {}", groupName);
                        throw new InternalServerErrorException();
                    } else if (groupEntities.size() > 1) {
                        LOGGER.warn("There's more than a group found in repository for name : {}", groupName);
                    }

                    GroupEntity groupEntity = groupEntities.get(0);
                    groupsToAdd.add(groupEntity);
                }
            }
        }
        return groupsToAdd;
    }

    private Set<RoleEntity> getRolesToAddUser(String username, List<ExpressionMapping> mappings, String userInfo) {
        Set<RoleEntity> rolesToAdd = new HashSet<>();

        for (ExpressionMapping mapping : mappings) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable("profile", userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(username, match, mapping);

            // Get roles
            if (match) {
                for (String roleName : mapping.getValues()) {
                    String [] roleAttributes = roleName.split(":");

                    try {
                        RoleEntity roleEntity = roleService.findById(
                                RoleScope.valueOf(roleAttributes[0].toUpperCase()),
                                roleAttributes[1].toUpperCase());
                        rolesToAdd.add(roleEntity);
                    } catch (RoleNotFoundException rnfe) {
                        LOGGER.error("Unable to create user, missing role in repository : {}", roleName);
                    }
                }
            }
        }
        return rolesToAdd;
    }

    private void trace(String username, boolean match, ExpressionMapping mapping) {
        if (LOGGER.isDebugEnabled()) {
            if (match) {
                LOGGER.debug("the expression {} match on {} user's info ", mapping.getCondition(), username);
            } else {
                LOGGER.debug("the expression {} didn't match {} on user's info ", mapping.getCondition(), username);
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
