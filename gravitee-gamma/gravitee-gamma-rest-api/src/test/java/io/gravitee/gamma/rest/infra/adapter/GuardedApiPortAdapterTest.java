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
import inmemory.ApiDefinitionCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardian.port.GuardedApiPortContractTest;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GuardedApiPortAdapterTest extends GuardedApiPortContractTest {

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    private final ApiDefinitionCrudServiceInMemory apiDefinitionCrudService = new ApiDefinitionCrudServiceInMemory(apiCrudService);
    private final GuardedApiPortAdapter adapter = new GuardedApiPortAdapter(apiCrudService, apiQueryService, apiDefinitionCrudService);

    @Override
    protected GuardedApiPort port() {
        return adapter;
    }

    @Override
    protected void givenApi(String environmentId, String apiId, String name, Flow internalFlow) {
        var api = ApiFixtures.aLLMProxyApiV4().toBuilder().id(apiId).name(name).environmentId(environmentId).build();
        api.getApiDefinitionHttpV4().setInternalFlow(internalFlow);
        apiCrudService.create(api);
    }

    @Test
    void should_tell_that_an_api_without_internal_flow_supports_no_guardian() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));

        assertThat(adapter.findById(ENVIRONMENT_ID, ApiFixtures.aProxyApiV2().getId()))
            .as("v2 API")
            .get()
            .satisfies(guardedApi -> assertThat(guardedApi.supportsGuardians()).as("supports guardians").isFalse());
    }

    @Test
    void should_keep_the_rest_of_the_definition_and_advance_the_update_date() {
        var api = ApiFixtures.aLLMProxyApiV4();
        apiCrudService.initWith(List.of(api));
        var internalFlow = Flow.builder().response(List.of(GuardianSteps.of("guardian-1", "One"))).build();

        adapter.updateInternalFlow(ENVIRONMENT_ID, api.getId(), api.getApiDefinitionHttpV4().getInternalFlow(), internalFlow);

        var stored = apiCrudService.get(api.getId());
        assertSoftly(softly -> {
            softly.assertThat(stored.getApiDefinitionHttpV4().getInternalFlow()).as("stored internal flow").isEqualTo(internalFlow);
            softly
                .assertThat(stored.getApiDefinitionHttpV4().getEndpointGroups())
                .as("stored endpoint groups")
                .isEqualTo(api.getApiDefinitionHttpV4().getEndpointGroups());
            softly.assertThat(stored.getUpdatedAt()).as("update date").isAfter(api.getUpdatedAt());
        });
    }

    /**
     * The lost update this adapter used to cause. It reads the API, computes a definition from that read, and writes.
     * Saving the whole API would carry every field of that read back and undo whatever landed in between — here, a
     * {@code PUT} on the description. Only the definition is written, so the description survives.
     */
    @Test
    void should_keep_a_change_made_to_another_field_of_the_api_between_the_read_and_the_write() {
        var api = ApiFixtures.aLLMProxyApiV4();
        var racing = new ApiCrudServiceInMemory() {
            @Override
            public Optional<Api> findById(String id) {
                var found = super.findById(id);
                // a PUT on another field lands between this adapter's read and its write
                found.ifPresent(read -> super.update(read.toBuilder().description("Changed by someone else").build()));
                return found;
            }
        };
        racing.initWith(List.of(api));
        var adapterOnRacingStore = new GuardedApiPortAdapter(racing, apiQueryService, new ApiDefinitionCrudServiceInMemory(racing));
        var internalFlow = Flow.builder().response(List.of(GuardianSteps.of("guardian-1", "One"))).build();

        adapterOnRacingStore.updateInternalFlow(ENVIRONMENT_ID, api.getId(), api.getApiDefinitionHttpV4().getInternalFlow(), internalFlow);

        var stored = racing.get(api.getId());
        assertSoftly(softly -> {
            softly.assertThat(stored.getDescription()).as("the field written in between").isEqualTo("Changed by someone else");
            softly.assertThat(stored.getApiDefinitionHttpV4().getInternalFlow()).as("stored internal flow").isEqualTo(internalFlow);
        });
    }

    /**
     * The same window, with a writer that also advances the update date, which every API write of the platform does.
     * The write then does not happen at all: the other writer may have touched the definition too, and merging the
     * two is not something this adapter can decide. The caller re-reads and retries.
     */
    @Test
    void should_refuse_to_write_when_the_api_moved_between_the_read_and_the_write() {
        var api = ApiFixtures.aLLMProxyApiV4();
        var racing = new ApiCrudServiceInMemory() {
            @Override
            public Optional<Api> findById(String id) {
                var found = super.findById(id);
                found.ifPresent(read ->
                    super.update(
                        read.toBuilder().description("Changed by someone else").updatedAt(read.getUpdatedAt().plusSeconds(1)).build()
                    )
                );
                return found;
            }
        };
        racing.initWith(List.of(api));
        var adapterOnRacingStore = new GuardedApiPortAdapter(racing, apiQueryService, new ApiDefinitionCrudServiceInMemory(racing));
        var readFlow = api.getApiDefinitionHttpV4().getInternalFlow();

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() ->
                    adapterOnRacingStore.updateInternalFlow(
                        ENVIRONMENT_ID,
                        api.getId(),
                        readFlow,
                        Flow.builder().response(List.of(GuardianSteps.of("guardian-1", "One"))).build()
                    )
                )
                .as("a write the API moved past")
                .isInstanceOf(ApiInternalFlowChangedException.class);
            var stored = racing.get(api.getId());
            softly.assertThat(stored.getDescription()).as("the field written in between").isEqualTo("Changed by someone else");
            softly.assertThat(stored.getApiDefinitionHttpV4().getInternalFlow()).as("stored internal flow").isEqualTo(readFlow);
        });
    }
}
