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
package io.gravitee.gamma.rest.resources.credential.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialDtoTest {

    private static final Credential CREDENTIAL = Credential.builder()
        .id("c1")
        .name("OpenAI")
        .strategy(CredentialStrategy.BEARER)
        .ownerId("owner")
        .secretReferences(Map.of())
        .build();

    @Test
    void should_be_manageable_by_the_owner() {
        assertThat(CredentialDto.from(CREDENTIAL, actor("owner", false)).manageable()).isTrue();
    }

    @Test
    void should_not_be_manageable_by_another_user_who_is_not_an_admin() {
        assertThat(CredentialDto.from(CREDENTIAL, actor("someone-else", false)).manageable()).isFalse();
    }

    @Test
    void should_be_manageable_by_an_admin() {
        assertThat(CredentialDto.from(CREDENTIAL, actor("someone-else", true)).manageable()).isTrue();
    }

    private static CredentialActor actor(String userId, boolean admin) {
        var auditInfo = AuditInfo.builder().actor(AuditActor.builder().userId(userId).build()).build();
        return new CredentialActor(auditInfo, admin, Set.of());
    }
}
