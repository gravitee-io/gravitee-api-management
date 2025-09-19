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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.gravitee.apim.infra.converter.oai.OAIServersConverter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OAIServersConverterTest {

    @ParameterizedTest
    @NullSource
    @EmptySource
    void should_return_empty_list_when_servers_is_null_or_empty(List<Server> servers) {
        assertThat(OAIServersConverter.INSTANCE.convert(servers)).isEmpty();
    }

    @Nested
    class WithoutVariables {

        @Test
        void should_return_server_url_when_server_has_no_variables() {
            // Given
            Server server1 = new Server();
            server1.setUrl("http://localhost:8080");
            Server server2 = new Server();
            server2.setUrl("http://localhost:8081");
            List<Server> servers = List.of(server1, server2);

            // When
            List<String> result = OAIServersConverter.INSTANCE.convert(servers);

            // Then
            assertThat(result).containsExactly("http://localhost:8080", "http://localhost:8081");
        }
    }

    @Nested
    class WithVariables {

        @Test
        void should_map_server_urls_with_single_variables() {
            // Given
            var server1 = new Server();
            server1.setUrl("https://api.company.com/{basePath}");
            server1.setVariables(
                new ServerVariables()
                    .addServerVariable("basePath", new ServerVariable().description("Base path")._default("v1"))
                    .addServerVariable("anotherVariable", new ServerVariable().description("Another Variable")._default("value"))
            );

            var server2 = new Server();
            server2.setUrl("https://api2.company.com");

            var server3 = new Server();
            server3.setUrl("https://api3.company.com/{basePath}");
            server3.setVariables(
                new ServerVariables().addServerVariable(
                    "basePath",
                    new ServerVariable().description("Base path")._enum(List.of("v2", "v3"))
                )
            );

            var server4 = new Server();
            server4.setUrl("https://api4.company.com/{basePath}");
            server4.setVariables(
                new ServerVariables()
                    .addServerVariable(null, new ServerVariable().description("Base path")._default("v1"))
                    .addServerVariable("variable", null)
            );

            // When
            var result = OAIServersConverter.INSTANCE.convert(List.of(server1, server2, server3, server4));

            // Then
            assertThat(result).containsExactlyInAnyOrder(
                "https://api.company.com/v1",
                "https://api2.company.com",
                "https://api3.company.com/v2",
                "https://api3.company.com/v3"
            );
        }

        @Test
        void should_map_server_urls_with_multiple_variables() {
            // Given
            var server = new Server();
            server.setUrl("https://demo.gravitee.io:{port}/gateway/{version}");
            server.setVariables(
                new ServerVariables()
                    .addServerVariable("port", new ServerVariable()._default("8443")._enum(List.of("443", "8443")))
                    .addServerVariable("version", new ServerVariable().description("Version")._enum(List.of("v1", "v2"))._default("v1"))
            );

            // When
            var result = OAIServersConverter.INSTANCE.convert(List.of(server));

            // Then
            assertThat(result).containsExactlyInAnyOrder(
                "https://demo.gravitee.io:8443/gateway/v1",
                "https://demo.gravitee.io:443/gateway/v1",
                "https://demo.gravitee.io:8443/gateway/v2",
                "https://demo.gravitee.io:443/gateway/v2"
            );
        }
    }
}
