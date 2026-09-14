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
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Promotes a form to environment default. An environment has at most one default, which the persistence layer
 * enforces: the previous default is demoted first so the promotion never collides with it, and a promotion that
 * still fails hands the role back to the previous default rather than leaving the environment without one.
 *
 * @author Gravitee.io Team
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class SubscriptionFormDefaultDomainService {

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;

    /**
     * Makes the form the default of its environment, demoting the previous default. No-op when it already is.
     *
     * @return the promoted form as persisted
     */
    public SubscriptionForm promote(SubscriptionForm form) {
        if (form.isDefaultForm()) {
            return form;
        }
        var previousDefault = subscriptionFormQueryService
            .findDefaultForEnvironmentId(form.getEnvironmentId())
            .filter(previous -> !previous.getId().equals(form.getId()));
        previousDefault.ifPresent(previous -> {
            previous.unmarkAsDefault();
            subscriptionFormCrudService.update(previous);
        });
        form.markAsDefault();
        try {
            var promoted = subscriptionFormCrudService.update(form);
            log.info("Subscription form [{}] is now the default of environment [{}]", promoted.getId(), form.getEnvironmentId());
            return promoted;
        } catch (RuntimeException promotionFailure) {
            form.unmarkAsDefault();
            previousDefault.ifPresent(previous -> handBackQuietly(previous, promotionFailure));
            throw promotionFailure;
        }
    }

    /** Compensates a failed promotion without masking its cause. */
    private void handBackQuietly(SubscriptionForm previousDefault, RuntimeException cause) {
        try {
            previousDefault.markAsDefault();
            subscriptionFormCrudService.update(previousDefault);
        } catch (RuntimeException handBackFailure) {
            previousDefault.unmarkAsDefault();
            log.warn(
                "Environment [{}] is left without a default subscription form: form [{}] could not be promoted and the role could not be handed back to form [{}]",
                previousDefault.getEnvironmentId(),
                cause.getMessage(),
                previousDefault.getId()
            );
            cause.addSuppressed(handBackFailure);
        }
    }
}
