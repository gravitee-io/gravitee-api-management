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
package io.gravitee.apim.core.api_product.model;

import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.common.utils.TimeProvider;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ApiProduct {

    public enum DeploymentState {
        DEPLOYED,
        NEED_REDEPLOY,
    }

    private String id;
    private String environmentId;
    private String name;
    private String description;
    private String version;
    private Set<String> apiIds;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private PrimaryOwnerEntity primaryOwner;
    private DeploymentState deploymentState;

    public void update(UpdateApiProduct updateApiProduct) {
        this.updatedAt = ZonedDateTime.now(TimeProvider.clock());
        if (updateApiProduct.getName() != null) {
            this.name = updateApiProduct.getName();
        }
        if (updateApiProduct.getDescription() != null) {
            this.description = updateApiProduct.getDescription();
        }
        if (updateApiProduct.getVersion() != null) {
            this.version = updateApiProduct.getVersion();
        }
        if (updateApiProduct.getApiIds() != null) {
            this.apiIds = updateApiProduct.getApiIds();
        }
    }

    public boolean addApiId(String apiId) {
        if (apiIds == null) {
            apiIds = new HashSet<>();
        }
        boolean added = apiIds.add(apiId);
        if (added) {
            this.updatedAt = ZonedDateTime.now(TimeProvider.clock());
        }
        return added;
    }

    public boolean removeApiId(String apiId) {
        if (apiIds != null && apiIds.remove(apiId)) {
            this.updatedAt = ZonedDateTime.now(TimeProvider.clock());
            return true;
        }
        return false;
    }

    public void removeAllApiIds() {
        if (apiIds != null && !apiIds.isEmpty()) {
            apiIds.clear();
            this.updatedAt = ZonedDateTime.now(TimeProvider.clock());
        }
    }
}
