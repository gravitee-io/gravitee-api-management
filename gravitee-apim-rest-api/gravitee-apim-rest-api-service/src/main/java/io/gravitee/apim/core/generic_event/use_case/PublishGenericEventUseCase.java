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
package io.gravitee.apim.core.generic_event.use_case;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;

/**
 * Publishes a generic event to the gateways of its environment, replacing the previous one with the same kind and id.
 */
@UseCase
@AllArgsConstructor
public class PublishGenericEventUseCase {

    static final Pattern KIND = Pattern.compile("[a-z0-9-]+(\\.[a-z0-9-]+)+");
    // Every gateway of the environment keeps each published event in memory.
    static final int MAX_PAYLOAD_BYTES = 256 * 1024;

    private final GenericEventWriterDomainService genericEventWriterDomainService;
    private final ObjectMapper objectMapper;

    /**
     * @param historized also records this version in the events history, as API deployments do. Only set it when the feature
     *     must be able to revert to a previous version: by default only the current version is kept.
     */
    public record Input(GenericEvent genericEvent, boolean historized, AuditInfo auditInfo) {
        public Input(GenericEvent genericEvent, AuditInfo auditInfo) {
            this(genericEvent, false, auditInfo);
        }
    }

    public record Output(GenericEvent genericEvent) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo();
        var genericEvent = input
            .genericEvent()
            .withEnvironmentId(auditInfo.environmentId())
            .withOrganizationId(auditInfo.organizationId())
            .withUpdatedAt(TimeProvider.instantNow());
        validate(genericEvent);

        genericEventWriterDomainService.write(EventType.PUBLISH_GENERIC_EVENT, genericEvent, auditInfo, input.historized());
        return new Output(genericEvent);
    }

    private void validate(GenericEvent genericEvent) {
        if (genericEvent.kind() == null || !KIND.matcher(genericEvent.kind()).matches()) {
            throw new InvalidDataException("A generic event kind must look like <feature>.<type>, got: " + genericEvent.kind());
        }
        if (genericEvent.id() == null || genericEvent.id().isBlank()) {
            throw new InvalidDataException("A generic event needs an id.");
        }
        if (genericEvent.payload() == null) {
            throw new InvalidDataException("A generic event needs a payload.");
        }
        if (genericEvent.payload().getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            throw new InvalidDataException("A generic event payload must not exceed " + MAX_PAYLOAD_BYTES + " bytes.");
        }
        try {
            // Stored unescaped, so invalid JSON would corrupt the row every gateway reads.
            objectMapper.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(genericEvent.payload());
        } catch (JsonProcessingException e) {
            throw new InvalidDataException("A generic event payload must be valid JSON: " + e.getOriginalMessage());
        }
    }
}
