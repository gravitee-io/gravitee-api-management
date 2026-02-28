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
package io.gravitee.apim.infra.zee;

import com.jsonschema.llm.engine.ChatCompletionsFormatter;
import com.jsonschema.llm.engine.LlmRequest;
import com.jsonschema.llm.engine.LlmRoundtripEngine;
import com.jsonschema.llm.engine.LlmTransport;
import com.jsonschema.llm.engine.LlmTransportException;
import com.jsonschema.llm.engine.ProviderConfig;
import com.jsonschema.llm.engine.RoundtripResult;
import io.gravitee.apim.core.zee.domain_service.LlmEngineService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of {@link LlmEngineService} using the
 * {@code json-schema-llm} roundtrip engine with Azure OpenAI.
 *
 * <p>
 * Wires the engine's structured output pipeline:
 * <ol>
 * <li>Schema conversion (handled by pre-built SDK components)</li>
 * <li>Request formatting via {@link ChatCompletionsFormatter}</li>
 * <li>HTTP transport to Azure OpenAI</li>
 * <li>Response rehydration via codec</li>
 * <li>Validation against original schema</li>
 * </ol>
 *
 * @author Derek Burger
 */
@Component
public class LlmEngineServiceImpl implements LlmEngineService {

    private static final Logger LOG = LoggerFactory.getLogger(LlmEngineServiceImpl.class);

    private final LlmRoundtripEngine engine;
    private final Map<String, ComponentInvoker> registry;

    /**
     * Functional interface for SDK component generation methods.
     * Every component (V2 and V4) exposes
     * {@code generate(String, LlmRoundtripEngine)}.
     */
    @FunctionalInterface
    interface ComponentInvoker {
        RoundtripResult generate(String prompt, LlmRoundtripEngine engine) throws IOException, LlmTransportException;
    }

    public LlmEngineServiceImpl(ZeeConfiguration config) {
        this(config, HttpClient.newHttpClient());
    }

    /**
     * Visible-for-testing constructor that accepts a custom {@link HttpClient}.
     */
    LlmEngineServiceImpl(ZeeConfiguration config, HttpClient httpClient) {
        var providerConfig = new ProviderConfig(config.getAzureUrl(), null, Map.of("api-key", config.getAzureApiKey()));

        LlmTransport transport = request -> executeHttp(httpClient, request);

        this.engine = LlmRoundtripEngine.create(new ChatCompletionsFormatter(), providerConfig, transport);

        this.registry = buildRegistry();

        LOG.info("Zee LLM engine initialized — {} components registered, endpoint: {}", registry.size(), config.getAzureUrl());
    }

    /**
     * Visible-for-testing constructor that accepts pre-built engine and registry.
     */
    LlmEngineServiceImpl(LlmRoundtripEngine engine, Map<String, ComponentInvoker> registry) {
        this.engine = engine;
        this.registry = registry;
    }

    @Override
    public LlmGenerationResult generate(String prompt, String componentName) {
        var invoker = registry.get(componentName);
        if (invoker == null) {
            throw new IllegalArgumentException("Unsupported component: " + componentName + ". Registered: " + registry.keySet());
        }

        try {
            var result = invoker.generate(prompt, engine);
            return toGenerationResult(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schema resources for component: " + componentName, e);
        } catch (LlmTransportException e) {
            throw new RuntimeException("LLM transport failed for component: " + componentName, e);
        }
    }

    private static LlmGenerationResult toGenerationResult(RoundtripResult result) {
        return new LlmGenerationResult(result.data(), result.isValid(), -1, result.warnings(), result.validationErrors());
    }

    private static String executeHttp(HttpClient httpClient, LlmRequest request) throws LlmTransportException {
        try {
            var builder = HttpRequest.newBuilder().uri(URI.create(request.url())).POST(HttpRequest.BodyPublishers.ofString(request.body()));

            request.headers().forEach(builder::header);

            var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new LlmTransportException(
                    "Azure OpenAI returned HTTP " + response.statusCode() + ": " + truncate(response.body(), 500),
                    response.statusCode()
                );
            }

            return response.body();
        } catch (LlmTransportException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmTransportException("HTTP transport failed: " + e.getMessage(), 0, e);
        }
    }

    private static String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private static Map<String, ComponentInvoker> buildRegistry() {
        var map = new LinkedHashMap<String, ComponentInvoker>();

        // V4 components
        map.put("Flow", io.gravitee.apim.definition.llm.v4.Flow::generate);
        map.put("Plan", io.gravitee.apim.definition.llm.v4.Plan::generate);
        map.put("Api", io.gravitee.apim.definition.llm.v4.Api::generate);
        map.put("Endpoint", io.gravitee.apim.definition.llm.v4.Endpoint::generate);
        map.put("Entrypoint", io.gravitee.apim.definition.llm.v4.Entrypoint::generate);
        map.put("Step", io.gravitee.apim.definition.llm.v4.Step::generate);
        map.put("EndpointGroup", io.gravitee.apim.definition.llm.v4.EndpointGroup::generate);
        map.put("HttpListener", io.gravitee.apim.definition.llm.v4.HttpListener::generate);
        map.put("SubscriptionListener", io.gravitee.apim.definition.llm.v4.SubscriptionListener::generate);
        map.put("Cors", io.gravitee.apim.definition.llm.v4.Cors::generate);
        map.put("Selector", io.gravitee.apim.definition.llm.v4.Selector::generate);

        // V2 components (prefixed with "v2/" to disambiguate)
        map.put("v2/Flow", io.gravitee.apim.definition.llm.v2.Flow::generate);
        map.put("v2/Plan", io.gravitee.apim.definition.llm.v2.Plan::generate);
        map.put("v2/Proxy", io.gravitee.apim.definition.llm.v2.Proxy::generate);
        map.put("v2/Endpoint", io.gravitee.apim.definition.llm.v2.Endpoint::generate);
        map.put("v2/EndpointGroup", io.gravitee.apim.definition.llm.v2.EndpointGroup::generate);
        map.put("v2/VirtualHost", io.gravitee.apim.definition.llm.v2.VirtualHost::generate);
        map.put("v2/Step", io.gravitee.apim.definition.llm.v2.Step::generate);
        map.put("v2/Rule", io.gravitee.apim.definition.llm.v2.Rule::generate);
        map.put("v2/Resource", io.gravitee.apim.definition.llm.v2.Resource::generate);

        return Map.copyOf(map);
    }
}
