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
package io.gravitee.gamma.module.platform.core.credential.use_case;

import static io.gravitee.gamma.module.platform.fixture.CredentialFixture.FOREIGN_USER_ID;
import static io.gravitee.gamma.module.platform.fixture.CredentialFixture.NEXT_YEAR;
import static io.gravitee.gamma.module.platform.fixture.CredentialFixture.OTHER_USER_ID;
import static io.gravitee.gamma.module.platform.fixture.CredentialFixture.OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import io.gravitee.gamma.module.platform.core.credential.exception.CredentialManagementNotAllowedException;
import io.gravitee.gamma.module.platform.core.credential.exception.CredentialNotFoundException;
import io.gravitee.gamma.module.platform.core.credential.exception.InvalidCredentialException;
import io.gravitee.gamma.module.platform.core.credential.exception.SecretPermissionRequiredException;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.gamma.module.platform.core.credential.model.SecretReference;
import io.gravitee.gamma.module.platform.core.credential.model.UpdateCredentialCommand;
import io.gravitee.gamma.module.platform.fixture.CredentialFixture;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateCredentialUseCaseTest extends AbstractUseCaseTest {

    private CredentialFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new RootFixture().credentialFixture;
    }

    private Credential update(
        CredentialActor actor,
        Credential credential,
        UnaryOperator<UpdateCredentialCommand.UpdateCredentialCommandBuilder> customizer
    ) {
        var unchanged = UpdateCredentialCommand.builder()
            .name(credential.name())
            .serviceId(credential.serviceId())
            .expiresAt(credential.expiresAt())
            .configuration(credential.configuration());
        return fixture.updateCredentialUseCase
            .execute(new UpdateCredentialUseCase.Input(actor, credential.id(), customizer.apply(unchanged).build()))
            .credential();
    }

    @Test
    void should_replace_the_details_and_keep_the_id_and_owner() {
        var credential = fixture.givenACredential();

        var updated = update(fixture.owner(), credential, c -> c.name("OpenAI staging").serviceId("openai-eu").expiresAt(NEXT_YEAR));

        assertThat(updated.id()).isEqualTo(credential.id());
        assertThat(updated.name()).isEqualTo("OpenAI staging");
        assertThat(updated.serviceId()).isEqualTo("openai-eu");
        assertThat(updated.expiresAt()).isEqualTo(NEXT_YEAR);
        assertThat(updated.ownerId()).isEqualTo(OWNER_ID);
        assertThat(updated.createdAt()).isEqualTo(credential.createdAt());
        assertThat(fixture.credentialRepository.storage()).containsExactly(updated);
    }

    @Test
    void should_change_the_references_of_a_credential_that_references_existing_secrets() {
        var credential = fixture.givenACredential();

        var updated = update(fixture.owner(), credential, c -> c.references(Map.of("token", "/vault/secret/openai:token-v2")));

        assertThat(updated.secretReferences()).containsOnly(Map.entry("token", new SecretReference("/vault/secret/openai:token-v2", null)));
    }

    @Test
    void should_require_the_secret_read_permission_to_point_at_a_secret_gravitee_stores() {
        var credential = fixture.givenACredential();
        var withoutRead = fixture.ownerWithSecretAccess(SecretAccess.CREATE, SecretAccess.UPDATE, SecretAccess.DELETE);

        assertThatThrownBy(() ->
            update(withoutRead, credential, c -> c.references(Map.of("token", "/gravitee/credential.other.token:value")))
        )
            .isInstanceOf(SecretPermissionRequiredException.class)
            .hasMessageContaining("read");
        assertThat(fixture.credentialRepository.storage()).containsExactly(credential);
    }

    @Test
    void should_not_need_the_secret_read_permission_when_the_references_do_not_change() {
        var credential = fixture.givenACredential(c -> c.references(Map.of("token", "/gravitee/my-secret:value")));

        var updated = update(fixture.ownerWithSecretAccess(), credential, c -> c.name("Renamed"));

        assertThat(updated.name()).isEqualTo("Renamed");
    }

    @Test
    void should_not_let_the_references_of_a_credential_gravitee_stores_change() {
        var credential = fixture.givenAManagedCredential();

        assertThatThrownBy(() -> update(fixture.owner(), credential, c -> c.references(Map.of("token", "/vault/elsewhere"))))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("Rotate");
    }

    @Test
    void should_accept_the_references_of_a_credential_gravitee_stores_sent_back_unchanged() {
        var credential = fixture.givenAManagedCredential();

        var updated = update(fixture.owner(), credential, c -> c.name("Renamed").references(referencesOf(credential)));

        assertThat(updated.name()).isEqualTo("Renamed");
        assertThat(updated.secretReferences()).isEqualTo(credential.secretReferences());
    }

    @Test
    void should_not_need_the_secret_read_permission_to_send_unchanged_references_back() {
        var credential = fixture.givenACredential(c -> c.references(Map.of("token", "/gravitee/my-secret:value")));

        var updated = update(fixture.ownerWithSecretAccess(), credential, c -> c.name("Renamed").references(referencesOf(credential)));

        assertThat(updated.name()).isEqualTo("Renamed");
    }

    private static Map<String, String> referencesOf(Credential credential) {
        var references = new LinkedHashMap<String, String>();
        credential.secretReferences().forEach((field, reference) -> references.put(field, reference.reference()));
        return references;
    }

    @Test
    void should_hand_the_credential_to_another_user_of_the_organization() {
        var credential = fixture.givenACredential();

        var updated = update(fixture.owner(), credential, c -> c.ownerId(OTHER_USER_ID));

        assertThat(updated.ownerId()).isEqualTo(OTHER_USER_ID);
    }

    @Test
    void should_reject_an_owner_from_another_organization() {
        var credential = fixture.givenACredential();

        assertThatThrownBy(() -> update(fixture.owner(), credential, c -> c.ownerId(FOREIGN_USER_ID))).isInstanceOf(
            InvalidCredentialException.class
        );
    }

    @Test
    void should_let_an_admin_update_a_credential_they_do_not_own() {
        var credential = fixture.givenACredential();

        var updated = update(fixture.admin(), credential, c -> c.name("Renamed by admin"));

        assertThat(updated.name()).isEqualTo("Renamed by admin");
    }

    @Test
    void should_not_let_another_user_update_the_credential() {
        var credential = fixture.givenACredential();

        assertThatThrownBy(() -> update(fixture.otherUser(), credential, c -> c.name("Hijacked"))).isInstanceOf(
            CredentialManagementNotAllowedException.class
        );
        assertThat(fixture.credentialRepository.storage()).containsExactly(credential);
    }

    @Test
    void should_keep_an_expiration_that_has_passed_since_it_was_set() {
        var credential = fixture.givenACredential(c -> c.expiresAt(NEXT_YEAR));
        fixture.credentialRepository.update(credential.toBuilder().expiresAt(Instant.parse("2023-01-01T00:00:00Z")).build());

        var updated = update(fixture.owner(), credential, c -> c.name("Renamed").expiresAt(Instant.parse("2023-01-01T00:00:00Z")));

        assertThat(updated.expiresAt()).isEqualTo(Instant.parse("2023-01-01T00:00:00Z"));
    }

    @Test
    void should_reject_a_new_expiration_in_the_past() {
        var credential = fixture.givenACredential();

        assertThatThrownBy(() -> update(fixture.owner(), credential, c -> c.expiresAt(Instant.parse("2023-01-01T00:00:00Z")))).isInstanceOf(
            InvalidCredentialException.class
        );
    }

    @ParameterizedTest
    @CsvSource({ "org-id,env-2", "other-org,env-id" })
    void should_not_find_a_credential_of_another_organization_or_environment(String organizationId, String environmentId) {
        var credential = fixture.givenACredential();

        assertThatThrownBy(() ->
            update(CredentialFixture.adminIn(organizationId, environmentId), credential, c -> c.name("Renamed"))
        ).isInstanceOf(CredentialNotFoundException.class);
        assertThat(fixture.credentialRepository.storage()).containsExactly(credential);
    }

    @Test
    void should_fail_when_the_credential_does_not_exist() {
        var credential = fixture.givenACredential();
        var missing = credential.toBuilder().id("missing").build();

        assertThatThrownBy(() -> update(fixture.owner(), missing, c -> c)).isInstanceOf(CredentialNotFoundException.class);
    }

    @Test
    void should_audit_the_update() {
        var credential = fixture.givenACredential();
        fixture.auditCrudService.reset();

        update(fixture.owner(), credential, c -> c.name("Renamed"));

        assertThat(fixture.auditCrudService.storage())
            .singleElement()
            .satisfies(audit -> {
                assertThat(audit.getEvent()).isEqualTo("CREDENTIAL_UPDATED");
                assertThat(audit.getProperties()).containsEntry("CREDENTIAL", credential.id());
                assertThat(audit.getPatch()).contains("Renamed");
            });
    }
}
