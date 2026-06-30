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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.validator.NoDuplicates;
import io.gravitee.rest.api.validator.ValidSenderAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single branded-sender configuration: the {@code from} address and {@code subject} prefix to
 * apply when an outbound notification email targets one of {@code domains}.
 *
 * <p>Persisted as part of a JSON-serialised list under the {@code email.branded_senders} parameter
 * (see {@link Email#getBrandedSenders()}).</p>
 *
 * @author GraviteeSource Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrandedSenderConfig {

    /**
     * RFC 1035 host name: dot-separated labels of letters/digits/hyphens (no leading or trailing
     * hyphen, max 63 chars each), ending in an alphabetic TLD.
     */
    private static final String DOMAIN_PATTERN = "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";

    /** Recipient domains (e.g. {@code "graviteecustomer.com"}) this configuration applies to. */
    @NotEmpty
    @NoDuplicates(ignoreCase = true, message = "must not contain duplicate domains")
    private List<@NotBlank @Pattern(regexp = DOMAIN_PATTERN, message = "must be a valid domain name") String> domains;

    /** Branded {@code From} address used for emails sent to a matching recipient domain. */
    @NotBlank
    @ValidSenderAddress
    private String from;

    /** Subject prefix wrapper (e.g. {@code "[Gravitee Customer] %s"}) applied to matching emails. */
    @Size(max = 255, message = "must be at most 255 characters")
    private String subject;
}
