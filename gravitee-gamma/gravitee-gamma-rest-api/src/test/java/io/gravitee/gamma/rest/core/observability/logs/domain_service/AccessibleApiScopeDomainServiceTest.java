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
package io.gravitee.gamma.rest.core.observability.logs.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AccessibleApiScopeDomainServiceTest {

    private final AccessibleApiScopeDomainService service = new AccessibleApiScopeDomainService();

    @Test
    void should_keep_only_apis_of_the_wanted_types() {
        var accessible = List.of(
            new AccessibleApi("api-proxy", "Proxy API", ApiType.HTTP_PROXY),
            new AccessibleApi("api-message", "Message API", ApiType.MESSAGE)
        );

        var scope = service.computeScope(accessible, Set.of(ApiType.HTTP_PROXY), null);

        assertThat(scope.apiIds()).containsExactly("api-proxy");
        assertThat(scope.apisById()).containsOnlyKeys("api-proxy");
    }

    @Test
    void should_expose_api_type_as_the_canonical_uppercase_wire_value() {
        var accessible = List.of(new AccessibleApi("api-1", "Petstore", ApiType.HTTP_PROXY));

        var scope = service.computeScope(accessible, Set.of(ApiType.HTTP_PROXY), null);

        var ref = scope.apisById().get("api-1");
        assertThat(ref.name()).isEqualTo("Petstore");
        // Must match the documented log-row apiType and the API_TYPE filter enum values (uppercase).
        assertThat(ref.apiType()).isEqualTo("HTTP_PROXY");
    }

    @Test
    void should_intersect_with_the_user_supplied_api_filter() {
        var accessible = List.of(
            new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY),
            new AccessibleApi("api-2", "API 2", ApiType.HTTP_PROXY)
        );

        var scope = service.computeScope(accessible, Set.of(ApiType.HTTP_PROXY), Set.of("api-1"));

        assertThat(scope.apiIds()).containsExactly("api-1");
    }
}
