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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

@Slf4j
public class KafkaConnectionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @POST
    @Path("/test-connection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testConnection(KafkaConnectionRequest request) {
        log.info("Testing Kafka connection to: {}", request.bootstrapServers);

        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, request.bootstrapServers);
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
            props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");

            // Add security configuration if present
            if (request.securityProtocol != null && !request.securityProtocol.equals("PLAINTEXT")) {
                props.put("security.protocol", request.securityProtocol);

                if (request.saslMechanism != null) {
                    props.put("sasl.mechanism", request.saslMechanism);

                    if (request.saslUsername != null && request.saslPassword != null) {
                        String jaasConfig = String.format(
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                            request.saslUsername,
                            request.saslPassword
                        );
                        props.put("sasl.jaas.config", jaasConfig);
                    }
                }
            }

            // Try to connect
            try (AdminClient adminClient = AdminClient.create(props)) {
                // Try to list topics as a connection test
                adminClient.listTopics().names().get(5, TimeUnit.SECONDS);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Successfully connected to " + request.bootstrapServers);

                log.info("Successfully connected to Kafka: {}", request.bootstrapServers);
                return Response.ok(response).build();
            }
        } catch (Exception e) {
            log.error("Failed to connect to Kafka: {}", request.bootstrapServers, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to connect: " + e.getMessage());

            return Response.ok(response).build();
        }
    }

    @Path("/clusters/{clusterId}/topics")
    public KafkaTopicsResource getKafkaTopicsResource() {
        return resourceContext.getResource(KafkaTopicsResource.class);
    }

    public static class KafkaConnectionRequest {

        public String bootstrapServers;
        public String securityProtocol;
        public String saslMechanism;
        public String saslUsername;
        public String saslPassword;
    }
}
