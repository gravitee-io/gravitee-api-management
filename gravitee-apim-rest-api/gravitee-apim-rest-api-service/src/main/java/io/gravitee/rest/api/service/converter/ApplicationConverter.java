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
package io.gravitee.rest.api.service.converter;

import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ApplicationConverter {

    public UpdateApplicationEntity toUpdateApplicationEntity(ApplicationEntity application) {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setSettings(application.getSettings());
        updateApplicationEntity.setBackground(application.getBackground());
        updateApplicationEntity.setDescription(application.getDescription());
        updateApplicationEntity.setDomain(application.getDomain());
        updateApplicationEntity.setGroups(application.getGroups());
        updateApplicationEntity.setPicture(application.getPicture());
        updateApplicationEntity.setName(application.getName());
        updateApplicationEntity.setApiKeyMode(application.getApiKeyMode());
        updateApplicationEntity.setDisableMembershipNotifications(application.isDisableMembershipNotifications());
        updateApplicationEntity.setType(application.getType());
        return updateApplicationEntity;
    }

    public Application toApplication(NewApplicationEntity newApplicationEntity) {
        Application application = new Application();

        application.setName(StringUtils.trim(newApplicationEntity.getName()));
        application.setDescription(StringUtils.trim(newApplicationEntity.getDescription()));
        application.setDomain(newApplicationEntity.getDomain());
        application.setGroups(newApplicationEntity.getGroups());

        if (newApplicationEntity.getSettings() != null) {
            if (newApplicationEntity.getSettings().getApp() != null) {
                application.setType(ApplicationType.SIMPLE);
            } else {
                application.setType(
                    ApplicationType.valueOf(newApplicationEntity.getSettings().getOAuthClient().getApplicationType().toUpperCase())
                );
            }
        }
        application.setPicture(newApplicationEntity.getPicture());
        application.setBackground(newApplicationEntity.getBackground());
        application.setMetadata(toMetadata(newApplicationEntity.getSettings()));
        application.setApiKeyMode(toModelApiKeyMode(newApplicationEntity.getApiKeyMode()));
        application.setOrigin(newApplicationEntity.getOrigin());
        return application;
    }

    public Application toApplication(UpdateApplicationEntity updateApplicationEntity) {
        Application application = new Application();
        application.setName(StringUtils.trim(updateApplicationEntity.getName()));
        application.setDescription(StringUtils.trim(updateApplicationEntity.getDescription()));
        application.setPicture(updateApplicationEntity.getPicture());
        application.setBackground(updateApplicationEntity.getBackground());
        application.setDomain(updateApplicationEntity.getDomain());
        application.setGroups(updateApplicationEntity.getGroups());
        application.setApiKeyMode(toModelApiKeyMode(updateApplicationEntity.getApiKeyMode()));
        application.setMetadata(toMetadata(updateApplicationEntity.getSettings()));
        application.setDisableMembershipNotifications(updateApplicationEntity.isDisableMembershipNotifications());
        return application;
    }

    private Map<String, String> toMetadata(ApplicationSettings applicationSettings) {
        Map<String, String> metadata = new HashMap<>();
        if (applicationSettings != null && applicationSettings.getApp() != null) {
            if (applicationSettings.getApp().getClientId() != null) {
                metadata.put("client_id", applicationSettings.getApp().getClientId());
            }
            if (applicationSettings.getApp().getType() != null) {
                metadata.put("type", applicationSettings.getApp().getType());
            }
        }
        if (applicationSettings != null && applicationSettings.getTls() != null) {
            if (applicationSettings.getTls().getClientCertificate() != null) {
                metadata.put(Application.METADATA_CLIENT_CERTIFICATE, applicationSettings.getTls().getClientCertificate());
            }
        }
        return metadata;
    }

    private ApiKeyMode toModelApiKeyMode(io.gravitee.rest.api.model.ApiKeyMode apiKeyMode) {
        return apiKeyMode != null ? ApiKeyMode.valueOf(apiKeyMode.name()) : ApiKeyMode.UNSPECIFIED;
    }
}
