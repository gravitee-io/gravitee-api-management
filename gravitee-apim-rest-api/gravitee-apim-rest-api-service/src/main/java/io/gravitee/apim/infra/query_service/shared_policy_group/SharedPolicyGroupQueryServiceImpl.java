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
import io.gravitee.apim.core.shared_policy_group.query_service.SharedPolicyGroupQueryService;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapter;
import io.gravitee.apim.infra.repository.PageUtils;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.api.search.SharedPolicyGroupCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class SharedPolicyGroupQueryServiceImpl implements SharedPolicyGroupQueryService {

    private final SharedPolicyGroupRepository sharedPolicyGroupRepository;
    private final SharedPolicyGroupAdapter sharedPolicyGroupAdapter;
    private static final Logger logger = LoggerFactory.getLogger(SharedPolicyGroupQueryServiceImpl.class);

    public SharedPolicyGroupQueryServiceImpl(
        @Lazy final SharedPolicyGroupRepository sharedPolicyGroupRepository,
        SharedPolicyGroupAdapter sharedPolicyGroupAdapter
    ) {
        this.sharedPolicyGroupRepository = sharedPolicyGroupRepository;
        this.sharedPolicyGroupAdapter = sharedPolicyGroupAdapter;
    }

    @Override
    public Page<SharedPolicyGroup> searchByEnvironmentId(String environmentId, String q, Pageable pageable, Sortable sortable) {
        try {
            var criteria = SharedPolicyGroupCriteria.builder().name(q).environmentId(environmentId).build();

            var result = sharedPolicyGroupRepository.search(
                criteria,
                new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build(),
                new SortableBuilder().field(sortable.getField()).setAsc(sortable.isAscOrder()).build()
            );

            return result.map(sharedPolicyGroupAdapter::toEntity);
        } catch (TechnicalException e) {
            logger.error("An error occurred while searching shared policy groups by environment ID {}", environmentId, e);
            throw new TechnicalDomainException(
                "An error occurred while trying to search shared policy groups by environment ID: " + environmentId,
                e
            );
        }
    }

    @Override
    public Stream<SharedPolicyGroup> streamByEnvironmentIdAndState(
        String environmentId,
        SharedPolicyGroup.SharedPolicyGroupLifecycleState lifecycleState,
        Sortable sortable
    ) {
        try {
            var criteria = SharedPolicyGroupCriteria
                .builder()
                .environmentId(environmentId)
                .lifecycleState(sharedPolicyGroupAdapter.fromEntity(lifecycleState))
                .build();
            var repositorySortable = new SortableBuilder().field(sortable.getField()).setAsc(sortable.isAscOrder()).build();

            return PageUtils
                .toStream(repositoryPageable -> sharedPolicyGroupRepository.search(criteria, repositoryPageable, repositorySortable))
                .map(sharedPolicyGroupAdapter::toEntity);
        } catch (TechnicalException e) {
            logger.error(
                "An error occurred while streaming shared policy groups by environment ID {} and lifecycleState {}",
                environmentId,
                lifecycleState,
                e
            );
            throw new TechnicalDomainException(
                "An error occurred while trying to stream shared policy groups by environment ID: " +
                environmentId +
                " and lifecycleState: " +
                lifecycleState,
                e
            );
        }
    }
}
