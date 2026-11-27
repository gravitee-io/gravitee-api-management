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

import java.util.List;
import java.util.Map;

public class ClusterConnectionsFixtures {

    public static final String SASL_PASSWORD = "sasl-password-value";
    public static final String TRUSTSTORE_PASSWORD = "truststore-password-value";
    public static final String CONNECTIONS_WITHOUT_CREDENTIALS_JSON =
        "{\"connections\": [{\"bootstrapServers\": \"broker:9093\", \"security\": {\"protocol\": \"SASL_SSL\"}}]}";

    private ClusterConnectionsFixtures() {}

    public static Map<String, Object> aConnectionsConfigurationWithCredentials() {
        return Map.of(
            "connections",
            List.of(
                Map.of(
                    "bootstrapServers",
                    "broker:9093",
                    "security",
                    Map.of(
                        "protocol",
                        "SASL_SSL",
                        "sasl",
                        Map.of("mechanism", Map.of("type", "PLAIN", "username", "user", "password", SASL_PASSWORD)),
                        "ssl",
                        Map.of("trustStore", Map.of("type", "JKS", "password", TRUSTSTORE_PASSWORD))
                    )
                )
            )
        );
    }
}
