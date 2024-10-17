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
package io.gravitee.apim.core.shared_policy_group.use_case;

import static java.util.Map.entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.plugin.crud_service.PolicyPluginCrudService;
import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupHistoryCrudService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PolicyNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@DomainService
@Slf4j
public class InitializeSharedPolicyGroupUseCase {

    private final SharedPolicyGroupCrudService sharedPolicyGroupCrudService;
    private final PolicyPluginCrudService policyPluginCrudService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final SharedPolicyGroupHistoryCrudService sharedPolicyGroupHistoryCrudService;

    public Output execute(Input input) {
        try {
            var aiRedirectToHuggingFace = createAiRedirectToHuggingFace(input);

            publishEvent(input, aiRedirectToHuggingFace.toDefinition(), aiRedirectToHuggingFace);

            sharedPolicyGroupHistoryCrudService.create(aiRedirectToHuggingFace);
        } catch (JsonProcessingException | PolicyNotFoundException e) {
            log.error("Error while initializing [AI - Rate Limit & Request token limit] shared policy groups", e);
        }

        try {
            var aiPromptTemplatingExample = createAiPromptTemplatingExample(input);

            publishEvent(input, aiPromptTemplatingExample.toDefinition(), aiPromptTemplatingExample);

            sharedPolicyGroupHistoryCrudService.create(aiPromptTemplatingExample);
        } catch (JsonProcessingException | PolicyNotFoundException e) {
            log.error("Error while initializing [AI - Prompt Templating Example] shared policy groups", e);
        }
        try {
            var aiRateLimitAndRequestTokenLimit = createAiRateLimitAndRequestTokenLimit(input);

            publishEvent(input, aiRateLimitAndRequestTokenLimit.toDefinition(), aiRateLimitAndRequestTokenLimit);

            sharedPolicyGroupHistoryCrudService.create(aiRateLimitAndRequestTokenLimit);
        } catch (JsonProcessingException | PolicyNotFoundException e) {
            log.error("Error while initializing [AI - Rate Limit & Request token limit] shared policy groups", e);
        }

        return new Output();
    }

    private SharedPolicyGroup createAiRateLimitAndRequestTokenLimit(Input input) throws JsonProcessingException, PolicyNotFoundException {
        String spgCrossId = "ai-rate-limit-and-request-token-limit";
        var hasSpg = sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(input.environmentId(), spgCrossId);
        if (hasSpg.isPresent()) {
            return hasSpg.get();
        }

        var groovyPolicy = policyPluginCrudService.get("groovy").orElseThrow(() -> new PolicyNotFoundException("policy-groovy"));
        var rateLimitPolicy = policyPluginCrudService.get("rate-limit").orElseThrow(() -> new PolicyNotFoundException("rate-limit"));

        SharedPolicyGroup aiRateLimitAndRequestTokenLimitSPG = initializeSharedPolicyGroupForRequestPhase(input)
            .name("\uD83E\uDD16 AI - Rate Limit & Request token limit")
            .crossId(spgCrossId)
            .description(
                "\uD83D\uDE80 This shared policy group limits the number of requests as well as the number of token sent per request."
            )
            .prerequisiteMessage(
                "You need the `#context.attributes['prompt']`, `context.attributes['maxTokens']`, `context.attributes['maxRequests']` set."
            )
            .steps(
                List.of(
                    Step
                        .builder()
                        .policy(rateLimitPolicy.getId())
                        .name(rateLimitPolicy.getName())
                        .description("Rate limit {#context.attributes['maxRequests']} request / minute")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "useKeyOnly",
                                        false,
                                        "addHeaders",
                                        false,
                                        "rate",
                                        Map.of(
                                            "useKeyOnly",
                                            true,
                                            "dynamicLimit",
                                            "{#context.attributes['maxRequests']}",
                                            "periodTime",
                                            1,
                                            "limit",
                                            0,
                                            "periodTimeUnit",
                                            "MINUTES",
                                            "key",
                                            ""
                                        )
                                    )
                                )
                        )
                        .build(),
                    Step
                        .builder()
                        .policy(groovyPolicy.getId())
                        .name(groovyPolicy.getName())
                        .description("Limit the number of tokens to `context.attributes['maxTokens']`")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "readContent",
                                        false,
                                        "scope",
                                        "REQUEST",
                                        "script",
                                        "def maxTokens =  Integer.parseInt(context.attributes.'maxTokens');\n\ndef characters = context.attributes.'prompt'.length();\ndef tokens = characters / 4;\n\ndef truncateValue = Math.min(tokens.intValue(), maxTokens.intValue()) * 4 - 1;\n context.attributes.'prompt' =  context.attributes.'prompt'[0..truncateValue];\n"
                                    )
                                )
                        )
                        .build()
                )
            )
            .build();
        return sharedPolicyGroupCrudService.create(aiRateLimitAndRequestTokenLimitSPG);
    }

    private SharedPolicyGroup createAiPromptTemplatingExample(Input input) throws JsonProcessingException, PolicyNotFoundException {
        String spgCrossId = "ai-prompt-templating-example";
        var hasSpg = sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(input.environmentId(), spgCrossId);
        if (hasSpg.isPresent()) {
            return hasSpg.get();
        }

        var assignAttributesPolicy = policyPluginCrudService
            .get("policy-assign-attributes")
            .orElseThrow(() -> new PolicyNotFoundException("policy-assign-attributes"));
        var httpCalloutPolicy = policyPluginCrudService
            .get("policy-http-callout")
            .orElseThrow(() -> new PolicyNotFoundException("policy-http-callout"));

        SharedPolicyGroup aiPromptTemplatingExampleSPG = initializeSharedPolicyGroupForRequestPhase(input)
            .name("\uD83E\uDD16 AI - Prompt Templating Example")
            .crossId(spgCrossId)
            .description("\uD83D\uDE80 An example on how to use Assign Content policy to create/enhance prompt based on external data.")
            .prerequisiteMessage("You need the `ip` field set in your request body.")
            .steps(
                List.of(
                    Step
                        .builder()
                        .policy(assignAttributesPolicy.getId())
                        .name(assignAttributesPolicy.getName())
                        .description("Assign variables")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "scope",
                                        "REQUEST",
                                        "attributes",
                                        List.of(Map.of("name", "requestIp", "value", "{#jsonPath(#request.content, '$.ip')}"))
                                    )
                                )
                        )
                        .build(),
                    Step
                        .builder()
                        .policy(httpCalloutPolicy.getId())
                        .name(httpCalloutPolicy.getName())
                        .description("Retrieve location information")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "variables",
                                        List.of(
                                            Map.of("name", "country", "value", "{#jsonPath(#calloutResponse.content, '$.country')}"),
                                            Map.of("name", "city", "value", "{#jsonPath(#calloutResponse.content, '$.city')}")
                                        ),
                                        "method",
                                        "GET",
                                        "fireAndForget",
                                        false,
                                        "scope",
                                        "REQUEST",
                                        "errorStatusCode",
                                        "500",
                                        "errorCondition",
                                        "{#calloutResponse.status >= 400 and #calloutResponse.status <= 599}",
                                        "url",
                                        "http://ip-api.com/json/{#context.attributes['requestIp']}",
                                        "exitOnError",
                                        false
                                    )
                                )
                        )
                        .build(),
                    Step
                        .builder()
                        .policy(assignAttributesPolicy.getId())
                        .name(assignAttributesPolicy.getName())
                        .description("Build our prompt to obtain some information")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "scope",
                                        "REQUEST",
                                        "attributes",
                                        List.of(
                                            Map.of(
                                                "name",
                                                "prompt",
                                                "value",
                                                "Given the the location: {#context.attributes['city']}, {#context.attributes['country']}, give a short description like you are tourist guide"
                                            )
                                        )
                                    )
                                )
                        )
                        .build()
                )
            )
            .build();
        return sharedPolicyGroupCrudService.create(aiPromptTemplatingExampleSPG);
    }

    private SharedPolicyGroup createAiRedirectToHuggingFace(Input input) throws JsonProcessingException, PolicyNotFoundException {
        String spgCrossId = "ai-redirect-to-huggingface";
        var hasSpg = sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(input.environmentId(), spgCrossId);
        if (hasSpg.isPresent()) {
            return hasSpg.get();
        }

        var assignContentPolicy = policyPluginCrudService
            .get("policy-assign-content")
            .orElseThrow(() -> new PolicyNotFoundException("policy-assign-content"));
        var dynamicRoutingPolicy = policyPluginCrudService
            .get("dynamic-routing")
            .orElseThrow(() -> new PolicyNotFoundException("dynamic-routing"));

        SharedPolicyGroup aiRedirectToHuggingFaceSPG = initializeSharedPolicyGroupForRequestPhase(input)
            .name("\uD83E\uDD16 AI - Redirect to HuggingFace")
            .crossId(spgCrossId)
            .description("\uD83D\uDE80 This shared policy group builds the content to reach a Text Generation model using Hugging Face.")
            .prerequisiteMessage(
                "These are required: `(#context.attributes['prompt']}`, `(#context.attributes['redirect-model']}` (the GPT model, try with: `meta-llama/Meta-Llama-3-8B-Instruct`, `(#context.attributes['redirect-source']}`: the source api path."
            )
            .steps(
                List.of(
                    Step
                        .builder()
                        .policy(assignContentPolicy.getId())
                        .name(assignContentPolicy.getName())
                        .description("Assign content for model")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "scope",
                                        "REQUEST",
                                        "body",
                                        "{\n    \"model\": \"${context.attributes['redirect-model']}\",\n    \"messages\": [\n        {\n            \"role\": \"user\",\n            \"content\": \"${context.attributes['prompt']}\"\n        }\n    ],\n    \"parameters\": {\n        \"temperature\": 0.6\n    },\n    \"stream\": false,\n    \"options\": {\n        \"wait_for_model\": true\n    }\n}"
                                    )
                                )
                        )
                        .build(),
                    Step
                        .builder()
                        .policy(dynamicRoutingPolicy.getId())
                        .name(dynamicRoutingPolicy.getName())
                        .description("Redirect to HuggingFace")
                        .configuration(
                            new ObjectMapper()
                                .writeValueAsString(
                                    Map.of(
                                        "rules",
                                        List.of(
                                            Map.of(
                                                "pattern",
                                                "{#context.attributes['redirect-source']}",
                                                "url",
                                                "/models/{#context.attributes['redirect-model']}/v1/chat/completions"
                                            )
                                        )
                                    )
                                )
                        )
                        .build()
                )
            )
            .build();
        return sharedPolicyGroupCrudService.create(aiRedirectToHuggingFaceSPG);
    }

    private static SharedPolicyGroup.SharedPolicyGroupBuilder initializeSharedPolicyGroupForRequestPhase(Input input) {
        return SharedPolicyGroup
            .builder()
            .id(UuidString.generateRandom())
            .organizationId(input.organizationId())
            .environmentId(input.environmentId())
            .apiType(ApiType.PROXY)
            .phase(FlowPhase.REQUEST)
            .version(0)
            .lifecycleState(SharedPolicyGroup.SharedPolicyGroupLifecycleState.DEPLOYED)
            .deployedAt(TimeProvider.now())
            .createdAt(TimeProvider.now())
            .updatedAt(TimeProvider.now());
    }

    private void publishEvent(
        InitializeSharedPolicyGroupUseCase.Input input,
        io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup definition,
        SharedPolicyGroup sharedPolicyGroup
    ) {
        final Event event = eventCrudService.createEvent(
            input.organizationId(),
            input.environmentId(),
            Set.of(input.environmentId),
            EventType.DEPLOY_SHARED_POLICY_GROUP,
            definition,
            Map.ofEntries(entry(Event.EventProperties.SHARED_POLICY_GROUP_ID, sharedPolicyGroup.getCrossId()))
        );

        eventLatestCrudService.createOrPatchLatestEvent(input.organizationId(), sharedPolicyGroup.getId(), event);
    }

    @Builder
    public record Input(String organizationId, String environmentId) {}

    public record Output() {}
}
