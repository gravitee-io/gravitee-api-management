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
package io.gravitee.rest.api.portal.rest.mapper;

import static io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper.usersURL;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.application.*;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationLinks;
import io.gravitee.rest.api.portal.rest.model.Group;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.UserService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriInfo;
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

    public Application convert(ApplicationListItem applicationListItem, UriInfo uriInfo) {
        final Application application = new Application();
        application.setApplicationType(applicationListItem.getType());
        application.setCreatedAt(applicationListItem.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setDescription(applicationListItem.getDescription());
        Set<String> groupEntities = applicationListItem.getGroups();
        if (groupEntities != null && !groupEntities.isEmpty()) {
            List<Group> groups = groupEntities
                .stream()
                .map(groupService::findById)
                .map(groupEntity -> new Group().id(groupEntity.getId()).name(groupEntity.getName()))
                .collect(Collectors.toList());
            application.setGroups(groups);
        }
        application.setId(applicationListItem.getId());
        application.setName(applicationListItem.getName());

        UserEntity primaryOwnerUserEntity = userService.findById(applicationListItem.getPrimaryOwner().getId());
        User owner = userMapper.convert(primaryOwnerUserEntity);
        owner.setLinks(
            userMapper.computeUserLinks(
                usersURL(uriInfo.getBaseUriBuilder(), primaryOwnerUserEntity.getId()),
                primaryOwnerUserEntity.getUpdatedAt()
            )
        );
        application.setOwner(owner);

        application.setUpdatedAt(applicationListItem.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        ApplicationSettings settings = applicationListItem.getSettings();
        application.setHasClientId(
            settings != null &&
            (
                (
                    settings.getoAuthClient() != null &&
                    settings.getoAuthClient().getClientId() != null &&
                    !settings.getoAuthClient().getClientId().isEmpty()
                ) ||
                (settings.getApp() != null && settings.getApp().getClientId() != null && !settings.getApp().getClientId().isEmpty())
            )
        );
        return application;
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

    public Application convert(ApplicationEntity applicationEntity, UriInfo uriInfo) {
        final Application application = new Application();

        application.setApplicationType(applicationEntity.getType());
        application.setCreatedAt(applicationEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setDescription(applicationEntity.getDescription());
        Set<String> groupEntities = applicationEntity.getGroups();
        if (groupEntities != null && !groupEntities.isEmpty()) {
            List<Group> groups = groupEntities
                .stream()
                .map(groupService::findById)
                .map(groupEntity -> new Group().id(groupEntity.getId()).name(groupEntity.getName()))
                .collect(Collectors.toList());
            application.setGroups(groups);
        }

        application.setId(applicationEntity.getId());
        application.setName(applicationEntity.getName());

        UserEntity primaryOwnerUserEntity = userService.findById(applicationEntity.getPrimaryOwner().getId());
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
            io.gravitee.rest.api.portal.rest.model.ApplicationSettings appSettings = new io.gravitee.rest.api.portal.rest.model.ApplicationSettings();

            final SimpleApplicationSettings simpleAppEntitySettings = applicationEntitySettings.getApp();
            if (simpleAppEntitySettings != null) {
                appSettings.app(
                    new io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings()
                        .clientId(simpleAppEntitySettings.getClientId())
                        .type(simpleAppEntitySettings.getType())
                );
                application.setHasClientId(simpleAppEntitySettings.getClientId() != null);
            } else {
                final OAuthClientSettings oAuthClientEntitySettings = applicationEntitySettings.getoAuthClient();

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
            application.setSettings(appSettings);
        }

        return application;
    }
}
