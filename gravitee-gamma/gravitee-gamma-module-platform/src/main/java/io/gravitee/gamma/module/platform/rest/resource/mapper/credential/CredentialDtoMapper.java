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
package io.gravitee.gamma.module.platform.rest.resource.mapper.credential;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialConsumer;
import io.gravitee.gamma.module.platform.core.credential.model.NewCredentialCommand;
import io.gravitee.gamma.module.platform.core.credential.model.UpdateCredentialCommand;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CreateCredentialRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CredentialConsumerResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CredentialConsumersResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CredentialResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.CredentialsResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.SecretReferenceResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.credential.CredentialDtos.UpdateCredentialRequest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;

public final class CredentialDtoMapper {

    private CredentialDtoMapper() {}

    public static NewCredentialCommand toCommand(CreateCredentialRequest request) {
        return NewCredentialCommand.builder()
            .name(request.name())
            .strategy(request.strategy())
            .serviceId(request.serviceId())
            .ownerId(request.ownerId())
            .expiresAt(toInstant(request.expiresAt()))
            .configuration(request.configuration())
            .references(request.references())
            .values(request.values())
            .build();
    }

    public static UpdateCredentialCommand toCommand(UpdateCredentialRequest request) {
        return UpdateCredentialCommand.builder()
            .name(request.name())
            .serviceId(request.serviceId())
            .ownerId(request.ownerId())
            .expiresAt(toInstant(request.expiresAt()))
            .configuration(request.configuration())
            .references(request.references())
            .build();
    }

    public static CredentialResponse toResponse(Credential credential) {
        var references = new LinkedHashMap<String, SecretReferenceResponse>();
        credential
            .secretReferences()
            .forEach((field, reference) ->
                references.put(field, new SecretReferenceResponse(reference.reference(), reference.isManaged()))
            );
        return new CredentialResponse(
            credential.id(),
            credential.name(),
            credential.strategy(),
            credential.serviceId(),
            credential.ownerId(),
            toDateTime(credential.expiresAt()),
            credential.configuration(),
            references,
            credential.isManaged(),
            toDateTime(credential.createdAt()),
            toDateTime(credential.updatedAt()),
            toDateTime(credential.rotatedAt())
        );
    }

    /** @param page the page asked for, from 1 */
    public static CredentialsResponse toResponse(Page<Credential> credentials, int page, int perPage) {
        var data = credentials.getContent().stream().map(CredentialDtoMapper::toResponse).toList();
        var total = credentials.getTotalElements();
        var pageCount = total == 0 ? 0 : (int) Math.ceil((double) total / perPage);
        return new CredentialsResponse(data, new CredentialsResponse.Pagination(total, page, perPage, pageCount, data.size()));
    }

    public static CredentialConsumersResponse toResponse(List<CredentialConsumer> consumers) {
        return new CredentialConsumersResponse(
            consumers
                .stream()
                .map(c -> new CredentialConsumerResponse(c.module(), c.type(), c.id(), c.name()))
                .toList()
        );
    }

    private static Instant toInstant(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }

    private static ZonedDateTime toDateTime(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneOffset.UTC);
    }
}
