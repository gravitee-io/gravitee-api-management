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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class AiFleetResource {

    private static final Logger log = LoggerFactory.getLogger(AiFleetResource.class);
    private static final String BASE_DIR = System.getProperty("user.home") + "/.daimon";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();

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
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void events(@PathParam("hostname") String hostname, @Context SseEventSink sink, @Context Sse sse) {
        java.nio.file.Path eventsFile = Paths.get(BASE_DIR, hostname, "events.jsonl");

        SSE_EXECUTOR.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(eventsFile.toFile()))) {
                // stream existing lines first
                String line;
                while ((line = reader.readLine()) != null && !sink.isClosed()) {
                    sink.send(sse.newEvent(line));
                }
                // then tail new lines
                while (!sink.isClosed()) {
                    line = reader.readLine();
                    if (line != null) {
                        sink.send(sse.newEvent(line));
                    } else {
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                log.warn("SSE stream closed for {}: {}", hostname, e.getMessage());
            } finally {
                sink.close();
            }
        });
    }
}
