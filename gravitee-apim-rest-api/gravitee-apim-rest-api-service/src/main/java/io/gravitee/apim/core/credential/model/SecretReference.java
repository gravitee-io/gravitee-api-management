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
package io.gravitee.apim.core.credential.model;

import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.secret.model.Secret;
import java.util.regex.Pattern;

/**
 * Where the gateway reads one secret field of a credential.
 *
 * @param reference the secret path the gateway resolves, e.g. {@code /vault/secret/openai:token}
 * @param secretId the Gravitee secret that holds the value, or {@code null} when the user referenced a secret of their own
 */
public record SecretReference(String reference, String secretId) {
    /** Lets the gateway pick up a rotated value without a redeploy. */
    static final String MANAGED_FLAGS = "?renewable=true&reloadOnChange=true";

    private static final String GRAVITEE_PROVIDER = "gravitee";
    private static final int MAX_LENGTH = 2048;
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    public static SecretReference external(String field, String reference) {
        if (
            reference == null ||
            !reference.startsWith("/") ||
            reference.length() > MAX_LENGTH ||
            WHITESPACE.matcher(reference).find() ||
            reference.contains("{#")
        ) {
            throw new InvalidCredentialException(
                "The reference of " + field + " must be a secret path such as /vault/secret/name:key, with no spaces or expressions."
            );
        }
        var external = new SecretReference(reference, null);
        if (external.isInGraviteeStore() && external.graviteeKey().startsWith(Secret.CREDENTIAL_KEY_PREFIX)) {
            throw new InvalidCredentialException(
                "The reference of " +
                    field +
                    " points at a secret that another credential holds. Reference a secret of your own or give the value."
            );
        }
        return external;
    }

    /** The secret key, read the way the gateway reads it: the path after the provider, before any query or {@code :key}. */
    private String graviteeKey() {
        var path = reference.substring(reference.indexOf('/', 1) + 1);
        var query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        var key = path.lastIndexOf(':');
        return key > path.lastIndexOf('/') ? path.substring(0, key) : path;
    }

    /** Whether the reference names Gravitee's own secret store, the provider the gateway reads from its first path segment. */
    public boolean isInGraviteeStore() {
        var providerEnd = reference.indexOf('/', 1);
        return providerEnd > 0 && reference.substring(1, providerEnd).equalsIgnoreCase(GRAVITEE_PROVIDER);
    }

    public static SecretReference managed(String path, String secretId) {
        return new SecretReference(path + MANAGED_FLAGS, secretId);
    }

    public boolean isManaged() {
        return secretId != null;
    }
}
