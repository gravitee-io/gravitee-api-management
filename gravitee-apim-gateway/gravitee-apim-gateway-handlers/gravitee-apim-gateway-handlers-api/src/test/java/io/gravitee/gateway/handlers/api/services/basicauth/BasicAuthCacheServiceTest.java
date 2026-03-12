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
package io.gravitee.gateway.handlers.api.services.basicauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BasicAuthCacheServiceTest {

    private BasicAuthCacheService basicAuthCacheService;

    @BeforeEach
    void beforeEach() {
        basicAuthCacheService = new BasicAuthCacheService();
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register_active_credential() {
            BasicAuthCredential credential = buildCredential("api-1", "user-1", false, null);

            basicAuthCacheService.register(credential);

            Optional<BasicAuthCredential> result = basicAuthCacheService.getByApiAndUsername("api-1", "user-1");
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(credential);
        }

        @Test
        void should_not_register_revoked_credential() {
            BasicAuthCredential credential = buildCredential("api-1", "user-1", true, null);

            basicAuthCacheService.register(credential);

            Optional<BasicAuthCredential> result = basicAuthCacheService.getByApiAndUsername("api-1", "user-1");
            assertThat(result).isEmpty();
        }

        @Test
        void should_not_register_expired_credential() {
            Calendar past = Calendar.getInstance();
            past.add(Calendar.DAY_OF_YEAR, -1);
            BasicAuthCredential credential = buildCredential("api-1", "user-1", false, past.getTime());

            basicAuthCacheService.register(credential);

            Optional<BasicAuthCredential> result = basicAuthCacheService.getByApiAndUsername("api-1", "user-1");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister_credential() {
            BasicAuthCredential credential = buildCredential("api-1", "user-1", false, null);
            basicAuthCacheService.register(credential);

            basicAuthCacheService.unregister(credential);

            Optional<BasicAuthCredential> result = basicAuthCacheService.getByApiAndUsername("api-1", "user-1");
            assertThat(result).isEmpty();
        }

        @Test
        void should_unregister_by_api_id() {
            BasicAuthCredential credential1 = buildCredential("api-1", "user-1", false, null);
            BasicAuthCredential credential2 = buildCredential("api-1", "user-2", false, null);
            basicAuthCacheService.register(credential1);
            basicAuthCacheService.register(credential2);

            basicAuthCacheService.unregisterByApiId("api-1");

            assertThat(basicAuthCacheService.getByApiAndUsername("api-1", "user-1")).isEmpty();
            assertThat(basicAuthCacheService.getByApiAndUsername("api-1", "user-2")).isEmpty();
        }
    }

    @Nested
    class GetTest {

        @Test
        void should_return_empty_for_unknown_username() {
            Optional<BasicAuthCredential> result = basicAuthCacheService.getByApiAndUsername("api-1", "unknown-user");

            assertThat(result).isEmpty();
        }
    }

    private BasicAuthCredential buildCredential(String api, String username, boolean revoked, Date expireAt) {
        return BasicAuthCredential.builder()
            .id("cred-" + username)
            .api(api)
            .username(username)
            .password("password")
            .subscription("sub-1")
            .plan("plan-1")
            .application("app-1")
            .expireAt(expireAt)
            .revoked(revoked)
            .build();
    }
}
