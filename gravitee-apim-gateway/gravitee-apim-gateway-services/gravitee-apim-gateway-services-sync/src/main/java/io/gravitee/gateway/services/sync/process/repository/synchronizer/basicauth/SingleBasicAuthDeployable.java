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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.basicauth;

import io.gravitee.gateway.handlers.api.services.basicauth.BasicAuthCredential;
import io.gravitee.gateway.services.sync.process.common.model.Deployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import lombok.Builder;

@Builder
public record SingleBasicAuthDeployable(BasicAuthCredential credential, SyncAction syncAction) implements Deployable {
    @Override
    public String id() {
        return credential.getId();
    }

    public String apiId() {
        return credential.getApi();
    }
}
