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
package io.gravitee.apim.infra.crud_service.subscription_form;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.infra.adapter.SubscriptionFormAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of SubscriptionFormCrudService.
 *
 * @author Gravitee.io Team
 */
@Component
public class SubscriptionFormCrudServiceImpl implements SubscriptionFormCrudService {

    private final SubscriptionFormRepository subscriptionFormRepository;
    private static final SubscriptionFormAdapter subscriptionFormAdapter = SubscriptionFormAdapter.INSTANCE;

    public SubscriptionFormCrudServiceImpl(@Lazy SubscriptionFormRepository subscriptionFormRepository) {
        this.subscriptionFormRepository = subscriptionFormRepository;
    }

    @Override
    public SubscriptionForm create(SubscriptionForm subscriptionForm) {
        SubscriptionForm toCreate = subscriptionForm.getId() == null
            ? SubscriptionForm.builder()
                .id(SubscriptionFormId.random())
                .environmentId(subscriptionForm.getEnvironmentId())
                .gmdContent(subscriptionForm.getGmdContent())
                .enabled(subscriptionForm.isEnabled())
                .build()
            : subscriptionForm;
        try {
            var result = subscriptionFormRepository.create(subscriptionFormAdapter.toRepository(toCreate));
            return subscriptionFormAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to create a SubscriptionForm for env: %s", toCreate.getEnvironmentId()),
                e
            );
        }
    }

    @Override
    public SubscriptionForm update(SubscriptionForm subscriptionForm) {
        try {
            var result = subscriptionFormRepository.update(subscriptionFormAdapter.toRepository(subscriptionForm));
            return subscriptionFormAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to update a SubscriptionForm with id: %s",
                    subscriptionForm.getId().toString()
                ),
                e
            );
        }
    }
}
