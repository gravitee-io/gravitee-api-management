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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class IdentityCacheTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final String ACME = "https://apim.example.com/mcp/organizations/acme";

    private final IdentityCache<String> cache = new IdentityCache<>();

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_return_an_identity_until_its_token_expires() {
        at(NOW);
        cache.put(ACME, "token", "jane", NOW.plusSeconds(60));

        at(NOW.plusSeconds(59));
        assertThat(cache.get(ACME, "token")).contains("jane");

        at(NOW.plusSeconds(60));
        assertThat(cache.get(ACME, "token")).isEmpty();
    }

    @Test
    void should_keep_the_identities_of_two_resources_apart() {
        at(NOW);
        cache.put(ACME, "token", "jane", NOW.plusSeconds(60));

        assertThat(cache.get("https://apim.example.com/mcp/organizations/globex", "token")).isEmpty();
    }

    @Test
    void should_not_return_an_identity_for_another_token() {
        at(NOW);
        cache.put(ACME, "token", "jane", NOW.plusSeconds(60));

        assertThat(cache.get(ACME, "another-token")).isEmpty();
    }

    private static void at(Instant instant) {
        TimeProvider.overrideClock(Clock.fixed(instant, ZoneOffset.UTC));
    }
}
