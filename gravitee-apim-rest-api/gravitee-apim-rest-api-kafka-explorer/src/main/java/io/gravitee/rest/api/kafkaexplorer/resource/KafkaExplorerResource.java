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
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.mapper.KafkaExplorerMapper;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/kafka-explorer")
public class KafkaExplorerResource {

    @Inject
    private DescribeKafkaClusterUseCase describeKafkaClusterUseCase;

    @POST
    @Path("/describe-cluster")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
}
