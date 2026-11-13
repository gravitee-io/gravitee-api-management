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
package io.gravitee.gateway.handlers.api.manager;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An approval rule as this gateway evaluates it.
 *
 * @param toolPattern the tool names the rule gates, as {@code |} alternatives that may hold {@code *}; {@code null} when
 *     the rule is only anchored on catalog tools
 * @param toolBindings the calls the rule gates whatever its pattern: an API and the name it exposes an anchored catalog
 *     tool under, for an API that does not tell the gateway the catalog entity of its tools
 * @param toolEntityIds the catalog entity ids of the tools the rule is anchored on: a call to a tool with one of them
 *     is gated whatever its pattern, under any name or alias an API serves the tool with
 */
public record DeployedApprovalRule(
    String id,
    String environmentId,
    String organizationId,
    String toolPattern,
    List<ToolBinding> toolBindings,
    List<String> toolEntityIds,
    List<ArgumentCondition> argumentConditions,
    int priority,
    long createdAt,
    long updatedAt
) {
    public DeployedApprovalRule {
        toolBindings = toolBindings == null ? List.of() : List.copyOf(toolBindings);
        toolEntityIds = toolEntityIds == null ? List.of() : List.copyOf(toolEntityIds);
        argumentConditions = argumentConditions == null ? List.of() : List.copyOf(argumentConditions);
    }

    /**
     * @param apiId the id of the called API
     * @param toolName the name of the called tool
     * @return whether the call is one of the rule's bindings
     */
    public boolean isBoundTo(String apiId, String toolName) {
        for (ToolBinding binding : toolBindings) {
            if (binding.apiId().equals(apiId) && binding.toolName().equals(toolName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param entityId the catalog entity id of the called tool, or {@code null} when it has none
     * @return whether the rule is anchored on that catalog tool
     */
    public boolean isAnchoredOn(String entityId) {
        return entityId != null && toolEntityIds.contains(entityId);
    }

    /** A tool exposed by an API: the API's id and the tool name it exposes. */
    public record ToolBinding(String apiId, String toolName) {
        public ToolBinding {
            Objects.requireNonNull(apiId, "apiId");
            Objects.requireNonNull(toolName, "toolName");
        }
    }

    /**
     * A condition on one argument of the call. A {@code MATCHES} condition compiles its regular expression once, here,
     * rather than on every call it is evaluated against.
     */
    public static final class ArgumentCondition {

        private static final String MATCHES = "MATCHES";

        private final String path;
        private final String operator;
        private final String value;
        private final Pattern compiledPattern;

        public ArgumentCondition(String path, String operator, String value) {
            this.path = path;
            this.operator = operator;
            this.value = value;
            this.compiledPattern = MATCHES.equals(operator) && value != null ? compileOrNull(value) : null;
        }

        public String path() {
            return path;
        }

        public String operator() {
            return operator;
        }

        public String value() {
            return value;
        }

        /**
         * The compiled {@code MATCHES} expression, or {@code null} when the operator is another one or the expression
         * does not compile. A {@code MATCHES} condition without a compiled expression holds, so the call stays gated:
         * the control plane rejects such expressions, and should one get through, holding a call costs a human a click
         * where letting it through cannot be undone.
         */
        public Pattern compiledPattern() {
            return compiledPattern;
        }

        private static Pattern compileOrNull(String regex) {
            try {
                return Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                return null;
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ArgumentCondition that)) {
                return false;
            }
            return Objects.equals(path, that.path) && Objects.equals(operator, that.operator) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, operator, value);
        }

        @Override
        public String toString() {
            return "ArgumentCondition[path=" + path + ", operator=" + operator + ", value=" + value + "]";
        }
    }
}
