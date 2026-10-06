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
package io.gravitee.apim.core.flow.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResourceNamesTest {

    @Test
    void shouldReturnEmptySetWhenResourcesNull() {
        assertThat(ApiResourceNames.from(null)).isEmpty();
    }

    @Test
    void shouldCollectNonNullResourceNames() {
        Resource named = new Resource();
        named.setName("schema-registry");
        Resource unnamed = new Resource();

        assertThat(ApiResourceNames.from(List.of(named, unnamed))).containsExactly("schema-registry");
    }

    @Test
    void shouldReadNamesFromHttpV4Api() {
        Resource named = new Resource();
        named.setName("sr");
        var definition = new io.gravitee.definition.model.v4.Api();
        definition.setResources(List.of(named));
        Api api = Api.builder().apiDefinitionHttpV4(definition).build();

        assertThat(ApiResourceNames.fromHttpV4(api)).containsExactly("sr");
        assertThat(ApiResourceNames.fromHttpV4(null)).isEmpty();
    }
}
