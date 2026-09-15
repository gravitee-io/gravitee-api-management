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
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Maps credentials to and from distributed events, so secondary nodes receive the credentials the primary node deployed.
 *
 * <p>The payload carries the organization the primary node resolved, because a secondary node cannot look it up and the
 * licence check needs it. The secret stays encrypted. An undeploy carries no payload: the credential id is enough.
 */
@RequiredArgsConstructor
@CustomLog
public class CredentialMapper {

    private final ObjectMapper objectMapper;

    public Maybe<CredentialDeployable> to(final DistributedEvent event) {
        return Maybe.fromCallable(() -> {
            try {
                SyncAction syncAction = SyncActionMapper.to(event.getSyncAction());
                if (syncAction == SyncAction.UNDEPLOY) {
                    return CredentialDeployable.builder().credentialId(event.getId()).syncAction(SyncAction.UNDEPLOY).build();
                }
                Payload payload = objectMapper.readValue(event.getPayload(), Payload.class);
                if (
                    isBlank(payload.id()) ||
                    isBlank(payload.environmentId()) ||
                    isBlank(payload.organizationId()) ||
                    isBlank(payload.encryptedSecret())
                ) {
                    log.warn(
                        "Skipping distributed credential event [{}] — missing id, environmentId, organizationId, or encryptedSecret",
                        event.getId()
                    );
                    return null;
                }
                return CredentialDeployable.builder()
                    .credentialId(payload.id())
                    .environmentId(payload.environmentId())
                    .organizationId(payload.organizationId())
                    .allowedApiIds(payload.allowedApiIds() == null ? Set.of() : Set.copyOf(payload.allowedApiIds()))
                    .encryptedSecret(payload.encryptedSecret())
                    .updatedAt(payload.updatedAt())
                    .syncAction(syncAction)
                    .build();
            } catch (Exception e) {
                log.warn("Error while reading credential from distributed event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<DistributedEvent> to(final CredentialDeployable deployable) {
        return Maybe.fromCallable(() -> {
            try {
                DistributedEvent.DistributedEventBuilder builder = DistributedEvent.builder()
                    .id(deployable.credentialId())
                    .type(DistributedEventType.CREDENTIAL)
                    .syncAction(SyncActionMapper.to(deployable.syncAction()))
                    .updatedAt(new Date());
                if (deployable.syncAction() == SyncAction.DEPLOY) {
                    builder.payload(
                        objectMapper.writeValueAsString(
                            new Payload(
                                deployable.credentialId(),
                                deployable.environmentId(),
                                deployable.organizationId(),
                                deployable.allowedApiIds(),
                                deployable.encryptedSecret(),
                                deployable.updatedAt()
                            )
                        )
                    );
                }
                return builder.build();
            } catch (Exception e) {
                log.warn("Error while building distributed event from credential [{}]", deployable.credentialId(), e);
                return null;
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record Payload(
        String id,
        String environmentId,
        String organizationId,
        Set<String> allowedApiIds,
        String encryptedSecret,
        long updatedAt
    ) {}
}
