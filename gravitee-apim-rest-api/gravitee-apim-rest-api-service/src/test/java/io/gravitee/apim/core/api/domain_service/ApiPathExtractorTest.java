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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiPathExtractorTest {

    @Test
    void extracts_sanitized_paths_from_v4_http_listener() {
        var api = ApiFixtures.aProxyApiV4();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).extracting("host", "path", "overrideAccess").containsExactly(tuple(null, "/http_proxy/", false));
    }

    @Test
    void extracts_sanitized_paths_from_v2_virtual_hosts() {
        var api = ApiFixtures.aProxyApiV2();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).extracting("host", "path", "overrideAccess").containsExactly(tuple(null, "/path/", false));
    }

    @Test
    void returns_empty_for_v4_native_api() {
        var api = ApiFixtures.aNativeApi();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).isEmpty();
    }

    @Test
    void returns_empty_for_federated_api() {
        var api = ApiFixtures.aFederatedApi();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).isEmpty();
    }

    @Test
    void returns_empty_when_api_definition_is_null() {
        var api = new io.gravitee.apim.core.api.model.Api();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).isEmpty();
    }

    @Test
    void getPathsWithoutHost_filters_paths_with_no_host() {
        var paths = List.of(
            Path.builder().path("/a").build(),
            Path.builder().host("h1").path("/b").build(),
            Path.builder().host("").path("/c").build()
        );

        var result = ApiPathExtractor.getPathsWithoutHost(paths);

        assertThat(result).containsExactly("/a", "/c");
    }

    @Test
    void getPathsWithHost_groups_by_host_excluding_empty_host() {
        var paths = List.of(
            Path.builder().host("h1").path("/a").build(),
            Path.builder().host("h1").path("/b").build(),
            Path.builder().host("h2").path("/c").build(),
            Path.builder().path("/no-host").build()
        );

        var result = ApiPathExtractor.getPathsWithHost(paths);

        assertThat(result).containsOnly(entry("h1", List.of("/a", "/b")), entry("h2", List.of("/c")));
    }

    @Test
    void findConflictingPathError_returns_error_when_existing_prefixes_candidate() {
        var error = ApiPathExtractor.findConflictingPathError("/foo/bar/", List.of("/foo/"));

        assertThat(error).isPresent();
        assertThat(error.get().getMessage()).isEqualTo("Path [/foo/] already exists");
        assertThat(error.get().isSevere()).isTrue();
    }

    @Test
    void findConflictingPathError_returns_error_when_candidate_prefixes_existing() {
        var error = ApiPathExtractor.findConflictingPathError("/foo/", List.of("/foo/bar/"));

        assertThat(error).isPresent();
        assertThat(error.get().getMessage()).isEqualTo("Path [/foo/bar/] already exists");
    }

    @Test
    void findConflictingPathError_returns_empty_when_paths_are_disjoint() {
        var error = ApiPathExtractor.findConflictingPathError("/foo/", List.of("/bar/", "/baz/"));

        assertThat(error).isEmpty();
    }

    @Test
    void extractPathsFromVirtualHosts_sanitizes_and_preserves_host_and_override() {
        var vh1 = new VirtualHost(null, "/a", false);
        var vh2 = new VirtualHost("domain.com", "/b", true);

        var paths = ApiPathExtractor.extractPathsFromVirtualHosts(List.of(vh1, vh2));

        assertThat(paths)
            .extracting("host", "path", "overrideAccess")
            .containsExactly(tuple(null, "/a/", false), tuple("domain.com", "/b/", true));
    }

    @Test
    void extractPaths_returns_empty_for_v2_api_with_null_proxy() {
        var apiDef = io.gravitee.definition.model.Api.builder().build();
        var api = io.gravitee.apim.core.api.model.Api.builder().apiDefinition(apiDef).build();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).isEmpty();
    }

    @Test
    void extractPaths_returns_empty_for_v4_api_with_null_listeners() {
        var apiDef = io.gravitee.definition.model.v4.Api.builder().build();
        var api = io.gravitee.apim.core.api.model.Api.builder().apiDefinitionHttpV4(apiDef).build();

        var paths = ApiPathExtractor.extractPaths(api);

        assertThat(paths).isEmpty();
    }

    @Test
    void extractPathsFromV4Listeners_picks_only_http_listeners() {
        var httpListener = HttpListener.builder()
            .paths(
                List.of(
                    io.gravitee.definition.model.v4.listener.http.Path.builder().path("/x").build(),
                    io.gravitee.definition.model.v4.listener.http.Path.builder().host("h").path("/y").build()
                )
            )
            .build();
        var tcpListener = TcpListener.builder().build();

        var paths = ApiPathExtractor.extractPathsFromV4Listeners(List.<Listener>of(tcpListener, httpListener));

        assertThat(paths).extracting("host", "path", "overrideAccess").containsExactly(tuple(null, "/x/", false), tuple("h", "/y/", false));
    }
}
