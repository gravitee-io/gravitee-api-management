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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.authorization.api.AuthzEventPayloadFields;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzEntityMapper {

    public static final String ENGINE_TYPE_RESOURCE = "Resource";
    public static final String ENGINE_TYPE_PRINCIPAL = "Principal";

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public Maybe<AuthzEntityReactorDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), PAYLOAD_TYPE);

                String entityId = (String) payload.get(AuthzEventPayloadFields.ENTITY_ID);
                String kindStr = (String) payload.get(AuthzEventPayloadFields.KIND);
                if (entityId == null || entityId.isBlank() || kindStr == null) {
                    log.warn("Skipping authz entity DEPLOY event [{}] — missing entityId or kind", event.getId());
                    return null;
                }
                if (AuthzEntityIdExtractor.isAutoDerived(entityId)) {
                    return null;
                }
                AuthzEntityReactorDeployable.Kind kind = parseKind(kindStr);
                if (kind == null) {
                    log.warn("Skipping authz entity DEPLOY event [{}] — unknown kind '{}'", event.getId(), kindStr);
                    return null;
                }

                Map<String, Object> attributes = asMapOrNull(payload.get(AuthzEventPayloadFields.ATTRIBUTES));
                List<String> parents = asStringListOrNull(payload.get(AuthzEventPayloadFields.PARENTS));

                return AuthzEntityReactorDeployable.builder()
                    .entityId(entityId)
                    .engineUid(toEngineUid(kind, entityId))
                    .kind(kind)
                    .attributes(attributes)
                    .parents(parents)
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
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), PAYLOAD_TYPE);
                String entityId = (String) payload.get(AuthzEventPayloadFields.ENTITY_ID);
                if (entityId == null || entityId.isBlank()) {
                    log.warn("Skipping authz entity UNDEPLOY event [{}] — missing entityId", event.getId());
                    return null;
                }
                if (AuthzEntityIdExtractor.isAutoDerived(entityId)) {
                    return null;
                }
                String kindStr = (String) payload.get(AuthzEventPayloadFields.KIND);
                AuthzEntityReactorDeployable.Kind kind = kindStr != null ? parseKind(kindStr) : null;
                if (kind == null) {
                    // I7: UNPUBLISH publishers historically omit kind. Default to RESOURCE so the
                    // engine still receives the removeEntity — without this, the orphan entity lingers.
                    kind = AuthzEntityReactorDeployable.Kind.RESOURCE;
                }
                return AuthzEntityReactorDeployable.builder()
                    .entityId(entityId)
                    .engineUid(toEngineUid(kind, entityId))
                    .kind(kind)
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz entity from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMapOrNull(Object raw) {
        if (raw instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        if (raw != null) {
            log.warn("Unexpected 'attributes' shape in authz event payload: {}", raw.getClass().getSimpleName());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringListOrNull(Object raw) {
        if (raw instanceof List<?> l) {
            return (List<String>) l;
        }
        if (raw != null) {
            log.warn("Unexpected 'parents' shape in authz event payload: {}", raw.getClass().getSimpleName());
        }
        return null;
    }

    private static AuthzEntityReactorDeployable.Kind parseKind(String raw) {
        try {
            return AuthzEntityReactorDeployable.Kind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static String toEngineUid(AuthzEntityReactorDeployable.Kind kind, String entityId) {
        if (kind == AuthzEntityReactorDeployable.Kind.PRINCIPAL) {
            return ENGINE_TYPE_PRINCIPAL + "::\"" + entityId + "\"";
        }
        return AuthzEntityIdExtractor.toResourceEngineUid(entityId);
    }
}
