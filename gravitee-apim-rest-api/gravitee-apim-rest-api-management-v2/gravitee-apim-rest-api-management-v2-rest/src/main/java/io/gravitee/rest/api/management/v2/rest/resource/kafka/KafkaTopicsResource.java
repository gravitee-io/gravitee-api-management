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
import io.gravitee.rest.api.management.v2.rest.model.KafkaTopicCreateRequest;
import io.gravitee.rest.api.management.v2.rest.model.KafkaTopicResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.kafka.KafkaTopicEntity;
import io.gravitee.rest.api.model.kafka.NewKafkaTopicEntity;
import io.gravitee.rest.api.service.KafkaManagementService;
import io.gravitee.rest.api.service.exceptions.KafkaTopicOperationException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST resource for Kafka topic management.
 * Provides endpoints for listing, creating, and deleting Kafka topics.
 *
 * @author Gravitee Team
 */
public class KafkaTopicsResource extends AbstractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTopicsResource.class);

    @Inject
    private KafkaManagementService kafkaManagementService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTopics(
        @QueryParam("bootstrapServers") String bootstrapServers,
        @QueryParam("securityProtocol") String securityProtocol
    ) {
        try {
            List<KafkaTopicEntity> topics = kafkaManagementService.listTopics(bootstrapServers, securityProtocol);
            List<KafkaTopicResponse> response = topics.stream().map(KafkaMapper::toTopicResponse).collect(Collectors.toList());

            return Response.ok(response).build();
        } catch (KafkaTopicOperationException e) {
            // Log once at REST layer
            LOGGER.error("Failed to list topics from Kafka cluster {}", bootstrapServers, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to list topics: " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTopic(
        @QueryParam("bootstrapServers") String bootstrapServers,
        @QueryParam("securityProtocol") String securityProtocol,
        @Valid KafkaTopicCreateRequest request
    ) {
        try {
            NewKafkaTopicEntity newTopic = KafkaMapper.toNewTopicEntity(request);
            KafkaTopicEntity topic = kafkaManagementService.createTopic(bootstrapServers, securityProtocol, newTopic);
            KafkaTopicResponse response = KafkaMapper.toTopicResponse(topic);

            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (KafkaTopicOperationException e) {
            // Log once at REST layer
            LOGGER.error("Failed to create Kafka topic {}", request.getName(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to create topic: " + e.getMessage()))
                .build();
        }
    }

    @DELETE
    @Path("/{topicName}")
    public Response deleteTopic(
        @PathParam("topicName") String topicName,
        @QueryParam("bootstrapServers") String bootstrapServers,
        @QueryParam("securityProtocol") String securityProtocol
    ) {
        try {
            kafkaManagementService.deleteTopic(bootstrapServers, securityProtocol, topicName);
            return Response.noContent().build();
        } catch (KafkaTopicOperationException e) {
            // Log once at REST layer
            LOGGER.error("Failed to delete Kafka topic {}", topicName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to delete topic: " + e.getMessage()))
                .build();
        }
    }

    private static class ErrorResponse {

        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
