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
package io.gravitee.rest.api.management.v2.rest.resource.kafka;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.KafkaMapper;
import io.gravitee.rest.api.management.v2.rest.model.KafkaConnectionRequest;
import io.gravitee.rest.api.management.v2.rest.model.KafkaConnectionResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.kafka.KafkaConnectionConfig;
import io.gravitee.rest.api.model.kafka.KafkaConnectionResult;
import io.gravitee.rest.api.service.KafkaManagementService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST resource for Kafka connection management.
 * Provides endpoint for testing Kafka cluster connectivity.
 *
 * @author Gravitee Team
 */
public class KafkaConnectionResource extends AbstractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnectionResource.class);

    @Context
    private ResourceContext resourceContext;

    @Inject
    private KafkaManagementService kafkaManagementService;

    @POST
    @Path("/test-connection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testConnection(KafkaConnectionRequest request) {
        try {
            KafkaConnectionConfig config = KafkaMapper.toConnectionConfig(request);
            KafkaConnectionResult result = kafkaManagementService.testConnection(config);

            KafkaConnectionResponse response = new KafkaConnectionResponse();
            response.setSuccess(result.isSuccess());
            response.setMessage(result.getMessage());

            return Response.ok(response).build();
        } catch (Exception e) {
            // Log once at REST layer (following style guide)
            LOGGER.error("Failed to test Kafka connection to {}", request.getBootstrapServers(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new KafkaConnectionResponse(false, "Failed to test connection: " + e.getMessage()))
                .build();
        }
    }

    @Path("/clusters/{clusterId}/topics")
    public KafkaTopicsResource getKafkaTopicsResource() {
        return resourceContext.getResource(KafkaTopicsResource.class);
    }
}
