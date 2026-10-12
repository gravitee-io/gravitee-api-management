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

import io.gravitee.apim.rest.api.mcp.spi.McpPrompt;
import io.gravitee.apim.rest.api.mcp.spi.McpResource;
import java.util.List;
import java.util.Map;

/**
 * Hands the model the triage method and the question, and nothing else: the judgement stays with the model.
 */
public class TriagePrompt implements McpPrompt {

    private final McpResource guide;

    public TriagePrompt(McpResource guide) {
        this.guide = guide;
    }

    @Override
    public String name() {
        return "triage";
    }

    @Override
    public String title() {
        return "Look for trouble";
    }

    @Override
    public String description() {
        return "Work out whether something is wrong with the traffic going through the gateways, where, since when and why, with the triage method of this server.";
    }

    @Override
    public List<Argument> arguments() {
        return List.of(
            new Argument(
                "environment",
                "Id, HRID or name of the environment to look at. Omit to look at every environment you can read.",
                false
            ),
            new Argument("api", "Id or name of one API to focus on. Omit to look at all of them.", false)
        );
    }

    @Override
    public String render(Map<String, String> arguments) {
        var environment = value(arguments, "environment");
        var api = value(arguments, "api");
        var where = environment == null ? "every environment I can read" : "the environment " + environment;
        var focus = api == null ? "" : ", focusing on the API " + api;
        return (
            "Look for trouble in " +
            where +
            focus +
            ". Use the method below, but set it aside wherever the situation calls for something else, and say what you base each conclusion on.\n\n" +
            guide.read()
        );
    }

    private static String value(Map<String, String> arguments, String name) {
        var value = arguments == null ? null : arguments.get(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
