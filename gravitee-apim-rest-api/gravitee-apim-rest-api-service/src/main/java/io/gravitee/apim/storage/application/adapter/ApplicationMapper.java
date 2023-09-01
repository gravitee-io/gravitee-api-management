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
package io.gravitee.apim.storage.application.adapter;

import io.gravitee.definition.model.Origin;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;

public class ApplicationMapper {

    public static BaseApplicationEntity mapToBaseApplication(Application application) {
        ApplicationEntity applicationEntity = new ApplicationEntity();

        applicationEntity.setId(application.getId());
        applicationEntity.setName(application.getName());
        applicationEntity.setDescription(application.getDescription());
        applicationEntity.setDomain(application.getDomain());
        applicationEntity.setType(application.getType() != null ? application.getType().name() : null);
        applicationEntity.setStatus(application.getStatus().toString());
        applicationEntity.setPicture(application.getPicture());
        applicationEntity.setBackground(application.getBackground());
        applicationEntity.setGroups(application.getGroups());
        applicationEntity.setCreatedAt(application.getCreatedAt());
        applicationEntity.setUpdatedAt(application.getUpdatedAt());
        applicationEntity.setDisableMembershipNotifications(application.isDisableMembershipNotifications());
        applicationEntity.setApiKeyMode(
            application.getApiKeyMode() != null ? ApiKeyMode.valueOf(application.getApiKeyMode().name()) : null
        );
        applicationEntity.setOrigin(application.getOrigin() != null ? application.getOrigin() : Origin.MANAGEMENT);
        return applicationEntity;
    }
}
