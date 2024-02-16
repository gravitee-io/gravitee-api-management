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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.notification.model.config.NotificationConfig;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface NotificationConfigAdapter {
    NotificationConfigAdapter INSTANCE = Mappers.getMapper(NotificationConfigAdapter.class);

    @Mapping(target = "type", source = "id", qualifiedByName = "computeType")
    NotificationConfig toEntity(GenericNotificationConfig source);

    GenericNotificationConfig toRepository(NotificationConfig source);

    @Named("computeType")
    default NotificationConfig.Type computeType(String id) {
        return id == null ? NotificationConfig.Type.PORTAL : NotificationConfig.Type.GENERIC;
    }
}
