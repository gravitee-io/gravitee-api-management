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
package io.gravitee.rest.api.kafkaexplorer.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.BrowseMessagesUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeBrokerUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeConsumerGroupUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeTopicUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListConsumerGroupsUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListTopicsUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.TailMessagesUseCase;
import io.gravitee.rest.api.kafkaexplorer.rest.model.BrowseMessagesRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeConsumerGroupRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListConsumerGroupsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsRequest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The environment-level permission is held by every publisher, so each endpoint must also check the
 * caller can read the target cluster, and must answer as if a denied cluster did not exist.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceAccessTest {

    private static final String CLUSTER_ID = "cluster-1";

    private PermissionService permissionService;
    private DescribeKafkaClusterUseCase describeCluster;
    private ListTopicsUseCase listTopics;
    private DescribeTopicUseCase describeTopic;
    private DescribeBrokerUseCase describeBroker;
    private ListConsumerGroupsUseCase listConsumerGroups;
    private DescribeConsumerGroupUseCase describeConsumerGroup;
    private BrowseMessagesUseCase browseMessages;
    private TailMessagesUseCase tailMessages;
    private KafkaExplorerResource resource;

    @BeforeEach
    void setUp() throws Exception {
        GraviteeContext.setCurrentEnvironment("env-1");
        permissionService = mock(PermissionService.class);
        describeCluster = mock(DescribeKafkaClusterUseCase.class);
        listTopics = mock(ListTopicsUseCase.class);
        describeTopic = mock(DescribeTopicUseCase.class);
        describeBroker = mock(DescribeBrokerUseCase.class);
        listConsumerGroups = mock(ListConsumerGroupsUseCase.class);
        describeConsumerGroup = mock(DescribeConsumerGroupUseCase.class);
        browseMessages = mock(BrowseMessagesUseCase.class);
        tailMessages = mock(TailMessagesUseCase.class);

        resource = new KafkaExplorerResource();
        inject("permissionService", permissionService);
        inject("describeKafkaClusterUseCase", describeCluster);
        inject("listTopicsUseCase", listTopics);
        inject("describeTopicUseCase", describeTopic);
        inject("describeBrokerUseCase", describeBroker);
        inject("listConsumerGroupsUseCase", listConsumerGroups);
        inject("describeConsumerGroupUseCase", describeConsumerGroup);
        inject("browseMessagesUseCase", browseMessages);
        inject("tailMessagesUseCase", tailMessages);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    static Stream<Arguments> endpoints() {
        return Stream.of(
            named("describe-cluster", r -> r.describeCluster(new DescribeClusterRequest().clusterId(CLUSTER_ID))),
            named("list-topics", r -> r.listTopics(new ListTopicsRequest().clusterId(CLUSTER_ID), 1, 10, "name", "asc")),
            named("describe-topic", r -> r.describeTopic(new DescribeTopicRequest().clusterId(CLUSTER_ID))),
            named("describe-broker", r -> r.describeBroker(new DescribeBrokerRequest().clusterId(CLUSTER_ID))),
            named("list-consumer-groups", r ->
                r.listConsumerGroups(new ListConsumerGroupsRequest().clusterId(CLUSTER_ID), 1, 25, "groupId", "asc")
            ),
            named("describe-consumer-group", r -> r.describeConsumerGroup(new DescribeConsumerGroupRequest().clusterId(CLUSTER_ID))),
            named("browse-messages", r -> r.browseMessages(new BrowseMessagesRequest().clusterId(CLUSTER_ID), 50)),
            named("tail-messages", r -> r.tailMessages(CLUSTER_ID, "topic", null, null, null, 10, 5, null, null))
        );
    }

    private static Arguments named(String name, Consumer<KafkaExplorerResource> call) {
        return Arguments.of(Named.of(name, call));
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void should_answer_like_a_missing_cluster_when_the_caller_cannot_read_it(Consumer<KafkaExplorerResource> call) {
        // only another permission is granted: nothing but CLUSTER_DEFINITION[READ] on this cluster may open the endpoint
        when(permissionService.hasPermission(any(), eq(RolePermission.CLUSTER_MEMBER), eq(CLUSTER_ID), any())).thenReturn(true);

        assertThatThrownBy(() -> call.accept(resource)).isInstanceOfSatisfying(DbEntityNotFoundException.class, e -> {
            var missing = new DbEntityNotFoundException(Cluster.class, CLUSTER_ID);
            assertThat(e.getMessage()).isEqualTo(missing.getMessage());
            assertThat(e.getTechnicalCode()).isEqualTo(missing.getTechnicalCode());
            assertThat(e.getParameters()).isEqualTo(missing.getParameters());
        });
        verifyNoInteractions(
            describeCluster,
            listTopics,
            describeTopic,
            describeBroker,
            listConsumerGroups,
            describeConsumerGroup,
            browseMessages,
            tailMessages
        );
    }

    @Test
    void should_reach_the_use_case_when_the_caller_can_read_exactly_this_cluster() {
        when(
            permissionService.hasPermission(any(), eq(RolePermission.CLUSTER_DEFINITION), eq(CLUSTER_ID), eq(RolePermissionAction.READ))
        ).thenReturn(true);
        when(describeCluster.execute(any())).thenThrow(new KafkaExplorerException("unreachable", TechnicalCode.TIMEOUT));

        var response = resource.describeCluster(new DescribeClusterRequest().clusterId(CLUSTER_ID));

        assertThat(response.getStatus()).isEqualTo(502);
        verify(describeCluster).execute(any());
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = KafkaExplorerResource.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(resource, value);
    }
}
