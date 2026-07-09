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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Contract of the shared single-address parsing used by both the save-time {@code SenderAddressValidator}
 * and the send-time branded-sender matching, so the two never disagree on what is a single, sendable address.
 *
 * @author GraviteeSource Team
 */
class EmailAddressesTest {

    @Test
    void should_return_a_bare_single_address() {
        assertThat(EmailAddresses.singleAddress("noreply@example.com")).contains("noreply@example.com");
    }

    @Test
    void should_unwrap_a_personal_name_address() {
        assertThat(EmailAddresses.singleAddress("Jane Developer <jane@example.com>")).contains("jane@example.com");
    }

    @Test
    void should_unwrap_a_bare_bracketed_address() {
        assertThat(EmailAddresses.singleAddress("<jane@example.com>")).contains("jane@example.com");
    }

    @Test
    void should_trim_surrounding_whitespace() {
        assertThat(EmailAddresses.singleAddress("  Jane <jane@example.com>  ")).contains("jane@example.com");
    }

    @Test
    void should_be_empty_for_null() {
        assertThat(EmailAddresses.singleAddress(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "   ", "not-an-email", "alice@example.com, bob@example.org", "Jane <jane@example.com>, John <john@example.org>" }
    )
    void should_be_empty_for_inputs_without_a_single_resolvable_address(String input) {
        // Blank, no '@', and both multi-address forms (bare and personal-name) all resolve to no single
        // sendable address. Cases that pin a specific documented decision are kept as named tests below.
        assertThat(EmailAddresses.singleAddress(input)).isEmpty();
    }

    @Test
    void should_be_empty_when_an_address_precedes_the_personal_name() {
        // The greedy display-name strip used to keep only the bracketed address and accept this; the '@' count
        // on the whole value must catch the earlier address first.
        assertThat(EmailAddresses.singleAddress("alice@example.com, Team <noreply@example.org>")).isEmpty();
    }

    @Test
    void should_reject_a_quoted_display_name_containing_an_at_sign() {
        // "Support @ Acme" <a@x.com> is technically sendable, but the whole-value '@' count rejects it. This is
        // a deliberate, safe narrowing (reject at save rather than accept-then-fail at send); pin the choice.
        assertThat(EmailAddresses.singleAddress("\"Support @ Acme\" <a@x.com>")).isEmpty();
    }

    @Test
    void should_be_empty_when_the_at_is_outside_the_brackets() {
        // "jane@work <Jane>" unwraps to display-name-only content with no '@'; the helper must not return a
        // domain-less fragment.
        assertThat(EmailAddresses.singleAddress("jane@work <Jane>")).isEmpty();
    }

    @Test
    void should_not_unwrap_trailing_content_after_the_bracket() {
        // A wrapper is only unwrapped when the value actually ends with '>'; trailing junk leaves the value
        // whole (and a downstream format check then rejects it).
        assertThat(EmailAddresses.singleAddress("Team <a@x.com> more")).contains("Team <a@x.com> more");
    }
}
