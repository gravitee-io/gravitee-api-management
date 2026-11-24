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
package io.gravitee.apim.infra.query_service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.model.DeploymentNumber;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.edge.EdgeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.EventLatestCursor;
import io.gravitee.repository.management.api.search.EventLatestPage;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventLatestQueryServiceImplTest {

    @Mock
    private EventLatestRepository eventLatestRepository;

    private EventLatestQueryServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new EventLatestQueryServiceImpl(eventLatestRepository);
    }

    @Test
    void should_return_every_requested_publication_beyond_the_jdbc_default_page_size() throws Exception {
        var ids = IntStream.range(0, 11)
            .mapToObj(i -> "guardrail-" + i)
            .collect(Collectors.toSet());
        var stored = ids
            .stream()
            .map(id ->
                io.gravitee.repository.management.model.Event.builder()
                    .id("event-" + id)
                    .type(EventType.PUBLISH_GENERIC_EVENT)
                    .properties(Map.of("generic_event_id", id))
                    .build()
            )
            .toList();
        when(eventLatestRepository.search(any(EventCriteria.class), any(), any(), any())).thenAnswer(invocation -> {
            Long size = invocation.getArgument(3);
            return stored.stream().limit(Objects.requireNonNullElse(size, 10L)).toList();
        });

        var result = cut.findLatestByEntityIds(
            ids,
            io.gravitee.rest.api.model.EventType.PUBLISH_GENERIC_EVENT,
            Event.EventProperties.GENERIC_EVENT_ID
        );

        assertThat(result)
            .as("the batch includes every latest publication, regardless of repository defaults")
            .extracting(Event::getId)
            .containsExactlyInAnyOrderElementsOf(stored.stream().map(io.gravitee.repository.management.model.Event::getId).toList());
    }

    @Test
    void should_find_all_by_type_and_environments() {
        var repoEvent = new io.gravitee.repository.management.model.Event();
        repoEvent.setId("event-1");
        repoEvent.setType(EventType.DEPLOY_CLUSTER);
        repoEvent.setPayload("payload");
        repoEvent.setProperties(Map.of());

        when(
            eventLatestRepository.search(
                any(EventCriteria.class),
                eq(io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID),
                isNull(),
                isNull()
            )
        ).thenReturn(List.of(repoEvent));

        List<Event> result = cut.findAllByTypeAndEnvironments(
            Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER),
            Set.of("env-1"),
            Event.EventProperties.CLUSTER_ID
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPayload()).isEqualTo("payload");
    }

    @Test
    void should_return_empty_list_when_no_events() {
        when(eventLatestRepository.search(any(EventCriteria.class), any(), isNull(), isNull())).thenReturn(List.of());

        List<Event> result = cut.findAllByTypeAndEnvironments(
            Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER),
            Set.of("env-1"),
            Event.EventProperties.CLUSTER_ID
        );

        assertThat(result).isEmpty();
    }

    @Test
    void should_pass_correct_criteria_to_repository() {
        when(eventLatestRepository.search(any(EventCriteria.class), any(), isNull(), isNull())).thenReturn(List.of());

        var criteriaCaptor = ArgumentCaptor.forClass(EventCriteria.class);

        cut.findAllByTypeAndEnvironments(
            Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER, io.gravitee.rest.api.model.EventType.UNDEPLOY_CLUSTER),
            Set.of("env-1", "env-2"),
            Event.EventProperties.CLUSTER_ID
        );

        org.mockito.Mockito.verify(eventLatestRepository).search(
            criteriaCaptor.capture(),
            eq(io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID),
            isNull(),
            isNull()
        );

        EventCriteria captured = criteriaCaptor.getValue();
        assertThat(captured.getTypes()).containsExactlyInAnyOrder(EventType.DEPLOY_CLUSTER, EventType.UNDEPLOY_CLUSTER);
        assertThat(captured.getEnvironments()).containsExactlyInAnyOrder("env-1", "env-2");
    }

    @Test
    void should_find_all_by_types_expiring_and_updated_before_instants() {
        when(eventLatestRepository.searchAfter(any(EventCriteria.class), isNull(), isNull(), anyInt())).thenReturn(
            new EventLatestPage(List.of(), null)
        );
        var criteriaCaptor = ArgumentCaptor.forClass(EventCriteria.class);

        cut
            .findAll(
                Set.of(io.gravitee.rest.api.model.EventType.UNPUBLISH_GENERIC_EVENT),
                Instant.ofEpochMilli(2000),
                Instant.ofEpochMilli(1000)
            )
            .toList();

        org.mockito.Mockito.verify(eventLatestRepository).searchAfter(criteriaCaptor.capture(), isNull(), isNull(), anyInt());
        EventCriteria captured = criteriaCaptor.getValue();
        assertThat(captured.getTypes()).containsExactly(EventType.UNPUBLISH_GENERIC_EVENT);
        assertThat(captured.getProperties()).isEmpty();
        assertThat(captured.getExpiresBefore()).isEqualTo(2000);
        assertThat(captured.getTo()).isEqualTo(1000);
        assertThat(captured.getEnvironments()).isEmpty();
    }

    @Test
    void should_find_all_across_pages() {
        var full = IntStream.range(0, EventLatestQueryServiceImpl.FIND_ALL_PAGE_SIZE)
            .mapToObj(i -> io.gravitee.repository.management.model.Event.builder().id("event-" + i).properties(Map.of()).build())
            .toList();
        var cursor = new EventLatestCursor(null, "event-99");
        when(eventLatestRepository.searchAfter(any(EventCriteria.class), isNull(), isNull(), anyInt())).thenReturn(
            new EventLatestPage(full, cursor)
        );
        when(eventLatestRepository.searchAfter(any(EventCriteria.class), isNull(), eq(cursor), anyInt())).thenReturn(
            new EventLatestPage(
                List.of(io.gravitee.repository.management.model.Event.builder().id("last").properties(Map.of()).build()),
                null
            )
        );

        var events = cut.findAll(Set.of(io.gravitee.rest.api.model.EventType.UNPUBLISH_GENERIC_EVENT), null, null).iterator();

        for (var i = 0; i < EventLatestQueryServiceImpl.FIND_ALL_PAGE_SIZE; i++) {
            events.next();
        }
        verify(eventLatestRepository, never()).searchAfter(any(EventCriteria.class), isNull(), eq(cursor), anyInt());
        assertThat(events.next().getId()).isEqualTo("last");
        assertThat(events.hasNext()).isFalse();
    }

    @Test
    void should_throw_technical_exception_on_error() {
        when(eventLatestRepository.search(any(EventCriteria.class), any(), isNull(), isNull())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
            cut.findAllByTypeAndEnvironments(
                Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER),
                Set.of("env-1"),
                Event.EventProperties.CLUSTER_ID
            )
        )
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("DB error");
    }

    /**
     * The deployment the gateways run for an API, typed. This port owns the (de)serialization of the payload, so no
     * caller of it reads the JSON of a deployment.
     */
    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class TheApiDeploymentTheGatewaysRun {

        private static final String API_ID = "api-id";
        private static final String ENVIRONMENT_ID = "environment-id";

        @Test
        void should_read_the_deployment_of_a_published_api() {
            givenTheGatewaysRun(deployment(EventType.PUBLISH_API, payloadOf(DefinitionVersion.V4, ApiType.PROXY, v4Definition()), "7"));

            assertThat(cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID))
                .as("the deployment the gateways run")
                .get()
                .satisfies(running ->
                    assertSoftly(softly -> {
                        softly.assertThat(running.eventId()).as("event id").isEqualTo("event-id");
                        softly.assertThat(running.type()).as("event type").isEqualTo(io.gravitee.rest.api.model.EventType.PUBLISH_API);
                        softly.assertThat(running.deploymentNumber()).as("deployment number").isEqualTo(DeploymentNumber.of("7"));
                        softly.assertThat(running.api().getId()).as("api id").isEqualTo(API_ID);
                        softly.assertThat(running.api().getName()).as("api name").isEqualTo("Support assistant");
                        softly.assertThat(running.api().getEnvironmentId()).as("environment id").isEqualTo(ENVIRONMENT_ID);
                        softly.assertThat(running.api().getApiDefinitionValue()).as("definition deployed").isEqualTo(v4Definition());
                    })
                );
        }

        @Test
        void should_read_the_deployment_of_a_started_api() {
            givenTheGatewaysRun(deployment(EventType.START_API, payloadOf(DefinitionVersion.V4, ApiType.PROXY, v4Definition()), null));

            assertThat(cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID))
                .as("the deployment the gateways run")
                .get()
                .satisfies(running ->
                    assertSoftly(softly -> {
                        softly.assertThat(running.type()).as("event type").isEqualTo(io.gravitee.rest.api.model.EventType.START_API);
                        softly
                            .assertThat(running.deploymentNumber())
                            .as("deployment number of a deployment carrying none")
                            .isEqualTo(DeploymentNumber.NONE);
                    })
                );
        }

        /** Every shape {@code ApiAdapter} knows, so the typed read is not a V4-only path. */
        @Test
        void should_read_the_definition_of_every_shape_an_api_can_be_deployed_with() {
            assertSoftly(softly -> {
                softly
                    .assertThat(definitionDeployed(DefinitionVersion.V4, ApiType.PROXY, v4Definition()))
                    .as("V4 HTTP definition")
                    .isInstanceOf(io.gravitee.definition.model.v4.Api.class);
                softly
                    .assertThat(definitionDeployed(DefinitionVersion.V4, ApiType.NATIVE, NativeApi.builder().id(API_ID).build()))
                    .as("V4 native definition")
                    .isInstanceOf(NativeApi.class);
                softly
                    .assertThat(definitionDeployed(DefinitionVersion.V4, ApiType.EDGE, EdgeApi.builder().id(API_ID).build()))
                    .as("V4 edge definition")
                    .isInstanceOf(EdgeApi.class);
                softly
                    .assertThat(definitionDeployed(DefinitionVersion.V2, ApiType.PROXY, v2Definition()))
                    .as("V2 definition")
                    .isInstanceOf(io.gravitee.definition.model.Api.class);
                softly
                    .assertThat(definitionDeployed(DefinitionVersion.FEDERATED, null, FederatedApi.builder().id(API_ID).build()))
                    .as("federated definition")
                    .isInstanceOf(FederatedApi.class);
            });
        }

        @Test
        void should_read_no_deployment_for_an_api_the_gateways_were_told_to_stop() {
            givenTheGatewaysRun(deployment(EventType.STOP_API, payloadOf(DefinitionVersion.V4, ApiType.PROXY, v4Definition()), null));

            assertThat(cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID)).as("the deployment the gateways run").isEmpty();
        }

        /**
         * The question is asked for one environment, so another tenant's deployment of an API of the same id is not
         * the answer: the caller would otherwise republish it into its own environment.
         */
        @Test
        void should_read_no_deployment_of_another_environment() {
            givenTheGatewaysRun(deployment(EventType.PUBLISH_API, payloadOf(DefinitionVersion.V4, ApiType.PROXY, v4Definition()), "7"));

            assertThat(cut.findRunningApiDeployment("another-environment", API_ID))
                .as("the deployment the gateways run, asked for from another environment")
                .isEmpty();
        }

        @Test
        void should_read_no_deployment_when_the_event_carries_no_payload() {
            givenTheGatewaysRun(deployment(EventType.PUBLISH_API, "   ", null));

            assertThat(cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID)).as("the deployment the gateways run").isEmpty();
        }

        /**
         * The contract: nothing half read. A payload that cannot be parsed is a failure, not a deployment with its
         * fields missing — what is read here is republished to the gateways.
         */
        @Test
        void should_fail_on_a_payload_that_is_not_a_deployment() {
            givenTheGatewaysRun(deployment(EventType.PUBLISH_API, "not json at all", null));

            assertThatThrownBy(() -> cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID))
                .as("a payload that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
        }

        /** {@code ApiAdapter.deserialize} answers {@code null} here, which would make an API with no definition. */
        @Test
        void should_fail_on_a_definition_that_cannot_be_read_rather_than_answer_a_hollow_api() {
            givenTheGatewaysRun(
                deployment(EventType.PUBLISH_API, "{\"id\":\"api-id\",\"definitionVersion\":\"4.0.0\",\"definition\":\"not json\"}", null)
            );

            assertThatThrownBy(() -> cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID))
                .as("a definition that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
        }

        /** The number is handed back to the gateways, opened one further: a number that cannot be read is a failure. */
        @Test
        void should_fail_on_a_deployment_number_that_cannot_be_read() {
            givenTheGatewaysRun(
                deployment(EventType.PUBLISH_API, payloadOf(DefinitionVersion.V4, ApiType.PROXY, v4Definition()), "not a number")
            );

            assertThatThrownBy(() -> cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID))
                .as("a deployment number that cannot be read")
                .isInstanceOf(TechnicalDomainException.class);
        }

        @Test
        void should_fail_on_a_deployment_that_carries_no_definition() {
            givenTheGatewaysRun(deployment(EventType.PUBLISH_API, "{\"id\":\"api-id\",\"definitionVersion\":\"4.0.0\"}", null));

            assertThatThrownBy(() -> cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID))
                .as("a deployment without a definition")
                .isInstanceOf(TechnicalDomainException.class);
        }

        private ApiDefinition definitionDeployed(DefinitionVersion definitionVersion, ApiType type, ApiDefinition definition) {
            givenTheGatewaysRun(deployment(EventType.PUBLISH_API, payloadOf(definitionVersion, type, definition), null));
            return cut.findRunningApiDeployment(ENVIRONMENT_ID, API_ID).orElseThrow().api().getApiDefinitionValue();
        }

        /** A V2 definition carries a proxy or its deserializer refuses it, so the minimal one is not empty. */
        private static io.gravitee.definition.model.Api v2Definition() {
            return io.gravitee.definition.model.Api.builder()
                .id(API_ID)
                .name("Support assistant")
                .version("1.0")
                .definitionVersion(DefinitionVersion.V2)
                .proxy(Proxy.builder().virtualHosts(List.of(new VirtualHost("/support"))).build())
                .build();
        }

        private static io.gravitee.definition.model.v4.Api v4Definition() {
            return io.gravitee.definition.model.v4.Api.builder()
                .id(API_ID)
                .name("Support assistant")
                .apiVersion("1.0")
                .definitionVersion(DefinitionVersion.V4)
                .type(ApiType.PROXY)
                .build();
        }

        @SneakyThrows
        private static String payloadOf(DefinitionVersion definitionVersion, ApiType type, ApiDefinition definition) {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(
                io.gravitee.repository.management.model.Api.builder()
                    .id(API_ID)
                    .name("Support assistant")
                    .environmentId(ENVIRONMENT_ID)
                    .definitionVersion(definitionVersion)
                    .type(type)
                    .definition(GraviteeJacksonMapper.getInstance().writeValueAsString(definition))
                    .build()
            );
        }

        private static io.gravitee.repository.management.model.Event deployment(EventType type, String payload, String deploymentNumber) {
            var properties = new HashMap<String, String>();
            properties.put(Event.EventProperties.API_ID.getLabel(), API_ID);
            if (deploymentNumber != null) {
                properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getLabel(), deploymentNumber);
            }
            return io.gravitee.repository.management.model.Event.builder()
                .id("event-id")
                .type(type)
                .payload(payload)
                .properties(properties)
                .environments(Set.of(ENVIRONMENT_ID))
                .build();
        }

        /**
         * {@code events_latest} holds one row per API, so only a criteria naming that row's type answers anything —
         * and only one naming its environment, which both stores restrict the search on.
         */
        private void givenTheGatewaysRun(io.gravitee.repository.management.model.Event latest) {
            when(eventLatestRepository.search(any(EventCriteria.class), any(), any(), any())).thenAnswer(invocation -> {
                EventCriteria criteria = invocation.getArgument(0);
                return Stream.of(latest)
                    .filter(event -> criteria.getTypes().contains(event.getType()))
                    .filter(event -> criteria.getEnvironments().stream().anyMatch(event.getEnvironments()::contains))
                    .toList();
            });
        }
    }
}
