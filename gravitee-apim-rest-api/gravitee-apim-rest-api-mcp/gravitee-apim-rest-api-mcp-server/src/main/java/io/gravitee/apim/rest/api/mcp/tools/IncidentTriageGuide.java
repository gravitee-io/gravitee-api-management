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
package io.gravitee.apim.rest.api.mcp.tools;

import io.gravitee.apim.rest.api.mcp.spi.McpResource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * How to use the triage tools and read their numbers.
 */
public class IncidentTriageGuide implements McpResource {

    private static final String LOCATION = "/mcp/resources/incident-triage.md";

    private final String text;

    public IncidentTriageGuide() {
        try (var stream = IncidentTriageGuide.class.getResourceAsStream(LOCATION)) {
            if (stream == null) {
                throw new IllegalStateException("No guide at " + LOCATION);
            }
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the guide at " + LOCATION, e);
        }
    }

    @Override
    public String uri() {
        return "gravitee://guides/incident-triage";
    }

    @Override
    public String name() {
        return "incident-triage";
    }

    @Override
    public String title() {
        return "Incident triage guide";
    }

    @Override
    public String description() {
        return "The triage loop, the conventions of the tools, how to read their numbers, and the usual incident patterns.";
    }

    @Override
    public String mimeType() {
        return "text/markdown";
    }

    @Override
    public String read() {
        return text;
    }
}
