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

import static java.util.Arrays.asList;

import io.gravitee.repository.management.model.PromotionStatus;
import java.util.List;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PromotionCriteria {

    private List<String> targetEnvCockpitIds;
    private List<PromotionStatus> statuses;
    private Boolean targetApiExists;
    private String apiId;

    PromotionCriteria(Builder builder) {
        this.targetEnvCockpitIds = builder.targetEnvCockpitIds;
        this.statuses = builder.statuses;
        this.targetApiExists = builder.targetApiExists;
        this.apiId = builder.apiId;
    }

    public List<String> getTargetEnvCockpitIds() {
        return targetEnvCockpitIds;
    }

    public void setTargetEnvCockpitIds(List<String> targetEnvCockpitIds) {
        this.targetEnvCockpitIds = targetEnvCockpitIds;
    }

    public List<PromotionStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<PromotionStatus> statuses) {
        this.statuses = statuses;
    }

    public Boolean getTargetApiExists() {
        return targetApiExists;
    }

    public void setTargetApiExists(Boolean targetApiExists) {
        this.targetApiExists = targetApiExists;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public static class Builder {

        private List<String> targetEnvCockpitIds;
        private List<PromotionStatus> statuses;
        private Boolean targetApiExists;
        private String apiId;

        public Builder targetEnvCockpitIds(String... targetEnvironmentIds) {
            this.targetEnvCockpitIds = asList(targetEnvironmentIds);
            return this;
        }

        public Builder statuses(List<PromotionStatus> statuses) {
            this.statuses = statuses;
            return this;
        }

        public Builder targetApiExists(Boolean targetApiExists) {
            this.targetApiExists = targetApiExists;
            return this;
        }

        public Builder apiId(String apiId) {
            this.apiId = apiId;
            return this;
        }

        public PromotionCriteria build() {
            return new PromotionCriteria(this);
        }
    }
}
