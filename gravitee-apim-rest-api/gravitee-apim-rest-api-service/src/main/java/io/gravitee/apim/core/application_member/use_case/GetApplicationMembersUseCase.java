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
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApplicationMembersUseCase {

    private final MemberQueryService memberQueryService;

    public record Input(String applicationId, String environmentId, String query, int page, int size) {}

    public record Output(List<Member> members, long totalElements) {}

    public Output execute(Input input) {
        var filteredMembers = memberQueryService
            .getMembersByReference(MembershipReferenceType.APPLICATION, input.applicationId())
            .stream()
            .filter(member -> matchesQuery(member, input.query()))
            .sorted(Comparator.comparing(Member::getId, Comparator.nullsLast(String::compareTo)))
            .toList();

        return new Output(paginate(filteredMembers, input.page(), input.size()), filteredMembers.size());
    }

    private List<Member> paginate(List<Member> members, int page, int size) {
        if (size == -1) {
            return members;
        }

        if (page < 1 || size < 1) {
            return List.of();
        }

        int fromIndex = (page - 1) * size;
        if (fromIndex >= members.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, members.size());
        return members.subList(fromIndex, toIndex);
    }

    private boolean matchesQuery(Member member, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        var normalizedQuery = query.toLowerCase(Locale.ROOT);
        return contains(member.getDisplayName(), normalizedQuery) || contains(member.getEmail(), normalizedQuery);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
