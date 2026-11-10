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
package io.gravitee.gamma.module.platform.infra.service_provider;

import io.gravitee.apim.plugin.gamma.api.credential.CredentialConsumerPort;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialConsumer;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialConsumers;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Asks every loaded Gamma module which of its objects use a credential.
 *
 * <p>The ports are looked up on each call: a module's beans reach the host context only once its plugin is loaded,
 * which may be after this context starts. A port copied into the host context can be seen under two names, so each
 * instance is asked once.
 */
@RequiredArgsConstructor
public class ModuleCredentialConsumers implements CredentialConsumers {

    private final ObjectProvider<CredentialConsumerPort> ports;

    @Override
    public List<CredentialConsumer> of(String environmentId, String credentialId) {
        var asked = Collections.newSetFromMap(new IdentityHashMap<CredentialConsumerPort, Boolean>());
        return ports
            .stream()
            .filter(asked::add)
            .flatMap(port ->
                port
                    .consumersOf(environmentId, credentialId)
                    .stream()
                    .map(consumer -> new CredentialConsumer(port.module(), consumer.type(), consumer.id(), consumer.name()))
            )
            .toList();
    }
}
