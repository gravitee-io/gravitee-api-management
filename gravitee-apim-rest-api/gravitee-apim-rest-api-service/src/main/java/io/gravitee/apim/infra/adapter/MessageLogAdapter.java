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

import io.gravitee.repository.log.v4.model.message.MessageLog;
import io.gravitee.rest.api.model.v4.connector.ConnectorType;
import io.gravitee.rest.api.model.v4.log.message.BaseMessageLog;
import io.gravitee.rest.api.model.v4.log.message.MessageOperation;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface MessageLogAdapter {
    MessageLogAdapter INSTANCE = Mappers.getMapper(MessageLogAdapter.class);

    @Mapping(target = "operation", expression = "java(mapOperation(messageLog))")
    @Mapping(target = "connectorType", expression = "java(mapConnectorType(messageLog))")
    BaseMessageLog toEntity(MessageLog messageLog);

    List<BaseMessageLog> toEntitiesList(List<MessageLog> messageLogs);

    @Named("mapOperation")
    default MessageOperation mapOperation(MessageLog messageLog) {
        if (Objects.isNull(messageLog)) {
            return null;
        }
        return MessageOperation.fromLabel(messageLog.getOperation());
    }

    @Named("mapConnectorType")
    default ConnectorType mapConnectorType(MessageLog messageLog) {
        if (Objects.isNull(messageLog)) {
            return null;
        }
        return ConnectorType.fromLabel(messageLog.getConnectorType());
    }
}
