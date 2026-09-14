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
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import java.util.Optional;

/**
 * Decides which subscription form applies when a consumer subscribes to an API. A form only applies to the APIs it
 * is dedicated to, and forms cannot be dedicated to APIs yet: no API has a form, so its subscription skips the form
 * step.
 *
 * @author Gravitee.io Team
 */
@DomainService
public class SubscriptionFormResolutionDomainService {

    public Optional<SubscriptionForm> resolveForApi(String environmentId, String apiId) {
        return Optional.empty();
    }
}
