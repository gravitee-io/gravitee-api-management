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

import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.adapter.ApiCRDEntityAdapter;
import io.gravitee.rest.api.model.api.ApiCRDEntity;
import io.gravitee.rest.api.model.api.ApiCRDEntity.Member;
import io.gravitee.rest.api.model.api.ApiValidationResult;
import io.gravitee.rest.api.service.ApiValidationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Override
    public ApiValidationResult<ApiCRDEntity> validateAndSanitizeApiDefinitionCRD(ExecutionContext executionContext, ApiCRDEntity api) {
        List<Validator.Error> errors = new ArrayList<>();

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
}
