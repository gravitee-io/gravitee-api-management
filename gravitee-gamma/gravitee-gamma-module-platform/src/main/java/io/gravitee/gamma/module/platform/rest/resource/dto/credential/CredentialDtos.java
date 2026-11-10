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
package io.gravitee.gamma.module.platform.rest.resource.dto.credential;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/** Request/response DTOs for the credential endpoints. No response carries a secret value. */
public final class CredentialDtos {

    private CredentialDtos() {}

    /** Give {@code references} to existing secrets, or {@code values} for Gravitee to store as secrets. */
    public record CreateCredentialRequest(
        @NotBlank String name,
        @NotNull CredentialStrategy strategy,
        String serviceId,
        String ownerId,
        ZonedDateTime expiresAt,
        Map<String, Object> configuration,
        Map<String, String> references,
        Map<String, String> values
    ) {
        @Override
        public String toString() {
            return (
                "CreateCredentialRequest[name=" +
                name +
                ", strategy=" +
                strategy +
                ", values=" +
                (values == null ? null : values.keySet()) +
                "]"
            );
        }
    }

    /** {@code ownerId} and {@code references} {@code null} keep the current ones. */
    public record UpdateCredentialRequest(
        @NotBlank String name,
        String serviceId,
        String ownerId,
        ZonedDateTime expiresAt,
        Map<String, Object> configuration,
        Map<String, String> references
    ) {}

    public record RotateCredentialRequest(@NotNull Map<String, String> values) {
        @Override
        public String toString() {
            return "RotateCredentialRequest[fields=" + (values == null ? null : values.keySet()) + "]";
        }
    }

    /** @param managed whether Gravitee stores the value behind the reference */
    public record SecretReferenceResponse(String reference, boolean managed) {}

    /** The host mapper writes dates as epoch seconds, so each timestamp asks for an ISO 8601 string instead. */
    public record CredentialResponse(
        String id,
        String name,
        CredentialStrategy strategy,
        String serviceId,
        String ownerId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime expiresAt,
        Map<String, Object> configuration,
        Map<String, SecretReferenceResponse> secretReferences,
        boolean managed,
        @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime updatedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) ZonedDateTime rotatedAt
    ) {}

    public record CredentialsResponse(List<CredentialResponse> data, Pagination pagination) {
        public record Pagination(long totalCount, int page, int perPage, int pageCount, int pageItemsCount) {}
    }

    public record CredentialConsumerResponse(String module, String type, String id, String name) {}

    public record CredentialConsumersResponse(List<CredentialConsumerResponse> data) {}
}
