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
package io.gravitee.apim.core.cluster.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateKafkaClusterBindingDomainServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String CLUSTER_SHARED = "{\"mode\":\"CLUSTER\",\"clusterCrossId\":\"my-cluster\"}";

    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final ObjectMapper objectMapper = new GraviteeMapper();
    private ValidateKafkaClusterBindingDomainService service;

    @BeforeEach
    void setUp() {
        eventLatestQueryService.reset();
        service = new ValidateKafkaClusterBindingDomainService(
            new DeployedClustersDomainService(eventLatestQueryService, objectMapper),
            objectMapper
        );
    }

    @Test
    void should_accept_an_endpoint_bound_to_an_existing_connection() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatCode(() ->
            service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null))))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_ignore_direct_mode_and_other_endpoint_types() {
        EndpointGroup mqtt = EndpointGroup.builder()
            .name("mqtt")
            .type("mqtt5")
            .sharedConfiguration(CLUSTER_SHARED)
            .endpoints(List.of())
            .build();

        assertThatCode(() ->
            service.validate(ENV_ID, List.of(group("{\"security\":{\"protocol\":\"PLAINTEXT\"}}", endpoint(null, true, null)), mqtt))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_reject_an_unknown_cluster() {
        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage(
                "Endpoint [default] of group [default-group]: Kafka cluster [my-cluster] is not deployed in this environment. Deploy it or pick a deployed cluster."
            );
    }

    @Test
    void should_reject_a_cluster_of_another_environment() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatThrownBy(() -> service.validate("other-env", List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null))))).isInstanceOf(
            InvalidDataException.class
        );
    }

    @Test
    void should_reject_a_cluster_that_is_not_a_registered_kafka_cluster() {
        deploy(kafkaCluster(ClusterType.KAFKA_VIRTUAL_CLUSTER, "PLAIN"));

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("KAFKA_CLUSTER");
    }

    @Test
    void should_reject_an_unknown_connection() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("unknown", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("connection [unknown] does not exist in the deployed Kafka cluster [my-cluster]");
    }

    @Test
    void should_reject_an_endpoint_without_connection() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint(null, true, null))))).isInstanceOf(
            InvalidDataException.class
        );
    }

    @Test
    void should_reject_a_sasl_connection_without_a_mechanism() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "NONE"));

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("uses the SASL_SSL protocol without a SASL mechanism");
    }

    @ParameterizedTest
    @ValueSource(strings = { "DELEGATE_TO_BROKER", "OAUTHBEARER", "OAUTHBEARER_TOKEN", "GSSAPI", "AWS_MSK_IAM" })
    void should_reject_a_connection_using_an_unsupported_mechanism(String mechanism) {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, mechanism));

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining(mechanism);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512" })
    void should_accept_a_connection_using_a_supported_mechanism(String mechanism) {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, mechanism));

        assertThatCode(() ->
            service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null))))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_accept_an_endpoint_overriding_its_cluster_group_with_a_direct_connection() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatCode(() ->
            service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint(null, false, "{\"security\":{\"protocol\":\"PLAINTEXT\"}}"))))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_validate_the_cluster_an_endpoint_override_uses_in_a_direct_group() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatCode(() ->
            service.validate(ENV_ID, List.of(group("{\"security\":{\"protocol\":\"PLAINTEXT\"}}", endpoint("sasl", false, CLUSTER_SHARED))))
        ).doesNotThrowAnyException();
        assertThatThrownBy(() ->
            service.validate(
                ENV_ID,
                List.of(group("{\"security\":{\"protocol\":\"PLAINTEXT\"}}", endpoint("unknown", false, CLUSTER_SHARED)))
            )
        )
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("connection [unknown] does not exist in the deployed Kafka cluster [my-cluster]");
    }

    @Test
    void should_accept_a_default_connection_of_the_group_cluster() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatCode(() ->
            service.validate(ENV_ID, List.of(group(withDefaultConnection(CLUSTER_SHARED, "sasl"), endpoint("sasl", true, null))))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_default_connection_missing_from_the_group_cluster() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN"));

        assertThatThrownBy(() ->
            service.validate(ENV_ID, List.of(group(withDefaultConnection(CLUSTER_SHARED, "unknown"), endpoint("sasl", true, null))))
        )
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("connection [unknown] does not exist in the deployed Kafka cluster [my-cluster]");
    }

    @Test
    void should_reject_a_default_connection_using_an_unsupported_mechanism() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "OAUTHBEARER"));

        assertThatThrownBy(() ->
            service.validate(
                ENV_ID,
                List.of(
                    // The endpoint overrides the group with a direct connection: only the group's default is checked.
                    group(
                        withDefaultConnection(CLUSTER_SHARED, "sasl"),
                        endpoint(null, false, "{\"security\":{\"protocol\":\"PLAINTEXT\"}}")
                    )
                )
            )
        )
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("OAUTHBEARER");
    }

    @Test
    void should_check_the_deployed_cluster_not_the_one_being_edited() {
        Cluster deployed = kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN");
        deploy(deployed);

        // A connection added to the cluster but not redeployed yet does not exist on the gateways.
        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("added-later", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("connection [added-later] does not exist in the deployed Kafka cluster [my-cluster]");
    }

    @Test
    void should_name_the_endpoint_and_what_to_do_when_its_connection_is_not_supported() {
        deploy(kafkaCluster(ClusterType.KAFKA_CLUSTER, "OAUTHBEARER_TOKEN"));

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage(
                "Endpoint [default] of group [default-group]: connection [sasl] of Kafka cluster [my-cluster] cannot be used by Message APIs. Message APIs do not support the SASL mechanism OAUTHBEARER_TOKEN yet. Pick another connection or switch the group to Direct mode."
            );
    }

    @Test
    void should_reject_a_trust_all_connection_on_save() {
        Cluster cluster = kafkaCluster(ClusterType.KAFKA_CLUSTER, "PLAIN");
        cluster.setConfiguration(
            Map.of(
                "connections",
                List.of(
                    Map.of(
                        "crossId",
                        "sasl",
                        "name",
                        "Trust all",
                        "bootstrapServers",
                        "kafka:9094",
                        "security",
                        Map.of("protocol", "SSL", "ssl", Map.of("trustAll", true))
                    )
                )
            )
        );
        deploy(cluster);

        assertThatThrownBy(() -> service.validate(ENV_ID, List.of(group(CLUSTER_SHARED, endpoint("sasl", true, null)))))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("cannot be used by Message APIs. Trust all is not supported: give a truststore.");
    }

    private void deploy(Cluster cluster) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, cluster.getCrossId());
        try {
            eventLatestQueryService.initWith(
                List.of(
                    Event.builder()
                        .id("event-" + cluster.getCrossId())
                        .type(EventType.DEPLOY_CLUSTER)
                        .payload(objectMapper.writeValueAsString(cluster))
                        .environments(Set.of(cluster.getEnvironmentId()))
                        .properties(properties)
                        .createdAt(ZonedDateTime.now())
                        .build()
                )
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String withDefaultConnection(String sharedConfiguration, String connectionCrossId) {
        return sharedConfiguration.replace("}", ",\"defaultConnectionCrossId\":\"" + connectionCrossId + "\"}");
    }

    private static EndpointGroup group(String sharedConfiguration, Endpoint endpoint) {
        return EndpointGroup.builder()
            .name("default-group")
            .type("kafka")
            .sharedConfiguration(sharedConfiguration)
            .endpoints(List.of(endpoint))
            .build();
    }

    private static Endpoint endpoint(String connectionCrossId, boolean inherit, String override) {
        return Endpoint.builder()
            .name("default")
            .type("kafka")
            .inheritConfiguration(inherit)
            .sharedConfigurationOverride(override)
            .configuration(
                connectionCrossId == null
                    ? "{\"bootstrapServers\":\"kafka:9092\"}"
                    : "{\"connectionCrossId\":\"" + connectionCrossId + "\"}"
            )
            .build();
    }

    private static Cluster kafkaCluster(ClusterType type, String mechanism) {
        return Cluster.builder()
            .id("cluster-id")
            .crossId("my-cluster")
            .type(type)
            .name("My cluster")
            .environmentId(ENV_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .configuration(
                Map.of(
                    "connections",
                    List.of(
                        Map.of(
                            "crossId",
                            "sasl",
                            "name",
                            "Sasl",
                            "bootstrapServers",
                            "kafka:9093",
                            "security",
                            Map.of("protocol", "SASL_SSL", "sasl", Map.of("mechanism", Map.of("type", mechanism)))
                        )
                    )
                )
            )
            .build();
    }
}
