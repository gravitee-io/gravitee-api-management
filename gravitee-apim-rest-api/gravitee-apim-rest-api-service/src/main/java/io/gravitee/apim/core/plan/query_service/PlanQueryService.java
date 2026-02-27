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
package io.gravitee.apim.core.plan.query_service;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.List;
import java.util.Set;

public interface PlanQueryService {
    List<Plan> findAllByApiIdAndGeneralConditionsAndIsActive(String apiId, DefinitionVersion definitionVersion, String pageId);

    List<Plan> findAllByReferenceIdAndReferenceType(String referenceId, String referenceType);

    List<Plan> findAllByApiId(String apiId);
    List<Plan> findAllByApiIds(Set<String> apiIds, Set<String> environmentIds);

    List<Plan> findAllForApiProduct(String referenceId);
    List<Plan> findAllForApiProducts(Set<String> apiProductIds, Set<String> environmentIds);
}
