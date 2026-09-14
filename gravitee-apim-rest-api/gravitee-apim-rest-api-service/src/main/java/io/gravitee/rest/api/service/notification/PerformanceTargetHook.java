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
package io.gravitee.rest.api.service.notification;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;

/**
 * What the owners of a performance target's subject are told about: the changes of verdict of its rules. Offered on
 * an API's notification settings beside the {@link ApiHook}s; agents are configured by the module that owns them.
 */
public enum PerformanceTargetHook implements Hook {
    RULE_MISSED("Target missed", "Triggered when a rule of a performance target goes from met to missed.", "PERFORMANCE TARGETS"),
    RULE_RECOVERED("Target met again", "Triggered when a missed rule of a performance target is met again.", "PERFORMANCE TARGETS"),
    RULE_NOT_EVALUABLE(
        "Target no longer evaluable",
        "Triggered when a rule of a performance target has had too little traffic to be evaluated for several windows.",
        "PERFORMANCE TARGETS"
    ),
    RULE_EVALUABLE_AGAIN(
        "Target evaluable again",
        "Triggered when a rule of a performance target reported as not evaluable can be evaluated again, and is met.",
        "PERFORMANCE TARGETS"
    );

    private final String label;
    private final String description;
    private final String category;

    PerformanceTargetHook(String label, String description, String category) {
        this.label = label;
        this.description = description;
        this.category = category;
    }

    public static PerformanceTargetHook of(PerformanceTargetRuleTransition.Kind kind) {
        return valueOf(kind.name());
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public HookScope getScope() {
        return HookScope.PERFORMANCE_TARGET;
    }
}
