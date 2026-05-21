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
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzPolicyMapper {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public Maybe<AuthzPolicyReactorDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), PAYLOAD_TYPE);

                String docId = (String) payload.get(AuthzEventPayloadFields.ID);
                String name = (String) payload.get(AuthzEventPayloadFields.NAME);
                String kindStr = (String) payload.get(AuthzEventPayloadFields.KIND);
                String policyText = (String) payload.get(AuthzEventPayloadFields.POLICY_TEXT);
                if (docId == null || docId.isBlank() || kindStr == null || policyText == null || policyText.isBlank()) {
                    log.warn(
                        "Skipping authz policy DEPLOY event [{}] — missing id, kind, or policyText (or policyText is blank)",
                        event.getId()
                    );
                    return null;
                }
                AuthzPolicyReactorDeployable.Kind kind = parseKind(kindStr);
                if (kind == null) {
                    log.warn("Skipping authz policy DEPLOY event [{}] — unknown kind '{}'", event.getId(), kindStr);
                    return null;
                }

                String entityId = payload.get(AuthzEventPayloadFields.ENTITY_ID) instanceof String s && !s.isBlank() ? s : null;
                if (kind == AuthzPolicyReactorDeployable.Kind.RESOURCE && entityId == null) {
                    log.warn(
                        "Skipping authz RESOURCE policy DEPLOY event [{}] — missing entityId (registry filter cannot run)",
                        event.getId()
                    );
                    return null;
                }

                String resolvedName = name != null && !name.isBlank() ? name : docId;

                return AuthzPolicyReactorDeployable.builder()
                    .docId(docId)
                    .name(resolvedName)
                    .policyText(policyText)
                    .kind(kind)
                    .entityId(entityId)
                    .syncAction(SyncAction.DEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz policy from PUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<AuthzPolicyReactorDeployable> toUndeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), PAYLOAD_TYPE);
                String docId = (String) payload.get(AuthzEventPayloadFields.ID);
                if (docId == null || docId.isBlank()) {
                    log.warn("Skipping authz policy UNDEPLOY event [{}] — missing id", event.getId());
                    return null;
                }
                String kindStr = (String) payload.get(AuthzEventPayloadFields.KIND);
                AuthzPolicyReactorDeployable.Kind kind = kindStr != null ? parseKind(kindStr) : null;
                if (kind == null) {
                    // I7: UNPUBLISH publishers historically omit kind. Kind is only used by the deployer
                    // to filter RESOURCE policies by registry on this node; on undeploy the engine just
                    // needs docId, so defaulting to GLOBAL is safe and ensures the removePolicy reaches it.
                    kind = AuthzPolicyReactorDeployable.Kind.GLOBAL;
                }
                String entityId = payload.get(AuthzEventPayloadFields.ENTITY_ID) instanceof String s && !s.isBlank() ? s : null;
                return AuthzPolicyReactorDeployable.builder()
                    .docId(docId)
                    .name(docId)
                    .kind(kind)
                    .entityId(entityId)
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz policy from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    private static AuthzPolicyReactorDeployable.Kind parseKind(String raw) {
        try {
            return AuthzPolicyReactorDeployable.Kind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
