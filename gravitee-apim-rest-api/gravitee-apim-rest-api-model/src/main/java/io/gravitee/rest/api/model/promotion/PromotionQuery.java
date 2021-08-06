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
package io.gravitee.rest.api.model.promotion;

import java.util.List;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PromotionQuery {

    private List<String> targetEnvCockpitIds;
    private List<PromotionEntityStatus> statuses;
    private Boolean targetApiExists;
    private String apiId;

    public List<String> getTargetEnvCockpitIds() {
        return targetEnvCockpitIds;
    }

    public void setTargetEnvCockpitIds(List<String> targetEnvCockpitIds) {
        this.targetEnvCockpitIds = targetEnvCockpitIds;
    }

    public List<PromotionEntityStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<PromotionEntityStatus> statuses) {
        this.statuses = statuses;
    }

    public void setTargetApiExists(Boolean targetApiExists) {
        this.targetApiExists = targetApiExists;
    }

    public Boolean getTargetApiExists() {
        return targetApiExists;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getApiId() {
        return apiId;
    }
}
