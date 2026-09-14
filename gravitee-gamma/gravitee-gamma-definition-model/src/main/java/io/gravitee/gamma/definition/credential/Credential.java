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
package io.gravitee.gamma.definition.credential;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Wire schema for credential events (PUBLISH_CREDENTIAL / UNPUBLISH_CREDENTIAL).
 *
 * <p>Shared contract between the publisher (the AIM module) and consumers (gateway-services-sync).
 * Mirrors {@code AuthzPolicy}: Lombok POJO with Bean Validation annotations and {@link Serializable},
 * no business invariants. The module's own credential model is richer; server-side code converts
 * domain → wire when emitting events.
 *
 * <p>The secret travels encrypted. {@link #encryptedSecret} is ciphertext produced with
 * {@code api.properties.encryption.secret}, the key both the management API and the gateway
 * already hold, so the secret is never in clear text in the events table. The gateway keeps it
 * encrypted and decrypts only when a request asks for it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credential implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String environmentId;

    @JsonProperty
    @ToString.Exclude
    private String encryptedSecret;

    /** ISO-8601 timestamp string (e.g. "2024-01-01T00:00:00Z"). Kept as String to avoid coupling
     *  to a specific Jackson time module on the gateway side. */
    @JsonProperty
    private String updatedAt;
}
