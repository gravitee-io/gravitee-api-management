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

import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.BrowseMessagesUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeBrokerUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeConsumerGroupUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeTopicUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListConsumerGroupsUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListTopicsUseCase;
import io.gravitee.rest.api.kafkaexplorer.mapper.KafkaExplorerMapper;
import io.gravitee.rest.api.kafkaexplorer.rest.model.BrowseMessagesRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeConsumerGroupRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListConsumerGroupsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.GraviteeLicenseFeature;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class KafkaExplorerResource {

    @Inject
    private BrowseMessagesUseCase browseMessagesUseCase;

    @Inject
    private DescribeBrokerUseCase describeBrokerUseCase;

    @Inject
    private DescribeKafkaClusterUseCase describeKafkaClusterUseCase;

    @Inject
    private ListTopicsUseCase listTopicsUseCase;

    @Inject
    private DescribeTopicUseCase describeTopicUseCase;

    @Inject
    private ListConsumerGroupsUseCase listConsumerGroupsUseCase;

    @Inject
    private DescribeConsumerGroupUseCase describeConsumerGroupUseCase;

    @POST
    @Path("/describe-cluster")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response describeCluster(DescribeClusterRequest request) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            var result = describeKafkaClusterUseCase.execute(new DescribeKafkaClusterUseCase.Input(request.getClusterId(), environmentId));
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.clusterInfo())).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @POST
    @Path("/list-topics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response listTopics(
        ListTopicsRequest request,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("10") int perPage
    ) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (page < 1) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("page must be >= 1").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (perPage < 1 || perPage > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new KafkaExplorerError()
                        .message("perPage must be between 1 and 100")
                        .technicalCode(TechnicalCode.INVALID_PARAMETERS.name())
                )
                .build();
        }

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            int page0Based = page - 1;
            var result = listTopicsUseCase.execute(
                new ListTopicsUseCase.Input(request.getClusterId(), environmentId, request.getNameFilter(), page0Based, perPage)
            );
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.topicsPage(), page, perPage)).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @POST
    @Path("/describe-topic")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response describeTopic(DescribeTopicRequest request) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (request.getTopicName() == null || request.getTopicName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("topicName is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            var result = describeTopicUseCase.execute(
                new DescribeTopicUseCase.Input(request.getClusterId(), environmentId, request.getTopicName())
            );
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.topicDetail())).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @POST
    @Path("/describe-broker")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response describeBroker(DescribeBrokerRequest request) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (request.getBrokerId() == null || request.getBrokerId() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("brokerId must be >= 0").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            var result = describeBrokerUseCase.execute(
                new DescribeBrokerUseCase.Input(request.getClusterId(), environmentId, request.getBrokerId())
            );
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.brokerInfo())).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @POST
    @Path("/list-consumer-groups")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response listConsumerGroups(
        ListConsumerGroupsRequest request,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("25") int perPage
    ) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (page < 1) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("page must be >= 1").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (perPage < 1 || perPage > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new KafkaExplorerError()
                        .message("perPage must be between 1 and 100")
                        .technicalCode(TechnicalCode.INVALID_PARAMETERS.name())
                )
                .build();
        }

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            int page0Based = page - 1;
            var result = listConsumerGroupsUseCase.execute(
                new ListConsumerGroupsUseCase.Input(
                    request.getClusterId(),
                    environmentId,
                    request.getNameFilter(),
                    request.getTopicFilter(),
                    page0Based,
                    perPage
                )
            );
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.consumerGroupsPage(), page, perPage)).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @POST
    @Path("/describe-consumer-group")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response describeConsumerGroup(DescribeConsumerGroupRequest request) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (request.getGroupId() == null || request.getGroupId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("groupId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            var result = describeConsumerGroupUseCase.execute(
                new DescribeConsumerGroupUseCase.Input(request.getClusterId(), environmentId, request.getGroupId())
            );
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.consumerGroupDetail())).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @POST
    @Path("/browse-messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public Response browseMessages(BrowseMessagesRequest request, @QueryParam("limit") @DefaultValue("50") int limit) {
        if (request == null || request.getClusterId() == null || request.getClusterId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("clusterId is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (request.getTopicName() == null || request.getTopicName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message("topicName is required").technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .build();
        }

        if (limit < 1 || limit > 1000) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    new KafkaExplorerError()
                        .message("limit must be between 1 and 1000")
                        .technicalCode(TechnicalCode.INVALID_PARAMETERS.name())
                )
                .build();
        }

        var offsetMode = request.getOffsetMode() != null ? request.getOffsetMode().name() : "NEWEST";

        try {
            var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
            var result = browseMessagesUseCase.execute(
                new BrowseMessagesUseCase.Input(
                    request.getClusterId(),
                    environmentId,
                    request.getTopicName(),
                    request.getPartition(),
                    offsetMode,
                    request.getOffsetValue(),
                    request.getKeyFilter(),
                    limit
                )
            );
            return Response.ok(KafkaExplorerMapper.INSTANCE.map(result.browseMessagesResult())).build();
        } catch (KafkaExplorerException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new KafkaExplorerError().message(e.getMessage()).technicalCode(e.getTechnicalCode().name()))
                .build();
        }
    }

    @GET
    @Path("/tail-messages")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @GraviteeLicenseFeature("apim-native-kafka-explorer")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = RolePermissionAction.READ) })
    public void tailMessages(
        @QueryParam("clusterId") String clusterId,
        @QueryParam("topicName") String topicName,
        @QueryParam("partition") Integer partition,
        @QueryParam("keyFilter") String keyFilter,
        @QueryParam("valueFilter") String valueFilter,
        @QueryParam("maxMessages") @DefaultValue("1000") int maxMessages,
        @QueryParam("durationSeconds") @DefaultValue("30") int durationSeconds,
        @Context SseEventSink eventSink,
        @Context Sse sse
    ) {
        if (clusterId == null || clusterId.isBlank()) {
            throw badRequest("clusterId is required");
        }

        if (topicName == null || topicName.isBlank()) {
            throw badRequest("topicName is required");
        }

        if (maxMessages < 1 || maxMessages > 5000) {
            throw badRequest("maxMessages must be between 1 and 5000");
        }

        if (durationSeconds < 1 || durationSeconds > 300) {
            throw badRequest("durationSeconds must be between 1 and 300");
        }

        if (activeTailStreams.get() >= MAX_TAIL_STREAMS) {
            throw new WebApplicationException(
                Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new KafkaExplorerError().message("Too many active tail streams").technicalCode(TechnicalCode.TIMEOUT.name()))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            );
        }

        var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();

        activeTailStreams.incrementAndGet();
        try {
            tailMessagesUseCase.execute(
                new TailMessagesUseCase.Input(
                    clusterId,
                    environmentId,
                    topicName,
                    partition,
                    keyFilter,
                    valueFilter,
                    maxMessages,
                    durationSeconds
                ),
                message -> {
                    if (eventSink.isClosed()) return false;
                    try {
                        eventSink.send(
                            sse.newEventBuilder().name("message").data(String.class, objectMapper.writeValueAsString(message)).build()
                        );
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            );
            if (!eventSink.isClosed()) {
                eventSink.send(sse.newEventBuilder().name("done").data("{}").build());
            }
        } catch (KafkaExplorerException e) {
            if (!eventSink.isClosed()) {
                eventSink.send(sse.newEventBuilder().name("error").data(e.getMessage()).build());
            }
        } catch (Exception e) {
            log.error("Unexpected error during tail", e);
            if (!eventSink.isClosed()) {
                eventSink.send(sse.newEventBuilder().name("error").data("An unexpected error occurred").build());
            }
        } finally {
            activeTailStreams.decrementAndGet();
            eventSink.close();
        }
    }

    private WebApplicationException badRequest(String message) {
        return new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST)
                .entity(new KafkaExplorerError().message(message).technicalCode(TechnicalCode.INVALID_PARAMETERS.name()))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build()
        );
    }
}
