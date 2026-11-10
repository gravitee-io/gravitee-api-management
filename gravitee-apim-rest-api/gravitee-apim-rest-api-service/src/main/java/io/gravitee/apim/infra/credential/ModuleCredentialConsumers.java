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
package io.gravitee.apim.infra.credential;

import io.gravitee.apim.core.credential.model.CredentialConsumer;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumerPort;
import io.gravitee.apim.core.credential.service_provider.CredentialConsumers;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Asks every loaded Gamma module which of its objects use a credential.
 *
 * <p>The ports are looked up on each call: a module's beans reach this context only once its plugin is loaded, which is
 * after this context starts. A port copied into this context can be seen under two names, so each instance is asked once.
 */
@Service
public class ModuleCredentialConsumers implements CredentialConsumers {

    private final ObjectProvider<CredentialConsumerPort> ports;

    public ModuleCredentialConsumers(ObjectProvider<CredentialConsumerPort> ports) {
        this.ports = ports;
    }

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
