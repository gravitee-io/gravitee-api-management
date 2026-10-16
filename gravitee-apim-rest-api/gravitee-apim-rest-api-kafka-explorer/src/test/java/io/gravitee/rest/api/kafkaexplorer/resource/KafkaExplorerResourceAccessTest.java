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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

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
        when(
            permissionService.hasPermission(any(), eq(RolePermission.CLUSTER_DEFINITION), eq(CLUSTER_ID), eq(RolePermissionAction.READ))
        ).thenReturn(false);
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

    @Test
    void should_answer_404_on_every_post_endpoint_when_the_caller_cannot_read_the_cluster() {
        List<Function<KafkaExplorerResource, Response>> calls = List.of(
            r -> r.describeCluster(new DescribeClusterRequest().clusterId(CLUSTER_ID)),
            r -> r.listTopics(new ListTopicsRequest().clusterId(CLUSTER_ID), 1, 10, "name", "asc"),
            r -> r.describeTopic(new DescribeTopicRequest().clusterId(CLUSTER_ID)),
            r -> r.describeBroker(new DescribeBrokerRequest().clusterId(CLUSTER_ID)),
            r -> r.listConsumerGroups(new ListConsumerGroupsRequest().clusterId(CLUSTER_ID), 1, 25, "groupId", "asc"),
            r -> r.describeConsumerGroup(new DescribeConsumerGroupRequest().clusterId(CLUSTER_ID)),
            r -> r.browseMessages(new BrowseMessagesRequest().clusterId(CLUSTER_ID), 50)
        );

        for (var call : calls) {
            assertThatThrownBy(() -> call.apply(resource)).isInstanceOfSatisfying(WebApplicationException.class, e ->
                assertThat(e.getResponse().getStatus()).isEqualTo(404)
            );
        }
        verifyNoInteractions(
            describeCluster,
            listTopics,
            describeTopic,
            describeBroker,
            listConsumerGroups,
            describeConsumerGroup,
            browseMessages
        );
    }

    @Test
    void should_answer_404_on_tail_messages_when_the_caller_cannot_read_the_cluster() {
        assertThatThrownBy(() -> resource.tailMessages(CLUSTER_ID, "topic", null, null, null, 10, 5, null, null)).isInstanceOfSatisfying(
            WebApplicationException.class,
            e -> assertThat(e.getResponse().getStatus()).isEqualTo(404)
        );
        verifyNoInteractions(tailMessages);
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = KafkaExplorerResource.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(resource, value);
    }
}
