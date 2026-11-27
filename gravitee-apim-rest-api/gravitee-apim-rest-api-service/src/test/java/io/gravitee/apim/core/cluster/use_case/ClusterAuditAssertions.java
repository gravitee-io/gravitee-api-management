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
package io.gravitee.apim.core.cluster.use_case;

import static fixtures.core.model.ClusterConnectionsFixtures.CONNECTIONS_WITHOUT_CREDENTIALS_JSON;
import static fixtures.core.model.ClusterConnectionsFixtures.SASL_PASSWORD;
import static fixtures.core.model.ClusterConnectionsFixtures.TRUSTSTORE_PASSWORD;
import static fixtures.core.model.ClusterConnectionsFixtures.aConnectionsConfigurationWithCredentials;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;

class ClusterAuditAssertions {

    private ClusterAuditAssertions() {}

    /**
     * Asserts that the audit patch records the connections without their credentials, while the event payload
     * and the stored cluster keep them, since the gateway reads both.
     */
    static void assertCredentialsKeptOutOfTheAuditOnly(String auditPatch, String eventPayload, Object storedConfiguration) {
        assertThatJson(auditPatch)
            .inPath("$[?(@.path == '/configuration')].value")
            .isArray()
            .containsExactly(json(CONNECTIONS_WITHOUT_CREDENTIALS_JSON));
        assertThat(auditPatch).doesNotContain(SASL_PASSWORD, TRUSTSTORE_PASSWORD);
        assertThat(eventPayload).contains(SASL_PASSWORD, TRUSTSTORE_PASSWORD);
        assertThat(storedConfiguration).isEqualTo(aConnectionsConfigurationWithCredentials());
    }
}
