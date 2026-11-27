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
package io.gravitee.rest.api.management.v2.rest.utils;

import static io.gravitee.rest.api.management.v2.rest.resource.api.ApiDefinitionFixtures.httpApi;
import static io.gravitee.rest.api.management.v2.rest.resource.api.ApiDefinitionFixtures.nativeApi;
import static io.gravitee.rest.api.management.v2.rest.resource.api.ApiDefinitionFixtures.v2Api;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ApiDefinitionFilterTest {

    @Test
    void should_remove_definition_details_of_v4_http_api() {
        var api = httpApi("api");

        ApiDefinitionFilter.filterSensitiveData(api);

        assertThat(api.getEndpointGroups()).isNull();
        assertThat(api.getProperties()).isNull();
        assertThat(api.getResources()).isNull();
        var listener = (HttpListener) api.getListeners().getFirst();
        assertThat(listener.getPaths()).hasSize(1);
        assertThat(listener.getPaths().getFirst().getHost()).isNull();
        assertThat(listener.getPaths().getFirst().getPath()).isEqualTo("/first");
        assertThat(listener.getPathMappings()).isNull();
    }

    @Test
    void should_remove_definition_details_of_native_api() {
        var api = nativeApi("api");

        ApiDefinitionFilter.filterSensitiveData(api);

        assertThat(api.getEndpointGroups()).isNull();
        assertThat(api.getProperties()).isNull();
        assertThat(api.getResources()).isNull();
    }

    @Test
    void should_keep_only_the_first_virtual_host_path_of_v2_api() {
        var api = v2Api("api");

        ApiDefinitionFilter.filterSensitiveData(api);

        assertThat(api.getProxy().getGroups()).isNullOrEmpty();
        assertThat(api.getProxy().getVirtualHosts()).hasSize(1);
        assertThat(api.getProxy().getVirtualHosts().getFirst().getHost()).isNull();
        assertThat(api.getProxy().getVirtualHosts().getFirst().getPath()).isEqualTo("/first");
        assertThat(api.getProperties()).isNull();
    }

    @Test
    void should_return_an_empty_proxy_for_v2_api_without_virtual_hosts() {
        var api = v2Api("api");
        api.getProxy().setVirtualHosts(new ArrayList<>());

        ApiDefinitionFilter.filterSensitiveData(api);

        assertThat(api.getProxy().getGroups()).isNullOrEmpty();
        assertThat(api.getProxy().getVirtualHosts()).isNullOrEmpty();
    }

    @Test
    void should_return_an_empty_proxy_for_v2_api_without_proxy() {
        var api = v2Api("api");
        api.setProxy(null);

        ApiDefinitionFilter.filterSensitiveData(api);

        assertThat(api.getProxy()).isNotNull();
        assertThat(api.getProxy().getGroups()).isNullOrEmpty();
    }

    @Test
    void should_ignore_a_missing_api() {
        assertThatCode(() -> ApiDefinitionFilter.filterSensitiveData(null)).doesNotThrowAnyException();
    }
}
