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
package io.gravitee.gamma.definition.approval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire schema of an approval rule: the payload of a generic event of kind {@code approval.rule}, whose id is the rule id.
 *
 * <p>Shared contract between the publisher (the AIM module) and the gateway.
 * Mirrors {@code Credential}: Lombok POJO with Bean Validation annotations and {@link Serializable},
 * plus the one readability check of {@link #unreadableFields()}, which the gateway applies before it evaluates a rule.
 * The module's own approval rule model is richer; server-side code converts
 * domain → wire when publishing the event.
 *
 * <p>Approvers and notification settings are deliberately absent: the gateway only decides whether a
 * tool call needs approval, it never contacts approvers.
 *
 * <p>Every class here ignores unknown properties so a gateway keeps reading rules a newer control plane extends.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalRule implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String environmentId;

    /** Tool the rule gates: an exact tool name, a glob such as {@code payments.*}, or alternatives such as {@code a|b}. */
    @JsonProperty
    private String toolPattern;

    /** Calls the rule gates whatever its {@link #toolPattern}: each pair is an API exposing a catalog tool the rule
     *  is anchored on, under the name that API exposes it. This serves an MCP proxy, where the gateway has no
     *  tool-level entity ids. {@code null} from a control plane that predates it. */
    @JsonProperty
    private List<@Valid ToolBinding> toolBindings;

    /** Catalog entity ids (slugs) of the MCP server tools the rule is anchored on. A call whose tool resolves to one
     *  of them is gated whatever the {@link #toolPattern}, under any name or alias an API serves the tool with.
     *  {@code null} from a control plane that predates it. */
    @JsonProperty
    private List<String> toolEntityIds;

    @JsonProperty
    private List<ArgumentCondition> argumentConditions;

    /** Lower is evaluated first. */
    @JsonProperty
    private Integer priority;

    /** ISO-8601 timestamp string (e.g. "2024-01-01T00:00:00Z"); breaks priority ties. Kept as String to avoid
     *  coupling to a specific Jackson time module on the gateway side. */
    @JsonProperty
    private String createdAt;

    /** ISO-8601 timestamp string (e.g. "2024-01-01T00:00:00Z"). Kept as String to avoid coupling
     *  to a specific Jackson time module on the gateway side. */
    @JsonProperty
    private String updatedAt;

    /** The tool bindings that name both an API and a tool. The others name no call a gateway can place. */
    public List<ToolBinding> usableToolBindings() {
        return toolBindings == null
            ? List.of()
            : toolBindings
                .stream()
                .filter(binding -> binding != null && !isBlank(binding.getApiId()) && !isBlank(binding.getToolName()))
                .toList();
    }

    /** The entity ids that name a tool, each once. */
    public List<String> usableToolEntityIds() {
        return toolEntityIds == null
            ? List.of()
            : toolEntityIds
                .stream()
                .filter(entityId -> !isBlank(entityId))
                .distinct()
                .toList();
    }

    /**
     * The fields that keep the rule from being evaluated, by name; empty when it can be. A rule that sends no selector
     * matches no call, so it is readable. A rule that sends selectors and has none usable covers calls that cannot be
     * placed, so it is not.
     */
    public List<String> unreadableFields() {
        List<String> unreadable = new ArrayList<>();
        boolean sendsNoSelector = toolPattern == null && isEmpty(toolBindings) && isEmpty(toolEntityIds);
        boolean hasUsableSelector = !isBlank(toolPattern) || !usableToolBindings().isEmpty() || !usableToolEntityIds().isEmpty();
        if (!sendsNoSelector && !hasUsableSelector) {
            unreadable.add("toolPattern, toolBindings or toolEntityIds");
        }
        if (
            argumentConditions != null &&
            argumentConditions
                .stream()
                .anyMatch(condition -> condition == null || isBlank(condition.getPath()) || condition.getOperator() == null)
        ) {
            unreadable.add("argumentConditions");
        }
        if (priority == null) {
            unreadable.add("priority");
        }
        if (!isInstant(createdAt)) {
            unreadable.add("createdAt");
        }
        if (!isInstant(updatedAt)) {
            unreadable.add("updatedAt");
        }
        return unreadable;
    }

    private static boolean isInstant(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            Instant.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArgumentCondition implements Serializable {

        /** JSON dot-path into the tool call arguments, e.g. {@code amount.value}. */
        @JsonProperty
        private String path;

        /**
         * {@code EQ}, {@code NEQ}, {@code GT}, {@code GTE}, {@code LT}, {@code LTE}, {@code CONTAINS} or {@code MATCHES}.
         * Kept as a String so a gateway that does not know a newer operator still receives the rule: the gateway
         * treats an unknown operator as a match, so the call stays gated.
         */
        @JsonProperty
        private String operator;

        @JsonProperty
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolBinding implements Serializable {

        @JsonProperty(required = true)
        @NotBlank
        private String apiId;

        @JsonProperty(required = true)
        @NotBlank
        private String toolName;
    }
}
