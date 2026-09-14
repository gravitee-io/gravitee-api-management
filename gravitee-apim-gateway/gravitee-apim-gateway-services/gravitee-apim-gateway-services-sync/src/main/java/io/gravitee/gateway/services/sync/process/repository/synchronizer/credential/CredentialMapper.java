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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.credential;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.definition.credential.Credential;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class CredentialMapper {

    private final ObjectMapper objectMapper;

    public Maybe<CredentialDeployable> toDeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                Credential wire = objectMapper.readValue(event.getPayload(), Credential.class);
                if (isBlank(wire.getId()) || isBlank(wire.getEnvironmentId()) || isBlank(wire.getEncryptedSecret())) {
                    log.warn("Skipping credential DEPLOY event [{}] — missing id, environmentId, or encryptedSecret", event.getId());
                    return null;
                }
                return CredentialDeployable.builder()
                    .credentialId(wire.getId())
                    .environmentId(wire.getEnvironmentId())
                    .encryptedSecret(wire.getEncryptedSecret())
                    .updatedAt(event.getUpdatedAt() != null ? event.getUpdatedAt().getTime() : 0L)
                    .syncAction(SyncAction.DEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract credential from PUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    public Maybe<CredentialDeployable> toUndeploy(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                Credential wire = objectMapper.readValue(event.getPayload(), Credential.class);
                if (isBlank(wire.getId())) {
                    log.warn("Skipping credential UNDEPLOY event [{}] — missing id", event.getId());
                    return null;
                }
                return CredentialDeployable.builder()
                    .credentialId(wire.getId())
                    .environmentId(wire.getEnvironmentId())
                    .syncAction(SyncAction.UNDEPLOY)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract credential from UNPUBLISH event [{}]", event.getId(), e);
                return null;
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
