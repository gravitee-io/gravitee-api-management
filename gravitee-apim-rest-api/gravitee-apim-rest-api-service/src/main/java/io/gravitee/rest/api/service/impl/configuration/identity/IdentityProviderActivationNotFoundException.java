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
package io.gravitee.rest.api.service.impl.configuration.identity;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.exceptions.AbstractNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderActivationNotFoundException extends AbstractNotFoundException {

    private final String identityProviderId;
    private final String referenceId;
    private final IdentityProviderActivationReferenceType referenceType;

    public IdentityProviderActivationNotFoundException(
        String identityProviderId,
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) {
        this.identityProviderId = identityProviderId;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    @Override
    public String getMessage() {
        return String.format("Identity Provider [{}] is not Activated for {}:{}.", identityProviderId, referenceType.name(), referenceId);
    }

    @Override
    public String getTechnicalCode() {
        return "identityProvider.notActivated";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("identityProviderId", identityProviderId);
        parameters.put("referenceId", referenceId);
        parameters.put("referenceType", referenceType.name());
        return parameters;
    }
}
