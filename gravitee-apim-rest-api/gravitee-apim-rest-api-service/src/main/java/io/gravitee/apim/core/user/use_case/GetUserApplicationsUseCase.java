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
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.model.UserApplicationEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserApplicationsUseCase {

    private final MembershipQueryService membershipQueryService;
    private final ApplicationQueryService applicationQueryService;
    private final EnvironmentCrudService environmentCrudService;

    public Output execute(Input input) {
        var memberships = membershipQueryService.findByMemberIdAndMemberTypeAndReferenceType(
            input.userId,
            Membership.Type.USER,
            Membership.ReferenceType.APPLICATION
        );

        Set<String> appIds = memberships.stream().map(Membership::getReferenceId).collect(Collectors.toCollection(HashSet::new));

        if (appIds.isEmpty()) {
            return new Output(List.of(), 0);
        }

        var appPage = applicationQueryService.searchByIds(appIds, input.environmentId, new PageableImpl(input.page, input.perPage));

        Set<String> environmentIds = appPage
            .getContent()
            .stream()
            .map(BaseApplicationEntity::getEnvironmentId)
            .filter(envId -> envId != null)
            .collect(Collectors.toSet());
        Map<String, String> environmentNames = resolveEnvironmentNames(environmentIds);

        List<UserApplicationEntity> data = appPage
            .getContent()
            .stream()
            .map(app -> {
                String envId = app.getEnvironmentId();
                return UserApplicationEntity.builder()
                    .id(app.getId())
                    .name(app.getName())
                    .environmentId(envId)
                    .environmentName(envId != null ? environmentNames.get(envId) : null)
                    .build();
            })
            .toList();

        return new Output(data, appPage.getTotalElements());
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

    public record Output(List<UserApplicationEntity> data, long totalCount) {}
}
