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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialTest {

    private static final String OWNER_ID = "owner-id";
    private static final String OTHER_USER_ID = "other-user-id";

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

    private static CredentialActor actor(String userId, boolean admin, Set<CredentialActor.SecretAccess> secretAccess) {
        var auditInfo = AuditInfo.builder().actor(AuditActor.builder().userId(userId).build()).build();
        return new CredentialActor(auditInfo, admin, secretAccess);
    }
}
