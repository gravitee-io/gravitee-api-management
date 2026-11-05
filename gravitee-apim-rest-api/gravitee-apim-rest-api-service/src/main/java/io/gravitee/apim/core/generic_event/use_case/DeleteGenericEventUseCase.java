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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import lombok.AllArgsConstructor;

/**
 * Removes a generic event from the gateways of its environment.
 */
@UseCase
@AllArgsConstructor
public class DeleteGenericEventUseCase {

    private final GenericEventWriterDomainService genericEventWriterDomainService;

    /**
     * @param historized also records the removal in the events history; set it when the event was published historized.
     */
    public record Input(String kind, String id, boolean historized, AuditInfo auditInfo) {
        public Input(String kind, String id, AuditInfo auditInfo) {
            this(kind, id, false, auditInfo);
        }
    }

    public void execute(Input input) {
        var auditInfo = input.auditInfo();
        var genericEvent = GenericEvent.builder()
            .kind(input.kind())
            .id(input.id())
            .environmentId(auditInfo.environmentId())
            .organizationId(auditInfo.organizationId())
            .updatedAt(TimeProvider.instantNow())
            .build();

        genericEventWriterDomainService.write(EventType.UNPUBLISH_GENERIC_EVENT, genericEvent, auditInfo, input.historized());
    }
}
