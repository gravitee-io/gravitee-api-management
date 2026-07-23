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
package io.gravitee.rest.api.model.email;

import java.util.Optional;

/**
 * Shared parsing of a single sender/recipient email address for the send-time branded-sender matching
 * ({@code BrandedSenders}): it unwraps a {@code Name <addr>} personal name and rejects multi-address lists,
 * so matching never keys off a value delivery cannot send from. Save-time validation
 * ({@code SenderAddressValidator}) enforces the same "single, sendable address" contract, but does so
 * directly against the {@code jakarta.mail} parser the SMTP send-path uses rather than through this helper.
 *
 * @author GraviteeSource Team
 */
public final class EmailAddresses {

    private EmailAddresses() {}

    /**
     * Extracts the single address from a raw value that may carry a personal-name wrapper
     * ({@code Name <addr>}). Returns {@link Optional#empty()} when the value is {@code null}/blank, has no
     * {@code '@'}, or is really a multi-address list — either the bare form ({@code a@x.com, b@y.com}) or the
     * comma-joined personal-name form ({@code Jane <jane@x.com>, John <john@y.com>}).
     *
     * <p>The single-address check counts {@code '@'} on the whole value <em>before</em> any bracket
     * stripping, so a trailing {@code Name <addr>} pair cannot hide earlier addresses from the check. A
     * consequence is that a quoted display name which itself contains {@code '@'} (e.g.
     * {@code "Support @ Acme" <a@x.com>}) is intentionally rejected even though it is technically sendable:
     * the narrowing errs on the safe side (reject at save rather than accept-then-fail at send) and such
     * values are vanishingly rare.</p>
     *
     * <p>The {@code Name <addr>} wrapper is only unwrapped when the value actually ends with {@code '>'}, so
     * trailing content after the bracket ({@code Name <a@x.com> more}) is not silently unwrapped. The returned
     * value always contains a {@code '@'}, but is not guaranteed to be a <em>well-formed</em> address — callers
     * must still validate it (that a domain is present and the format is legal).</p>
     */
    public static Optional<String> singleAddress(String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String trimmed = value.trim();
        // A single address carries exactly one '@'. Zero means there is no address; more than one means the
        // value is really an address list, which delivery cannot send from and branding must not match.
        final int firstAt = trimmed.indexOf('@');
        if (firstAt < 0 || firstAt != trimmed.lastIndexOf('@')) {
            return Optional.empty();
        }
        String address = trimmed;
        if (address.endsWith(">")) {
            final int lt = address.lastIndexOf('<');
            if (lt >= 0) {
                address = address.substring(lt + 1, address.length() - 1).trim();
            }
        }
        // When the single '@' sat outside the brackets (e.g. "jane@work <Jane>"), unwrapping leaves
        // display-name-only content with no '@'. Honour the "contains a local@domain" contract by returning
        // empty here rather than a domain-less fragment a trusting caller might use.
        return address.indexOf('@') < 0 ? Optional.empty() : Optional.of(address);
    }
}
