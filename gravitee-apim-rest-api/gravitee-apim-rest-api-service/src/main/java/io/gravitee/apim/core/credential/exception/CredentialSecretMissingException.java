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
package io.gravitee.apim.core.credential.exception;

import io.gravitee.apim.core.exception.TechnicalDomainException;

/** A secret that Gravitee stores for a credential is gone, so the credential cannot be rotated. */
public class CredentialSecretMissingException extends TechnicalDomainException {

    public CredentialSecretMissingException(String credentialId, String field) {
        super(
            "Credential " +
                credentialId +
                " cannot be rotated: the secret of " +
                field +
                " no longer exists. Delete the credential and create it again."
        );
    }
}
