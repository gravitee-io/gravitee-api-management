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
package io.gravitee.gamma.authorization.core.am.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.authorization.core.am.model.AmUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pins down which inputs reproduce a real AM-issued token {@code sub}, using a live default-IdP user
 * ({@code demo@gravitee.io}). The sub is {@code UUID.nameUUIDFromBytes("<idpId>:<externalId>")} — the
 * IdP *id*, not the IdP *name* (which is what listUsers returns in {@code source}), and the user's
 * {@code externalId}, not its {@code id}.
 */
class ComputeSubParameterTest {

    private static final String USER_ID = "5846a39d-062d-4803-86a3-9d062d980384";
    private static final String EXTERNAL_ID = "7dfafc00-a4aa-4ac5-bafc-00a4aa5ac58c";
    private static final String SOURCE_ID = "default-idp-3e149b8b-f2b8-4d36-949b-8bf2b8ed362e";
    private static final String SOURCE_NAME = "Default Identity Provider";
    private static final String EXPECTED_SUB = "d5854721-cfee-3172-b040-94a319d11c36";

    private static String hash(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static AmUser user(String source, String externalId) {
        return new AmUser(USER_ID, source, externalId, null, "demo@gravitee.io", null, true, List.of(), List.of());
    }

    @Test
    void idpId_colon_externalId_reproduces_the_real_sub() {
        assertThat(hash(SOURCE_ID + ":" + EXTERNAL_ID)).isEqualTo(EXPECTED_SUB);
        assertThat(SyncAmUsersUseCase.computeSub(user(SOURCE_ID, EXTERNAL_ID))).isEqualTo(EXPECTED_SUB);
    }

    @Test
    void source_name_does_not_match() {
        // The bug: hashing the IdP name (what listUsers puts in `source`) instead of the IdP id.
        assertThat(SyncAmUsersUseCase.computeSub(user(SOURCE_NAME, EXTERNAL_ID))).isNotEqualTo(EXPECTED_SUB);
    }

    @Test
    void hashing_the_user_id_instead_of_externalId_does_not_match() {
        assertThat(SyncAmUsersUseCase.computeSub(user(SOURCE_ID, USER_ID))).isNotEqualTo(EXPECTED_SUB);
    }

    @Test
    void null_externalId_does_not_match() {
        // computeSub falls back to the raw user id when externalId is null.
        assertThat(SyncAmUsersUseCase.computeSub(user(SOURCE_ID, null))).isNotEqualTo(EXPECTED_SUB);
    }
}
