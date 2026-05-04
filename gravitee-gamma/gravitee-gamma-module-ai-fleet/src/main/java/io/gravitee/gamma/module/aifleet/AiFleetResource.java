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
package io.gravitee.gamma.module.aifleet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class AiFleetResource {

    private static final Logger log = LoggerFactory.getLogger(AiFleetResource.class);
    private static final String BASE_DIR = System.getProperty("user.home") + "/.daimon";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response devices() {
        File base = new File(BASE_DIR);
        if (!base.exists() || !base.isDirectory()) {
            return Response.ok("[]").build();
        }

        List<Object> devices = new ArrayList<>();
        File[] dirs = base.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                File deviceFile = new File(dir, "device.json");
                if (deviceFile.exists()) {
                    try {
                        Object device = MAPPER.readValue(deviceFile, Object.class);
                        devices.add(device);
                    } catch (IOException e) {
                        log.warn("Failed to read device file {}: {}", deviceFile, e.getMessage());
                    }
                }
            }
        }

        return Response.ok(devices).build();
    }

    @GET
    @Path("/events/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response events(@PathParam("hostname") String hostname, @QueryParam("since") String since) {
        java.nio.file.Path eventsFile = Paths.get(BASE_DIR, hostname, "events.jsonl");
        if (!eventsFile.toFile().exists()) {
            return Response.ok(new ArrayList<>()).build();
        }

        List<Object> events = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(eventsFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                if (since != null && !since.isBlank() && line.contains("\"timestamp\"")) {
                    // keep only lines with timestamp > since (lexicographic ISO comparison)
                    int tsStart = line.indexOf("\"timestamp\":\"") + 13;
                    int tsEnd = line.indexOf("\"", tsStart);
                    if (tsStart > 12 && tsEnd > tsStart) {
                        String ts = line.substring(tsStart, tsEnd);
                        if (ts.compareTo(since) <= 0) continue;
                    }
                }
                try {
                    events.add(MAPPER.readValue(line, Object.class));
                } catch (IOException e) {
                    log.debug("Skipping malformed event line: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read events for {}: {}", hostname, e.getMessage());
        }

        return Response.ok(events).build();
    }

    @GET
    @Path("/stats/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stats(@PathParam("hostname") String hostname) {
        java.nio.file.Path eventsFile = Paths.get(BASE_DIR, hostname, "events.jsonl");
        ObjectNode stats = MAPPER.createObjectNode();
        stats.put("requests", 0);
        stats.put("blocked", 0);
        stats.put("tokens_in", 0);
        stats.put("tokens_in_system", 0);
        stats.put("tokens_in_history", 0);
        stats.put("tokens_in_user", 0);
        stats.put("tokens_out", 0);

        if (!eventsFile.toFile().exists()) {
            return Response.ok(stats).build();
        }

        int requests = 0, blocked = 0, tokensIn = 0, tokensInSystem = 0, tokensInHistory = 0, tokensInUser = 0, tokensOut = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(eventsFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    JsonNode node = MAPPER.readTree(line);
                    String type = node.path("type").asText();
                    if ("request".equals(type)) {
                        requests++;
                        tokensIn += node.path("tokens_in").asInt(0);
                        tokensInSystem += node.path("tokens_in_system").asInt(0);
                        tokensInHistory += node.path("tokens_in_history").asInt(0);
                        tokensInUser += node.path("tokens_in_user").asInt(0);
                        tokensOut += node.path("tokens_out").asInt(0);
                    } else if ("policy_block".equals(type)) {
                        blocked++;
                    }
                } catch (IOException e) {
                    log.debug("Skipping malformed line: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read events for stats {}: {}", hostname, e.getMessage());
        }

        stats.put("requests", requests);
        stats.put("blocked", blocked);
        stats.put("tokens_in", tokensIn);
        stats.put("tokens_in_system", tokensInSystem);
        stats.put("tokens_in_history", tokensInHistory);
        stats.put("tokens_in_user", tokensInUser);
        stats.put("tokens_out", tokensOut);
        return Response.ok(stats).build();
    }

    @GET
    @Path("/policies/{hostname}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPolicies(@PathParam("hostname") String hostname) {
        java.nio.file.Path deviceFile = Paths.get(BASE_DIR, hostname, "device.json");
        if (!deviceFile.toFile().exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            JsonNode device = MAPPER.readTree(deviceFile.toFile());
            String policiesPath = device.path("policies_path").asText();
            if (policiesPath.isBlank()) {
                return Response.status(Response.Status.NOT_FOUND).entity("policies_path not registered").build();
            }
            return Response.ok(Files.readString(Paths.get(policiesPath))).build();
        } catch (IOException e) {
            log.warn("Failed to read policies for {}: {}", hostname, e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/policies/{hostname}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response putPolicies(@PathParam("hostname") String hostname, String body) {
        java.nio.file.Path deviceFile = Paths.get(BASE_DIR, hostname, "device.json");
        if (!deviceFile.toFile().exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            JsonNode device = MAPPER.readTree(deviceFile.toFile());
            String policiesPath = device.path("policies_path").asText();
            if (policiesPath.isBlank()) {
                return Response.status(Response.Status.NOT_FOUND).entity("policies_path not registered").build();
            }
            Files.writeString(Paths.get(policiesPath), body);
            return Response.noContent().build();
        } catch (IOException e) {
            log.warn("Failed to write policies for {}: {}", hostname, e.getMessage());
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
