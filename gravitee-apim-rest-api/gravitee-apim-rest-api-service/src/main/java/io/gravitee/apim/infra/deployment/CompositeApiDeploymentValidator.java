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
package io.gravitee.apim.infra.deployment;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentGate;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentValidator;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Runs every {@link ApiDeploymentValidator} the context holds, so what a module owns inside a definition — the
 * Guardians of an API, the cluster its endpoints bind to — is checked wherever a definition is about to reach the
 * gateways.
 *
 * <p>An {@link ObjectProvider} and not a collection: a Gamma module registers its validator as a singleton in this
 * context after the context is refreshed, and a provider resolves the beans on every call. An injected collection
 * would not — not even a {@code @Lazy} one, whose proxy caches the beans it resolved the first time as soon as they
 * are all singletons, so the Guardian check would silently stop running until the next restart.
 *
 * @author GraviteeSource Team
 */
@Service
public class CompositeApiDeploymentValidator implements ApiDeploymentGate {

    private final ObjectProvider<ApiDeploymentValidator> validators;

    public CompositeApiDeploymentValidator(ObjectProvider<ApiDeploymentValidator> validators) {
        this.validators = validators;
    }

    @Override
    public void validateDeployable(Api api) {
        askEveryValidator(validator -> validator.validateDeployable(api));
    }

    @Override
    public void validateInternalFlowDeployable(Api api) {
        askEveryValidator(validator -> validator.validateInternalFlowDeployable(api));
    }

    private void askEveryValidator(Consumer<ApiDeploymentValidator> question) {
        validators.orderedStream().forEach(question);
    }
}
