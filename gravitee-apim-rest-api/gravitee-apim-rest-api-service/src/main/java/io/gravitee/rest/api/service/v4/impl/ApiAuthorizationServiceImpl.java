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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component("ApiAuthzServiceImplV4")
public class ApiAuthorizationServiceImpl extends AbstractService implements ApiAuthorizationService {

    private final ApiRepository apiRepository;
    private final CategoryService categoryService;
    private final MembershipService membershipService;
    private final RoleService roleService;
    private final ApplicationService applicationService;
    private final GroupService groupService;
    private final SubscriptionService subscriptionService;
    private final PrimaryOwnerService primaryOwnerService;
    private final SearchEngineService searchEngineService;

    public ApiAuthorizationServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final CategoryService categoryService,
        final MembershipService membershipService,
        final RoleService roleService,
        final ApplicationService applicationService,
        final GroupService groupService,
        final SubscriptionService subscriptionService,
        final PrimaryOwnerService primaryOwnerService,
        final SearchEngineService searchEngineService
    ) {
        this.apiRepository = apiRepository;
        this.categoryService = categoryService;
        this.membershipService = membershipService;
        this.roleService = roleService;
        this.applicationService = applicationService;
        this.groupService = groupService;
        this.subscriptionService = subscriptionService;
        this.primaryOwnerService = primaryOwnerService;
        this.searchEngineService = searchEngineService;
    }

    @Override
    public boolean canManageApi(final RoleEntity role) {
        return (
            role != null &&
            role.getScope() == RoleScope.API &&
            role
                .getPermissions()
                .entrySet()
                .stream()
                .filter(entry -> isApiManagementPermission(entry.getKey()))
                .anyMatch(entry -> {
                    String stringPerm = new String(entry.getValue());
                    return stringPerm.contains("C") || stringPerm.contains("U") || stringPerm.contains("D");
                })
        );
    }

    private boolean isApiManagementPermission(String permissionAsString) {
        return Arrays
            .stream(ApiPermission.values())
            .filter(permission -> permission != ApiPermission.RATING && permission != ApiPermission.RATING_ANSWER)
            .anyMatch(permission -> permission.name().equals(permissionAsString));
    }

    @Override
    public Set<String> findAccessibleApiIdsForUser(final ExecutionContext executionContext, final String userId, ApiQuery apiQuery) {
        if (apiQuery == null) {
            apiQuery = new ApiQuery();
        }
        apiQuery.setLifecycleStates(singletonList(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED));
        return findIdsByUser(executionContext, userId, apiQuery, false);
    }

    @Override
    public Set<String> findIdsByUser(
        ExecutionContext executionContext,
        String userId,
        ApiQuery apiQuery,
        Sortable sortable,
        boolean manageOnly
    ) {
        Optional<Collection<String>> optionalTargetIds = this.searchInDefinition(executionContext, apiQuery);

        if (optionalTargetIds.isPresent()) {
            Collection<String> targetIds = optionalTargetIds.get();
            if (targetIds.isEmpty()) {
                return Collections.emptySet();
            }
            apiQuery.setIds(targetIds);
        }
        List<ApiCriteria> apiCriteriaList = computeApiCriteriaForUser(executionContext, userId, apiQuery, manageOnly);

        if (apiCriteriaList.isEmpty()) {
            return Set.of();
        }
        // Just one call to apiRepository to preserve sort
        // FIXME: Remove this hardcoded page size, it should be handled properly in the service
        Pageable pageable = new PageableImpl(1, Integer.MAX_VALUE);
        List<String> apiIds = apiRepository.searchIds(apiCriteriaList, convert(pageable), convert(sortable)).getContent();
        return new LinkedHashSet<>(apiIds);
    }

    @Override
    public Set<String> findIdsByEnvironment(final String environmentId) {
        if (isBlank(environmentId)) {
            return Set.of();
        }
        final ApiCriteria.Builder builder = new ApiCriteria.Builder().environmentId(environmentId);
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        apiCriteriaList.add(builder.build());

        Pageable pageable = new PageableImpl(1, Integer.MAX_VALUE);
        List<String> apiIds = apiRepository.searchIds(apiCriteriaList, convert(pageable), null).getContent();
        return new LinkedHashSet<>(apiIds);
    }

    /**
     * This method use ApiQuery to search in indexer for fields in api definition
     *
     * @param executionContext
     * @param apiQuery
     * @return Optional<List < String>> an optional list of api ids and Optional.empty()
     * if ApiQuery doesn't contain fields stores in the api definition.
     */
    private Optional<Collection<String>> searchInDefinition(final ExecutionContext executionContext, final ApiQuery apiQuery) {
        if (apiQuery == null) {
            return Optional.empty();
        }
        Query<GenericApiEntity> searchEngineQuery = convert(apiQuery).build();
        if (isBlank(searchEngineQuery.getQuery())) {
            return Optional.empty();
        }
        SearchResult matchApis = searchEngineService.search(executionContext, searchEngineQuery);
        return Optional.of(matchApis.getDocuments());
    }

    private QueryBuilder<GenericApiEntity> convert(ApiQuery query) {
        QueryBuilder<GenericApiEntity> searchEngineQuery = QueryBuilder.create(GenericApiEntity.class);
        if (query.getIds() != null && !query.getIds().isEmpty()) {
            Map<String, Object> filters = new HashMap<>();
            filters.put("api", query.getIds());
            searchEngineQuery.setFilters(filters);
        }

        if (!isBlank(query.getContextPath())) {
            searchEngineQuery.addExplicitFilter("paths", query.getContextPath());
        }
        if (!isBlank(query.getTag())) {
            searchEngineQuery.addExplicitFilter("tag", query.getTag());
        }
        if (query.getDefinitionVersions() != null && !query.getDefinitionVersions().isEmpty()) {
            var allPossibleDefinitionVersions = DefinitionVersion.values();
            List<String> definitionVersionsToExclude = Arrays
                .stream(allPossibleDefinitionVersions)
                .filter(definitionVersion -> !query.getDefinitionVersions().contains(definitionVersion))
                .map(DefinitionVersion::getLabel)
                .collect(toList());
            searchEngineQuery.setExcludedFilters(Map.of(ApiDocumentTransformer.FIELD_DEFINITION_VERSION, definitionVersionsToExclude));
        }
        return searchEngineQuery;
    }

    @Override
    public boolean canConsumeApi(ExecutionContext executionContext, String userId, GenericApiEntity api) {
        if (io.gravitee.rest.api.model.Visibility.PUBLIC.equals(api.getVisibility())) {
            return true;
        }

        boolean isDirectMember = membershipService
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.API)
            .stream()
            .anyMatch(membership -> membership.getReferenceId().equals(api.getId()));

        if (isDirectMember) {
            return true;
        }

        Set<String> apiGroups = api.getGroups();

        if (apiGroups != null && !apiGroups.isEmpty()) {
            boolean isApiGroupMember = membershipService
                .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP)
                .stream()
                .anyMatch(membership -> apiGroups.contains(membership.getReferenceId()));

            if (isApiGroupMember) {
                return true;
            }
        }

        Set<String> applicationIds = applicationService.findUserApplicationsIds(executionContext, userId, ApplicationStatus.ACTIVE);

        if (applicationIds.isEmpty()) {
            return false;
        }

        final SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(applicationIds);
        query.setApi(api.getId());
        query.setStatuses(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.RESUMED));

        return !subscriptionService.search(executionContext, query).isEmpty();
    }

    public List<ApiCriteria> computeApiCriteriaForUser(
        ExecutionContext executionContext,
        String userId,
        ApiQuery apiQuery,
        boolean manageOnly
    ) {
        List<ApiCriteria> apiCriteriaList = new ArrayList<>();
        if (!manageOnly) {
            // if manageOnly is false, return all visible apis for the user. Often used by portal-api resources.
            apiCriteriaList.add(queryToCriteria(executionContext, apiQuery).visibility(PUBLIC).build());
        }

        if (apiQuery == null) {
            apiQuery = new ApiQuery();
        }

        // for others, user must be authenticated
        if (userId != null) {
            // get user apis
            final Set<String> userApiIds = this.findUserApiIdsFromMemberships(userId, manageOnly);

            // add dedicated criteria for user apis
            if (!userApiIds.isEmpty()) {
                apiCriteriaList.add(queryToCriteria(executionContext, apiQuery).ids(userApiIds).build());
            }

            // get user groups apis
            final Set<String> userGroupApiIds = findApiIdsByUserGroups(executionContext, userId, apiQuery, manageOnly);

            // add dedicated criteria for groups apis
            if (!userGroupApiIds.isEmpty()) {
                apiCriteriaList.add(queryToCriteria(executionContext, apiQuery).ids(userGroupApiIds).build());
            }

            // get user subscribed apis, useful when an API becomes private and an app owner is not anymore in members.
            if (!manageOnly) {
                Set<String> applicationIds = applicationService.findUserApplicationsIds(executionContext, userId, ApplicationStatus.ACTIVE);

                if (!applicationIds.isEmpty()) {
                    final SubscriptionQuery query = new SubscriptionQuery();
                    query.setApplications(applicationIds);
                    query.setExcludedApis(
                        apiCriteriaList
                            .stream()
                            .map(ApiCriteria::getIds)
                            .filter(Objects::nonNull)
                            .flatMap(Collection::stream)
                            .collect(toSet())
                    );

                    final Collection<SubscriptionEntity> subscriptions = subscriptionService.search(executionContext, query);
                    if (subscriptions != null && !subscriptions.isEmpty()) {
                        apiCriteriaList.add(
                            queryToCriteria(executionContext, apiQuery)
                                .ids(subscriptions.stream().map(SubscriptionEntity::getApi).distinct().collect(toList()))
                                .build()
                        );
                    }
                }
            }
        }
        return apiCriteriaList;
    }

    private Set<String> getUserApplicationIds(ExecutionContext executionContext, String userId) {
        Set<String> userApplicationIds = membershipService.getReferenceIdsByMemberAndReference(
            MembershipMemberType.USER,
            userId,
            MembershipReferenceType.APPLICATION
        );

        Set<String> applicationIds = new HashSet<>(userApplicationIds);

        Set<String> userGroupIds = membershipService.getReferenceIdsByMemberAndReference(
            MembershipMemberType.USER,
            userId,
            MembershipReferenceType.GROUP
        );

        ApplicationQuery appQuery = new ApplicationQuery();
        appQuery.setGroups(userGroupIds);
        appQuery.setStatus(ApplicationStatus.ACTIVE.name());

        Set<String> groupApplicationIds = applicationService.searchIds(executionContext, appQuery, null);
        applicationIds.addAll(groupApplicationIds);

        return applicationIds;
    }

    public Set<String> findApiIdsByUserId(ExecutionContext executionContext, String userId, ApiQuery apiQuery, boolean manageOnly) {
        if (Objects.isNull(userId)) {
            return new HashSet<>();
        }

        Set<String> apiIds = new HashSet<>();

        if (Objects.isNull(apiQuery)) {
            apiQuery = new ApiQuery();
        }

        // get user apis
        final Set<String> userApiIds = this.findUserApiIdsFromMemberships(userId, manageOnly);
        apiIds.addAll(userApiIds);

        // get user groups apis
        final Set<String> userGroupApiIds = this.findApiIdsByUserGroups(executionContext, userId, apiQuery, manageOnly);
        apiIds.addAll(userGroupApiIds);

        return apiIds;
    }

    private Set<String> findUserApiIdsFromMemberships(String userId, boolean manageOnly) {
        return membershipService
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.API)
            .stream()
            .filter(membership -> membership.getRoleId() != null)
            .filter(membership -> {
                final RoleEntity role = roleService.findById(membership.getRoleId());
                if (manageOnly) {
                    return canManageApi(role);
                }
                return role.getScope().equals(RoleScope.API);
            })
            .map(MembershipEntity::getReferenceId)
            .collect(toSet());
    }

    private Set<String> findApiIdsByUserGroups(ExecutionContext executionContext, String userId, ApiQuery apiQuery, boolean manageOnly) {
        Set<String> apis = new HashSet<>();

        // keep track of API roles mapped to their ID to avoid querying in a loop later
        Map<String, RoleEntity> apiRoles = roleService
            .findByScope(RoleScope.API, executionContext.getOrganizationId())
            .stream()
            .collect(toMap(RoleEntity::getId, Function.identity()));

        List<String> nonPOGroupApiIds = findApiIdsByGroupWithUserHavingNonPOApiRole(
            executionContext,
            userId,
            apiQuery,
            apiRoles,
            manageOnly
        );
        List<String> poGroupApiIds = findApiIdsByGroupWithUserHavingPOApiRole(executionContext, userId, apiQuery, apiRoles, manageOnly);

        apis.addAll(nonPOGroupApiIds);
        apis.addAll(poGroupApiIds);

        return apis;
    }

    private List<String> findApiIdsByGroupWithUserHavingNonPOApiRole(
        ExecutionContext executionContext,
        String userId,
        ApiQuery apiQuery,
        Map<String, RoleEntity> apiRoles,
        boolean manageOnly
    ) {
        Set<String> nonPoRoleIds = apiRoles
            .values()
            .stream()
            .filter(role -> !role.isApiPrimaryOwner())
            .map(RoleEntity::getId)
            .collect(toSet());

        String[] groupIds = membershipService
            .getMembershipsByMemberAndReferenceAndRoleIn(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP, nonPoRoleIds)
            .stream()
            .filter(membership -> {
                final RoleEntity roleInGroup = apiRoles.get(membership.getRoleId());
                if (manageOnly) {
                    return canManageApi(roleInGroup);
                }
                return roleInGroup.getScope().equals(RoleScope.API);
            })
            .map(MembershipEntity::getReferenceId)
            .filter(Objects::nonNull)
            .toArray(String[]::new);

        if (groupIds.length > 0) {
            List<String> apiIds = apiRepository
                .searchIds(
                    List.of(queryToCriteria(executionContext, apiQuery).groups(groupIds).build()),
                    new PageableBuilder().pageSize(Integer.MAX_VALUE).build(),
                    null
                )
                .getContent();
            return apiIds != null ? apiIds : List.of();
        }

        return List.of();
    }

    /*
     * If the user has the PRIMARY_OWNER role on the API scope in a group,
     * the user will keep this role only if the group is primary owner
     * on the API. If not, his role will be set to the default API role
     * for this group.
     *
     * If no default role has been defined on the group,
     * the API is removed from the list.
     *
     * see https://github.com/gravitee-io/issues/issues/6360
     */
    private List<String> findApiIdsByGroupWithUserHavingPOApiRole(
        ExecutionContext executionContext,
        String userId,
        ApiQuery apiQuery,
        Map<String, RoleEntity> apiRoles,
        boolean manageOnly
    ) {
        String apiPrimaryOwnerRoleId = apiRoles
            .values()
            .stream()
            .filter(RoleEntity::isApiPrimaryOwner)
            .map(RoleEntity::getId)
            .findFirst()
            .orElseThrow(() -> new TechnicalManagementException("Unable to find API Primary Owner System Role"));

        String[] poGroupIds = membershipService
            .getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.GROUP,
                apiPrimaryOwnerRoleId
            )
            .stream()
            .map(MembershipEntity::getReferenceId)
            .filter(Objects::nonNull)
            .toArray(String[]::new);

        // keep track of roles mapped to their name to be able to evaluate the default API role permission for the groups
        Map<String, RoleEntity> apiRolesByName = apiRoles.values().stream().collect(toMap(RoleEntity::getName, Function.identity()));

        // keep track of all the groups where the user has the role Primary Owner on the API scope
        Set<GroupEntity> userGroups = groupService.findByIds(Set.of(poGroupIds));

        if (poGroupIds.length > 0) {
            return apiRepository
                .search(queryToCriteria(executionContext, apiQuery).groups(poGroupIds).build(), null, ApiFieldFilter.allFields())
                .filter(api -> {
                    PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(
                        executionContext.getOrganizationId(),
                        api.getId()
                    );
                    /*
                     * If one of the groups where the user has the API Primary Owner Role
                     * is the actual Primary Owner of the API, grant permission
                     */
                    if (Set.of(poGroupIds).contains(primaryOwner.getId())) {
                        return true;
                    }
                    /*
                     * Otherwise, check if the default API role for one of the groups
                     * grants permission to the user
                     */
                    return userGroups
                        .stream()
                        .map(GroupEntity::getRoles)
                        .filter(Objects::nonNull)
                        .anyMatch(groupDefaultRoles -> {
                            String defaultApiRoleName = groupDefaultRoles.get(RoleScope.API);
                            final RoleEntity role = apiRolesByName.get(defaultApiRoleName);
                            if (manageOnly) {
                                return canManageApi(role);
                            }
                            return role != null && role.getScope().equals(RoleScope.API);
                        });
                })
                .map(Api::getId)
                .collect(toList());
        }
        return List.of();
    }

    private ApiCriteria.Builder queryToCriteria(ExecutionContext executionContext, ApiQuery query) {
        final ApiCriteria.Builder builder = new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId());
        if (query == null) {
            return builder;
        }
        builder.label(query.getLabel()).name(query.getName()).version(query.getVersion());

        if (query.getDefinitionVersions() != null && !query.getDefinitionVersions().isEmpty()) {
            builder.definitionVersion(query.getDefinitionVersions());
        }

        if (!isBlank(query.getCategory())) {
            builder.category(categoryService.findById(query.getCategory(), executionContext.getEnvironmentId()).getId());
        }
        if (query.getGroups() != null && !query.getGroups().isEmpty()) {
            builder.groups(query.getGroups());
        }
        if (!isBlank(query.getState())) {
            builder.state(LifecycleState.valueOf(query.getState()));
        }
        if (query.getVisibility() != null) {
            builder.visibility(Visibility.valueOf(query.getVisibility().name()));
        }
        if (query.getLifecycleStates() != null) {
            builder.lifecycleStates(
                query
                    .getLifecycleStates()
                    .stream()
                    .map(apiLifecycleState -> ApiLifecycleState.valueOf(apiLifecycleState.name()))
                    .collect(toList())
            );
        }
        if (query.getIds() != null && !query.getIds().isEmpty()) {
            builder.ids(query.getIds());
        }
        if (query.getCrossId() != null && !query.getCrossId().isEmpty()) {
            builder.crossId(query.getCrossId());
        }

        return builder;
    }
}
