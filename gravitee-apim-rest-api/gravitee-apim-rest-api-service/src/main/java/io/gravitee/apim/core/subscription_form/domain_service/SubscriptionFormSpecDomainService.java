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
package io.gravitee.apim.core.subscription_form.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Checks an Automation API subscription form spec the way the Console checks a form, but reports the
 * findings as {@link Validator.Error}s instead of throwing, so an apply and its dry run share one
 * verdict. The form id derives from the spec's human readable id.
 *
 * @author Gravitee.io Team
 */
@DomainService
@RequiredArgsConstructor
public class SubscriptionFormSpecDomainService {

    private final SubscriptionFormDefinitionDomainService definitionDomainService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;

    public record Spec(
        AuditInfo auditInfo,
        String hrid,
        String name,
        String gmdContent,
        boolean enabled,
        boolean defaultForm,
        @Nullable List<String> apiIds
    ) {
        public SubscriptionFormId subscriptionFormId() {
            return SubscriptionFormId.of(HRIDToUUID.subscriptionForm().context(auditInfo).hrid(hrid).id());
        }
    }

    public record Validation(
        Optional<SubscriptionForm> existing,
        @Nullable SubscriptionFormDefinitionDomainService.Definition definition,
        List<String> apiIds,
        List<Validator.Error> errors
    ) {
        public boolean hasSevereErrors() {
            return errors.stream().anyMatch(Validator.Error::isSevere);
        }
    }

    public Validation validate(Spec spec) {
        var environmentId = spec.auditInfo().environmentId();
        var errors = new ArrayList<Validator.Error>();
        if (spec.hrid() == null || spec.hrid().isBlank()) {
            errors.add(Validator.Error.severe("hrid must not be blank"));
            return new Validation(Optional.empty(), null, List.of(), errors);
        }
        var existing = subscriptionFormQueryService.findByIdAndEnvironmentId(environmentId, spec.subscriptionFormId());
        var existingId = existing.map(SubscriptionForm::getId).orElse(null);

        SubscriptionFormDefinitionDomainService.Definition definition = null;
        List<String> apiIds = List.of();
        try {
            definitionDomainService.validateName(environmentId, spec.name(), existingId);
            apiIds = definitionDomainService.validateApiIds(environmentId, spec.apiIds(), existingId);
            definition = definitionDomainService.compile(spec.gmdContent());
        } catch (AbstractDomainException e) {
            errors.add(Validator.Error.severe("%s", e.getMessage()));
        }
        if (!spec.defaultForm() && existing.map(SubscriptionForm::isDefaultForm).orElse(false)) {
            errors.add(
                Validator.Error.severe("The default subscription form cannot be demoted; apply another form with default=true first.")
            );
        }
        return new Validation(existing, definition, apiIds, errors);
    }
}
