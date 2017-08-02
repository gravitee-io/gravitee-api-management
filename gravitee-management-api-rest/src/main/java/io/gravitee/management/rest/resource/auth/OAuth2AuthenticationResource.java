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

import io.gravitee.common.http.MediaType;
import io.gravitee.el.TemplateEngine;
import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.NewExternalUserEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UpdateUserEntity;
import io.gravitee.management.rest.resource.auth.oauth2.AuthorizationServerConfigurationParser;
import io.gravitee.management.rest.resource.auth.oauth2.ExpressionMapping;
import io.gravitee.management.rest.resource.auth.oauth2.ServerConfiguration;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private Response processUser(String userInfo)  throws IOException {

        Map<String, Object> userInfosAsMap = getEntity(userInfo);

        String username = (String) userInfosAsMap.get(serverConfiguration.getUserMapping().getEmail());

        if (username == null) {
            throw new BadRequestException("No public email linked to your account");
        }

        try {
            userService.findByName(username, false);
        } catch (UserNotFoundException unfe) {

            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setUsername(username);
            newUser.setSource(AuthenticationSource.OAUTH2.getName());
            newUser.setSourceId((String) userInfosAsMap.get(serverConfiguration.getUserMapping().getId()));
            newUser.setLastname((String) userInfosAsMap.get(serverConfiguration.getUserMapping().getLastname()));
            newUser.setFirstname((String) userInfosAsMap.get(serverConfiguration.getUserMapping().getFirstname()));
            newUser.setEmail(username);

            List<ExpressionMapping> mappings = serverConfiguration.getGroupsMapping();

            if(mappings.isEmpty()) {
                userService.create(newUser, true);
            } else {
                //can fail if a group in config does not exist in gravitee --> HTTP 500
                Set<GroupEntity> groupsToAdd = getGroupsToAddUser(username, mappings, userInfo);

                userService.create(newUser, false);

                addUserToApiAndAppGroupsWithDefaultRole(newUser, groupsToAdd);
            }
        }

        // User refresh
        UpdateUserEntity user = new UpdateUserEntity();
        user.setUsername(username);
        user.setPicture((String) userInfosAsMap.get(serverConfiguration.getUserMapping().getPicture()));

        userService.update(user);

        return connectUser(username);
    }

    private void addUserToApiAndAppGroupsWithDefaultRole(NewExternalUserEntity newUser, Collection<GroupEntity> groupsToAdd) {
        List<RoleEntity> roleEntities = roleService.findDefaultRoleByScopes(RoleScope.API,RoleScope.APPLICATION);

        //add groups to user
        for(GroupEntity groupEntity : groupsToAdd) {
            for(RoleEntity roleEntity : roleEntities) {
                membershipService.addOrUpdateMember(MembershipReferenceType.GROUP, groupEntity.getId(), newUser.getUsername(), mapScope(roleEntity.getScope()), roleEntity.getName());
            }
        }
    }

    private Set<GroupEntity> getGroupsToAddUser(String userName, List<ExpressionMapping> mappings, String userInfo) {
        Set<GroupEntity> groupsToAdd = new HashSet<>();

        for (ExpressionMapping mapping: mappings) {

            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable("profile",userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(userName, match, mapping);

            //get groups
            if(match) {
                for(String groupName : mapping.getGroupNames()) {
                    List<GroupEntity> groupEntities = groupService.findByName(groupName);

                    if(groupEntities.isEmpty()) {
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

    private void trace(String userName, boolean match, ExpressionMapping mapping) {
        if(LOGGER.isDebugEnabled()) {
            if(match) {
                LOGGER.debug("the expression {} match on {} user's info ", mapping.getCondition(), userName);
            } else {
                LOGGER.debug("the expression {} didn't match {} on user's info ", mapping.getCondition(), userName);
            }
        }
    }

    private RoleScope mapScope(io.gravitee.management.model.permissions.RoleScope scope) {
        if(io.gravitee.management.model.permissions.RoleScope.API == scope) {
            return RoleScope.API;
        } else {
            return RoleScope.APPLICATION;
        }
    }

}
