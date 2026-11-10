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
package io.gravitee.apim.core.secret.model;

import io.gravitee.apim.core.secret.exception.InvalidSecretKeyException;
import io.gravitee.apim.core.secret.exception.SecretKeptForCredentialException;
import java.time.Instant;
import java.util.regex.Pattern;
import lombok.Builder;

@Builder(toBuilder = true)
public record Secret(String id, String organizationId, String environmentId, String key, Instant createdAt, Instant updatedAt) {
    /** Keys under this prefix hold the values of Platform credentials, which only the credential API may change. */
    public static final String CREDENTIAL_KEY_PREFIX = "credential.";
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}");

    public static Secret create(String id, String organizationId, String environmentId, String key, Instant now) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new InvalidSecretKeyException();
        }
        return new Secret(id, organizationId, environmentId, key, now, now);
    }

    public void requireChangeableBy(boolean credential) {
        if (!credential && key.startsWith(CREDENTIAL_KEY_PREFIX)) {
            throw new SecretKeptForCredentialException(key);
        }
    }

    public Secret withUpdatedAt(Instant now) {
        return toBuilder().updatedAt(now).build();
    }

    /** The reference that an expression uses to resolve the value, in the environment where the expression runs. */
    public String path() {
        return "/gravitee/" + key + ":value";
    }
}
