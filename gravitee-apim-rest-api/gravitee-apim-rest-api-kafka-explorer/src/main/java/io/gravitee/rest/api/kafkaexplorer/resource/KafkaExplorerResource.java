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
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeBrokerUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeTopicUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListTopicsUseCase;
import io.gravitee.rest.api.kafkaexplorer.mapper.KafkaExplorerMapper;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
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
    private DescribeBrokerUseCase describeBrokerUseCase;

    @Inject
    private DescribeKafkaClusterUseCase describeKafkaClusterUseCase;

    @Inject
    private ListTopicsUseCase listTopicsUseCase;

    @Inject
    private DescribeTopicUseCase describeTopicUseCase;

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
}
