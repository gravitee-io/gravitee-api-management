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

import static java.util.Objects.requireNonNull;

import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.DuplicateOptions;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.MembershipDuplicateService;
import io.gravitee.rest.api.service.PageDuplicateService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiDuplicateException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.v4.ApiDuplicateService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiDuplicateServiceImpl extends AbstractService implements ApiDuplicateService {

    private final ApiService apiService;
    private final PageDuplicateService pageDuplicateService;
    private final PlanService planService;
    private final MembershipDuplicateService membershipDuplicateService;

    public ApiDuplicateServiceImpl(
        ApiService apiService,
        PageDuplicateService pageDuplicateService,
        PlanService planService,
        MembershipDuplicateService membershipDuplicateService
    ) {
        this.apiService = apiService;
        this.pageDuplicateService = pageDuplicateService;
        this.planService = planService;
        this.membershipDuplicateService = membershipDuplicateService;
    }

    @Override
    public ApiEntity duplicate(ExecutionContext executionContext, ApiEntity sourceApi, DuplicateOptions duplicateOptions) {
        requireNonNull(sourceApi, "Missing source ApiEntity");

        final String apiId = sourceApi.getId();
        log.debug("Duplicate API [apiId={}]", apiId);

        var duplicatedListeners = sourceApi
            .getListeners()
            .stream()
            .map(l -> {
                if (l instanceof HttpListener httpListener) {
                    if (duplicateOptions.getContextPath() == null) {
                        throw new ApiDuplicateException("Cannot find a context-path for HTTP Listener");
                    }
                    return httpListener.toBuilder().paths(List.of(Path.builder().path(duplicateOptions.getContextPath()).build())).build();
                }
                if (l instanceof TcpListener tcpListener) {
                    if (duplicateOptions.getHost() == null) {
                        throw new ApiDuplicateException("Cannot find a host for TCP Listener");
                    }
                    return tcpListener.toBuilder().hosts(List.of(duplicateOptions.getHost())).build();
                }
                return l;
            })
            .toList();

        ApiEntity duplicateEntity = sourceApi
            .toBuilder()
            .id(null)
            .crossId(null)
            .primaryOwner(null)
            .apiVersion(duplicateOptions.getVersion() == null ? sourceApi.getApiVersion() : duplicateOptions.getVersion())
            .listeners(duplicatedListeners)
            .groups(duplicateOptions.isGroupsFiltered() ? null : sourceApi.getGroups())
            .build();

        ApiEntity duplicated = apiService.createWithImport(executionContext, duplicateEntity, getAuthenticatedUsername());

        var duplicatedPagesIdMapping = duplicatePages(executionContext, sourceApi, duplicated, duplicateOptions);
        duplicated.setPlans(duplicatePlans(executionContext, duplicated, duplicateOptions, sourceApi.getPlans(), duplicatedPagesIdMapping));
        duplicateMembers(executionContext, sourceApi, duplicated, duplicateOptions);
        return duplicated;
    }

    private Map<String, String> duplicatePages(
        final ExecutionContext executionContext,
        ApiEntity sourceApi,
        ApiEntity duplicate,
        DuplicateOptions duplicateOptions
    ) {
        if (duplicateOptions.isPagesFiltered()) {
            return Map.of();
        }

        return pageDuplicateService.duplicatePages(executionContext, sourceApi.getId(), duplicate.getId(), getAuthenticatedUsername());
    }

    private Set<PlanEntity> duplicatePlans(
        final ExecutionContext executionContext,
        ApiEntity duplicate,
        DuplicateOptions duplicateOptions,
        Set<PlanEntity> sourcePlans,
        Map<String, String> pagesIdMapping
    ) {
        if (duplicateOptions.isPlansFiltered()) {
            return null;
        }

        Set<PlanEntity> duplicatedPlans = sourcePlans
            .stream()
            .map(planEntity -> {
                planEntity.setApiId(duplicate.getId());
                planEntity.setId(UuidString.generateRandom());
                if (planEntity.getGeneralConditions() != null) {
                    planEntity.setGeneralConditions(pagesIdMapping.get(planEntity.getGeneralConditions()));
                }

                try {
                    return planService.createOrUpdatePlan(executionContext, planEntity);
                } catch (Exception e) {
                    log.warn(
                        "Unable to create plan [planName={}] for duplicated API [apiId={}]' due to : {}",
                        planEntity.getName(),
                        duplicate.getId(),
                        e.getMessage()
                    );
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        log.debug("Plans successfully duplicated for api [apiId={}]", duplicate.getId());
        return duplicatedPlans;
    }

    private void duplicateMembers(
        final ExecutionContext executionContext,
        ApiEntity sourceApi,
        ApiEntity duplicate,
        DuplicateOptions duplicateOptions
    ) {
        if (duplicateOptions.isMembersFiltered()) {
            return;
        }

        membershipDuplicateService.duplicateMemberships(executionContext, sourceApi.getId(), duplicate.getId(), getAuthenticatedUsername());
    }
}
