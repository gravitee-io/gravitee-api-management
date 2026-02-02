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

package io.gravitee.rest.api.model.clientcertificate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;

/**
 * DTO for creating a client certificate.
 */
@Schema(name = "CreateClientCertificate", description = "DTO for creating a client certificate in PEM format")
public record CreateClientCertificate(
    @NotNull @Size(max = 255) @Schema(description = "Name of this client certificate.") String name,
    @Schema(
        description = "Date when this certificate will become available. If null it will be deployed along with current or future subscription."
    )
    Date startsAt,
    @Schema(
        description = "Date when this certificate will be removed from existing subscriptions. If null it will stay until the subscription is removed."
    )
    Date endsAt,
    @NotNull @Schema(description = "The certificate in PEM format.") String certificate
) {}
