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
import io.gravitee.apim.core.application_member.model.ApplicationMemberSearchUser;
import io.gravitee.apim.core.application_member.query_service.ApplicationMemberUserQueryService;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchUsersForApplicationMemberUseCase {

    private final ApplicationMemberUserQueryService applicationMemberUserQueryService;
    private final MemberQueryService memberQueryService;

    public record Input(String applicationId, String query, String environmentId) {}

    public record Output(List<ApplicationMemberSearchUser> users) {}

    public Output execute(Input input) {
        var existingMemberIds = getExistingMemberIds(input.applicationId());
        var users = applicationMemberUserQueryService
            .search(normalizeQuery(input.query()))
            .stream()
            .filter(user -> user.id() == null || !existingMemberIds.contains(user.id()))
            .sorted(
                Comparator.comparing(
                    ApplicationMemberSearchUser::lastName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ).thenComparing(ApplicationMemberSearchUser::displayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            )
            .toList();

        return new Output(users);
    }

    private Set<String> getExistingMemberIds(String applicationId) {
        return memberQueryService
            .getMembersByReference(MembershipReferenceType.APPLICATION, applicationId)
            .stream()
            .map(Member::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? "*" : query;
    }
}
