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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.authz.AuthzEntity;
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class AuthzEntityMapper {

    private final ObjectMapper objectMapper;

    public Maybe<AuthzEntityReactorDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzEntity wire = objectMapper.readValue(event.getPayload(), AuthzEntity.class);
                if (wire.getEntityId() == null || wire.getEntityId().isBlank() || wire.getKind() == null) {
                    log.warn("Skipping distributed authz entity event [{}] — missing entityId or kind", event.getId());
                    return null;
                }
                AuthzEntityReactorDeployable.Kind kind = AuthzEntityReactorDeployable.Kind.valueOf(wire.getKind().name());
                return AuthzEntityReactorDeployable.builder()
                    .entityId(wire.getEntityId())
                    .engineUid(toEngineUid(kind, wire.getEntityType(), wire.getEntityId()))
                    .kind(kind)
                    .entityType(wire.getEntityType())
                    .attributes(wire.getAttributes())
                    .parents(wire.getParents())
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                log.warn("Error while reading authz entity from distributed event payload", e);
                return null;
            }
        });
    }

    public Flowable<DistributedEvent> to(final AuthzEntityReactorDeployable deployable) {
        return Flowable.fromCallable(() -> {
            try {
                AuthzEntity wire = AuthzEntity.builder()
                    .entityId(deployable.entityId())
                    .kind(io.gravitee.gamma.definition.authz.AuthzEntityKind.valueOf(deployable.kind().name()))
                    .entityType(deployable.entityType())
                    .attributes(deployable.attributes())
                    .parents(deployable.parents())
                    .build();
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.entityId())
                    .type(DistributedEventType.AUTHZ_ENTITY)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(objectMapper.writeValueAsString(wire));
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from authz entity deployable", e);
                return null;
            }
        }).filter(java.util.Objects::nonNull);
    }

    private static String toEngineUid(AuthzEntityReactorDeployable.Kind kind, String entityType, String entityId) {
        if (entityType != null && !entityType.isBlank()) {
            return entityType + "::\"" + entityId + "\"";
        }
        if (kind == AuthzEntityReactorDeployable.Kind.PRINCIPAL) {
            return AuthzEntityIdConstants.ENGINE_TYPE_PRINCIPAL + "::\"" + entityId + "\"";
        }
        return AuthzEntityIdExtractor.toResourceEngineUid(entityId);
    }
}
