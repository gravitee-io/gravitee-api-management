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
import io.gravitee.management.service.exceptions.UserNotFoundException;
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
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response oauth2(@Valid final Payload payload) throws IOException {
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

        // Step 2. Retrieve profile information about the current user.
        final String accessToken = (String) getResponseEntity(response).get(serverConfiguration.getAccessTokenProperty());
        response = client
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
            return processUser(userInfo);
        }

        return Response.status(response.getStatusInfo()).build();
    }

    private Response processUser(String userInfo) throws IOException {
        HashMap<String, String> attrs = getUserProfileAttrs(userInfo);
        List<ExpressionMapping> mappings = serverConfiguration.getGroupsMapping();

        String username = attrs.get(UserProfile.EMAIL);
        if (username == null) {
            throw new BadRequestException("No public email linked to your account");
        }

        //set user to Authentication Context
        UserDetails userDetails = new UserDetails(username, "", Collections.emptyList());
        userDetails.setEmail(username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        try {
            UserEntity registeredUser = userService.findByUsername(username, false);
            userDetails.setUsername(registeredUser.getId());
        } catch (UserNotFoundException unfe) {
            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setUsername(username);
            newUser.setEmail(username);
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

            if (!mappings.isEmpty()) {
                //can fail if a group in config does not exist in gravitee --> HTTP 500
                Set<GroupEntity> groupsToAdd = getGroupsToAddUser(username, mappings, userInfo);

                UserEntity createdUser = userService.create(newUser, true);
                userDetails.setUsername(createdUser.getId());

                addUserToApiAndAppGroupsWithDefaultRole(createdUser.getId(), groupsToAdd);
            } else {
                UserEntity createdUser = userService.create(newUser, true);
                userDetails.setUsername(createdUser.getId());
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

        return connectUser(updatedUser.getId());
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

        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put(UserProfile.EMAIL, emailMap);
        hashMap.put(UserProfile.ID, idMap);
        hashMap.put(UserProfile.LASTNAME, lastNameMap);
        hashMap.put(UserProfile.FIRSTNAME, firstNameMap);
        hashMap.put(UserProfile.PICTURE, pictureMap);

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
        List<RoleEntity> roleEntities = roleService.findDefaultRoleByScopes(RoleScope.API, RoleScope.APPLICATION);

        //add groups to user
        for (GroupEntity groupEntity : groupsToAdd) {
            for (RoleEntity roleEntity : roleEntities) {
                membershipService.addOrUpdateMember(
                        new MembershipService.MembershipReference(MembershipReferenceType.GROUP, groupEntity.getId()),
                        new MembershipService.MembershipUser(userId, null),
                        new MembershipService.MembershipRole(mapScope(roleEntity.getScope()), roleEntity.getName()));
            }
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
                for (String groupName : mapping.getGroupNames()) {
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
