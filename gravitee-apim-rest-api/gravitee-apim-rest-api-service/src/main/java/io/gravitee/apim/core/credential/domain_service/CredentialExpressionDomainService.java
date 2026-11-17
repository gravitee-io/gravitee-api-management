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
package io.gravitee.apim.core.credential.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.credential.service_provider.SecretExpressionParser;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CredentialExpressionDomainService {

    private final SecretExpressionParser secretExpressionParser;

    /**
     * An expression made only of literals names its path, so it gets every check a typed reference gets. One that reads
     * the request can resolve to any secret of the environment, and only the gateway can check where it lands.
     */
    public void requireValidExpressions(Credential credential) {
        credential
            .secretReferences()
            .forEach((field, reference) -> {
                if (reference.isExpression()) {
                    secretExpressionParser
                        .staticPath(field, reference.reference())
                        .ifPresent(path -> SecretReference.external(field, path));
                }
            });
    }
}
