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
package io.gravitee.apim.infra.query_service.shared_policy_group;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.query_service.SharedPolicyGroupHistoryQueryService;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapter;
import io.gravitee.apim.infra.repository.PageUtils;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SharedPolicyGroupHistoryRepository;
import io.gravitee.repository.management.api.search.SharedPolicyGroupHistoryCriteria;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SharedPolicyGroupHistoryQueryServiceImpl implements SharedPolicyGroupHistoryQueryService {

    private final SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository;
    private final SharedPolicyGroupAdapter sharedPolicyGroupAdapter;
    private static final Logger logger = LoggerFactory.getLogger(SharedPolicyGroupHistoryQueryServiceImpl.class);

    public SharedPolicyGroupHistoryQueryServiceImpl(
        @Lazy final SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository,
        SharedPolicyGroupAdapter sharedPolicyGroupAdapter
    ) {
        this.sharedPolicyGroupHistoryRepository = sharedPolicyGroupHistoryRepository;
        this.sharedPolicyGroupAdapter = sharedPolicyGroupAdapter;
    }

    @Override
    public Stream<SharedPolicyGroup> streamLatestBySharedPolicyPolicyGroupId(String environmentId) {
        try {
            return PageUtils
                .toStream(repositoryPageable ->
                    sharedPolicyGroupHistoryRepository.searchLatestBySharedPolicyPolicyGroupId(environmentId, repositoryPageable)
                )
                .map(sharedPolicyGroupAdapter::toEntity);
        } catch (TechnicalException e) {
            logger.error("An error occurred while streaming all last shared policy groups by environment ID {}", environmentId, e);
            throw new TechnicalDomainException(
                "An error occurred while trying to stream all last shared policy groups by environment ID: " + environmentId + e
            );
        }
    }
}
