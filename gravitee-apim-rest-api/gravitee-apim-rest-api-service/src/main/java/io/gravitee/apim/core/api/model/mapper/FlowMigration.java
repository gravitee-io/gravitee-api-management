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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.api.model.utils.MigrationResult.State.*;
import static io.gravitee.apim.core.utils.CollectionUtils.size;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FlowMigration {

    private final ObjectMapper OBJECT_MAPPER;

    private static final Collection<String> INCOMPATIBLES_POLICIES = Set.of(
        "cloud-events",
        "html-json",
        "message-filtering",
        "metrics-reporter",
        "policy-interrupt",
        "policy-wssecurity-authentication"
    );
    private static final Collection<String> GRAVITEE_POLICIES = Set.of(
        "ai-prompt-guard-rails",
        "ai-prompt-token-tracking",
        "api-key",
        "avro-json",
        "avro-protobuf",
        "aws-lambda",
        "cache",
        "cloud-events",
        "custom-query-parameters-parser",
        "data-cache",
        "dynamic-routing",
        "generate-http-signature",
        "groovy",
        "html-json",
        "http-redirect",
        "http-signature",
        "ip-filtering",
        "javascript",
        "json-threat-protection",
        "json-to-json",
        "json-validation",
        "json-xml",
        "jws",
        "jwt",
        "key-less",
        "latency",
        "message-filtering",
        "metrics-reporter",
        "mock",
        "mtls",
        "oas-validation",
        "oauth2",
        "policy-assign-attributes",
        "policy-assign-content",
        "policy-assign-metrics",
        "policy-basic-authentication",
        "policy-circuit-breaker",
        "policy-data-logging-masking",
        "policy-generate-jwt",
        "policy-geoip-filtering",
        "policy-graphql-ratelimit",
        "policy-http-callout",
        "policy-interops-a-sp",
        "policy-interops-r-sp",
        "policy-interrupt",
        "policy-openid-userinfo",
        "policy-override-request-method",
        "policy-request-validation",
        "policy-wssecurity-authentication",
        "protobuf-json",
        "quota",
        "rate-limit",
        "rbac",
        "regex-threat-protection",
        "request-content-limit",
        "resource-filtering",
        "rest-to-soap",
        "retry",
        "spike-arrest",
        "ssl-enforcement",
        "status-code",
        "traffic-shadowing",
        "transform-headers",
        "transform-queryparams",
        "url-rewriting",
        "ws-security-sign",
        "xml-json",
        "xml-threat-protection",
        "xml-validation",
        "xslt"
    );

    public MigrationResult<List<Flow>> mapFlows(Iterable<io.gravitee.definition.model.flow.Flow> flows) {
        return stream(flows).map(f -> mapFlow(f).map(List::of)).reduce(MigrationResult.value(List.of()), MigrationResult::mergeList);
    }

    public MigrationResult<Flow> mapFlow(io.gravitee.definition.model.flow.Flow v2Flow) {
        var preSteps = stream(v2Flow.getPre()).map(this::migrateV4).map(step -> step.map(MigratedSteps::pre));
        var postSteps = stream(v2Flow.getPost()).map(this::migrateV4).map(step -> step.map(MigratedSteps::post));
        var reduce = Stream
            .concat(preSteps, postSteps)
            .reduce(MigrationResult.value(new MigratedSteps()), (a, b) -> a.foldLeft(b, MigratedSteps::merge));
        return reduce.map(e ->
            Flow
                .builder()
                .id(v2Flow.getId())
                .selectors(
                    Stream
                        .concat(
                            nullIfEmpty(v2Flow.getCondition()) == null
                                ? Stream.empty()
                                : Stream.of(ConditionSelector.builder().condition(v2Flow.getCondition()).build()),
                            Stream.of(
                                HttpSelector
                                    .builder()
                                    .methods(v2Flow.getMethods())
                                    .pathOperator(v2Flow.getOperator())
                                    .path(v2Flow.getPath())
                                    .build()
                            )
                        )
                        .toList()
                )
                .request(e.pre())
                .response(e.post())
                .name(v2Flow.getName())
                .enabled(v2Flow.isEnabled())
                .build()
        );
    }

    private MigrationResult<Step> migrateV4(io.gravitee.definition.model.flow.Step v2Step) {
        if (INCOMPATIBLES_POLICIES.contains(v2Step.getPolicy())) {
            return MigrationResult.issue("Policy %s is not compatible with V4 APIs".formatted(v2Step.getPolicy()), IMPOSSIBLE);
        } else if (!GRAVITEE_POLICIES.contains(v2Step.getPolicy())) {
            return MigrationResult.issue(
                "Policy %s is not a Gravitee policy. Please ensure it is compatible with V4 API before migrating to V4".formatted(
                        v2Step.getPolicy()
                    ),
                CAN_BE_FORCED
            );
        }
        var config = !"groovy".equals(v2Step.getPolicy())
            ? MigrationResult.value(v2Step.getConfiguration())
            : migrateGroovyPolicy(v2Step.getConfiguration());
        return config.map(cfg ->
            new Step(
                v2Step.getName(),
                nullIfEmpty(v2Step.getDescription()),
                v2Step.isEnabled(),
                v2Step.getPolicy(),
                cfg,
                nullIfEmpty(v2Step.getCondition()),
                null
            )
        );
    }

    private String nullIfEmpty(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record MigratedSteps(List<Step> pre, List<Step> post) {
        private MigratedSteps() {
            this(new ArrayList<>(), new ArrayList<>());
        }

        @Nullable
        public static MigratedSteps merge(@Nullable MigratedSteps first, @Nullable MigratedSteps second) {
            if (first == null) {
                return second;
            } else if (second == null) {
                return first;
            }
            return new MigratedSteps(
                Stream.concat(stream(first.pre), stream(second.pre)).toList(),
                Stream.concat(stream(first.post), stream(second.post)).toList()
            );
        }

        public static MigratedSteps pre(@Nullable Step step) {
            var steps = step != null ? List.of(step) : List.<Step>of();
            return new MigratedSteps(new ArrayList<>(steps), new ArrayList<>());
        }

        public static MigratedSteps post(@Nullable Step step) {
            var steps = step != null ? List.of(step) : List.<Step>of();
            return new MigratedSteps(new ArrayList<>(), new ArrayList<>(steps));
        }
    }

    private MigrationResult<String> migrateGroovyPolicy(String config) {
        try {
            List<String> v2CfgScripts = List.of("onRequestContentScript", "onResponseContentScript", "onRequestScript", "onResponseScript");
            ObjectNode jsonNode = OBJECT_MAPPER.readTree(config).deepCopy();
            var upds = v2CfgScripts
                .stream()
                .filter(jsonNode::has)
                .map(scriptName ->
                    buildV4groovyCfg(jsonNode.get(scriptName).asText(), scriptName.contains("Content"), scriptName.contains("Content"))
                )
                .toList();
            return switch (size(upds)) {
                case 1 -> MigrationResult.value(upds.getFirst().apply(jsonNode).remove(v2CfgScripts).toString());
                case 0 -> MigrationResult.issue("Impossible to find script in groovy policy configuration", IMPOSSIBLE);
                default -> MigrationResult.issue(
                    "Multiple groovy scripts found in groovy policy configuration (non 'content' scripts are ignored if a 'content' script is present)",
                    CAN_BE_FORCED
                );
            };
        } catch (JsonProcessingException e) {
            log.error("Unable to parse groovy configuration", e);
            return MigrationResult.issue("Impossible to parse groovy policy configuration: " + e.getMessage(), IMPOSSIBLE);
        }
    }

    private UnaryOperator<ObjectNode> buildV4groovyCfg(String script, boolean readContent, boolean overrideContent) {
        return objectNode -> objectNode.put("script", script).put("readContent", readContent).put("overrideContent", overrideContent);
    }
}
