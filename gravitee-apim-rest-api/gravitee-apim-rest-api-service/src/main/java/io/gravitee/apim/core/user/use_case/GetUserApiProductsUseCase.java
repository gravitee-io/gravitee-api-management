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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.model.UserApiProduct;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserApiProductsUseCase {

    private final MembershipQueryService membershipQueryService;
    private final ApiProductQueryService apiProductQueryService;
    private final EnvironmentCrudService environmentCrudService;

    public Output execute(Input input) {
        var memberships = membershipQueryService.findByMemberIdAndMemberTypeAndReferenceType(
            input.userId,
            Membership.Type.USER,
            Membership.ReferenceType.API_PRODUCT
        );

        Set<String> apiProductIds = memberships.stream().map(Membership::getReferenceId).collect(Collectors.toSet());

        if (apiProductIds.isEmpty()) {
            return new Output(List.of(), 0);
        }

        var page = apiProductQueryService.searchByIds(apiProductIds, input.environmentId, new PageableImpl(input.page, input.perPage));

        Set<String> environmentIds = page
            .getContent()
            .stream()
            .map(ApiProduct::getEnvironmentId)
            .filter(envId -> envId != null)
            .collect(Collectors.toSet());
        Map<String, String> environmentNames = resolveEnvironmentNames(environmentIds);

        List<UserApiProduct> data = page
            .getContent()
            .stream()
            .map(apiProduct -> {
                String envId = apiProduct.getEnvironmentId();
                return UserApiProduct.builder()
                    .id(apiProduct.getId())
                    .name(apiProduct.getName())
                    .version(apiProduct.getVersion())
                    .environmentId(envId)
                    .environmentName(envId != null ? environmentNames.get(envId) : null)
                    .build();
            })
            .toList();

        return new Output(data, page.getTotalElements());
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

    public record Output(List<UserApiProduct> data, long totalCount) {}
}
