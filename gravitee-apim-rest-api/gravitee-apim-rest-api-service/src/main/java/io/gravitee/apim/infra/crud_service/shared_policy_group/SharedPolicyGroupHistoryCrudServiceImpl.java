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
package io.gravitee.apim.infra.crud_service.shared_policy_group;

import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupHistoryCrudService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SharedPolicyGroupHistoryRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SharedPolicyGroupHistoryCrudServiceImpl implements SharedPolicyGroupHistoryCrudService {

    private final SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository;
    private final SharedPolicyGroupAdapter sharedPolicyGroupAdapter;

    public SharedPolicyGroupHistoryCrudServiceImpl(
        @Lazy SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository,
        SharedPolicyGroupAdapter sharedPolicyGroupAdapter
    ) {
        this.sharedPolicyGroupHistoryRepository = sharedPolicyGroupHistoryRepository;
        this.sharedPolicyGroupAdapter = sharedPolicyGroupAdapter;
    }

    @Override
    public SharedPolicyGroup create(SharedPolicyGroup sharedPolicyGroupEntity) {
        try {
            var result = sharedPolicyGroupHistoryRepository.create(sharedPolicyGroupAdapter.fromEntity(sharedPolicyGroupEntity));
            return sharedPolicyGroupAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a SharedPolicyGroup with id: %s", sharedPolicyGroupEntity.getId()),
                e
            );
        }
    }
}
