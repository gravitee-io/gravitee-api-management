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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.authz.AuthzSchema;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzSchemaMapper {

    private final ObjectMapper objectMapper;

    public Maybe<AuthzSchemaReactorDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzSchema wire = objectMapper.readValue(event.getPayload(), AuthzSchema.class);
                if (wire.getId() == null || wire.getId().isBlank()) {
                    log.warn("Skipping authz schema DEPLOY event [{}] — missing id", event.getId());
                    return null;
                }
                if (wire.getSchemaText() == null || wire.getSchemaText().isBlank()) {
                    log.warn("Skipping authz schema DEPLOY event [{}] — missing or blank schemaText", event.getId());
                    return null;
                }
                String resolvedName = wire.getName() != null && !wire.getName().isBlank() ? wire.getName() : wire.getId();
                return AuthzSchemaReactorDeployable.builder()
                    .docId(wire.getId())
                    .name(resolvedName)
                    .schemaText(wire.getSchemaText())
                    .environmentId(wire.getEnvironmentId())
                    .targetPdpIds(AuthzWire.targetPdpIdsOrEmpty(wire.getTargetPdpIds()))
                    .updatedAt(event.getUpdatedAt() != null ? event.getUpdatedAt().getTime() : 0L)
                    .syncAction(SyncAction.DEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz schema from PUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<AuthzSchemaReactorDeployable> toUndeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzSchema wire = objectMapper.readValue(event.getPayload(), AuthzSchema.class);
                if (wire.getId() == null || wire.getId().isBlank()) {
                    log.warn("Skipping authz schema UNDEPLOY event [{}] — missing id", event.getId());
                    return null;
                }
                return AuthzSchemaReactorDeployable.builder()
                    .docId(wire.getId())
                    .name(wire.getId())
                    .environmentId(wire.getEnvironmentId())
                    .targetPdpIds(AuthzWire.targetPdpIdsOrEmpty(wire.getTargetPdpIds()))
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz schema from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }
}
