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
package io.gravitee.rest.api.management.v4.rest.mapper;

import io.gravitee.rest.api.management.v4.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ConnectorPluginMapper {
    ConnectorPluginMapper INSTANCE = Mappers.getMapper(ConnectorPluginMapper.class);

    ConnectorPlugin convert(ConnectorPluginEntity connectorPluginEntity);

    Set<ConnectorPlugin> convertSet(Set<ConnectorPluginEntity> connectorPluginEntitySet);
}
