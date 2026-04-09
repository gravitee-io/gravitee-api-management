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
package io.gravitee.apim.core.application.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.model.SearchApplicationMembersCriteria;
import io.gravitee.apim.core.membership.domain_service.SearchApplicationMembersDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@UseCase
public class SearchApplicationMembersUseCase {

    private final SearchApplicationMembersDomainService searchApplicationMembersDomainService;
    private final UserCrudService userCrudService;

    public SearchApplicationMembersUseCase(
        SearchApplicationMembersDomainService searchApplicationMembersDomainService,
        UserCrudService userCrudService
    ) {
        this.searchApplicationMembersDomainService = searchApplicationMembersDomainService;
        this.userCrudService = userCrudService;
    }

    public Output execute(Input input) {
        var memberships = searchApplicationMembersDomainService.searchApplicationMembers(
            input.executionContext().getEnvironmentId(),
            input.applicationId()
        );

        var userIds = memberships
            .stream()
            .filter(membership -> membership.getMemberType() == Membership.Type.USER)
            .map(Membership::getMemberId)
            .distinct()
            .toList();
        var usersById = findUsersById(userIds);

        var sortedAndFilteredMemberships = sortAndFilterMemberships(memberships, usersById, input.criteria());
        var page = paginate(sortedAndFilteredMemberships, input.pageable());
        var usersInPage = page
            .getContent()
            .stream()
            .filter(membership -> membership.getMemberType() == Membership.Type.USER)
            .map(Membership::getMemberId)
            .map(usersById::get)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        return new Output(page, usersInPage);
    }

    private Map<String, BaseUserEntity> findUsersById(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userCrudService.findBaseUsersByIds(userIds).stream().collect(Collectors.toMap(BaseUserEntity::getId, Function.identity()));
    }

    private List<Membership> sortAndFilterMemberships(
        Collection<Membership> memberships,
        Map<String, BaseUserEntity> usersById,
        SearchApplicationMembersCriteria criteria
    ) {
        var searchedDisplayName = criteria != null ? criteria.displayName() : null;

        return memberships
            .stream()
            .filter(membership -> displayNameMatches(membership, searchedDisplayName, usersById))
            .sorted(Comparator.comparing(membership -> normalizedDisplayName(membership, usersById)))
            .toList();
    }

    private boolean displayNameMatches(Membership membership, String searchedDisplayName, Map<String, BaseUserEntity> usersById) {
        if (searchedDisplayName == null || searchedDisplayName.isBlank()) {
            return true;
        }

        if (membership.getMemberType() != Membership.Type.USER) {
            return false;
        }

        var user = usersById.get(membership.getMemberId());
        if (user == null) {
            return false;
        }

        var displayName = user.displayName();
        if (displayName == null) {
            return false;
        }

        return displayName.toLowerCase(Locale.ROOT).contains(searchedDisplayName.toLowerCase(Locale.ROOT));
    }

    private String normalizedDisplayName(Membership membership, Map<String, BaseUserEntity> usersById) {
        if (membership.getMemberType() != Membership.Type.USER) {
            return "";
        }

        var user = usersById.get(membership.getMemberId());
        if (user == null) {
            return "";
        }

        var displayName = user.displayName();
        return displayName != null ? displayName.toLowerCase(Locale.ROOT) : "";
    }

    private Page<Membership> paginate(Collection<Membership> memberships, Pageable pageable) {
        var resolvedPageable = Optional.ofNullable(pageable).orElseGet(() -> new PageableImpl(1, Integer.MAX_VALUE));

        var totalCount = memberships.size();
        var startIndex = (resolvedPageable.getPageNumber() - 1) * resolvedPageable.getPageSize();
        if (resolvedPageable.getPageNumber() < 1 || (totalCount > 0 && startIndex >= totalCount)) {
            throw new PaginationInvalidException();
        }

        var pageContent = memberships.stream().skip(startIndex).limit(resolvedPageable.getPageSize()).toList();
        return new Page<>(pageContent, resolvedPageable.getPageNumber(), resolvedPageable.getPageSize(), totalCount);
    }

    public record Input(
        ExecutionContext executionContext,
        String applicationId,
        SearchApplicationMembersCriteria criteria,
        Pageable pageable
    ) {}

    public record Output(Page<Membership> memberships, List<BaseUserEntity> users) {}
}
