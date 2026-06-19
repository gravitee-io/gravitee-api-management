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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import org.junit.jupiter.api.Test;

class AuthzPdpProvisionDeployableTest {

    @Test
    void exposes_scope_and_action_and_id_is_scope_id() {
        AuthzPdpProvisionDeployable deployable = AuthzPdpProvisionDeployable.builder()
            .targetPdpId("scope-1")
            .syncAction(SyncAction.DEPLOY)
            .build();

        assertThat(deployable.targetPdpId()).isEqualTo("scope-1");
        assertThat(deployable.syncAction()).isEqualTo(SyncAction.DEPLOY);
        assertThat(deployable.id()).isEqualTo("scope-1");
    }
}
