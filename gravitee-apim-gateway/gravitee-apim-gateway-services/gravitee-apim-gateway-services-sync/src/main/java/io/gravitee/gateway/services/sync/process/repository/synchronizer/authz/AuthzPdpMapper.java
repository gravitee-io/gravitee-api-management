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
import io.gravitee.gamma.definition.authz.AuthzPdp;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzPdpMapper {

    private final ObjectMapper objectMapper;

    public Maybe<AuthzPdpProvisionDeployable> toDeploy(Event event) {
        return map(event, SyncAction.DEPLOY);
    }

    public Maybe<AuthzPdpProvisionDeployable> toUndeploy(Event event) {
        return map(event, SyncAction.UNDEPLOY);
    }

    private Maybe<AuthzPdpProvisionDeployable> map(Event event, SyncAction syncAction) {
        return Maybe.fromCallable(() -> {
            try {
                AuthzPdp wire = objectMapper.readValue(event.getPayload(), AuthzPdp.class);
                if (wire.getTargetPdpId() == null || wire.getTargetPdpId().isBlank()) {
                    log.warn("Skipping AUTHZ_PDP {} event [{}] — missing targetPdpId", syncAction, event.getId());
                    return null;
                }
                if (wire.getEnvironmentId() == null || wire.getEnvironmentId().isBlank()) {
                    log.warn("Skipping AUTHZ_PDP {} event [{}] — missing environmentId", syncAction, event.getId());
                    return null;
                }
                return AuthzPdpProvisionDeployable.builder()
                    .targetPdpId(wire.getTargetPdpId())
                    .environmentId(wire.getEnvironmentId())
                    .tag(wire.getTag())
                    .syncAction(syncAction)
                    .build();
            } catch (Exception e) {
                log.error("Unable to extract AuthzPdp from {} event [{}]", syncAction, event.getId(), e);
                return null;
            }
        });
    }
}
