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
package io.gravitee.apim.infra.query_service.subscription_form;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import io.gravitee.apim.infra.adapter.SubscriptionFormAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of SubscriptionFormQueryService.
 *
 * @author Gravitee.io Team
 */
@Component
public class SubscriptionFormQueryServiceImpl implements SubscriptionFormQueryService {

    private final SubscriptionFormRepository subscriptionFormRepository;
    private static final SubscriptionFormAdapter subscriptionFormAdapter = SubscriptionFormAdapter.INSTANCE;

    public SubscriptionFormQueryServiceImpl(@Lazy SubscriptionFormRepository subscriptionFormRepository) {
        this.subscriptionFormRepository = subscriptionFormRepository;
    }

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String environmentId, SubscriptionFormId subscriptionFormId) {
        try {
            return subscriptionFormRepository
                .findByIdAndEnvironmentId(subscriptionFormId.toString(), environmentId)
                .map(subscriptionFormAdapter::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to find a SubscriptionForm with id: %s in environment: %s",
                    subscriptionFormId,
                    environmentId
                ),
                e
            );
        }
    }

    @Override
    public Optional<SubscriptionForm> findDefaultForEnvironmentId(String environmentId) {
        try {
            return subscriptionFormRepository.findByEnvironmentId(environmentId).map(subscriptionFormAdapter::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to find a SubscriptionForm for environment: %s", environmentId),
                e
            );
        }
    }
}
