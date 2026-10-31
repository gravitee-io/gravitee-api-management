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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static io.gravitee.repository.management.model.Event.EventProperties.GUARDIAN_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Turns a {@code DEPLOY_GUARDIAN} / {@code UNDEPLOY_GUARDIAN} event into what the gateway deploys. The payload of a
 * deploy event is the {@link Guardian} definition itself.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
public class GuardianMapper {

    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;

    public Maybe<String> toId(Event guardianEvent) {
        return Maybe.fromCallable(() -> {
            String guardianId = null;
            if (guardianEvent.getProperties() != null) {
                guardianId = guardianEvent.getProperties().get(GUARDIAN_ID.getValue());
            }
            if (guardianId == null) {
                log.warn("Unable to extract guardian info from event [{}].", guardianEvent.getId());
            }
            return guardianId;
        });
    }

    public Maybe<ReactableGuardian> to(Event guardianEvent) {
        return Maybe.fromCallable(() -> {
            try {
                Guardian definition = objectMapper.readValue(guardianEvent.getPayload(), Guardian.class);
                ReactableGuardian reactableGuardian = new ReactableGuardian(definition);
                environmentService.fill(definition.getEnvironmentId(), reactableGuardian);
                return reactableGuardian;
            } catch (SyncException e) {
                // Transient enrichment failure: fail the sync so this event is retried instead of dropped.
                throw e;
            } catch (Exception e) {
                log.error("Unable to extract guardian definition from event [{}].", guardianEvent.getId(), e);
                return null;
            }
        });
    }
}
