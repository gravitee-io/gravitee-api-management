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
package fixtures.core.model;

import java.util.Map;

public class ClusterConfigurationFixtures {

    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String SASL_PASSWORD = "sasl-password-value";
    public static final String NEW_SASL_PASSWORD = "new-sasl-password-value";
    public static final String SASL_JAAS_CONFIG = "jaas-config-value";
    public static final String TRUSTSTORE_PASSWORD = "truststore-password-value";
    public static final String KEYSTORE_CONTENT = "keystore-content-value";

    private ClusterConfigurationFixtures() {}

    public static Map<String, Object> aConfigurationWithCredentials() {
        return aConfigurationWithCredentials(BOOTSTRAP_SERVERS, SASL_PASSWORD);
    }

    public static Map<String, Object> aConfigurationWithCredentials(String bootstrapServers, String saslPassword) {
        return Map.of(
            "bootstrapServers",
            bootstrapServers,
            "security",
            Map.of(
                "protocol",
                "SASL_SSL",
                "sasl",
                Map.of(
                    "mechanism",
                    Map.of("type", "SCRAM-SHA-512", "username", "user", "password", saslPassword, "saslJaasConfig", SASL_JAAS_CONFIG)
                ),
                "ssl",
                Map.of(
                    "trustStore",
                    Map.of("type", "JKS", "password", TRUSTSTORE_PASSWORD),
                    "keyStore",
                    Map.of("type", "PEM", "content", KEYSTORE_CONTENT)
                )
            )
        );
    }
}
