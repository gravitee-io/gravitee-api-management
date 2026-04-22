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

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.model.User;
import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.apim.core.user.query_service.UserQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchUsersUseCase {

    private final UserQueryService userQueryService;
    private final MembershipQueryService membershipQueryService;
    private final ApplicationCrudService applicationCrudService;

    public Output execute(Input input) {
        input
            .applicationMembership()
            .ifPresent(applicationId -> applicationCrudService.findById(applicationId, input.executionContext().getEnvironmentId()));

        var searchedUsers = userQueryService.search(input.searchQuery());
        var sortedUsers = sortUsers(searchedUsers);

        return new Output(
            sortedUsers,
            sortedUsers.size(),
            input
                .applicationMembership()
                .map(applicationId ->
                    sortedUsers.isEmpty() ? Map.<String, Boolean>of() : buildApplicationMembership(applicationId, sortedUsers)
                )
                .orElse(null)
        );
    }

    private List<User> sortUsers(List<User> users) {
        return users.stream().sorted(Comparator.comparing(User::lastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList();
    }

    private Map<String, Boolean> buildApplicationMembership(String applicationId, List<User> users) {
        var userIds = users.stream().map(User::id).toList();
        Set<String> alreadyAddedUserIds = membershipQueryService
            .findByMemberIdsAndMemberTypeAndReferenceType(userIds, Membership.Type.USER, Membership.ReferenceType.APPLICATION)
            .stream()
            .filter(membership -> applicationId.equals(membership.getReferenceId()))
            .map(Membership::getMemberId)
            .collect(Collectors.toSet());

        return users.stream().collect(Collectors.toMap(User::id, user -> alreadyAddedUserIds.contains(user.id())));
    }

    public record Input(
        @NotNull ExecutionContext executionContext,
        @NotNull UserSearchQuery searchQuery,
        @NotNull Optional<String> applicationMembership
    ) {
        public Input {
            applicationMembership = requireNonNull(applicationMembership, "applicationMembership must not be null").filter(
                not(String::isBlank)
            );
        }
    }

    public record Output(List<User> data, long totalCount, Map<String, Boolean> applicationMembership) {}
}
