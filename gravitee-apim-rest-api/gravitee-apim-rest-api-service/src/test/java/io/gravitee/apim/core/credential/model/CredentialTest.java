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
package io.gravitee.apim.core.credential.model;

import static io.gravitee.apim.core.credential.use_case.CredentialFixture.OTHER_USER_ID;
import static io.gravitee.apim.core.credential.use_case.CredentialFixture.OWNER_ID;
import static io.gravitee.apim.core.credential.use_case.CredentialFixture.actor;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialTest {

    private final Credential credential = Credential.builder().id("c1").ownerId(OWNER_ID).build();

    @Test
    void should_be_manageable_by_its_owner() {
        assertThat(credential.isManageableBy(actor(OWNER_ID, false, Set.of()))).isTrue();
    }

    @Test
    void should_be_manageable_by_an_admin() {
        assertThat(credential.isManageableBy(actor(OTHER_USER_ID, true, Set.of()))).isTrue();
    }

    @Test
    void should_not_be_manageable_by_another_user() {
        assertThat(credential.isManageableBy(actor(OTHER_USER_ID, false, Set.of()))).isFalse();
    }
}
