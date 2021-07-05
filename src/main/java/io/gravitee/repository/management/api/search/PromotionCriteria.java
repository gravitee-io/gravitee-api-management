/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.api.search;

import io.gravitee.repository.management.model.PromotionStatus;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PromotionCriteria {

    private List<String> targetEnvCockpitIds;

    private PromotionStatus status;

    PromotionCriteria(PromotionCriteria.Builder builder) {
        this.targetEnvCockpitIds = builder.targetEnvCockpitIds;
        this.status = builder.status;
    }

    public List<String> getTargetEnvCockpitIds() {
        return targetEnvCockpitIds;
    }

    public void setTargetEnvCockpitIds(List<String> targetEnvCockpitIds) {
        this.targetEnvCockpitIds = targetEnvCockpitIds;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public void setStatus(PromotionStatus status) {
        this.status = status;
    }

    public static class Builder {

        private List<String> targetEnvCockpitIds;

        private PromotionStatus status;

        public Builder targetEnvCockpitIds(String... targetEnvironmentIds) {
            this.targetEnvCockpitIds = asList(targetEnvironmentIds);
            return this;
        }


        public Builder status(PromotionStatus status) {
            this.status = status;
            return this;
        }

        public PromotionCriteria build() {
            return new PromotionCriteria(this);
        }
    }
}
