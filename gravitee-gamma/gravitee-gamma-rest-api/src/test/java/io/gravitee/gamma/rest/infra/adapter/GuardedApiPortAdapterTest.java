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
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.DeployApiInternalFlowDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardrail.port.GuardedApiPortContractTest;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GuardedApiPortAdapterTest extends GuardedApiPortContractTest {

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    /**
     * Numbers its pages from zero, as {@code ApiQueryServiceImpl} does: it hands over the page of the API repository,
     * which counts from zero, where {@link ApiQueryServiceInMemory} counts from one.
     */
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService) {
        @Override
        public Page<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, Pageable pageable, ApiFieldFilter apiFieldFilter) {
            var page = super.search(apiCriteria, sortable, pageable, apiFieldFilter);
            return new Page<>(page.getContent(), page.getPageNumber() - 1, (int) page.getPageElements(), page.getTotalElements());
        }
    };
    private final ApiDefinitionCrudServiceInMemory apiDefinitionCrudService = new ApiDefinitionCrudServiceInMemory(apiCrudService);
    private final EventCrudInMemory events = new EventCrudInMemory();
    private final EventLatestCrudInMemory latestEvents = new EventLatestCrudInMemory();
    private final EventLatestQueryServiceInMemory deployments = new EventLatestQueryServiceInMemory();
    private final DeployApiInternalFlowDomainService deployApiInternalFlow = new DeployApiInternalFlowDomainService(
        apiDefinitionCrudService,
        events,
        latestEvents,
        deployments
    );
    private final GuardedApiPortAdapter adapter = new GuardedApiPortAdapter(apiCrudService, apiQueryService, deployApiInternalFlow);

    @Override
    protected GuardedApiPort port() {
        return adapter;
    }

    /** The same adapter over another API store, so a test can make that store race with it. */
    private GuardedApiPortAdapter adapterOver(ApiCrudServiceInMemory store) {
        return new GuardedApiPortAdapter(
            store,
            apiQueryService,
            new DeployApiInternalFlowDomainService(new ApiDefinitionCrudServiceInMemory(store), events, latestEvents, deployments)
        );
    }

    @Override
    protected void givenApi(String environmentId, String apiId, String name, Flow internalFlow) {
        var api = ApiFixtures.aLLMProxyApiV4().toBuilder().id(apiId).name(name).environmentId(environmentId).build();
        api.getApiDefinitionHttpV4().setInternalFlow(internalFlow);
        apiCrudService.create(api);
    }

    @Test
    void should_tell_that_an_api_without_internal_flow_supports_no_guardrail() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));

        assertThat(adapter.findById(ENVIRONMENT_ID, ApiFixtures.aProxyApiV2().getId()))
            .as("v2 API")
            .get()
            .satisfies(guardedApi -> assertThat(guardedApi.supportsGuardrails()).as("supports guardrails").isFalse());
    }

    @Test
    void should_keep_the_rest_of_the_definition_and_advance_the_update_date() {
        var api = ApiFixtures.aLLMProxyApiV4();
        apiCrudService.initWith(List.of(api));
        var internalFlow = Flow.builder().response(List.of(GuardrailSteps.of("guardrail-1", "One"))).build();

        adapter.updateInternalFlow(CALLER, api.getId(), api.getApiDefinitionHttpV4().getInternalFlow(), internalFlow);

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
        var adapterOnRacingStore = adapterOver(racing);
        var internalFlow = Flow.builder().response(List.of(GuardrailSteps.of("guardrail-1", "One"))).build();

        adapterOnRacingStore.updateInternalFlow(CALLER, api.getId(), api.getApiDefinitionHttpV4().getInternalFlow(), internalFlow);

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
        var adapterOnRacingStore = adapterOver(racing);
        var readFlow = api.getApiDefinitionHttpV4().getInternalFlow();

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() ->
                    adapterOnRacingStore.updateInternalFlow(
                        CALLER,
                        api.getId(),
                        readFlow,
                        Flow.builder().response(List.of(GuardrailSteps.of("guardrail-1", "One"))).build()
                    )
                )
                .as("a write the API moved past")
                .isInstanceOf(ApiInternalFlowChangedException.class);
            var stored = racing.get(api.getId());
            softly.assertThat(stored.getDescription()).as("the field written in between").isEqualTo("Changed by someone else");
            softly.assertThat(stored.getApiDefinitionHttpV4().getInternalFlow()).as("stored internal flow").isEqualTo(readFlow);
        });
    }

    /**
     * The write applies the change: the gateways are given the deployment they already run with the new internal
     * flow, and nothing else of the API travels with it.
     */
    @Nested
    class SendingTheChangeToTheGateways {

        @Test
        void should_send_the_guardrail_turned_on_to_the_gateways() {
            var api = ApiFixtures.aLLMProxyApiV4();
            apiCrudService.initWith(List.of(api));
            givenTheGatewaysRun(api, EventType.PUBLISH_API);
            var internalFlow = Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"))).build();

            adapter.updateInternalFlow(CALLER, api.getId(), api.getApiDefinitionHttpV4().getInternalFlow(), internalFlow);

            assertSoftly(softly -> {
                softly
                    .assertThat(events.storage())
                    .as("events written")
                    .singleElement()
                    .satisfies(event -> {
                        softly.assertThat(event.getType()).as("event type").isEqualTo(EventType.PUBLISH_API);
                        softly.assertThat(event.getPayload()).as("payload sent").contains("guardrail-1");
                    });
                softly.assertThat(latestEvents.storage()).as("latest events").hasSize(1);
            });
        }

        @Test
        void should_send_the_guardrail_turned_off_to_the_gateways() {
            var ran = Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"))).build();
            var api = ApiFixtures.aLLMProxyApiV4();
            api.getApiDefinitionHttpV4().setInternalFlow(ran);
            apiCrudService.initWith(List.of(api));
            givenTheGatewaysRun(api, EventType.PUBLISH_API);

            adapter.updateInternalFlow(CALLER, api.getId(), ran, null);

            assertThat(events.storage())
                .as("events written")
                .singleElement()
                .satisfies(event -> assertThat(event.getPayload()).as("payload sent").doesNotContain("guardrail-1"));
        }

        /** E1: nothing runs, so nothing is applied. The definition is written and the API reads as out of sync. */
        @Test
        void should_send_nothing_for_an_api_the_gateways_do_not_run() {
            var api = ApiFixtures.aLLMProxyApiV4();
            apiCrudService.initWith(List.of(api));
            var internalFlow = Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"))).build();

            adapter.updateInternalFlow(CALLER, api.getId(), api.getApiDefinitionHttpV4().getInternalFlow(), internalFlow);

            assertSoftly(softly -> {
                softly
                    .assertThat(apiCrudService.get(api.getId()).getApiDefinitionHttpV4().getInternalFlow())
                    .as("stored internal flow")
                    .isEqualTo(internalFlow);
                softly.assertThat(events.storage()).as("events written").isEmpty();
            });
        }

        /** E3: the order of the writes is what makes this possible — the payload is built before anything is written. */
        @Test
        void should_write_nothing_at_all_when_what_the_gateways_run_cannot_be_read() {
            var api = ApiFixtures.aLLMProxyApiV4();
            apiCrudService.initWith(List.of(api));
            deployments.initWith(
                List.of(
                    Event.builder()
                        .id("event-id")
                        .type(EventType.PUBLISH_API)
                        .payload("not a deployment")
                        .environments(Set.of(ENVIRONMENT_ID))
                        .properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, api.getId())))
                        .build()
                )
            );
            var readFlow = api.getApiDefinitionHttpV4().getInternalFlow();

            assertSoftly(softly -> {
                softly
                    .assertThatThrownBy(() ->
                        adapter.updateInternalFlow(
                            CALLER,
                            api.getId(),
                            readFlow,
                            Flow.builder().request(List.of(GuardrailSteps.of("guardrail-1", "One"))).build()
                        )
                    )
                    .as("a deployment that cannot be read")
                    .isInstanceOf(TechnicalDomainException.class);
                softly
                    .assertThat(apiCrudService.get(api.getId()).getApiDefinitionHttpV4().getInternalFlow())
                    .as("stored internal flow")
                    .isEqualTo(readFlow);
                softly.assertThat(events.storage()).as("events written").isEmpty();
            });
        }

        @SneakyThrows
        private void givenTheGatewaysRun(Api api, EventType type) {
            var payload = io.gravitee.repository.management.model.Api.builder()
                .id(api.getId())
                .name(api.getName())
                .environmentId(api.getEnvironmentId())
                .definitionVersion(DefinitionVersion.V4)
                .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(api.getApiDefinitionHttpV4()))
                .build();
            deployments.initWith(
                List.of(
                    Event.builder()
                        .id("event-id")
                        .type(type)
                        .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(payload))
                        // The deployment is read for the environment of the caller, as both stores scope it.
                        .environments(Set.of(ENVIRONMENT_ID))
                        .properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, api.getId())))
                        .build()
                )
            );
        }
    }
}
