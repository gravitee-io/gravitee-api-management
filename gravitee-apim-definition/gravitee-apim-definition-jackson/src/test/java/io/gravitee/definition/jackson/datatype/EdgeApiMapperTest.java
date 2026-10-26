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
package io.gravitee.definition.jackson.datatype;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.edge.EdgeApi;
import io.gravitee.definition.model.v4.edge.EdgeDaemonAuthentication;
import io.gravitee.definition.model.v4.edge.EdgeFleetCertificate;
import io.gravitee.definition.model.v4.edge.EdgeKeylessAuthentication;
import io.gravitee.definition.model.v4.edge.EdgeMtlsAuthentication;
import io.gravitee.definition.model.v4.edge.EdgeProxyDefinition;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EdgeApiMapperTest {

    private static final String PEM = "-----BEGIN CERTIFICATE-----MIIBszCCAVmgAwIBAgIU-----END CERTIFICATE-----";

    private final GraviteeMapper mapper = new GraviteeMapper();

    @Nested
    class Daemon_authentication {

        @Test
        void should_serialize_to_the_contract_read_by_the_edge_reactor() throws Exception {
            // Given
            var edgeApi = edgeApiWith(
                new EdgeDaemonAuthentication(new EdgeKeylessAuthentication(true), new EdgeMtlsAuthentication(true, List.of(certificate())))
            );

            // When
            var json = mapper.writeValueAsString(edgeApi);

            // Then
            assertThatJson(json)
                .node("daemonAuthentication")
                .isEqualTo(
                    """
                    {
                      "keyless": { "enabled": true },
                      "mtls": {
                        "enabled": true,
                        "certificates": [
                          {
                            "id": "certificate-id",
                            "name": "fleet-2026",
                            "certificate": "-----BEGIN CERTIFICATE-----MIIBszCCAVmgAwIBAgIU-----END CERTIFICATE-----",
                            "startsAt": 1790812800.000000000,
                            "endsAt": 1822348800.000000000
                          }
                        ]
                      }
                    }"""
                );
        }

        @Test
        void should_keep_the_certificates_while_mtls_is_disabled() throws Exception {
            // Given
            var daemonAuthentication = new EdgeDaemonAuthentication(
                new EdgeKeylessAuthentication(true),
                new EdgeMtlsAuthentication(false, List.of(certificate()))
            );

            // When
            var read = mapper.readValue(mapper.writeValueAsString(edgeApiWith(daemonAuthentication)), EdgeApi.class);

            // Then
            assertThat(read.getDaemonAuthentication()).isEqualTo(daemonAuthentication);
        }

        @Test
        void should_read_a_definition_stored_before_the_daemon_authentication() throws Exception {
            // Given
            var json = """
                { "id": "edge-api", "name": "Edge", "type": "edge", "proxy": {} }""";

            // When
            var edgeApi = mapper.readValue(json, EdgeApi.class);

            // Then
            assertThat(edgeApi.getDaemonAuthentication()).isNull();
        }

        @Test
        void should_read_mtls_without_certificates_as_an_empty_list() throws Exception {
            // Given
            var json = """
                { "id": "edge-api", "name": "Edge", "type": "edge", "proxy": {}, "daemonAuthentication": { "mtls": { "enabled": false } } }""";

            // When
            var edgeApi = mapper.readValue(json, EdgeApi.class);

            // Then
            assertThat(edgeApi.getDaemonAuthentication().mtls().certificates()).isEmpty();
        }
    }

    private static EdgeFleetCertificate certificate() {
        return new EdgeFleetCertificate(
            "certificate-id",
            "fleet-2026",
            PEM,
            Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2027-10-01T00:00:00Z")
        );
    }

    private static EdgeApi edgeApiWith(EdgeDaemonAuthentication daemonAuthentication) {
        return EdgeApi.builder()
            .id("edge-api")
            .name("Edge")
            .proxy(EdgeProxyDefinition.builder().build())
            .daemonAuthentication(daemonAuthentication)
            .build();
    }
}
