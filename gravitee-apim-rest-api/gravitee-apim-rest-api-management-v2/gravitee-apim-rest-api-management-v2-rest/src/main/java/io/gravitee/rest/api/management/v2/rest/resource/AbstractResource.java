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
package io.gravitee.rest.api.management.v2.rest.resource;

import static io.gravitee.rest.api.model.MembershipMemberType.USER;
import static io.gravitee.rest.api.model.MembershipReferenceType.API;
import static io.gravitee.rest.api.model.MembershipReferenceType.GROUP;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationLinks;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import io.gravitee.rest.api.service.exceptions.PreconditionFailedException;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.MatchingEntityTag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    public static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    @Context
    protected SecurityContext securityContext;

    @Context
    protected UriInfo uriInfo;

    @Inject
    protected MembershipService membershipService;

    @Inject
    protected RoleService roleService;

    @Inject
    protected ApiService apiService;

    @Inject
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Inject
    protected ApiSearchService apiSearchService;

    @Inject
    protected ApiAuthorizationService apiAuthorizationService;

    @Inject
    protected PermissionService permissionService;

    protected UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected String getAuthenticatedUser() {
        return securityContext.getUserPrincipal().getName();
    }

    protected String getAuthenticatedUserOrNull() {
        return isAuthenticated() ? getAuthenticatedUser() : null;
    }

    protected boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    protected boolean isAdmin() {
        return isUserInRole(ORGANIZATION_ADMIN);
    }

    private boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }

    protected boolean hasPermission(final ExecutionContext executionContext, RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(executionContext, permission, null, acls);
    }

    protected boolean hasPermission(
        ExecutionContext executionContext,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    ) {
        return isAuthenticated() && permissionService.hasPermission(executionContext, permission, referenceId, acls);
    }

    protected boolean canReadAPIConfiguration() {
        if (!isAdmin()) {
            return retrieveApiMembership().findFirst().isPresent();
        }
        return true;
    }

    /**
     * @return The list of API Membership for the authenticated user (direct membership or through groups)
     */
    private Stream<MembershipEntity> retrieveApiMembership() {
        Stream<MembershipEntity> streamUserMembership = membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), API)
            .stream();

        Stream<MembershipEntity> streamGroupMembership = membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), GROUP)
            .stream()
            .filter(m -> m.getRoleId() != null && roleService.findById(m.getRoleId()).getScope().equals(RoleScope.API));

        return Stream.concat(streamUserMembership, streamGroupMembership);
    }

    protected boolean canManageApi(final GenericApiEntity api) {
        return isAdmin() || isDirectMember(api.getId()) || isMemberThroughGroup(api.getGroups());
    }

    private boolean isDirectMember(String apiId) {
        return membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), API)
            .stream()
            .filter(membership -> membership.getReferenceId().equals(apiId))
            .filter(membership -> membership.getRoleId() != null)
            .anyMatch(membership -> {
                RoleEntity role = roleService.findById(membership.getRoleId());
                return apiAuthorizationService.canManageApi(role);
            });
    }

    public Collection<String> listGroupsOfUser() {
        if (!isAuthenticated()) {
            return Set.of();
        }

        return membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), GROUP)
            .stream()
            .map(MembershipEntity::getReferenceId)
            .collect(Collectors.toSet());
    }

    private boolean isMemberThroughGroup(Set<String> apiGroups) {
        if (CollectionUtils.isEmpty(apiGroups)) {
            return false;
        }

        Set<String> groups = membershipService
            .getMembershipsByMemberAndReference(USER, getAuthenticatedUser(), GROUP)
            .stream()
            .filter(membership -> membership.getRoleId() != null)
            .filter(membership -> {
                RoleEntity role = roleService.findById(membership.getRoleId());
                return apiAuthorizationService.canManageApi(role);
            })
            .map(MembershipEntity::getReferenceId)
            .collect(Collectors.toSet());

        groups.retainAll(apiGroups);

        return !groups.isEmpty();
    }

    protected void canReadApi(final ExecutionContext executionContext, final String apiId) {
        if (!isAdmin()) {
            // get memberships of the current user
            List<MembershipEntity> memberships = retrieveApiMembership().collect(Collectors.toList());

            // if the current user is member of the API, continue
            Optional<MembershipEntity> directMembershipEntity = memberships
                .stream()
                .filter(m -> API.equals(m.getReferenceType()))
                .findAny();

            if (directMembershipEntity.isPresent()) {
                return;
            }

            // fetch group memberships
            Set<String> groups = memberships
                .stream()
                .filter(m -> GROUP.equals(m.getReferenceType()))
                .map(MembershipEntity::getReferenceId)
                .collect(Collectors.toSet());

            final ApiQuery apiQuery = new ApiQuery();
            apiQuery.setGroups(new ArrayList<>(groups));
            apiQuery.setIds(Collections.singletonList(apiId));
            final Collection<String> ids = apiService.searchIds(executionContext, apiQuery, new PageableImpl(1, 1), null).getContent();
            final boolean canReadAPI = ids != null && ids.contains(apiId);
            if (!canReadAPI) {
                throw new ForbiddenAccessException();
            }
        }
    }

    protected UriBuilder getRequestUriBuilder() {
        return this.uriInfo.getRequestUriBuilder();
    }

    protected URI getLocationHeader(String... paths) {
        final UriBuilder requestUriBuilder = this.getRequestUriBuilder();
        for (String path : paths) {
            requestUriBuilder.path(path);
        }
        return requestUriBuilder.build();
    }

    protected void evaluateIfMatch(final HttpHeaders headers, final String etagValue) {
        String ifMatch = headers.getHeaderString(HttpHeaders.IF_MATCH);

        if (Objects.nonNull(ifMatch) && !ifMatch.isEmpty()) {
            // Handle case for -gzip appended automatically (and sadly) by Apache
            ifMatch = ifMatch.replaceAll("-gzip", "");

            Set<MatchingEntityTag> matchingTags;
            try {
                matchingTags = HttpHeaderReader.readMatchingEntityTag(ifMatch);
            } catch (java.text.ParseException e) {
                return;
            }

            MatchingEntityTag ifMatchHeader = matchingTags.iterator().next();
            EntityTag eTag = new EntityTag(etagValue, ifMatchHeader.isWeak());

            if (matchingTags != MatchingEntityTag.ANY_MATCH && !matchingTags.contains(eTag)) {
                throw new PreconditionFailedException();
            }
        }
    }

    protected <T> List<T> computePaginationData(Collection<T> list, PaginationParam paginationParam) {
        int numberOfItems = list.size();

        if (paginationParam.getPerPage() == 0 || numberOfItems == 0) {
            return new ArrayList<>();
        }

        int currentPage = paginationParam.getPage();
        int numberOfItemPerPage = paginationParam.getPerPage();

        int startIndex = (currentPage - 1) * numberOfItemPerPage;
        int lastIndex = Math.min(startIndex + numberOfItemPerPage, numberOfItems);

        if (startIndex >= numberOfItems || currentPage < 1) {
            throw new PaginationInvalidException();
        }
        return new ArrayList<>(list).subList(startIndex, lastIndex);
    }

    protected Links computePaginationLinks(long totalElements, PaginationParam paginationParam) {
        return PaginationLinks.computePaginationLinks(
            uriInfo.getRequestUri(),
            uriInfo.getQueryParameters(),
            totalElements,
            paginationParam
        );
    }

    protected AuditInfo getAuditInfo() {
        var executionContext = GraviteeContext.getExecutionContext();
        var user = getAuthenticatedUserDetails();
        return AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build())
            .build();
    }
}
