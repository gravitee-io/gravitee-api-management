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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuardedApiPortAdapterTest {

    private static final String ENVIRONMENT_ID = "environment-id";

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final GuardedApiPortAdapter adapter = new GuardedApiPortAdapter(apiCrudService);

    @Test
    void should_read_the_internal_flow_of_a_v4_api() {
        var internalFlow = Flow.builder().request(List.of(GuardianSteps.of("guardian-1", "One"))).build();
        var api = ApiFixtures.aLLMProxyApiV4();
        api.getApiDefinitionHttpV4().setInternalFlow(internalFlow);
        apiCrudService.initWith(List.of(api));

        var guardedApi = adapter.findById(ENVIRONMENT_ID, api.getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(guardedApi.name()).as("name").isEqualTo(api.getName());
            softly.assertThat(guardedApi.type()).as("type").isEqualTo(ApiType.LLM_PROXY);
            softly.assertThat(guardedApi.supportsGuardians()).as("supports guardians").isTrue();
            softly.assertThat(guardedApi.internalFlow()).as("internal flow").isEqualTo(internalFlow);
        });
    }

    @Test
    void should_not_find_an_api_of_another_environment() {
        var api = ApiFixtures.aLLMProxyApiV4();
        apiCrudService.initWith(List.of(api));

        assertThat(adapter.findById("other-environment", api.getId())).as("API of another environment").isEmpty();
    }

    @Test
    void should_tell_that_an_api_without_internal_flow_supports_no_guardian() {
        var api = ApiFixtures.aProxyApiV2();
        apiCrudService.initWith(List.of(api));

        assertThat(adapter.findById(ENVIRONMENT_ID, api.getId()))
            .as("v2 API")
            .get()
            .satisfies(guardedApi -> assertThat(guardedApi.supportsGuardians()).as("supports guardians").isFalse());
    }
}
