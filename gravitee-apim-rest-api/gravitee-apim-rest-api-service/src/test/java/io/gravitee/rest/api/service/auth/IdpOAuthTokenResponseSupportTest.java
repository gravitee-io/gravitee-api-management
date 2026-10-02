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
package io.gravitee.rest.api.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdpOAuthTokenResponseSupportTest {

    @Test
    void should_not_add_access_token_when_not_an_oauth_login() {
        assertThat(IdpOAuthTokenResponseSupport.accessTokenField(null, null, true)).isEmpty();
        assertThat(IdpOAuthTokenResponseSupport.accessTokenField(null, null, false)).isEmpty();
    }

    @Test
    void should_return_empty_access_token_when_hidden_for_oauth_login() {
        Optional<String> hidden = IdpOAuthTokenResponseSupport.accessTokenField("secret", "id", false);
        assertThat(hidden).contains("");

        Optional<String> hiddenWithIdTokenOnly = IdpOAuthTokenResponseSupport.accessTokenField(null, "id", false);
        assertThat(hiddenWithIdTokenOnly).contains("");
    }

    @Test
    void should_expose_access_token_independently_of_id_token() {
        assertThat(IdpOAuthTokenResponseSupport.accessTokenField("secret", null, true)).contains("secret");
        assertThat(IdpOAuthTokenResponseSupport.idTokenField(null, true)).isEmpty();
    }

    @Test
    void should_return_empty_id_token_when_hidden() {
        assertThat(IdpOAuthTokenResponseSupport.idTokenField("id-secret", false)).contains("");
    }

    @Test
    void should_expose_id_token_when_flag_is_on() {
        assertThat(IdpOAuthTokenResponseSupport.idTokenField("id-secret", true)).contains("id-secret");
    }
}
