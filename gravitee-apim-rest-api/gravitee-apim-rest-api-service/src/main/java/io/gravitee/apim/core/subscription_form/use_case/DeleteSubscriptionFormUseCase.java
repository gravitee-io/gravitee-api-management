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
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormIsDefaultException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Removes a form from the environment catalog, with its definition and its API mappings. The
 * environment default cannot be removed: promote another form first.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class DeleteSubscriptionFormUseCase {

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;

    public void execute(Input input) {
        var form = subscriptionFormQueryService
            .findByIdAndEnvironmentId(input.environmentId(), input.subscriptionFormId())
            .orElseThrow(() ->
                new SubscriptionFormNotFoundException(
                    "Subscription form not found with id [ " + input.subscriptionFormId() + " ]",
                    input.subscriptionFormId().toString()
                )
            );
        if (form.isDefaultForm()) {
            throw new SubscriptionFormIsDefaultException(input.subscriptionFormId().toString());
        }
        subscriptionFormCrudService.delete(form);
        log.info("Deleted subscription form [{}] '{}' from environment [{}]", form.getId(), form.getName(), input.environmentId());
    }

    public record Input(String environmentId, SubscriptionFormId subscriptionFormId) {}
}
