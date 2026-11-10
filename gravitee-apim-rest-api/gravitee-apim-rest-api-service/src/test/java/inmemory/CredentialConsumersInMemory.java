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
package io.gravitee.gamma.module.platform.inmemory;

import io.gravitee.gamma.module.platform.core.credential.model.CredentialConsumer;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialConsumers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryCredentialConsumers implements CredentialConsumers {

    private final Map<String, List<CredentialConsumer>> byCredential = new HashMap<>();

    public void given(String environmentId, String credentialId, CredentialConsumer consumer) {
        byCredential.computeIfAbsent(environmentId + "/" + credentialId, k -> new ArrayList<>()).add(consumer);
    }

    @Override
    public List<CredentialConsumer> of(String environmentId, String credentialId) {
        return List.copyOf(byCredential.getOrDefault(environmentId + "/" + credentialId, List.of()));
    }
}
