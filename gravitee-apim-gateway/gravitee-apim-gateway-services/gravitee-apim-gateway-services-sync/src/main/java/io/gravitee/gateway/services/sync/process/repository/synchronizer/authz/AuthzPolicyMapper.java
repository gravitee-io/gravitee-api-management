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
import io.gravitee.gamma.definition.authz.AuthzPolicy;
import io.gravitee.gamma.definition.authz.AuthzPolicyKind;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzPolicyMapper {

    private final ObjectMapper objectMapper;

    public Maybe<AuthzPolicyReactorDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzPolicy wire = objectMapper.readValue(event.getPayload(), AuthzPolicy.class);
                if (
                    wire.getId() == null ||
                    wire.getId().isBlank() ||
                    wire.getKind() == null ||
                    wire.getPolicyText() == null ||
                    wire.getPolicyText().isBlank()
                ) {
                    log.warn(
                        "Skipping authz policy DEPLOY event [{}] — missing id, kind, or policyText (or policyText is blank)",
                        event.getId()
                    );
                    return null;
                }
                AuthzPolicyReactorDeployable.Kind kind = toGatewayKind(wire.getKind());
                if (kind == AuthzPolicyReactorDeployable.Kind.RESOURCE && (wire.getEntityId() == null || wire.getEntityId().isBlank())) {
                    log.warn("Skipping authz RESOURCE policy DEPLOY event [{}] — missing entityId", event.getId());
                    return null;
                }
                String resolvedName = wire.getName() != null && !wire.getName().isBlank() ? wire.getName() : wire.getId();
                return AuthzPolicyReactorDeployable.builder()
                    .docId(wire.getId())
                    .name(resolvedName)
                    .policyText(wire.getPolicyText())
                    .kind(kind)
                    .entityId(wire.getEntityId())
                    .environmentId(wire.getEnvironmentId())
                    .targetPdpIds(AuthzWire.targetPdpIdsOrEmpty(wire.getTargetPdpIds()))
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
                AuthzPolicy wire = objectMapper.readValue(event.getPayload(), AuthzPolicy.class);
                if (wire.getId() == null || wire.getId().isBlank()) {
                    log.warn("Skipping authz policy UNDEPLOY event [{}] — missing id", event.getId());
                    return null;
                }
                // UNPUBLISH publishers historically omit kind. On undeploy the engine only needs
                // docId, so defaulting to GLOBAL is safe and ensures the removePolicy reaches it.
                AuthzPolicyReactorDeployable.Kind kind = wire.getKind() != null
                    ? toGatewayKind(wire.getKind())
                    : AuthzPolicyReactorDeployable.Kind.GLOBAL;
                return AuthzPolicyReactorDeployable.builder()
                    .docId(wire.getId())
                    .name(wire.getId())
                    .kind(kind)
                    .entityId(wire.getEntityId())
                    .environmentId(wire.getEnvironmentId())
                    .targetPdpIds(AuthzWire.targetPdpIdsOrEmpty(wire.getTargetPdpIds()))
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract authz policy from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    private static AuthzPolicyReactorDeployable.Kind toGatewayKind(AuthzPolicyKind wireKind) {
        return AuthzPolicyReactorDeployable.Kind.valueOf(wireKind.name());
    }
}
