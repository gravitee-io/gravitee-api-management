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
import io.gravitee.gamma.definition.authz.AuthzEntity;
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
import io.gravitee.gamma.definition.authz.AuthzEntityKind;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzEntityMapper {

    private final ObjectMapper objectMapper;

    public Maybe<AuthzEntityReactorDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzEntity wire = objectMapper.readValue(event.getPayload(), AuthzEntity.class);
                if (wire.getEntityId() == null || wire.getEntityId().isBlank() || wire.getKind() == null) {
                    log.warn("Skipping authz entity DEPLOY event [{}] — missing entityId or kind", event.getId());
                    return null;
                }
                AuthzEntityReactorDeployable.Kind kind = toGatewayKind(wire.getKind());
                return AuthzEntityReactorDeployable.builder()
                    .entityId(wire.getEntityId())
                    .engineUid(toEngineUid(kind, wire.getEntityType(), wire.getEntityId()))
                    .kind(kind)
                    .entityType(wire.getEntityType())
                    .attributes(wire.getAttributes())
                    .parents(wire.getParents())
                    .syncAction(SyncAction.DEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz entity from PUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<AuthzEntityReactorDeployable> toUndeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzEntity wire = objectMapper.readValue(event.getPayload(), AuthzEntity.class);
                if (wire.getEntityId() == null || wire.getEntityId().isBlank()) {
                    log.warn("Skipping authz entity UNDEPLOY event [{}] — missing entityId", event.getId());
                    return null;
                }
                // UNPUBLISH publishers historically omit kind. Default to RESOURCE so the
                // engine still receives the removeEntity — without this, the orphan entity lingers.
                AuthzEntityReactorDeployable.Kind kind = wire.getKind() != null
                    ? toGatewayKind(wire.getKind())
                    : AuthzEntityReactorDeployable.Kind.RESOURCE;
                return AuthzEntityReactorDeployable.builder()
                    .entityId(wire.getEntityId())
                    .engineUid(toEngineUid(kind, wire.getEntityType(), wire.getEntityId()))
                    .kind(kind)
                    .entityType(wire.getEntityType())
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz entity from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    private static AuthzEntityReactorDeployable.Kind toGatewayKind(AuthzEntityKind wireKind) {
        return AuthzEntityReactorDeployable.Kind.valueOf(wireKind.name());
    }

    /**
     * Build the engine UID emitted to the PDP:
     * <ul>
     *   <li>If the wire carries an explicit {@code entityType} (post typed-entity-type rollout)
     *       — use it verbatim so GAPL policies like {@code principal == User::"alice"} match
     *       the entity in the snapshot.</li>
     *   <li>Otherwise (legacy publisher) — fall back to the kind default
     *       ({@code Principal::"<id>"} / {@code Resource::"<id>"}).</li>
     * </ul>
     */
    static String toEngineUid(AuthzEntityReactorDeployable.Kind kind, String entityType, String entityId) {
        if (entityType != null && !entityType.isBlank()) {
            return entityType + "::\"" + entityId + "\"";
        }
        if (kind == AuthzEntityReactorDeployable.Kind.PRINCIPAL) {
            return AuthzEntityIdConstants.ENGINE_TYPE_PRINCIPAL + "::\"" + entityId + "\"";
        }
        return AuthzEntityIdExtractor.toResourceEngineUid(entityId);
    }
}
