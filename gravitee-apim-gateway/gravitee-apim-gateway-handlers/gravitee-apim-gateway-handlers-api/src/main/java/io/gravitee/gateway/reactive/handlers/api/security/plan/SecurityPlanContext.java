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
package io.gravitee.gateway.reactive.handlers.api.security.plan;

import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.v4.plan.AbstractPlan;

/**
 * Context information needed for security plan execution.
 * Contains the essential plan data required by security policies.
 *
 * @author GraviteeSource Team
 */
public record SecurityPlanContext(String planId, String planName, String selectionRule) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String planId;
        private String planName;
        private String selectionRule;

        public Builder fromV2(Plan plan) {
            this.planId = plan.getId();
            this.planName = plan.getName();
            this.selectionRule = plan.getSelectionRule();
            return this;
        }

        public Builder fromV4(AbstractPlan plan) {
            this.planId = plan.getId();
            this.planName = plan.getName();
            this.selectionRule = plan.getSelectionRule();
            return this;
        }

        public SecurityPlanContext build() {
            return new SecurityPlanContext(planId, planName, selectionRule);
        }
    }
}
