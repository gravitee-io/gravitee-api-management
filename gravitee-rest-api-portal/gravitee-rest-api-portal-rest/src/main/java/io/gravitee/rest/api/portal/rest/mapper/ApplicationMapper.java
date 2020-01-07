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

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.application.*;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationLinks;
import io.gravitee.rest.api.portal.rest.model.User;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class ApplicationMapper {

    public Application convert(ApplicationListItem applicationListItem) {
        final Application application = new Application();
        application.setApplicationType(applicationListItem.getType());
        application.setCreatedAt(applicationListItem.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setDescription(applicationListItem.getDescription());
        Set<String> groups = applicationListItem.getGroups();
        if(groups != null) {
            application.setGroups(new ArrayList<String>(groups));
        }
        application.setId(applicationListItem.getId());
        application.setName(applicationListItem.getName());
        User owner = new User();
        final PrimaryOwnerEntity primaryOwner = applicationListItem.getPrimaryOwner();
        owner.id(primaryOwner.getId());
        owner.setDisplayName(primaryOwner.getDisplayName());
        application.setOwner(owner);
        application.setUpdatedAt(applicationListItem.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        ApplicationListItemSettings settings = applicationListItem.getSettings();
        application.setHasClientId(settings != null && settings.getClientId() != null);
        return application;
    }

    public ApplicationLinks computeApplicationLinks(String basePath) {
        ApplicationLinks applicationLinks = new ApplicationLinks();
        applicationLinks.setMembers(basePath+"/members");
        applicationLinks.setNotifications(basePath+"/notifications");
        applicationLinks.setPicture(basePath+"/picture");
        applicationLinks.setSelf(basePath);

        return applicationLinks;
    }

    public Application convert(ApplicationEntity applicationEntity) {
        final Application application = new Application();

        application.setApplicationType(applicationEntity.getType());
        application.setCreatedAt(applicationEntity.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        application.setDescription(applicationEntity.getDescription());
        Set<String> groups = applicationEntity.getGroups();
        if(groups != null) {
            application.setGroups(new ArrayList<String>(groups));
        }
        application.setId(applicationEntity.getId());
        application.setName(applicationEntity.getName());
        User owner = new User();
        owner.id(applicationEntity.getPrimaryOwner().getId());
        application.setOwner(owner);
        application.setUpdatedAt(applicationEntity.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));


        final ApplicationSettings applicationEntitySettings = applicationEntity.getSettings();
        if(applicationEntitySettings != null) {
            io.gravitee.rest.api.portal.rest.model.ApplicationSettings appSettings = new io.gravitee.rest.api.portal.rest.model.ApplicationSettings();

            final SimpleApplicationSettings simpleAppEntitySettings = applicationEntitySettings.getApp();
            if(simpleAppEntitySettings != null) {
                appSettings.app(new io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings()
                            .clientId(simpleAppEntitySettings.getClientId())
                            .type(simpleAppEntitySettings.getType())
                            )
                ;
            } else {
                final OAuthClientSettings oAuthClientEntitySettings = applicationEntitySettings.getoAuthClient();

                appSettings.oauth(new io.gravitee.rest.api.portal.rest.model.OAuthClientSettings()
                        .applicationType(oAuthClientEntitySettings.getApplicationType())
                        .clientId(oAuthClientEntitySettings.getClientId())
                        .clientSecret(oAuthClientEntitySettings.getClientSecret())
                        .clientUri(oAuthClientEntitySettings.getClientUri())
                        .logoUri(oAuthClientEntitySettings.getLogoUri())
                        .grantTypes(oAuthClientEntitySettings.getGrantTypes())
                        .redirectUris(oAuthClientEntitySettings.getRedirectUris())
                        .responseTypes(oAuthClientEntitySettings.getResponseTypes())
                        .renewClientSecretSupported(Boolean.valueOf(oAuthClientEntitySettings.isRenewClientSecretSupported()))
                        )
                ;
            }
            application.setSettings(appSettings);
        }

        return application;
    }
}
