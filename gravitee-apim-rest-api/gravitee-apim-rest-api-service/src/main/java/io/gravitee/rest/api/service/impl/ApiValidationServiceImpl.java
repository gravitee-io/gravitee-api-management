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
package io.gravitee.rest.api.service.impl;

import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.adapter.ApiCRDEntityAdapter;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.api.ApiCRDEntity;
import io.gravitee.rest.api.model.api.ApiValidationResult;
import io.gravitee.rest.api.service.ApiValidationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service("apiV2ValidationService")
public class ApiValidationServiceImpl extends AbstractService implements ApiValidationService {

    @Inject
    private ValidateCategoryIdsDomainService validateCategoryIdsDomainService;

    @Inject
    private ValidateCRDMembersDomainService validateCRDMembersDomainService;

    @Inject
    private ValidateGroupsDomainService validateGroupsDomainService;

    @Inject
    private VerifyApiPathDomainService apiPathValidator;

    @Inject
    private ValidatePagesDomainService pagesValidator;

    @Override
    public ApiValidationResult<ApiCRDEntity> validateAndSanitizeApiDefinitionCRD(ExecutionContext executionContext, ApiCRDEntity api) {
        List<Validator.Error> errors = new ArrayList<>();

        apiPathValidator
            .validateAndSanitize(new VerifyApiPathDomainService.Input(executionContext.getEnvironmentId(), api.getId(), extractPaths(api)))
            .peek(sanitized -> api.getProxy().setVirtualHosts(mapPaths(sanitized.paths())), errors::addAll);

        validateCategoryIdsDomainService
            .validateAndSanitize(new ValidateCategoryIdsDomainService.Input(executionContext.getEnvironmentId(), api.getCategories()))
            .peek(sanitized -> api.setCategories(sanitized.idOrKeys()), errors::addAll);

        validateCRDMembersDomainService
            .validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(
                    executionContext.getOrganizationId(),
                    api.getId(),
                    MembershipReferenceType.API,
                    ApiCRDEntityAdapter.INSTANCE.toMemberCRDs(api.getMembers())
                )
            )
            .peek(sanitized -> api.setMembers(ApiCRDEntityAdapter.INSTANCE.toApiCRDMembers(sanitized.members())), errors::addAll);

        validateGroupsDomainService
            .validateAndSanitize(new ValidateGroupsDomainService.Input(executionContext.getEnvironmentId(), api.getGroups()))
            .peek(sanitized -> api.setGroups(sanitized.groups()), errors::addAll);

        pagesValidator
            .validateAndSanitize(
                new ValidatePagesDomainService.Input(
                    AuditInfo
                        .builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .build(),
                    api.getId(),
                    ApiCRDEntityAdapter.INSTANCE.toCoreApiCRDPages(api.getPagesMap())
                )
            )
            .peek(sanitized -> api.setPages(ApiCRDEntityAdapter.INSTANCE.toRestApiCRDPages(sanitized.pages())), errors::addAll);

        return convertToValidationResult(api, errors);
    }

    private static @NotNull ApiValidationResult<ApiCRDEntity> convertToValidationResult(ApiCRDEntity api, List<Validator.Error> errors) {
        List<String> severe = new ArrayList<>();
        List<String> warning = new ArrayList<>();
        for (Validator.Error error : errors) {
            if (error.isSevere()) {
                severe.add(error.getMessage());
            } else {
                warning.add(error.getMessage());
            }
        }

        return new ApiValidationResult<>(api, severe, warning);
    }

    private static List<Path> extractPaths(ApiCRDEntity api) {
        return api
            .getProxy()
            .getVirtualHosts()
            .stream()
            .map(vh -> Path.builder().host(vh.getHost()).path(vh.getPath()).overrideAccess(vh.isOverrideEntrypoint()).build())
            .toList();
    }

    private static List<VirtualHost> mapPaths(List<Path> paths) {
        return paths.stream().map(path -> new VirtualHost(path.getHost(), path.getPath(), path.isOverrideAccess())).toList();
    }
}
