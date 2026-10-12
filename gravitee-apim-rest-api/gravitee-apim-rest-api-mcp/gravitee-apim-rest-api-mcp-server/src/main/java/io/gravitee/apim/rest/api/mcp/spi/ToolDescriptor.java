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
package io.gravitee.apim.rest.api.mcp.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * The contract of a tool, as MCP clients see it. Descriptions are read by a model: they are product copy.
 */
/**
 * @param readOnly false for a tool that acts on the platform; the contract says so with {@code "readOnly": false}
 */
public record ToolDescriptor(String name, String title, String description, JsonNode inputSchema, JsonNode outputSchema, boolean readOnly) {
    public ToolDescriptor(String name, String title, String description, JsonNode inputSchema, JsonNode outputSchema) {
        this(name, title, description, inputSchema, outputSchema, true);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Loads {@code /mcp/tools/<name>.json}, the contract written before the tool. */
    public static ToolDescriptor load(String name) {
        var location = "/mcp/tools/" + name + ".json";
        try (var stream = ToolDescriptor.class.getResourceAsStream(location)) {
            if (stream == null) {
                throw new IllegalStateException("No tool contract at " + location);
            }
            var contract = MAPPER.readTree(stream);
            return new ToolDescriptor(
                contract.required("name").asText(),
                contract.required("title").asText(),
                contract.required("description").asText(),
                contract.required("inputSchema"),
                contract.required("outputSchema"),
                contract.path("readOnly").asBoolean(true)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the tool contract at " + location, e);
        }
    }
}
