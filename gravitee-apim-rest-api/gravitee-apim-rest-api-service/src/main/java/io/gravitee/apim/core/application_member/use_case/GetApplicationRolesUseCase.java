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
package io.gravitee.apim.core.application_member.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApplicationRolesUseCase {

    private final RoleQueryService roleQueryService;

    public record Input(String organizationId) {}

    public record Output(List<Role> roles) {}

    public Output execute(Input input) {
        var roles = roleQueryService
            .findByScope(Role.Scope.APPLICATION, input.organizationId())
            .stream()
            .sorted(Comparator.comparing(Role::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();

        return new Output(roles);
    }
}
