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
package io.gravitee.apim.core.shared_policy_group.use_case;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupPolicyPlugin;
import io.gravitee.apim.core.shared_policy_group.query_service.SharedPolicyGroupHistoryQueryService;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class GetSharedPolicyGroupPolicyPluginsUseCase {

    private final SharedPolicyGroupHistoryQueryService sharedPolicyGroupHistoryQueryService;

    /**
     * Get all deployed shared policy group for policy plugins usage in steps
     */
    public Output execute(Input input) {
        var result = sharedPolicyGroupHistoryQueryService
            .streamLatestBySharedPolicyPolicyGroupId(input.environmentId())
            .filter(SharedPolicyGroup::isDeployed)
            .map(SharedPolicyGroupPolicyPlugin::fromSharedPolicyGroup)
            .toList();

        return new Output(result);
    }

    @Builder
    public record Input(String environmentId) {}

    public record Output(List<SharedPolicyGroupPolicyPlugin> sharedPolicyGroupPolicyList) {}
}
