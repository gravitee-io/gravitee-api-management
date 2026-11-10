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
import java.util.List;

/** Some secrets hold the new values and the rest the old ones: the user must rotate the failed field again. */
public class CredentialRotationIncompleteException extends TechnicalDomainException {

    public CredentialRotationIncompleteException(String credentialId, List<String> rotatedFields, String failedField, Throwable cause) {
        super(
            "Credential " +
                credentialId +
                " is partly rotated: the new values of " +
                String.join(", ", rotatedFields) +
                " are written, but " +
                failedField +
                " could not be written. Rotate " +
                failedField +
                " again.",
            cause
        );
    }
}
