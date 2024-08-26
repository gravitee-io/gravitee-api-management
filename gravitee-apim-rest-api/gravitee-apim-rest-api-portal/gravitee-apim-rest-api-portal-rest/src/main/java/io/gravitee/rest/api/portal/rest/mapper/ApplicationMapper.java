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

import static io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper.usersURL;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class ApplicationMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    public Application convert(ExecutionContext executionContext, ApplicationListItem applicationListItem, UriInfo uriInfo) {
        return convert(executionContext, applicationListItem, uriInfo, true);
    }

    public ApplicationLinks computeApplicationLinks(String basePath, OffsetDateTime updateDate) {
        ApplicationLinks applicationLinks = new ApplicationLinks();
        applicationLinks.setMembers(basePath + "/members");
        applicationLinks.setNotifications(basePath + "/notifications");
        applicationLinks.setPicture(basePath + "/picture" + (updateDate == null ? "" : "?" + updateDate.hashCode()));
        applicationLinks.setBackground(basePath + "/background" + (updateDate == null ? "" : "?" + updateDate.hashCode()));
        applicationLinks.setSelf(basePath);

        return applicationLinks;
    }

    public Application convert(ExecutionContext executionContext, ApplicationEntity applicationEntity, UriInfo uriInfo) {
        final Application application = new Application();

        application.setApplicationType(applicationEntity.getType());
        application.setCreatedAt(applicationEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setDescription(applicationEntity.getDescription());
        application.setDomain(applicationEntity.getDomain());
        Set<String> groupEntities = applicationEntity.getGroups();
        if (groupEntities != null && !groupEntities.isEmpty()) {
            List<Group> groups = groupEntities
                .stream()
                .map(groupId -> groupService.findById(executionContext, groupId))
                .map(groupEntity -> new Group().id(groupEntity.getId()).name(groupEntity.getName()))
                .collect(Collectors.toList());
            application.setGroups(groups);
        }

        application.setId(applicationEntity.getId());
        application.setName(applicationEntity.getName());

        UserEntity primaryOwnerUserEntity = userService.findById(executionContext, applicationEntity.getPrimaryOwner().getId());
        User owner = userMapper.convert(primaryOwnerUserEntity);
        owner.setLinks(
            userMapper.computeUserLinks(
                usersURL(uriInfo.getBaseUriBuilder(), primaryOwnerUserEntity.getId()),
                primaryOwnerUserEntity.getUpdatedAt()
            )
        );
        application.setOwner(owner);

        application.setUpdatedAt(applicationEntity.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setPicture(applicationEntity.getPicture());
        application.setBackground(applicationEntity.getBackground());

        final ApplicationSettings applicationEntitySettings = applicationEntity.getSettings();
        if (applicationEntitySettings != null) {
            io.gravitee.rest.api.portal.rest.model.ApplicationSettings appSettings =
                new io.gravitee.rest.api.portal.rest.model.ApplicationSettings();

            final SimpleApplicationSettings simpleAppEntitySettings = applicationEntitySettings.getApp();
            if (simpleAppEntitySettings != null) {
                appSettings.app(
                    new io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings()
                        .clientId(simpleAppEntitySettings.getClientId())
                        .type(simpleAppEntitySettings.getType())
                );
                application.setHasClientId(simpleAppEntitySettings.getClientId() != null);
            } else {
                final OAuthClientSettings oAuthClientEntitySettings = applicationEntitySettings.getOauth();

                appSettings.oauth(
                    new io.gravitee.rest.api.portal.rest.model.OAuthClientSettings()
                        .applicationType(oAuthClientEntitySettings.getApplicationType())
                        .clientId(oAuthClientEntitySettings.getClientId())
                        .clientSecret(oAuthClientEntitySettings.getClientSecret())
                        .clientUri(oAuthClientEntitySettings.getClientUri())
                        .logoUri(oAuthClientEntitySettings.getLogoUri())
                        .grantTypes(oAuthClientEntitySettings.getGrantTypes())
                        .redirectUris(oAuthClientEntitySettings.getRedirectUris())
                        .responseTypes(oAuthClientEntitySettings.getResponseTypes())
                        .renewClientSecretSupported(oAuthClientEntitySettings.isRenewClientSecretSupported())
                );
                application.setHasClientId(oAuthClientEntitySettings.getClientId() != null);
            }
            if (applicationEntitySettings.getTls() != null) {
                appSettings.setTls(new TlsClientSettings().clientCertificate(applicationEntitySettings.getTls().getClientCertificate()));
            }
            application.setSettings(appSettings);
        }

        if (applicationEntity.getApiKeyMode() != null) {
            application.setApiKeyMode(ApiKeyModeEnum.valueOf(applicationEntity.getApiKeyMode().name()));
        } else {
            application.setApiKeyMode(ApiKeyModeEnum.UNSPECIFIED);
        }
        return application;
    }

    public Application convert(
        ExecutionContext executionContext,
        ApplicationListItem applicationListItem,
        UriInfo uriInfo,
        boolean withUserDetails
    ) {
        final Application application = new Application();
        application.setApplicationType(applicationListItem.getType());
        application.setCreatedAt(applicationListItem.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setDescription(applicationListItem.getDescription());
        application.setDomain(applicationListItem.getDomain());
        Set<String> groupEntities = applicationListItem.getGroups();
        if (groupEntities != null && !groupEntities.isEmpty()) {
            List<Group> groups = groupEntities
                .stream()
                .map(groupId -> groupService.findById(executionContext, groupId))
                .map(groupEntity -> new Group().id(groupEntity.getId()).name(groupEntity.getName()))
                .collect(Collectors.toList());
            application.setGroups(groups);
        }
        application.setId(applicationListItem.getId());
        application.setName(applicationListItem.getName());

        PrimaryOwnerEntity primaryOwner = applicationListItem.getPrimaryOwner();
        User owner;
        if (withUserDetails) {
            UserEntity primaryOwnerUserEntity = userService.findById(executionContext, primaryOwner.getId());
            owner = userMapper.convert(primaryOwnerUserEntity);
            owner.setLinks(
                userMapper.computeUserLinks(
                    usersURL(uriInfo.getBaseUriBuilder(), primaryOwnerUserEntity.getId()),
                    primaryOwnerUserEntity.getUpdatedAt()
                )
            );
        } else {
            owner = userMapper.convert(primaryOwner);
            owner.setLinks(userMapper.computeUserLinks(usersURL(uriInfo.getBaseUriBuilder(), primaryOwner.getId()), null));
        }

        application.setOwner(owner);
        application.setUpdatedAt(applicationListItem.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        ApplicationSettings settings = applicationListItem.getSettings();
        application.setHasClientId(
            settings != null &&
            (
                (
                    settings.getOauth() != null && settings.getOauth().getClientId() != null && !settings.getOauth().getClientId().isEmpty()
                ) ||
                (settings.getApp() != null && settings.getApp().getClientId() != null && !settings.getApp().getClientId().isEmpty())
            )
        );
        if (applicationListItem.getApiKeyMode() != null) {
            application.setApiKeyMode(ApiKeyModeEnum.valueOf(applicationListItem.getApiKeyMode().name()));
        } else {
            application.setApiKeyMode(ApiKeyModeEnum.UNSPECIFIED);
        }
        return application;
    }
}
