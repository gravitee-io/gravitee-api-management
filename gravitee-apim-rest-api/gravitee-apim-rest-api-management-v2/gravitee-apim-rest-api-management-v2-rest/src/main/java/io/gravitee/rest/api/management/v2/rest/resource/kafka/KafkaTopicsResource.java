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
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;

@Slf4j
public class KafkaTopicsResource extends AbstractResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTopics(
        @QueryParam("bootstrapServers") String bootstrapServers,
        @QueryParam("securityProtocol") String securityProtocol
    ) {
        log.info("Listing topics for Kafka cluster: {}", bootstrapServers);

        try {
            Properties props = createKafkaProperties(bootstrapServers, securityProtocol);

            try (AdminClient adminClient = AdminClient.create(props)) {
                // List all topics
                ListTopicsResult listTopicsResult = adminClient.listTopics();
                Set<String> topicNames = listTopicsResult.names().get(10, TimeUnit.SECONDS);

                // Get detailed info for each topic
                DescribeTopicsResult describeResult = adminClient.describeTopics(topicNames);
                Map<String, TopicDescription> topicDescriptions = describeResult.all().get(10, TimeUnit.SECONDS);

                List<Map<String, Object>> topics = new ArrayList<>();
                for (Map.Entry<String, TopicDescription> entry : topicDescriptions.entrySet()) {
                    TopicDescription desc = entry.getValue();

                    Map<String, Object> topicInfo = new HashMap<>();
                    topicInfo.put("id", "topic-" + desc.name().hashCode());
                    topicInfo.put("name", desc.name());
                    topicInfo.put("partitions", desc.partitions().size());
                    topicInfo.put("replicas", desc.partitions().isEmpty() ? 0 : desc.partitions().get(0).replicas().size());
                    topicInfo.put("messages", "0"); // Would need to fetch from consumer offsets

                    topics.add(topicInfo);
                }

                log.info("Found {} topics in Kafka cluster", topics.size());
                return Response.ok(topics).build();
            }
        } catch (Exception e) {
            log.error("Failed to list topics from Kafka: {}", bootstrapServers, e);
            return Response.status(500).entity(Map.of("error", "Failed to list topics: " + e.getMessage())).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTopic(
        @QueryParam("bootstrapServers") String bootstrapServers,
        @QueryParam("securityProtocol") String securityProtocol,
        CreateTopicRequest request
    ) {
        log.info("Creating topic '{}' in Kafka cluster: {}", request.name, bootstrapServers);

        try {
            Properties props = createKafkaProperties(bootstrapServers, securityProtocol);

            try (AdminClient adminClient = AdminClient.create(props)) {
                NewTopic newTopic = new NewTopic(request.name, request.partitions, request.replicas.shortValue());

                // Add topic configurations
                Map<String, String> configs = new HashMap<>();
                if (request.retentionMs != null) {
                    configs.put("retention.ms", String.valueOf(request.retentionMs));
                }
                if (request.cleanupPolicy != null) {
                    configs.put("cleanup.policy", request.cleanupPolicy);
                }
                newTopic.configs(configs);

                CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
                result.all().get(10, TimeUnit.SECONDS);

                Map<String, Object> response = new HashMap<>();
                response.put("id", "topic-" + request.name.hashCode());
                response.put("name", request.name);
                response.put("partitions", request.partitions);
                response.put("replicas", request.replicas);
                response.put("messages", "0");

                log.info("Successfully created topic: {}", request.name);
                return Response.status(201).entity(response).build();
            }
        } catch (Exception e) {
            log.error("Failed to create topic '{}': {}", request.name, e.getMessage(), e);
            return Response.status(500).entity(Map.of("error", "Failed to create topic: " + e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/{topicName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTopic(
        @PathParam("topicName") String topicName,
        @QueryParam("bootstrapServers") String bootstrapServers,
        @QueryParam("securityProtocol") String securityProtocol
    ) {
        log.info("Deleting topic '{}' from Kafka cluster: {}", topicName, bootstrapServers);

        try {
            Properties props = createKafkaProperties(bootstrapServers, securityProtocol);

            try (AdminClient adminClient = AdminClient.create(props)) {
                DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
                result.all().get(10, TimeUnit.SECONDS);

                log.info("Successfully deleted topic: {}", topicName);
                return Response.noContent().build();
            }
        } catch (Exception e) {
            log.error("Failed to delete topic '{}': {}", topicName, e.getMessage(), e);
            return Response.status(500).entity(Map.of("error", "Failed to delete topic: " + e.getMessage())).build();
        }
    }

    private Properties createKafkaProperties(String bootstrapServers, String securityProtocol) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

        if (securityProtocol != null && !securityProtocol.equals("PLAINTEXT")) {
            props.put("security.protocol", securityProtocol);
        }

        return props;
    }

    public static class CreateTopicRequest {

        public String name;
        public Integer partitions;
        public Integer replicas;
        public Long retentionMs;
        public String cleanupPolicy;
    }
}
