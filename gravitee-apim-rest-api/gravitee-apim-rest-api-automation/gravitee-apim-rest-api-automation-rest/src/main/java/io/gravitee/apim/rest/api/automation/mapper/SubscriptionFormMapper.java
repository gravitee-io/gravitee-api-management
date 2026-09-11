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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSpecDomainService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.use_case.CreateOrUpdateSubscriptionFormUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.SubscriptionFormSpec;
import io.gravitee.apim.rest.api.automation.model.SubscriptionFormState;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SubscriptionFormMapper {
    SubscriptionFormMapper INSTANCE = Mappers.getMapper(SubscriptionFormMapper.class);

    default SubscriptionFormSpecDomainService.Spec toSpec(SubscriptionFormSpec spec, AuditInfo auditInfo) {
        return new SubscriptionFormSpecDomainService.Spec(
            auditInfo,
            spec.getHrid(),
            spec.getName(),
            spec.getGmdContent(),
            Boolean.TRUE.equals(spec.getEnabled()),
            Boolean.TRUE.equals(spec.getDefault()),
            toApiIds(spec.getApiHrids(), auditInfo)
        );
    }

    /**
     * APIs are referenced by hrid, like everywhere else in this API: the id derives from it. A value that already
     * is an API id is taken as such, so the state of a form dedicated to an API this Automation API did not create
     * — whose hrid does not derive into its id — can be applied back unchanged.
     */
    default List<String> toApiIds(List<String> apiHrids, AuditInfo auditInfo) {
        if (apiHrids == null) {
            return null;
        }
        return apiHrids
            .stream()
            .filter(apiHrid -> apiHrid != null && !apiHrid.isBlank())
            .map(apiHrid -> isApiId(apiHrid) ? apiHrid : HRIDToUUID.api().context(auditInfo).hrid(apiHrid).id())
            .toList();
    }

    /**
     * Back from the ids the domain stores to what a spec can reference: the hrid of an API whose hrid derives into
     * that id, the id itself for every other API, so re-applying a state maps the very same APIs.
     */
    default List<String> toApiHrids(List<String> apiIds, ApiCrudService apiCrudService, AuditInfo auditInfo) {
        if (apiIds == null || apiIds.isEmpty()) {
            return List.of();
        }
        var hridById = apiCrudService
            .findByIds(apiIds)
            .stream()
            .filter(api -> api.getHrid() != null)
            .filter(api -> api.getId().equals(HRIDToUUID.api().context(auditInfo).hrid(api.getHrid()).id()))
            .collect(Collectors.toMap(Api::getId, Api::getHrid));
        return apiIds
            .stream()
            .map(apiId -> hridById.getOrDefault(apiId, apiId))
            .toList();
    }

    private static boolean isApiId(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * State of an apply or dry run: the persisted form when there is one, the submitted spec otherwise.
     */
    default SubscriptionFormState toState(
        SubscriptionFormSpec spec,
        String formId,
        CreateOrUpdateSubscriptionFormUseCase.Output output,
        AuditInfo auditInfo,
        List<String> apiHrids
    ) {
        var form = output.subscriptionForm();
        var state = new SubscriptionFormState(formId, auditInfo.environmentId(), auditInfo.organizationId(), toErrors(output.errors()));
        state.setHrid(spec.getHrid());
        state.setName(form != null ? form.getName() : spec.getName());
        state.setGmdContent(form != null ? form.getGmdContent().value() : spec.getGmdContent());
        state.setEnabled(form != null ? form.isEnabled() : Boolean.TRUE.equals(spec.getEnabled()));
        state.setDefault(form != null ? form.isDefaultForm() : Boolean.TRUE.equals(spec.getDefault()));
        state.setApiHrids(apiHrids);
        return state;
    }

    default SubscriptionFormState toState(SubscriptionForm form, String hrid, AuditInfo auditInfo, List<String> apiHrids) {
        var state = new SubscriptionFormState(form.getId().toString(), auditInfo.environmentId(), auditInfo.organizationId(), null);
        state.setHrid(hrid);
        state.setName(form.getName());
        state.setGmdContent(form.getGmdContent().value());
        state.setEnabled(form.isEnabled());
        state.setDefault(form.isDefaultForm());
        state.setApiHrids(apiHrids);
        return state;
    }

    default Errors toErrors(List<Validator.Error> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return null;
        }
        var wire = new Errors();
        wire.setSevere(validationErrors.stream().filter(Validator.Error::isSevere).map(Validator.Error::getMessage).toList());
        wire.setWarning(validationErrors.stream().filter(Validator.Error::isWarning).map(Validator.Error::getMessage).toList());
        return wire;
    }
}
