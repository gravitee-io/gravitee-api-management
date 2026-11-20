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
package io.gravitee.apim.infra.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.common.cluster.KafkaClusterDefinitionSecretRefsFinder;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClusterSecretsConfiguration {

    /**
     * Finds the secret references of the clusters {@link SecretsServiceClusterSecretResolver} has the secrets service
     * discover. The secrets service collects the finders when it starts, so this one lives in the management API context,
     * as the gateway declares its own: a finder that a plugin declares arrives too late.
     */
    @Bean
    public DefinitionSecretRefsFinder<?> kafkaClusterDefinitionSecretRefsFinder() {
        return new KafkaClusterDefinitionSecretRefsFinder(new ObjectMapper());
    }
}
