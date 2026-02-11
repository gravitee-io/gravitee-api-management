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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.services.sync.process.common.model.ApiProductDeployable;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public class ApiProductReactorDeployable implements ApiProductDeployable {

    private String apiProductId;
    private ReactableApiProduct reactableApiProduct;
    private SyncAction syncAction;
    private Set<String> subscribablePlans;
    private List<Plan> definitionPlans;

    @Override
    public String id() {
        return apiProductId;
    }

    public String apiProductId() {
        if (apiProductId == null && reactableApiProduct != null) {
            return reactableApiProduct.getId();
        }
        return apiProductId;
    }

    public Set<String> subscribablePlans() {
        return subscribablePlans != null ? subscribablePlans : Set.of();
    }

    public List<Plan> definitionPlans() {
        return definitionPlans != null ? definitionPlans : List.of();
    }
}
