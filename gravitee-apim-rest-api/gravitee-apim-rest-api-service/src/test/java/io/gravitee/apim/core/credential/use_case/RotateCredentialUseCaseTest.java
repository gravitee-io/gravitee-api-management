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
package io.gravitee.apim.core.credential.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.credential.exception.CredentialManagementNotAllowedException;
import io.gravitee.apim.core.credential.exception.CredentialNotFoundException;
import io.gravitee.apim.core.credential.exception.CredentialRotationIncompleteException;
import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.credential.exception.SecretPermissionRequiredException;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.secret.model.SecretValue;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RotateCredentialUseCaseTest extends AbstractUseCaseTest {

    private CredentialFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new CredentialFixture();
    }

    private Credential rotate(CredentialActor actor, Credential credential, Map<String, String> values) {
        return fixture.rotateCredentialUseCase.execute(new RotateCredentialUseCase.Input(actor, credential.id(), values)).credential();
    }

    private Credential givenABasicCredential() {
        return fixture.givenAManagedCredential(c ->
            c.strategy(CredentialStrategy.BASIC).values(Map.of("username", "bob", "password", "initial"))
        );
    }

    @Test
    void should_write_the_new_values_and_keep_the_id_and_references() {
        var credential = givenABasicCredential();

        var rotated = rotate(fixture.owner(), credential, Map.of("username", "alice", "password", "rotated"));

        assertThat(rotated.id()).isEqualTo(credential.id());
        assertThat(rotated.secretReferences()).isEqualTo(credential.secretReferences());
        assertThat(rotated.rotatedAt()).isEqualTo(INSTANT_NOW);
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".username")).contains("alice");
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("rotated");
        assertThat(fixture.credentialCrudService.storage()).containsExactly(rotated);
    }

    @Test
    void should_rotate_only_the_fields_given() {
        var credential = givenABasicCredential();

        rotate(fixture.owner(), credential, Map.of("password", "rotated"));

        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".username")).contains("bob");
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("rotated");
    }

    @Test
    void should_reject_a_field_the_strategy_does_not_have() {
        var credential = givenABasicCredential();

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("token", "x"))).isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void should_reject_no_values() {
        var credential = givenABasicCredential();

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of())).isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void should_reject_a_blank_value() {
        var credential = givenABasicCredential();

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("password", " "))).isInstanceOf(
            InvalidCredentialException.class
        );
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("initial");
    }

    @Test
    void should_reject_a_value_larger_than_a_secret_holds_before_writing_any_field() {
        var credential = givenABasicCredential();

        assertThatThrownBy(() ->
            rotate(fixture.owner(), credential, Map.of("username", "alice", "password", "p".repeat(SecretValue.MAX_BYTES + 1)))
        )
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("password");
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".username")).contains("bob");
    }

    @Test
    void should_count_the_size_of_a_value_in_utf_8_bytes() {
        var credential = givenABasicCredential();
        var twoBytesEach = "é".repeat(SecretValue.MAX_BYTES / 2 + 1);

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("password", twoBytesEach))).isInstanceOf(
            InvalidCredentialException.class
        );
    }

    @Test
    void should_accept_a_value_of_exactly_the_size_a_secret_holds() {
        var credential = givenABasicCredential();

        rotate(fixture.owner(), credential, Map.of("password", "p".repeat(SecretValue.MAX_BYTES)));

        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).hasValueSatisfying(v ->
            assertThat(v).hasSize(SecretValue.MAX_BYTES)
        );
    }

    @ParameterizedTest
    @CsvSource({ "org-id,env-2", "other-org,env-id" })
    void should_not_find_a_credential_of_another_organization_or_environment(String organizationId, String environmentId) {
        var credential = givenABasicCredential();

        assertThatThrownBy(() ->
            rotate(CredentialFixture.adminIn(organizationId, environmentId), credential, Map.of("password", "rotated"))
        ).isInstanceOf(CredentialNotFoundException.class);
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("initial");
    }

    @Test
    void should_not_rotate_a_credential_that_references_existing_secrets() {
        var credential = fixture.givenACredential();

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("token", "x")))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("references");
    }

    @Test
    void should_require_the_secret_update_permission() {
        var credential = givenABasicCredential();

        assertThatThrownBy(() ->
            rotate(fixture.ownerWithSecretAccess(SecretAccess.CREATE, SecretAccess.DELETE), credential, Map.of("password", "rotated"))
        ).isInstanceOf(SecretPermissionRequiredException.class);
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("initial");
    }

    @Test
    void should_not_let_another_user_rotate_the_credential() {
        var credential = givenABasicCredential();

        assertThatThrownBy(() -> rotate(fixture.otherUser(), credential, Map.of("password", "rotated"))).isInstanceOf(
            CredentialManagementNotAllowedException.class
        );
    }

    @Test
    void should_let_an_admin_rotate_a_credential_they_do_not_own() {
        var credential = givenABasicCredential();

        rotate(fixture.admin(), credential, Map.of("password", "rotated"));

        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("rotated");
    }

    @Test
    void should_rethrow_the_failure_when_no_field_was_rotated() {
        var credential = givenABasicCredential();
        fixture.secretStore.failOn("username");

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("username", "alice", "password", "rotated"))).isInstanceOf(
            IllegalStateException.class
        );
        assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
    }

    @Test
    void should_put_back_the_values_already_rotated_when_a_later_field_fails() {
        var credential = givenABasicCredential();
        fixture.secretStore.failOn("password");

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("username", "alice", "password", "rotated"))).isInstanceOf(
            IllegalStateException.class
        );
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".username")).contains("bob");
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("initial");
        assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
    }

    @Test
    void should_put_back_the_rotated_values_when_the_credential_cannot_be_saved() {
        var credential = givenABasicCredential();
        fixture.credentialCrudService.failOnUpdate();

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("username", "alice", "password", "rotated"))).isInstanceOf(
            IllegalStateException.class
        );
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".username")).contains("bob");
        assertThat(fixture.secretStore.valueOf("credential." + credential.id() + ".password")).contains("initial");
    }

    @Test
    void should_name_the_fields_left_rotated_when_an_old_value_cannot_be_put_back() {
        var credential = givenABasicCredential();
        fixture.secretStore.failOn("password");
        fixture.secretStore.failUpdateOf("username", 1);

        assertThatThrownBy(() -> rotate(fixture.owner(), credential, Map.of("username", "alice", "password", "n3w-pa55")))
            .isInstanceOf(CredentialRotationIncompleteException.class)
            .hasMessageContaining("username")
            .hasMessageNotContaining("alice")
            .hasMessageNotContaining("n3w-pa55")
            .hasRootCauseMessage("secret store unavailable for credential." + credential.id() + ".password");
    }

    @Test
    void should_audit_the_rotation_without_the_values() {
        var credential = givenABasicCredential();
        fixture.auditCrudService.reset();

        rotate(fixture.owner(), credential, Map.of("password", "rotated-value"));

        var audits = fixture.auditCrudService.storage();
        assertThat(audits).extracting(AuditEntity::getEvent).containsExactly("SECRET_UPDATED", "CREDENTIAL_ROTATED");
        assertThat(audits.getLast().getProperties()).containsEntry("CREDENTIAL", credential.id());
        assertThat(audits).allSatisfy(audit -> assertThat(audit.getPatch()).doesNotContain("rotated-value"));
    }
}
