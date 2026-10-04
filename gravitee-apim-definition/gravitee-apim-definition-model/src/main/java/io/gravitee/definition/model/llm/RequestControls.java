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
package io.gravitee.definition.model.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Optional request assistance. Unset controls make no change; runtime inability to safely apply these
 * controls is reported without rejecting the request. Native compaction enforcement is configured
 * separately by {@link ContextManagementPolicy}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequestControls(Integer maxOutputTokens, AnthropicCache anthropicCache, ContextReduction contextReduction) implements
    Serializable {
    public RequestControls {
        if (maxOutputTokens != null && maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
    }

    /** Per-checkpoint lifetimes; absence disables managed insertion at that location. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AnthropicCache(CacheTtl toolsEnd, CacheTtl systemEnd, CacheTtl conversationEnd) implements Serializable {}

    public enum CacheTtl {
        @JsonProperty("5m")
        FIVE_MINUTES,
        @JsonProperty("1h")
        ONE_HOUR,
    }

    /**
     * Best-effort local retention and tool-output cleaning, activated only by explicit enabled=true.
     * Off clears every retained target and tuning value. Enabled policies use defaults for omitted tuning;
     * zero disables its clean clock or oversized-output threshold, and retains zero budget elsewhere.
     * Frozen head counts opening user and assistant messages; tail protects explicit message and token budgets.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContextReduction(
        boolean enabled,
        Integer targetInputTokens,
        Integer frozenHeadMessages,
        Integer retainedTailTokens,
        Integer cleanAfterMessages,
        Integer cleanAfterToolSteps,
        Integer condenseKeepTokens,
        Integer oversizeMinTokens,
        Integer oversizeKeepTokens,
        Integer reclaimTargetInputTokens,
        Integer retainedTailMessages
    ) implements Serializable {
        public ContextReduction {
            if (!enabled) {
                targetInputTokens = null;
                frozenHeadMessages = null;
                retainedTailTokens = null;
                cleanAfterMessages = null;
                cleanAfterToolSteps = null;
                condenseKeepTokens = null;
                oversizeMinTokens = null;
                oversizeKeepTokens = null;
                reclaimTargetInputTokens = null;
                retainedTailMessages = null;
            } else if (targetInputTokens == null || targetInputTokens <= 0) {
                throw new IllegalArgumentException("targetInputTokens must be positive");
            }
            if (
                enabled &&
                reclaimTargetInputTokens != null &&
                (reclaimTargetInputTokens <= 0 || reclaimTargetInputTokens > targetInputTokens)
            ) {
                throw new IllegalArgumentException("reclaimTargetInputTokens must be positive and no greater than targetInputTokens");
            }
            requireNonnegative("retainedTailMessages", retainedTailMessages);
            requireNonnegative("frozenHeadMessages", frozenHeadMessages);
            requireNonnegative("retainedTailTokens", retainedTailTokens);
            requireNonnegative("cleanAfterMessages", cleanAfterMessages);
            requireNonnegative("cleanAfterToolSteps", cleanAfterToolSteps);
            requireNonnegative("condenseKeepTokens", condenseKeepTokens);
            requireNonnegative("oversizeMinTokens", oversizeMinTokens);
            requireNonnegative("oversizeKeepTokens", oversizeKeepTokens);
        }

        /** Blank optional form groups disable reduction; partially populated policies remain invalid. */
        @JsonCreator
        public static ContextReduction fromJson(
            @JsonProperty("enabled") Boolean enabled,
            @JsonProperty("targetInputTokens") Integer targetInputTokens,
            @JsonProperty("frozenHeadMessages") Integer frozenHeadMessages,
            @JsonProperty("retainedTailTokens") Integer retainedTailTokens,
            @JsonProperty("cleanAfterMessages") Integer cleanAfterMessages,
            @JsonProperty("cleanAfterToolSteps") Integer cleanAfterToolSteps,
            @JsonProperty("condenseKeepTokens") Integer condenseKeepTokens,
            @JsonProperty("oversizeMinTokens") Integer oversizeMinTokens,
            @JsonProperty("oversizeKeepTokens") Integer oversizeKeepTokens,
            @JsonProperty("reclaimTargetInputTokens") Integer reclaimTargetInputTokens,
            @JsonProperty("retainedTailMessages") Integer retainedTailMessages
        ) {
            if (
                enabled == null &&
                (targetInputTokens != null ||
                    frozenHeadMessages != null ||
                    retainedTailTokens != null ||
                    cleanAfterMessages != null ||
                    cleanAfterToolSteps != null ||
                    condenseKeepTokens != null ||
                    oversizeMinTokens != null ||
                    oversizeKeepTokens != null ||
                    reclaimTargetInputTokens != null ||
                    retainedTailMessages != null)
            ) {
                throw new IllegalArgumentException("enabled must be explicit when retention controls are configured");
            }
            if (enabled == null) return null;
            return new ContextReduction(
                enabled,
                targetInputTokens,
                frozenHeadMessages,
                retainedTailTokens,
                cleanAfterMessages,
                cleanAfterToolSteps,
                condenseKeepTokens,
                oversizeMinTokens,
                oversizeKeepTokens,
                reclaimTargetInputTokens,
                retainedTailMessages
            );
        }

        private static void requireNonnegative(String name, Integer value) {
            if (value != null && value < 0) {
                throw new IllegalArgumentException(name + " must not be negative");
            }
        }
    }
}
