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
package io.gravitee.apim.core.subscription_form.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormDefaultDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSpecDomainService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.validation.Validator;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Automation API upsert: the form id derives from the human readable id, so applying the same spec
 * twice updates the form created the first time. Severe findings are reported in
 * {@link Output#errors()} rather than thrown, so the caller receives the structured
 * {@code errors.severe[]} array; nothing is persisted when any is present.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class CreateOrUpdateSubscriptionFormUseCase {

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormSpecDomainService specDomainService;
    private final SubscriptionFormDefaultDomainService defaultDomainService;

    public record Output(@Nullable SubscriptionForm subscriptionForm, List<Validator.Error> errors) {}

    public Output execute(SubscriptionFormSpecDomainService.Spec spec) {
        var validation = specDomainService.validate(spec);
        if (validation.hasSevereErrors()) {
            return new Output(validation.existing().orElse(null), validation.errors());
        }

        SubscriptionForm saved = validation
            .existing()
            .map(form -> update(form, spec, validation))
            .orElseGet(() -> create(spec, validation));
        if (spec.defaultForm()) {
            saved = defaultDomainService.promote(saved);
        }
        log.info(
            "Applied subscription form [{}] '{}' on environment [{}] through automation",
            saved.getId(),
            saved.getName(),
            spec.auditInfo().environmentId()
        );
        return new Output(saved, validation.errors());
    }

    private SubscriptionForm create(SubscriptionFormSpecDomainService.Spec spec, SubscriptionFormSpecDomainService.Validation validation) {
        return subscriptionFormCrudService.create(
            SubscriptionForm.builder()
                .id(spec.subscriptionFormId())
                .environmentId(spec.auditInfo().environmentId())
                .name(spec.name().trim())
                .gmdContent(validation.definition().gmdContent())
                .validationConstraints(validation.definition().constraints())
                .apiIds(validation.apiIds())
                .enabled(spec.enabled())
                .defaultForm(false)
                .build()
        );
    }

    private SubscriptionForm update(
        SubscriptionForm form,
        SubscriptionFormSpecDomainService.Spec spec,
        SubscriptionFormSpecDomainService.Validation validation
    ) {
        form.rename(spec.name().trim());
        form.update(validation.definition().gmdContent(), validation.definition().constraints());
        form.assignApis(validation.apiIds());
        if (spec.enabled()) {
            form.enable();
        } else {
            form.disable();
        }
        return subscriptionFormCrudService.update(form);
    }
}
