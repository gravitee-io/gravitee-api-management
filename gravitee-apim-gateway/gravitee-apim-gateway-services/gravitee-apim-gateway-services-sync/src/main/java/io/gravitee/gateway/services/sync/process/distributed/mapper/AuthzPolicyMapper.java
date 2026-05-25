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
import io.gravitee.gamma.definition.authz.AuthzPolicy;
import io.gravitee.gamma.definition.authz.AuthzPolicyKind;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Distributed-sync mapper for authz policy events. Mirrors {@link SharedPolicyGroupMapper}.
 */
@RequiredArgsConstructor
@CustomLog
public class AuthzPolicyMapper {

    private final ObjectMapper objectMapper;

    public Maybe<AuthzPolicyReactorDeployable> to(final DistributedEvent event) {
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
                    log.warn("Skipping distributed authz policy event [{}] — missing id, kind, or policyText", event.getId());
                    return null;
                }
                AuthzPolicyReactorDeployable.Kind kind = AuthzPolicyReactorDeployable.Kind.valueOf(wire.getKind().name());
                if (kind == AuthzPolicyReactorDeployable.Kind.RESOURCE && (wire.getEntityId() == null || wire.getEntityId().isBlank())) {
                    log.warn("Skipping distributed authz RESOURCE policy event [{}] — missing entityId", event.getId());
                    return null;
                }
                String resolvedName = wire.getName() != null && !wire.getName().isBlank() ? wire.getName() : wire.getId();
                return AuthzPolicyReactorDeployable.builder()
                    .docId(wire.getId())
                    .name(resolvedName)
                    .policyText(wire.getPolicyText())
                    .kind(kind)
                    .entityId(wire.getEntityId())
                    .syncAction(SyncActionMapper.to(event.getSyncAction()))
                    .build();
            } catch (Exception e) {
                log.warn("Error while reading authz policy from distributed event payload", e);
                return null;
            }
        });
    }

    public Flowable<DistributedEvent> to(final AuthzPolicyReactorDeployable deployable) {
        return Flowable.fromCallable(() -> {
            try {
                AuthzPolicy wire = AuthzPolicy.builder()
                    .id(deployable.docId())
                    .name(deployable.name())
                    .kind(AuthzPolicyKind.valueOf(deployable.kind().name()))
                    .entityId(deployable.entityId())
                    .policyText(deployable.policyText())
                    .build();
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.docId())
                    .type(DistributedEventType.AUTHZ_POLICY)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(objectMapper.writeValueAsString(wire));
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from authz policy deployable", e);
                return null;
            }
        }).filter(java.util.Objects::nonNull);
    }
}
