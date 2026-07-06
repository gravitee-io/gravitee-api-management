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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.CustomLog;

/**
 * Serialises / deserialises the branded-sender configuration list to and from the single
 * {@code email.branded_senders} parameter value, keeping the storage-format concerns out of the
 * {@link Email} settings model so that it stays a plain data holder.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public final class BrandedSenders {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Maximum length of the serialised {@code email.branded_senders} JSON value. Mirrors the JDBC
     * {@code parameters.value} column (nvarchar(4000)); a longer value would be truncated on save.
     */
    static final int MAX_SERIALIZED_LENGTH = 4000;

    private BrandedSenders() {}

    /**
     * Deserialises the stored value. Never {@code null}: a malformed value (e.g. a hand-edited DB row
     * or a partial migration) is logged and treated as an empty list, so a corrupt parameter cannot
     * break the whole settings response. Invalid input is rejected loudly on the write path instead
     * (see {@link #write(List)}).
     */
    public static List<BrandedSenderConfig> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        try {
            var parsed = MAPPER.readValue(raw, new TypeReference<List<BrandedSenderConfig>>() {});
            // A stored JSON literal `null` deserialises to a Java null; coerce to empty to honour the
            // never-null contract.
            return parsed != null ? parsed : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.error("Ignoring malformed '{}' configuration [{}]; returning an empty list", Key.EMAIL_BRANDED_SENDERS.key(), raw, e);
            return new ArrayList<>();
        }
    }

    /**
     * Resolves the branded-sender configuration to apply for an outbound email by matching the recipient's
     * domain (the part after {@code '@'}, compared case-insensitively) against each configuration's domains,
     * returning the first match. Returns {@link Optional#empty()} when the stored value is blank/malformed,
     * the recipient has no parseable domain, or no configuration matches — in which case the caller keeps the
     * default {@code EMAIL_FROM} / {@code EMAIL_SUBJECT}. When more than one configuration lists the same
     * domain, the first is used (the console UI rejects cross-configuration duplicates, but a hand-edited
     * value could still contain them).
     */
    public static Optional<BrandedSenderConfig> resolve(String raw, String recipientEmail) {
        return match(parse(raw), recipientEmail);
    }

    /**
     * Matches an already-parsed configuration list against a recipient, so a caller sending to many recipients
     * can {@link #parse(String)} once and match per recipient rather than re-deserialising the value each time.
     * Same semantics as {@link #resolve(String, String)}.
     */
    public static Optional<BrandedSenderConfig> match(List<BrandedSenderConfig> configs, String recipientEmail) {
        final String domain = extractDomain(recipientEmail);
        if (domain == null) {
            return Optional.empty();
        }
        return configs
            .stream()
            .filter(config -> matchesDomain(config, domain))
            .findFirst();
    }

    private static boolean matchesDomain(BrandedSenderConfig config, String domain) {
        if (config == null || config.getDomains() == null) {
            return false;
        }
        return config.getDomains().stream().filter(Objects::nonNull).map(BrandedSenders::normalizeDomain).anyMatch(domain::equals);
    }

    /**
     * Extracts the lower-cased domain from a recipient address, accepting both a bare address
     * ({@code user@example.com}) and the personal-name form ({@code Name <user@example.com>}). Returns
     * {@code null} when no domain can be extracted, or when the string is a multi-address list
     * ({@code a@x.com, b@y.com}) — matching such a value on a single domain would brand every address for
     * one tenant, so it falls through to the default sender instead.
     */
    private static String extractDomain(String recipientEmail) {
        if (recipientEmail == null) {
            return null;
        }
        final String trimmed = recipientEmail.trim();
        // A single recipient carries exactly one '@'. Zero means there is no domain to match; more than one
        // means the value is really an address list — either the bare form ("a@x.com, b@y.com") or the
        // personal-name form ("Jane <jane@x.com>, John <john@y.com>"). Matching such a value on a single
        // domain would brand every address for one tenant, so it falls through to the default sender instead.
        // This count is taken on the whole value (before any bracket stripping) so a trailing "Name <addr>"
        // pair cannot hide the earlier addresses from the check.
        final int firstAt = trimmed.indexOf('@');
        if (firstAt < 0 || firstAt != trimmed.lastIndexOf('@')) {
            return null;
        }
        String address = trimmed;
        final int lt = address.lastIndexOf('<');
        final int gt = address.lastIndexOf('>');
        if (lt >= 0 && gt > lt) {
            address = address.substring(lt + 1, gt);
        }
        final int at = address.lastIndexOf('@');
        if (at < 0 || at == address.length() - 1) {
            return null;
        }
        return normalizeDomain(address.substring(at + 1));
    }

    /**
     * Serialises the configurations into the parameter value, guarding against exceeding
     * {@link #MAX_SERIALIZED_LENGTH}, the storage column limit.
     *
     * <p>Any {@code ';'} in the serialised JSON is replaced with its JSON unicode escape so the stored
     * value contains no literal {@code ';'}. The value is persisted as a single {@code ParameterService}
     * parameter whose read path splits on {@code ';'} (the parameter separator) and keeps only the first
     * fragment; since JSON structure never uses {@code ';'} (only {@code , : &#123; &#125; [ ] "}), every
     * {@code ';'} here is inside a free-text {@code from} / {@code subject}. Jackson decodes the escape
     * back to {@code ';'} transparently on read.</p>
     */
    static String write(List<BrandedSenderConfig> configs) {
        final List<BrandedSenderConfig> value = configs == null ? new ArrayList<>() : configs;
        value.forEach(BrandedSenders::validateEntry);
        value.forEach(BrandedSenders::normalizeDomains);
        try {
            final String json = MAPPER.writeValueAsString(value).replace(";", "\\u003b");
            if (json.length() > MAX_SERIALIZED_LENGTH) {
                throw new IllegalArgumentException(
                    "'" +
                        Key.EMAIL_BRANDED_SENDERS.key() +
                        "' configuration exceeds the maximum supported size of " +
                        MAX_SERIALIZED_LENGTH +
                        " characters"
                );
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialise '" + Key.EMAIL_BRANDED_SENDERS.key() + "' configuration", e);
        }
    }

    /**
     * Validates a single configuration entry before serialisation. Rejects a {@code null} entry (which
     * would otherwise persist as a JSON {@code null} and surface as a null element on read), and rejects
     * CR/LF in any header-bound field as defence-in-depth against email header injection (OWASP A05):
     * {@code from} and {@code subject} flow into email headers downstream, where a line break could
     * smuggle additional headers. Domains are checked too; none of these fields legitimately contains a
     * line break.
     */
    private static void validateEntry(BrandedSenderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("'" + Key.EMAIL_BRANDED_SENDERS.key() + "' must not contain null entries");
        }
        rejectLineBreak("from", config.getFrom());
        rejectLineBreak("subject", config.getSubject());
        if (config.getDomains() != null) {
            config.getDomains().forEach(domain -> rejectLineBreak("domain", domain));
        }
    }

    private static void rejectLineBreak(String field, String value) {
        if (value != null && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)) {
            throw new IllegalArgumentException("'" + Key.EMAIL_BRANDED_SENDERS.key() + "' " + field + " must not contain line breaks");
        }
    }

    /**
     * Canonicalises domains for storage: trims surrounding whitespace and lower-cases each entry
     * (RFC 1035 host names are case-insensitive), so send-time matching can compare directly. Null
     * entries are kept (not silently dropped) so the {@code @NotBlank} domain constraint rejects them
     * loudly, the same way a blank entry is handled.
     */
    private static void normalizeDomains(BrandedSenderConfig config) {
        if (config.getDomains() == null) {
            return;
        }
        config.setDomains(config.getDomains().stream().map(BrandedSenders::normalizeDomain).toList());
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) {
            return null;
        }
        return domain.trim().toLowerCase(Locale.ROOT);
    }
}
