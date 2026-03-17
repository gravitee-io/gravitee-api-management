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
package io.gravitee.apim.core.user.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.model.UserApiEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserApisUseCase {

    private final MembershipQueryService membershipQueryService;
    private final ApiQueryService apiQueryService;
    private final EnvironmentCrudService environmentCrudService;

    public Output execute(Input input) {
        var memberships = membershipQueryService.findByMemberIdAndMemberTypeAndReferenceType(
            input.userId,
            Membership.Type.USER,
            Membership.ReferenceType.API
        );

        List<String> apiIds = memberships.stream().map(Membership::getReferenceId).toList();

        if (apiIds.isEmpty()) {
            return new Output(List.of(), 0);
        }

        var criteria = ApiSearchCriteria.builder().ids(apiIds).environmentId(input.environmentId).build();
        var fieldFilter = ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build();

        Page<Api> apiPage = apiQueryService.search(criteria, null, new PageableImpl(input.page, input.perPage), fieldFilter);

        Set<String> environmentIds = apiPage
            .getContent()
            .stream()
            .map(Api::getEnvironmentId)
            .filter(envId -> envId != null)
            .collect(Collectors.toSet());
        Map<String, String> environmentNames = resolveEnvironmentNames(environmentIds);

        List<UserApiEntity> data = apiPage
            .getContent()
            .stream()
            .map(api -> {
                String envId = api.getEnvironmentId();
                return UserApiEntity.builder()
                    .id(api.getId())
                    .name(api.getName())
                    .version(api.getVersion())
                    .visibility(api.getVisibility() != null ? api.getVisibility().name() : null)
                    .environmentId(envId)
                    .environmentName(envId != null ? environmentNames.get(envId) : null)
                    .build();
            })
            .toList();

        return new Output(data, apiPage.getTotalElements());
    }

    private Map<String, String> resolveEnvironmentNames(Set<String> environmentIds) {
        Map<String, String> result = new HashMap<>();
        for (String envId : environmentIds) {
            try {
                result.put(envId, environmentCrudService.get(envId).getName());
            } catch (Exception e) {
                // Skip environments that cannot be resolved
            }
        }
        return result;
    }

    public record Input(String userId, String environmentId, int page, int perPage) {}

    public record Output(List<UserApiEntity> data, long totalCount) {}
}
