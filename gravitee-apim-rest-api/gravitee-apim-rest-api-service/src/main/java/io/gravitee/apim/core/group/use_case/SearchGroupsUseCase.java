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
package io.gravitee.apim.core.group.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@UseCase
public class SearchGroupsUseCase {

    private final GroupQueryService groupQueryService;

    public SearchGroupsUseCase(GroupQueryService groupQueryService) {
        this.groupQueryService = groupQueryService;
    }

    public Page<Group> execute(ExecutionContext executionContext, Set<String> groupIds, Pageable pageable) {
        if (groupIds == null || groupIds.isEmpty()) return new Page<>(List.of(), 0, 0, 0);
        return groupQueryService.searchGroups(executionContext, groupIds, pageable);
    }
}
